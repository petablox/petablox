package chord.logicblox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chord.bddbddb.RelSign;
import chord.core.DatalogMetadata;
import chord.core.IDatalogParser;
import chord.project.ChordException;
import chord.project.Messages;
import chord.util.Utils;

/**
 * A simple parser for LogiQL files.
 * 
 * <p>
 * This does not implement the full LogiQL language.  
 * We support specially formatted comments that hold metadata 
 * which would be contained in the BDD versions of these files.
 * <p>
 * Domains are a comma separated list of names, for example:<br/>
 * <tt>// :domains: A,B,C</tt>
 * <p>
 * Inputs and outputs are a comma separated list of signatures, each of 
 * the form <i>Rel</i>(<i>Dom</i><sub>1</sub>, ..., <i>Dom</i><sub>n</sub>), 
 * for example:<br />
 * <tt> // :inputs: Foo(A,B), Bar(C)</tt><br />
 * <tt> // :outputs: Baz(A,B,C)</tt>
 * <p>
 * The task name is given by:<br />
 * <tt> // :name: taskname</tt>
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxParser implements IDatalogParser {
    private static final Pattern metaCommentPattern = 
        Pattern.compile("^\\s*//\\s*:(inputs|outputs|domains|name):\\s*(.+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern relationSignaturePattern =
        Pattern.compile("([a-zA-Z][^!=:\\-\\s<>(),]+)\\(([^\\)]+)\\)");
    
    // for error messages
    private File currentFile;
    private String currentRelation;

    public LogicBloxParser() {
    }
    

    @SuppressWarnings("resource") // closed by Utils.close
    public DatalogMetadata parseMetadata(File file) throws IOException {
        BufferedReader in = null;
        try {
            currentFile = file;
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            
            DatalogMetadata metadata = new DatalogMetadata();
            metadata.setFileName(file.getAbsolutePath());
            
            HashSet<String> domNames = new HashSet<String>();

            HashMap<String, RelSign> consumedRels = new HashMap<String, RelSign>(),
                                     producedRels = new HashMap<String, RelSign>();
            
            
            String line;
            // FIXME this is ok for //-style comments but rule definitions can span lines or be multiple per line
            while (null != (line = in.readLine())) {
                Matcher metaMatcher = metaCommentPattern.matcher(line);
                if (!metaMatcher.matches())
                    continue;
                String type = metaMatcher.group(1).toLowerCase(Locale.US),
                       data = metaMatcher.group(2).trim();
                if ("inputs".equals(type)) {
                    addRelSigns(consumedRels, data);
                } else if ("outputs".equals(type)) {
                    addRelSigns(producedRels, data);
                } else if ("domains".equals(type)) {
                    for (String domName: data.split(","))
                        domNames.add(domName.trim());
                } else if ("name".equals(type)) {
                    if (metadata.getDlogName() != null)
                        throw new ChordException("Got duplicate name entry in: " + file.getAbsolutePath());
                    metadata.setDlogName(data);
                } else {
                    throw new ChordException("Unrecognized metadata type: " + type);
                }
            }
            
            metadata.setMajorDomNames(domNames);
            metadata.setConsumedRels(consumedRels);
            metadata.setProducedRels(producedRels);
            
            return metadata;
            
        } catch (UnsupportedEncodingException e) {
            // by standard, utf-8 is always supported
            throw new ChordException("UTF-8 not supported?", e);
        } finally {
            currentRelation = null;
            currentFile = null;
            Utils.close(in);
        }
    }
    
    /**
     * Adds the relation signatures corresponding to an <tt>inputs</tt> or 
     * <tt>outputs</tt> declaration.
     * 
     * @param signMap the signature map to populate
     * @param data    the signature list in string format
     */
    private void addRelSigns(Map<String, RelSign> signMap, String data) {
        Matcher sigMatcher = relationSignaturePattern.matcher(data);
        while (sigMatcher.find()) {
            String relName = sigMatcher.group(1),
                    sigData = sigMatcher.group(2);
            currentRelation = relName;
            RelSign sign = parseRelationSignature(sigData);
            if (signMap.containsKey(relName)) {
                Messages.warn("%s has multiple signatures, replacing %s with %s.", relName, signMap.get(relName), sign);
            }
            signMap.put(relName, sign);
        }
    }
    
    /**
     * Parses the domain names out of a signature list.
     *   
     * <p>Unless manually specified, minor numbers are assigned in order 
     * of occurrence of a particular name starting from 0.
     * <p>
     * For a list should be of the form:<br />
     * <tt>A, B, A</tt><br />
     * Will return a signature with:<br />
     * <tt>A0, B0, A1</tt>
     * <p>
     * If any minor number is specified for a given relation, then all minor numbers must 
     * be specified.
     * <p>
     * The returned signature will have an arbitrary domain order (which is a BDD-specific concept).
     * 
     * @param signature the signature to parse
     * @return the relation signature
     */
    private RelSign parseRelationSignature(String signature) {
        String[] sigParts = signature.split(",");
        for (int i = 0; i < sigParts.length; ++i)
            sigParts[i] = sigParts[i].trim();
        
        String[] domNames;
        if (areMinorsSpecified(sigParts)) {
            domNames = sigParts;
        } else {
            HashMap<String, Integer> numMap = new HashMap<String, Integer>();
            domNames = new String[sigParts.length];
            for (int i = 0; i < sigParts.length; ++i) {
                String domain = sigParts[i];
                Integer num = numMap.get(domain);
                if (num == null)
                    num = 0;
                domNames[i] = domain + num;
                numMap.put(domain, num + 1);
            }
        }
        
        // LB has no concept of var order, so we just make one up
        String varOrder = Utils.join(Arrays.asList(domNames), "x"); 
        return new RelSign(domNames, varOrder);
    }
    
    /**
     * Checks whether all or no minor numbers are specified.  If they 
     * are only partially specified an exception is thrown.
     * 
     * @param domains the domain specs to check
     * @return <code>true</code> if all minors are specified or <code>false</code> is none are
     * @throws ChordException if minors are only partially specified
     */
    private boolean areMinorsSpecified(String[] domains) {
        String first = domains[0];
        boolean firstHasMinors = Character.isDigit(first.charAt(first.length() - 1));
        for (int i = 1; i < domains.length; ++i) {
            String sigPart = domains[i];
            boolean hasMinor = Character.isDigit(sigPart.charAt(sigPart.length() - 1));
            if (hasMinor != firstHasMinors) {
               throw new ChordException(String.format(
                   "Minor domains only partially specified for relation %s in %s", 
                   currentRelation, currentFile
               ));
            }
        }
        return firstHasMinors;
    }

}
