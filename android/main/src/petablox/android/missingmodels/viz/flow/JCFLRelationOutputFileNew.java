package petablox.android.missingmodels.viz.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import petablox.android.missingmodels.util.FileManager.FileType;
import petablox.android.missingmodels.util.FileManager.StampOutputFile;
import petablox.android.missingmodels.util.jcflsolver.Edge;
import petablox.android.missingmodels.util.jcflsolver.Graph;

public class JCFLRelationOutputFileNew implements StampOutputFile {
	private final String relationName;
	private final Graph g;
	private final TupleConverter tupleConverter;
	
	public static abstract class TupleConverter {
		public abstract int[] convert(Edge edge);
		
		public List<int[]> convert(Collection<Edge> edges) {
			List<int[]> res = new ArrayList<int[]>();
			for(Edge edge : edges) {
				res.add(this.convert(edge));
			}
			return res;
		}
	}
	
	public JCFLRelationOutputFileNew(String relationName, Graph g, TupleConverter tupleConverter) {
		this.relationName = relationName;
		this.g = g;
		this.tupleConverter = tupleConverter;
	}
	
	@Override
	public String getName() {
		return "relations/" + this.relationName + ".rel";
	}

	@Override
	public FileType getType() {
		return FileType.OUTPUT;
	}

	@Override
	public String getContent() {
		StringBuilder sb = new StringBuilder();
		
		Collection<Edge> edges = this.g.getEdges(this.g.symbolToKind(this.relationName));
		List<int[]> res = this.tupleConverter.convert(edges);
		
		Collections.sort(res, new Comparator<int[]>() {
			@Override
			public int compare(int[] arg0, int[] arg1) {
				if(arg0.length != arg1.length) {
					throw new RuntimeException("Error in comparator argument lengths: " + arg0.length + " vs. " + arg1.length + "!");
				}
				for(int i=0; i<arg0.length; i++) {
					if(arg0[i] < arg1[i]) {
						return -1;
					} else if(arg0[i] > arg1[i]) {
						return 1;
					}
				}
				return 0;
			}			
		});
		
		for(int[] tuple : res) {
			for(int i=0; i<tuple.length; i++) {
				sb.append(tuple[i] + ",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
