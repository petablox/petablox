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
        
        HashSet<String> domNames = new HashSet<String>();

        HashMap<String, RelSign> consumedRels = new HashMap<String, RelSign>(),
                                 producedRels = new HashMap<String, RelSign>();
		try {
			HashMap<String, RelSign> rels = new HashMap<String, RelSign>();
			HashSet<String> outputRels = new HashSet<String>();
			in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			
			String line;
			while (null != (line = in.readLine())) {
				line = line.trim();
				if (line.startsWith(".symbol_type")) {
					domNames.add(line.substring(13));
				} else if (line.startsWith(".output")) {
					outputRels.add(line.substring(8, line.indexOf('(')));
				} else if (line.startsWith(".decl")) {
					String name = line.substring(5, line.indexOf('('));
					RelSign r = getRelSign(line.substring(line.indexOf('(') + 1, line.length() - 1));
					rels.put(name, r);
				}
			}
			
			for (String s : rels.keySet()) {
				if (outputRels.contains(s)) {
					producedRels.put(s, rels.get(s));
				} else {
					consumedRels.put(s, rels.get(s));
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
		String[] fields = substring.split(",");
		for (String s : fields) {
			s = s.trim();
			if (s.isEmpty()) continue;
			
			String domain = s.substring(s.indexOf(':') + 1);
			if (index.containsKey(domain)) {
				index.put(domain, index.get(domain) + 1);
			} else {
				index.put(domain, 0);
			}
			
			l.add(domain + index.get(domain));
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

}
