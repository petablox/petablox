package petablox.android.missingmodels.viz.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import petablox.android.missingmodels.util.ConversionUtils;
import petablox.android.missingmodels.util.StubLookup;
import petablox.android.missingmodels.util.StubLookup.StubLookupKey;
import petablox.android.missingmodels.util.StubLookup.StubLookupValue;
import petablox.android.missingmodels.util.Util.Pair;
import petablox.android.missingmodels.util.jcflsolver.Edge;
import petablox.android.missingmodels.util.jcflsolver.EdgeData;
import petablox.android.missingmodels.util.jcflsolver.Graph;
import petablox.android.missingmodels.viz.html.HTMLObject;
import petablox.android.missingmodels.viz.html.HTMLObject.DivObject;
import petablox.android.srcmap.sourceinfo.SourceInfo;

public abstract class FlowObject extends DivObject {	
	private int index;
	private Graph g;
	private StubLookup s;
	private List<Pair<Edge,Boolean>> path;
	private boolean subpath;
	private SourceInfo sourceInfo;

	public FlowObject(List<Pair<Edge,Boolean>> path, Graph g, StubLookup s, SourceInfo sourceInfo) {
		this(path, g, s, sourceInfo, false);
	}

	public FlowObject(List<Pair<Edge,Boolean>> path, Graph g, StubLookup s, SourceInfo sourceInfo, boolean subpath) {
		this.path = path;
		this.g = g;
		this.s = s;
		this.sourceInfo = sourceInfo;

		if(this.path.size() == 0) return;

		this.index = -this.countUnmatched();
		this.subpath = subpath;

		if(!this.subpath) {
			this.index--;
			this.putStyle("overflow", "scroll");
			this.putStyle("height", "720px");
			this.putStyle("width", "100%");
		}

		this.addObject(this.process());
	}

	public abstract boolean isStart(int i);
	public abstract boolean isEnd(int i);
	public abstract HTMLObject getAlternateObject(int startIndex, int endIndex);
	public abstract String getMainLabel();
	public abstract String getAlternateLabel();

	public boolean isSubpath() {
		return this.subpath;
	}

	public int getSize() {
		return this.path.size();
	}

	public Pair<Edge,Boolean> getEdge(int i) {
		return this.path.get(i);
	}

	public Graph getGraph() {
		return this.g;
	}
	
	public StubLookup getStubLookup() {
		return this.s;
	}
	
	public SourceInfo getSourceInfo() {
		return this.sourceInfo;
	}

	private int countUnmatched() {
		int pre = 0;
		int counter = 0;
		for(int i=0; i<this.path.size(); i++) {
			if(this.isStart(i)) {
				counter++;
			} else if(this.isEnd(i)) {
				if(counter == 0) {
					pre++;
				} else {
					counter--;
				}
			}
		}
		return pre;
	}

	public HTMLObject process() {
		return this.process(true);
	}

	public boolean useAlternate(int startIndex, int endIndex) {
		return this.isStart(startIndex) && this.isEnd(endIndex);
	}

	public String parseEdge(EdgeData edge, boolean forward) {
		StringBuilder sb = new StringBuilder();
		sb.append(edge.toString(forward));
	
		StubLookupValue info = this.s.get(new StubLookupKey(edge.symbol, edge.from, edge.to));
		if(info == null) {
			return sb.toString();
		}
	
		String sourceFileName = sourceInfo.filePath(info.method.getDeclaringClass());
		int methodLineNum = sourceInfo.methodLineNum(info.method);
	
		// TODO: make this print more than just the method name
		String methStr = " <a onclick=\"showSource('" + sourceFileName + "','false','" + methodLineNum + "')\">" + "[" + info.method.getName() + "]</a>";
		sb.append(methStr);
	
		return sb.toString();
	}

