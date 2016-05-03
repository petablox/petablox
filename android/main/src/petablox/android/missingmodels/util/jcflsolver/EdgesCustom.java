package petablox.android.missingmodels.util.jcflsolver;

abstract class EdgesCustom implements Edges
{
	private final Next next;

	EdgesCustom(boolean useNextA)
	{
		if(useNextA)
			this.next = new NextA();
		else
			this.next = new NextB();
	}
	
	protected final Edge getNext(Edge e)
	{
		return next.getNext(e);
	}

	protected final void setNext(Edge edge, Edge nextEdge)
	{
		next.setNext(edge, nextEdge);
	}

	interface Next
	{
		public Edge getNext(Edge e);
		public void setNext(Edge e, Edge next);
	}

	private class NextA implements Next
	{
		public Edge getNext(Edge e){ return e.nextA; }
		public void setNext(Edge e, Edge next){ e.nextA = next; }
	}

	private class NextB implements Next
	{
		public Edge getNext(Edge e){ return e.nextB; }
		public void setNext(Edge e, Edge next){ e.nextB = next; }
	}

}
