package stamp.missingmodels.util.viz.jcflsolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import stamp.missingmodels.util.FileManager.FileType;
import stamp.missingmodels.util.FileManager.StampInputFile;
import stamp.missingmodels.util.jcflsolver.EdgeData;

public class JCFLRelationInputFile implements StampInputFile<Set<EdgeData>> {
	private final String symbol;
	private final FileType fileType;
	private final short defaultWeight;

	public JCFLRelationInputFile(FileType fileType, String symbol) {
		this(fileType, symbol, (short)0);
	}

	public JCFLRelationInputFile(FileType fileType, String symbol, short defaultWeight) {
		this.symbol = symbol;
		this.fileType = fileType;
		this.defaultWeight = defaultWeight;
	}

	@Override
	public String getName() {
		return this.symbol + ".dat";
	}

	@Override
	public FileType getType() {
		return this.fileType;
	}

	@Override
	public Set<EdgeData> getObject(BufferedReader br) throws IOException {
		Set<EdgeData> edges = new HashSet<EdgeData>();
		String line;		
		while((line = br.readLine()) != null) {
			String[] tokens = line.split(" ");
			if(tokens.length < 3) {
				continue;
			}
			
			String fromName = tokens[0];
			String toName = tokens[1];
			String index = tokens[2];

			short weight = tokens.length >= 4 ? Short.parseShort(tokens[3]) : this.defaultWeight;
			EdgeData e;
			if(index.equals("*")) {
				e = new EdgeData(fromName, toName, this.symbol, weight);
			} else {
				e = new EdgeData(fromName, toName, this.symbol, weight, index);
			}
			edges.add(e);
		}
		return edges;
	}
}
