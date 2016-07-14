package stamp.reporting.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import stamp.reporting.processor.Insertion.KeySpanStart;
import stamp.reporting.processor.Insertion.SpanEnd;
import stamp.reporting.processor.Insertion.SrcSinkSpanStart;

public class TaintedVariableRecord {
	private final int startPos;
	private final int endPos;
	private final Set<String> sources;
	private final Set<String> sinks;

	public int getStart() { return startPos; }
	public int getEnd() { return endPos; }

	public TaintedVariableRecord(int startPos, int endPos) {
		//System.out.println("Created TaintedVariableRecord " + startPos + " " + endPos);
		this.startPos = startPos;
		this.endPos = endPos;
		this.sources = new HashSet<String>();
		this.sinks = new HashSet<String>();
	}

	public void addSource(String s) {
		//System.out.println("Added source " + s + " to " + startPos + " " + endPos);
		sources.add(s);
	}

	public void addSink(String s) {
		//System.out.println("Added sink " + s + " to " + startPos + " " + endPos);
		sinks.add(s);
	}

	public void makeInsertions(List<Insertion> insertions) {
		//System.out.println("Inserted TaintedVariableRecord " + startPos + " " + endPos);
		insertions.add(new SrcSinkSpanStart(startPos, sources, sinks));
		insertions.add(new KeySpanStart(startPos, "taintedVariable"));
		insertions.add(new SpanEnd(endPos));
		insertions.add(new SpanEnd(endPos));
	}

}
