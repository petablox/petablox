package petablox.android.missingmodels.util.jcflsolver;

import java.util.*;

public interface Edges extends Iterable<Edge>
{
	public static final Edges EMPTY = new Edges() {
			private final Iterator<Edge> it = new Iterator(){
					public boolean hasNext(){ return false; }
					public Edge next(){ throw new RuntimeException(); }
					public void remove(){ throw new RuntimeException(); }
				};
			
			public Iterator<Edge> iterator() { 
				return it;
			}
			
			public Edge addEdge(Edge edge) {
				throw new RuntimeException();
			}
		};

	public abstract Edge addEdge(Edge edge);
	
}