	public HTMLObject process(boolean start) {

		DivObject d = new DivObject();
		d.putStyle("margin-left", "10px");

		while(this.index < path.size()) {
			if(this.index < 0 || (isStart(index) && !start)) {
				// start a new div

				int startIndex = this.index;
				if(this.index < 0) {
					this.index++;
				}

				HTMLObject subdiv = process(true);
				int endIndex = this.index;

				SwitchHTMLObject s;
				if(this.useAlternate(startIndex, endIndex)) {
					s = new SwitchHTMLObject(getAlternateObject(startIndex, endIndex), subdiv, this.getMainLabel(), this.getAlternateLabel());
				} else {
					s = new SwitchHTMLObject(subdiv, getAlternateObject(startIndex, endIndex), this.getAlternateLabel(), this.getMainLabel());
				}

						d.removeLastObject();
						d.addObject(s.getButton());
						d.addObject(s.getObj1());
						d.addObject(s.getObj2());

						if(index < path.size()) {
							Pair<Edge,Boolean> pair = this.path.get(index);
							Edge edge = pair.getX();
							boolean forward = pair.getY();

							if(!subpath || index < path.size()-1) {
								d.addObject(new SpanObject(ConversionUtils.getNodeInfo(sourceInfo, edge.getData(this.g).getTo(forward))));
								d.addBreak();
							}
							this.index++;
						}

						start = false;
			}  else {
				// add the next pair
				Pair<Edge,Boolean> pair = this.path.get(index);
				Edge edge = pair.getX();
				boolean forward = pair.getY();

				if(this.index == 0 && !subpath) {
					d.addObject(new SpanObject(ConversionUtils.getNodeInfo(sourceInfo, edge.getData(this.g).getFrom(forward))));
					d.addBreak();
				}

				SpanObject span = new SpanObject("&nbsp;| " + this.parseEdge(edge.getData(this.g), forward));
				if(edge.weight > 0) {
					span.putStyle("color", "red");
				}
				d.addObject(span);
				d.addBreak();

				if(isEnd(index)) {
					return d;
				}

				if(!subpath || index < path.size()-1) {
					d.addObject(new SpanObject(ConversionUtils.getNodeInfo(sourceInfo, edge.getData(this.g).getTo(forward))));
					d.addBreak();
				}

				this.index++;

				start = false;
			}
		}
		return d;
	}

	public static class SwitchHTMLObject {
		private HTMLObject obj1;
		private HTMLObject obj2;

		private String buttonHTML1;
		private String buttonHTML2;

		public SwitchHTMLObject(HTMLObject obj1, HTMLObject obj2, String buttonHTML1, String buttonHTML2) {
			this.obj1 = obj1;
			this.obj2 = obj2;

			this.buttonHTML1 = buttonHTML1;
			this.buttonHTML2 = buttonHTML2;
		}

		public HTMLObject getObj1() {
			return this.obj1;
		}

		public HTMLObject getObj2() {
			this.obj2.putStyle("display", "none");
			return this.obj2;
		}

		public ButtonObject getButton() {
			ButtonObject button = new ButtonObject(buttonHTML2);
			button.putOnClick("switchHTMLObject(this,'" + buttonHTML1 + "','" + buttonHTML2 + "','" + obj1.getId() + "','" + obj2.getId() + "')");
			return button;
		}

		public void switchObjs() {
			HTMLObject tempObj = this.obj2;
			this.obj2 = this.obj1;
			this.obj1 = tempObj;

			//String tempString = this.buttonHTML2;
			this.buttonHTML2 = this.buttonHTML1;
			this.buttonHTML1 = this.buttonHTML2;
		}
	}

	public static class MethodCompressedFlowObject extends FlowObject {
		private static Set<String> startSymbols = new HashSet<String>();
		private static Set<String> endSymbols = new HashSet<String>();
		static {
			startSymbols.add("cs_refAssignArg");
			endSymbols.add("cs_refAssignRet");
		}

		public MethodCompressedFlowObject(List<Pair<Edge,Boolean>> path, Graph g, StubLookup s, SourceInfo sourceInfo, boolean subpath) {
			super(path, g, s, sourceInfo, subpath);
		}

		public MethodCompressedFlowObject(List<Pair<Edge,Boolean>> path, Graph g, StubLookup s, SourceInfo sourceInfo) {
			super(path, g, s, sourceInfo);
		}

		@Override public String getMainLabel() {
			return "Compress";
		}

