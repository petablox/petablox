package petablox.souffle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import petablox.bddbddb.RelSign;
import petablox.core.DatalogMetadata;
import petablox.core.IDatalogParser;
import petablox.project.PetabloxException;
import petablox.util.Utils;

public class SouffleParser implements IDatalogParser {
	
	/* 
	 * (non-Javadoc)
	 * @see petablox.core.IDatalogParser#parseMetadata(java.io.File)
	 * 
	 * In this implementation we only look for things of the form .decl and .output.
	 */
	@Override
	public DatalogMetadata parseMetadata(File file) throws IOException {
		BufferedReader in = null;
		
		DatalogMetadata metadata = new DatalogMetadata();
        metadata.setFileName(file.getAbsolutePath());
        // TODO: may want to actually put a comment with this in the file
        metadata.setDlogName(getFileName(file).replaceAll("\\.|_", "-") + "og");
        
        HashSet<String> domNames = new HashSet<String>();

        HashMap<String, RelSign> consumedRels = new HashMap<String, RelSign>(),
                                 producedRels = new HashMap<String, RelSign>();
		try {
			HashMap<String, String> rels = new HashMap<String, String>();
			HashSet<String> outputRels = new HashSet<String>();
			HashSet<String> inputRels = new HashSet<String>();
			in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			
			String line;
			while (null != (line = in.readLine())) {
				line = line.trim();
				if (line.startsWith(".number_type")) { 
					// We don't consider symbol types because BDDBDDB's intermediate output is numbers.
					domNames.add(line.substring(13));
				} else if (line.startsWith(".type")) {
					domNames.add(line.substring(6));
				} else if (line.startsWith(".output")) {
					outputRels.add(line.substring(8, line.indexOf('(')));	
				} else if (line.startsWith(".input")) {
					inputRels.add(line.substring(7, line.indexOf('(')));
				} else if (line.startsWith(".decl")) {
					String name = line.substring(6, line.indexOf('('));
					if (!name.endsWith("Map")) {
						// Special case here: Map should only be used for 
						// mapping from bddbddb indexes to strings
						rels.put(name, line.substring(line.indexOf('(') + 1));
					}
				} else if (line.startsWith("// name=")) {
					metadata.setDlogName(line.substring(8));
				}
			}
			
			for (String s : rels.keySet()) {
				if (outputRels.contains(s)) {
					producedRels.put(s, getRelSign(rels.get(s)));
				} else if (inputRels.contains(s)) {
					consumedRels.put(s, getRelSign(rels.get(s)));
				}
			}
		} catch (UnsupportedEncodingException e) {
			// should never happen, given Java spec
			throw new PetabloxException("UTF-8 not supported", e);
		} finally {
			in.close();
		}

		metadata.setMajorDomNames(domNames);
		metadata.setConsumedRels(consumedRels);
		metadata.setProducedRels(producedRels);
		return metadata;
	}

	private RelSign getRelSign(String substring) {
		Map<String, Integer> index = new HashMap<String, Integer>(); 
		List<String> l = new LinkedList<String>();
		String domain_names = substring.substring(substring.indexOf("//") + 2);
		String[] fields = domain_names.split(",");
		boolean hasMinors = areMinorsSpecified(fields);
		for (String s : fields) {
			s = s.trim();
			if (s.isEmpty()) continue;
			
			if (!hasMinors) {
				if (index.containsKey(s)) {
					index.put(s, index.get(s) + 1);
				} else {
					index.put(s, 0);
				}
			}
			
			if (!hasMinors) {
				l.add(s + index.get(s));
			} else {
				l.add(s);
			}
		}
		String[] toReturn = new String[l.size()];
		int i = 0;
		for (String s : l) {
			toReturn[i] = s;
			i++;
		}
		// Souffle has no concept of var order, so we just make one up
        String varOrder = Utils.join(Arrays.asList(toReturn), "x"); 
		
		return new RelSign(toReturn, varOrder);
	}

	/**
     * Checks whether all or no minor numbers are specified.  If they 
     * are only partially specified an exception is thrown.
     * 
     * @param domains the domain specs to check
     * @return <code>true</code> if some minor is specified or <code>false</code> is none are
     */
    private boolean areMinorsSpecified(String[] domains) {
        boolean hasMinors = false;
        for (int i = 0; i < domains.length; ++i) {
            String sigPart = domains[i];
            hasMinors = hasMinors || Character.isDigit(sigPart.charAt(sigPart.length() - 1));
        }
        return hasMinors;
    }
	
    private String getFileName(File f) {
    		String s = f.getName();
    		int slash = s.indexOf('/');
    		if (slash < 0) return s;
    		else return s.substring(s.indexOf("/"));
    }
}
