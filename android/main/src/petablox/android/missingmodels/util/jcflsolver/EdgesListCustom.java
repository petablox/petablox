package petablox.android.missingmodels.util.jcflsolver;

import java.util.*;

public class EdgesListCustom extends EdgesCustom 
{
	private Edge head;

	EdgesListCustom(boolean useNextAField)
	{
		super(useNextAField);
	}

	public Iterator<Edge> iterator()
	{
		return new ListIterator();
	}

	public Edge addEdge(Edge edge)
	{
		setNext(edge, head);
		head = edge;
		return null;
	}

	private class ListIterator implements Iterator<Edge>  
	{
		private Edge current;

		public ListIterator() {
			current = head;
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
			return toReturn;
		}

		public void remove() {
			throw new RuntimeException("unimplemented");
		}
	}
}
