package stamp.missingmodels.util.viz.jcflsolver;

import java.util.HashSet;
import java.util.Set;

import stamp.missingmodels.util.FileManager.FileType;
import stamp.missingmodels.util.FileManager.StampOutputFile;
import stamp.missingmodels.util.Util.Pair;
import stamp.missingmodels.util.jcflsolver.Edge;
import stamp.missingmodels.util.jcflsolver.EdgeData;
import stamp.missingmodels.util.jcflsolver.Graph;
import stamp.missingmodels.util.jcflsolver.LabeledEdge;
import stamp.missingmodels.util.jcflsolver.Node;

public class JCFLRelationOutputFile implements StampOutputFile {
	private final Graph g;
	private final String symbol;
	private final FileType fileType;
	private final boolean printPaths;
	private final Set<String> terminals = new HashSet<String>();

	public JCFLRelationOutputFile(FileType fileType, Graph g, String symbol) {
		this(fileType, g, symbol, false, null); 
	}

	public JCFLRelationOutputFile(FileType fileType, Graph g, String symbol, boolean printPaths) {
		this(fileType, g, symbol, printPaths, null);
	}

	public JCFLRelationOutputFile(FileType fileType, Graph g, String symbol, boolean printPaths, Set<String> terminals) {
		this.g = g;
		this.symbol = symbol;
		this.fileType = fileType;
		this.printPaths = printPaths;
		if(terminals != null) {
			this.terminals.addAll(terminals);
		}
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
	public String getContent() {
		int kind;
		try {
			kind = this.g.symbolToKind(this.symbol);
		} catch(RuntimeException e) {
			return "";
		}

		System.out.println("Writing " + this.getName());

		StringBuilder sb = new StringBuilder();
		for(Node node : this.g.allNodes()) {
			String fromName = node.getName();
			for(Edge edge : node.getOutEdges(kind)){
				String path = "";
				if(this.printPaths) {
					StringBuilder sbPath = new StringBuilder();
					sbPath.append(" ");
					for(Pair<Edge,Boolean> pair : g.getPath(edge, terminals)) {
						EdgeData e = pair.getX().getData(g);
						sbPath.append(e.symbol + (pair.getY() ? "" : "Bar") + "," + e.from + "," + e.to + (e.hasLabel() ? "," + e.label : "") + ";");
					}
					path = sbPath.toString();
				}

				if(edge instanceof LabeledEdge) {
					sb.append(fromName + " " + edge.to.getName() + " " + ((LabeledEdge) edge).label + " " + edge.weight + path);
				} else {
					sb.append(fromName + " " + edge.to.getName() + " * " + edge.weight + path);
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}
}