		@Override public String getAlternateLabel() {
			return "Expand";
		}

		@Override public boolean isStart(int i) {
			if(i < 0 || i >= super.getSize()) return false;
			Graph g = super.getGraph();
			Pair<Edge,Boolean> pair = super.getEdge(i);
			return (pair.getY() && startSymbols.contains(pair.getX().getData(g).symbol))
					|| (!pair.getY() && endSymbols.contains(pair.getX().getData(g).symbol));
		}

		@Override public boolean isEnd(int i) {
			if(i < 0 || i >= super.getSize()) return false;
			Graph g = super.getGraph();
			Pair<Edge,Boolean> pair = super.getEdge(i);
			return (!pair.getY() && startSymbols.contains(pair.getX().getData(g).symbol))
					|| (pair.getY() && endSymbols.contains(pair.getX().getData(g).symbol));
		}

		@Override public HTMLObject getAlternateObject(int startIndex, int endIndex) {
			DivObject d = new DivObject();
			d.addObject(new SpanObject("&nbsp;| PassThrough"));
			d.putStyle("margin-left", "10px");
			return d;
		}
	}

	public static class AliasCompressedFlowObject extends FlowObject {
		private static Set<String> symbols = new HashSet<String>();

		static {
			symbols.add("cs_refAssign");
			symbols.add("cs_refAssignArg");
			symbols.add("cs_refAssignRet");
			symbols.add("cs_refAlloc");
			symbols.add("cs_refLoad");
			symbols.add("cs_refStore");
			symbols.add("cs_primStore");
			symbols.add("cs_primLoad");
			symbols.add("cs_primAssign");
			symbols.add("cs_primAssignArg");
			symbols.add("cs_primAssignRet");
		}

		public AliasCompressedFlowObject(List<Pair<Edge,Boolean>> path, Graph g, StubLookup s, SourceInfo sourceInfo, boolean subpath) {
			super(path, g, s, sourceInfo, subpath);
		}

		public AliasCompressedFlowObject(List<Pair<Edge,Boolean>> path, Graph g, StubLookup s, SourceInfo sourceInfo) {
			super(path, g, s, sourceInfo);
		}

		@Override public boolean useAlternate(int startIndex, int endIndex) {
			return false;
		}

		@Override public String getMainLabel() {
			return "+";//"View Flow Path";
		}

		@Override public String getAlternateLabel() {
			return "-";//"View Taint Path";
		}

		@Override public boolean isStart(int i) {
			/*
    		if(i <= 0 || i >= super.getSize()) return false;
    			return super.getEdge(i-1).getY() && !super.getEdge(i).getY();
			 */
			if(i < 0 || i >= super.getSize()) {
				return false;
			} else {
				return super.getEdge(i).getX().getData(super.getGraph()).symbol.equals("FlowsTo") && !super.getEdge(i).getY();
			}
		}

		@Override public boolean isEnd(int i) {
			/*
    		if(i < 0 || i >= super.getSize()-1) return false;
    			return !symbols.contains(super.getEdge(i+1).getX().getData(super.getGraph()).symbol);
			 */
			if(i < 0 || i >= super.getSize()) return false;
			return super.getEdge(i).getX().getData(super.getGraph()).symbol.equals("FlowsTo") && super.getEdge(i).getY();
		}

		@Override public HTMLObject getAlternateObject(int startIndex, int endIndex) {
			if(startIndex < 0 || endIndex >= super.getSize()) {
				DivObject d = new DivObject();
				d.addObject(new SpanObject("&nbsp;| Alias"));
				d.putStyle("margin-left", "10px");
				return d;
			} else {
				List<Pair<Edge,Boolean>> aliasList = new ArrayList<Pair<Edge,Boolean>>();
				for(int i=startIndex; i<=endIndex; i++) {
					aliasList.addAll(super.getGraph().getPath(super.getEdge(i).getX(), super.getEdge(i).getY()));
				}
				return new AliasCompressedFlowObject(aliasList, super.getGraph(), super.getStubLookup(), super.getSourceInfo(), true);
			}
		}
	}
}
