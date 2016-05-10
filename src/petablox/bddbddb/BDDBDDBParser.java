package petablox.bddbddb;

import gnu.trove.list.array.TIntArrayList;
import petablox.core.DatalogMetadata;
import petablox.core.IDatalogParser;
import petablox.project.Messages;
import petablox.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for <a href="http://bddbddb.sourceforge.net/"><tt>bddbddb</tt></a>-style Datalog files.  
 * 
 * Most of this code is adapted from the original <tt>DlogAnalysis</tt> 
 * implementation.
 */
public class BDDBDDBParser implements IDatalogParser {
    
    /**
     * Temporary state during a call to {@link BDDBDDBParser#parseMetadata(File)}.
     */
    private static class ParserState {
        DatalogMetadata meta = new DatalogMetadata();

        Set<String> majorDomNames = new HashSet<String>();
        Map<String, RelSign> consumedRels = new HashMap<String, RelSign>();
        Map<String, RelSign> producedRels = new HashMap<String, RelSign>();
        List<String> minorDomNames = new ArrayList<String>();

        String order;
        String fileName;
        int lineNum;
        
        ParserState(File file) {
            fileName = file.getPath();
            meta.setFileName(fileName);
        }

        void error(String errMsg) {
            Messages.log("ERROR: DlogAnalysis: " + fileName + ": line " + lineNum + ": " + errMsg);
            meta.setHasNoErrors(false);
        }
        
        /**
         * Sets all the temporary state onto the metadata and returns it.
         * @return
         */
        DatalogMetadata finish() {
            meta.setConsumedRels(consumedRels);
            meta.setProducedRels(producedRels);
            meta.setMajorDomNames(majorDomNames);
            meta.setMinorDomNames(minorDomNames);
            meta.setBddOrder(order);
            return meta;
        }
    }

    public BDDBDDBParser() {
    }
    
