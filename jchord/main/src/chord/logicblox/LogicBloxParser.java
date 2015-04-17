package chord.logicblox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chord.bddbddb.RelSign;
import chord.core.DatalogMetadata;
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
public class LogicBloxParser {
    private static final Pattern metaCommentPattern = 
        Pattern.compile("^\\s*//\\s*:(inputs|outputs|domains|name):\\s*(.+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern relationSignaturePattern =
        Pattern.compile("([a-zA-Z_:]+)\\(([^\\)]+)\\)");

    public LogicBloxParser() {
    }
    

    @SuppressWarnings("resource") // closed by Utils.close
    public DatalogMetadata parseMetadata(File file) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            
            DatalogMetadata metadata = new DatalogMetadata();
            metadata.setFileName(file.getName());
            
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
            RelSign sign = parseRelationSignature(sigData);
            if (signMap.containsKey(relName)) {
                Messages.warn("%s has multiple signatures, replacing %s with %s.", relName, signMap.get(relName), sign);
            }
            signMap.put(relName, sign);
        }
    }
    
    /**
     * Parses the domain names out of a signature list, assigning minor numbers in order 
     * of occurrence of a particular name starting from 0.
     * <p>
     * For a list should be of the form:<br />
     * <tt>A, B, A</tt><br />
     * Will return a signature with:<br />
     * <tt>A0, B0, A1</tt>
     * <p>
     * The returned signature will not have a domain order (which is a BDD-specific concept).
     * 
     * @param signature
     * @return
     */
    private RelSign parseRelationSignature(String signature) {
        HashMap<String, Integer> numMap = new HashMap<String, Integer>();
        
        String[] sigParts = signature.split(",");
        String[] domNames = new String[sigParts.length];
        for (int i = 0; i < sigParts.length; ++i) {
            String domain = sigParts[i].trim();
            Integer num = numMap.get(domain);
            if (num == null)
                num = 0;
            domNames[i] = domain + num;
            numMap.put(domain, num + 1);
        }
        
        return new RelSign(domNames, /* LB has no domain order concept */ null);
    }

}
