package stamp.missingmodels.util.jcflsolver;

import java.util.*;

public class EdgesSetCustom extends EdgesCustom
{
	private Edge[] table = new Edge[INITIAL_TABLE_SIZE];
	private int size = 0;

	private static final double MAX_LOAD_FACTOR = 0.9;
	private static final int INITIAL_TABLE_SIZE = 16;

	EdgesSetCustom(boolean useNextAField)
	{
		super(useNextAField);
	}

	public Iterator<Edge> iterator()
	{
		return new SetIterator();
	}

	public Edge addEdge(Edge edge)
	{
		Edge e = get(edge);
		if(e != null) {
			return e;
		}
		size++;
		int numBuckets = table.length;
		double loadFactor = (double) size / numBuckets;
		if (loadFactor > MAX_LOAD_FACTOR) {
			expandCapacity();
			numBuckets = table.length;
		}
		int index = edge.to.id % numBuckets;//& (numBuckets - 1);
		setNext(edge, table[index]);
		table[index] = edge;
		return null;
	}
	
	private Edge get(Edge edge) {
		int toNodeId = edge.to.id;
		int label = edge.label();
		int numBuckets = table.length;
		int index = toNodeId % numBuckets;//& (numBuckets - 1);
		Edge e = table[index];
		while(e != null){
			if(e.to.id == toNodeId && e.label() == label){
				//if(!e.equals(edge)){
				//	assert false : edge.toString() + " " + e.toString();
				//}
				return e;
			}
			e = getNext(e);
		}
		return null;		
	}

	private boolean contains(Edge edge)
	{
		int toNodeId = edge.to.id;
		int label = edge.label();
		int numBuckets = table.length;
		int index = toNodeId % numBuckets;//& (numBuckets - 1);
		Edge e = table[index];
		while(e != null){
			if(e.to.id == toNodeId && e.label() == label){
				//if(!e.equals(edge)){
				//	assert false : edge.toString() + " " + e.toString();
				//}
				return true;
			}
			e = getNext(e);
		}
		return false;
	}

	private void expandCapacity() 
	{
		Edge[] oldTable = this.table;
		int oldNumBuckets = oldTable.length;
		int newNumBuckets = oldNumBuckets << 1;
		Edge[] newTable = new Edge[newNumBuckets];

		for(int i = 0; i < oldNumBuckets; i++){
			Edge e = oldTable[i];
			while(e != null){
				int index = e.to.id % newNumBuckets;//& (newNumBuckets - 1);
				Edge tmp = getNext(e);
				setNext(e, newTable[index]);
				newTable[index] = e;
				e = tmp;
			}
		}

		this.table = newTable;
	}

	private class SetIterator implements Iterator<Edge>  
	{
		private Edge current;
		private int index = 0;

		public SetIterator() {
			current = table[0];
			while(current == null){
				index++;
				if(index >= table.length)
					break;
				current = table[index];
			}
		}

		public boolean hasNext() {
			return current != null;
		}

		public Edge next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			Edge toReturn = current;
			current = getNext(current);
			while(current == null){
				index++;
				if(index >= table.length)
					break;
				current = table[index];
			}
			return toReturn;
		}

		public void remove() {
			throw new RuntimeException("unimplemented");
		}
	}

	int count()
	{
		int count = 0;
		for(int i = 0; i < table.length; i++){
			Set<Integer> ids = new HashSet();
			Edge edge = table[i];
			while(edge != null){
				if(edge instanceof LabeledEdge){
					int nodeId = edge.to.id;
					int label = ((LabeledEdge) edge).label;
					if(!ids.contains(nodeId)){
						ids.add(nodeId);
					} else
						count++;
				}
				edge = getNext(edge);
			}
		}
		return count;
	}
}