    /**
     * Parses the Datalog analysis in the specified file.
     * 
     * @param    fileName A file containing a Datalog analysis.
     * 
     * @return    true iff the Datalog analysis parses successfully.
     */
    @SuppressWarnings("resource") // closed by Utils.close in finally block
    public DatalogMetadata parseMetadata(File file) throws IOException {
        ParserState state = new ParserState(file);
        
        Set<String> majorDomNames = state.majorDomNames;
        Map<String, RelSign> consumedRels = state.consumedRels;
        Map<String, RelSign> producedRels = state.producedRels;
        List<String> minorDomNames = state.minorDomNames;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            
            Pattern p = Pattern.compile("(\\w)+\\((\\w)+:(\\w)+(,(\\w)+:(\\w)+)*\\)(printtuples)*((input)|(output))(printtuples)*");
            for (state.lineNum = 1; true; state.lineNum++) {
                String s;
                s = in.readLine();
                if (s == null)
                    break;
                if (s.startsWith("#")) {
                    if (s.startsWith("# name=")) {
                        if (state.meta.getDlogName() == null)
                            state.meta.setDlogName(s.trim().substring(7));
                        else
                            state.error("Name redeclared via # name=...");
                    }
                    continue;
                }
                int k = s.indexOf('#');
                if (k != -1) s = s.substring(0, k);
                s = s.trim();
                if (s.length() == 0)
                     continue;
                // strip all whitespaces from line
                StringBuffer t = new StringBuffer(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (!Character.isWhitespace(c))
                        t.append(c);
                }
                s = t.toString();
                if (s.startsWith(".bddvarorder")) {
                    if (state.order != null) {
                        state.error(".bddvarorder redefined.");
                        continue;
                    }
                    state.order = s.substring(12);
                    String[] a = state.order.split("_|x");
                    for (String minorDomName : a) {
                        if (minorDomNames.contains(minorDomName)) {
                            state.error("Domain name '" + minorDomName + "' occurs multiple times in .bddvarorder; " +
                                "considering first occurrence.");
                        } else {
                            minorDomNames.add(minorDomName);
                            String majorDomName = Utils.trimNumSuffix(minorDomName);
                            majorDomNames.add(majorDomName);
                        }
                    }
                    continue;
                }
                Matcher m = p.matcher(s);
                if (!m.matches())
                    continue;
                if (state.order == null) {
                    state.error(".bddvarorder not defined before first relation declared");
                    return state.finish();
                }
                int i = s.indexOf('(');
                String relName = s.substring(0, i);
                if (consumedRels.containsKey(relName)) {
                    state.error("Relation '" + relName + "' redeclared");
                    continue;
                }
                if (producedRels.containsKey(relName)) {
                    state.error("Relation '" + relName + "' redeclared");
                    continue;
                }
                s = s.substring(i + 1);
                boolean done = false;
                boolean ignore = false;
                List<String> relMinorDomNames = new ArrayList<String>();
                List<String> relMajorDomNames = new ArrayList<String>();
                TIntArrayList indices = new TIntArrayList();
                while (!done) {
                    i = s.indexOf(':');
                    assert (i != -1);
                    s = s.substring(i + 1);
                    i = s.indexOf(',');
                    if (i == -1) {
                        i = s.indexOf(')');
                        assert (i != -1);
                        done = true;
                    }
                    String domName = s.substring(0, i);
                    String minorDomName;
                    String majorDomName;
                    int index;
                    if (!Character.isDigit(domName.charAt(i - 1))) {
                        majorDomName = domName;
                        index = 0;
                        int num = indices.size();
                        while (true) {
                            int j = 0;
                            for (String majorDomName2 : relMajorDomNames) {
                                if (majorDomName2.equals(majorDomName) &&
                                        indices.get(j) == index) {
                                    index++;
                                    break;
                                }
                                j++;
                            }
                            if (j == num)
                                break;
                        }
                        minorDomName = majorDomName + Integer.toString(index);
                    } else {
                        minorDomName = domName;
                        int j = i - 1;
                        while (true) {
                            char c = domName.charAt(j);
                            if (Character.isDigit(c))
                                j--;
                            else
                                break;
                        }
                        majorDomName = domName.substring(0, j + 1);
                        index = Integer.parseInt(domName.substring(j + 1, i));
                    }
                    if (relMinorDomNames.contains(minorDomName)) {
                        state.error("Domain name '" + minorDomName + "' occurs multiple times in declaration of " +
                            "relation '" + relName + "'");
                        ignore = true;
                    } else if (!minorDomNames.contains(minorDomName)) {
                        state.error("Domain name '" + domName + "' in declaration of relation '" + relName +
                            "' does not occur in .bddvarorder");
                        ignore = true;
                    } else {
                        relMinorDomNames.add(minorDomName);
                        relMajorDomNames.add(majorDomName);
                        indices.add(index);
                    }
                    s = s.substring(i + 1);
                }
                if (ignore)
                    continue;
                int numDoms = relMinorDomNames.size();
                String[] domNames = new String[numDoms];
                String domOrder = getSubOrder(relMinorDomNames, state);
                for (int j = 0; j < numDoms; j++)
                    domNames[j] = relMinorDomNames.get(j);
                Map<String, RelSign> map = null;
                if (s.indexOf("input") >= 0)
                    map = consumedRels;
                else if (s.indexOf("output") >= 0)
                    map = producedRels;
                else
                    assert false; 
                RelSign relSign;
                try {
                    relSign = new RelSign(domNames, domOrder);
                } catch (RuntimeException ex) {
                    state.error(ex.getMessage());
                    continue;
                }
                map.put(relName, relSign);
            }
            
            return state.finish();

        } catch (IOException ex) {
            Messages.log(ex.getMessage());
            throw ex;
        } finally {
            Utils.close(in);
        }
    }
    
    private String getSubOrder(List<String> relMinorDomNames, ParserState state) {
        String order = state.order;
        int orderLen = order.length();
        String subOrder = null;
        char lastSep = ' ';
        int i = 0;
        for (String domName : state.minorDomNames) {
            i += domName.length();
            if (relMinorDomNames.contains(domName)) {
                if (subOrder == null)
                    subOrder = domName;
                else
                    subOrder = subOrder + lastSep + domName;
                if (i != orderLen)
                    lastSep = order.charAt(i);
            } else {
                if (i != orderLen && order.charAt(i) == '_')
                    lastSep = '_';
            }
            i++;
        }
        return subOrder;
    }
}
