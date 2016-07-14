package stamp.missingmodels.util.jcflsolver;

import java.util.ArrayList;
import java.util.List;

public class BucketHeap {
	private List<Edge> buckets = new ArrayList<Edge>();
	private int minBucket = 0;
	private int size = 0;
	
	public void ensure(int index) {
		for(int i=this.buckets.size(); i<=index; i++) {
			this.buckets.add(null);
		}
	}

	public void push(Edge t) {
		this.ensure(t.weight);
		Edge head = this.buckets.get(t.weight);
		if(head != null) {
			t.nextWorklist = head;
			head.prevWorklist = t;
		} else {
			t.nextWorklist = null;
		}
		t.prevWorklist = null;
		this.buckets.set(t.weight, t);
		this.size++;
	}

	public Edge pop() {
		Edge head = this.buckets.get(this.minBucket);
		while(head == null) {
			this.minBucket++;
			if(this.minBucket >= this.buckets.size()) {
				return null;
			}
			head = this.buckets.get(this.minBucket);
		}
		this.buckets.set(this.minBucket, head.nextWorklist);
		this.size--;
		return head;
	}

	public void update(Edge edge, Edge newEdge) {
		if(newEdge.weight < edge.weight) {
			if(edge.prevWorklist != null) {
				edge.prevWorklist.nextWorklist = edge.nextWorklist;
			} else {
				this.buckets.set(edge.weight, edge.nextWorklist);
			}
			if(edge.nextWorklist != null) {
				edge.nextWorklist.prevWorklist = edge.prevWorklist;
			}
			edge.weight = newEdge.weight;
			edge.firstInput = newEdge.firstInput;
			edge.secondInput = newEdge.secondInput;
			this.size--;
			push(edge);
		}
	}

	public int size() {
		return this.size;
	}
}

