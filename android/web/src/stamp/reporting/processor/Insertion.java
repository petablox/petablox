package stamp.reporting.processor;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONValue;

import stamp.droidrecordweb.DroidrecordProxyWeb;
import edu.stanford.droidrecord.logreader.CoverageReport;
import edu.stanford.droidrecord.logreader.analysis.CallValueAnalysis;
import edu.stanford.droidrecord.logreader.analysis.CallValueAnalysis.CallValueResult;
import edu.stanford.droidrecord.logreader.events.info.MethodInfo;
import edu.stanford.droidrecord.logreader.events.info.ParamInfo;

public abstract class Insertion implements Comparable<Insertion> {
	// -1: beginning, 0: middle, 1: ending
	private int position;
	private int order;

	public Insertion(int position) {
		this.position = position;
		this.order = 0;
	}

	public Insertion(int position, int order) {
		this.position = position;
		this.order = order;
	}

	public int getPosition() {
		return position;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public int compareTo(Insertion i) {
		if(this.position < i.getPosition()) {
			return 1;
		} else if(this.position == i.getPosition()) {
			if(this.order < i.getOrder()) {
				return 1;
			} else if(this.order == i.getOrder()) {
				return 0;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	public abstract String toString();
	
	
	/*
	 * Various insertions.
	 */

	public static class LineBegin extends Insertion {
		private int lineNum;
		private DroidrecordProxyWeb droidrecord;
		private String methodSig;

		public LineBegin(int position, DroidrecordProxyWeb droidrecord,
				String methodSig, int lineNum) {
			super(position, -8);
			this.droidrecord = droidrecord;
			this.methodSig = methodSig;
			this.lineNum = lineNum;
		}

		@Override public String toString() {
			String coveredClass = "src-ln-not-covered";
			if(droidrecord.isAvailable() && methodSig != null) {
				CoverageReport coverage = droidrecord.getCoverage();
				if(coverage != null && coverage.isCoveredLocation(methodSig, lineNum)) {
					coveredClass = "src-ln-covered";
				}
			}
			return "<span id='"+lineNum+"' class='"+coveredClass+"' name='"+lineNum+"'>";
		}
	}

	public static class LineEnd extends Insertion {
		public LineEnd(int position) {
			super(position, 8);
		}

		@Override public String toString() {
			return "</span>";
		}
	}

	public static class PreMethodName extends Insertion 
	{
		private String chordSig;

		public PreMethodName(int position, String chordSig, boolean isReachable) {
			super(position, 4);
			this.chordSig = chordSig;
		}

		@Override public String toString() {
			return "<span data-chordsig='"+StringEscapeUtils.escapeHtml4(chordSig)+"' name='PreMethodName'></span>";
			//return "<span id='PreMethodName"++"' name='PreMethodName'></span>";
		}
	}

	public static class MethodNameStart extends Insertion 
	{
		private String chordSig;
		private boolean reachable;
		private boolean reached;

		public MethodNameStart(int position, String chordSig, boolean isReachable, boolean reached) {
			super(position, 6);
			this.reachable = isReachable;
			this.chordSig = chordSig;
			this.reached  = reached;
		}

		@Override public String toString() {
			return "<span data-chordsig='"+StringEscapeUtils.escapeHtml4(chordSig)+"' data-reachable='"+reachable+"' reached='"+reached+"' reached='"+reached+"' name='MethodName'>";
		}
	}


	public static class TypeRefStart extends Insertion 
	{
		private String chordSig;

		public TypeRefStart(int position, String chordSig) {
			super(position, 6);
			this.chordSig = chordSig;
		}

		@Override public String toString() {
			return "<span data-chordsig='"+StringEscapeUtils.escapeHtml4(chordSig)+"' name='TypeRef'>";
		}
	}

	public static class SpanEnd extends Insertion {
		public SpanEnd(int position) {
			super(position, -6);
		}

		@Override public String toString() {
			return "</span>";
		}
	}
	public static class InvocationExpressionBegin extends Insertion {
		private DroidrecordProxyWeb droidrecord;
		private String methodSig;
		private int lineNum;
		private String calleeMethodSubSig;

		public InvocationExpressionBegin(int position, DroidrecordProxyWeb droidrecord,
				String methodSig, int lineNum, String calleeMethodSubSig) {
			super(position, -8);
			this.droidrecord = droidrecord;
			this.methodSig = methodSig;
			this.lineNum = lineNum;
			this.calleeMethodSubSig = calleeMethodSubSig;
		}

		@Override public String toString() {
			String paramsDataStr = "";
			if(droidrecord.isAvailable() && methodSig != null) {
				CallValueAnalysis cva = droidrecord.getCallValueAnalysis();
				if(cva.isReady()) {
					List<CallValueResult> cinfo = 
							cva.queryCallInfo(calleeMethodSubSig, 
									methodSig, lineNum);
					if(cinfo.size() != 0) {
						Map jsonObj = new LinkedHashMap();
						MethodInfo method = cinfo.get(0).getMethod();
						jsonObj.put("methodName", method.getName());
						jsonObj.put("parameterTypes", method.getArguments());
						LinkedList list = new LinkedList();
						Set<String> seenParamChoices = new HashSet<String>();
						for(CallValueResult cvr : cinfo) {
							String jsonInvkParams = "";
							Map callMap = new LinkedHashMap();
							LinkedList paramList = new LinkedList();
							for(ParamInfo pi : cvr.getArguments()) {
								Map paramMap = new LinkedHashMap();
								paramMap.put("type", pi.getType());
								if(pi.isObjectLikeType()) {
									paramMap.put("klass", pi.getKlass());
									paramMap.put("id", pi.getId());
								} else {
									paramMap.put("value", pi.getValue());
								}
								jsonInvkParams += pi.toSimpleString();
								paramList.add(paramMap);
							}
							callMap.put("params",paramList);
							ParamInfo returnVal = cvr.getReturnValue();
							if(returnVal != null) {
								Map paramMap = new LinkedHashMap();
								paramMap.put("type", returnVal.getType());
								if(returnVal.isObjectLikeType()) {
									paramMap.put("klass", returnVal.getKlass());
									paramMap.put("id", returnVal.getId());
								} else {
									paramMap.put("value", returnVal.getValue());
								}
								jsonInvkParams += returnVal.toSimpleString();
								callMap.put("returnValue", paramMap);
							}
							if(seenParamChoices.contains(jsonInvkParams)) continue;
							list.add(callMap);
							seenParamChoices.add(jsonInvkParams);
						}
						jsonObj.put("calls", list);
						paramsDataStr = JSONValue.toJSONString(jsonObj);
					}
				}
			}
			try {
				paramsDataStr = new String(Base64.encodeBase64(paramsDataStr.getBytes("UTF-8")));
			} catch(UnsupportedEncodingException e) {
				throw new Error(e);
			}
			String entity = "<span class='invocationExpression' ";
			entity += "data-droidrecord-params='"+paramsDataStr;
			entity += "' data-droidrecord-callee-sub='"+StringEscapeUtils.escapeHtml4(calleeMethodSubSig);
			entity += "' data-droidrecord-caller='"+StringEscapeUtils.escapeHtml4(methodSig);
			entity += "' data-droidrecord-line='"+lineNum;
			entity += "'>";
			return entity;
		}
	}

	// this is actually post invocation now
	public static class PreInvocation extends Insertion {
		private String chordSig;
		private String filePath;
		private int lineNum;

		public PreInvocation(int position, String chordSig, String filePath, int lineNum) {
			super(position, 4);
			this.chordSig = chordSig;
			this.filePath = filePath;
			this.lineNum = lineNum;
		}
		@Override public String toString() {
			return "<span data-chordsig='"+StringEscapeUtils.escapeHtml4(chordSig)+"' name='PreInvocation' data-filePath='"+this.filePath+"' data-lineNum='"+this.lineNum+"'></span>";
			//return "<span id='PreInvocation"+StringEscapeUtils.escapeHTML(chordSig)+"' name='PreInvocation'></span>";
		}
	}

	public static class InvocationExpressionEnd extends Insertion {
		public InvocationExpressionEnd(int position) {
			super(position, 8);
		}

		@Override public String toString() {
			return "</span>";
		}
	}

	public static class SrcSinkSpanStart extends Insertion {
		private Set<String> sources;
		private Set<String> sinks;

		public SrcSinkSpanStart(int position, Set<String> sources, Set<String> sinks) {
			super(position, 6);
			this.sources = sources;
			this.sinks = sinks;
		}

		@Override public String toString() {
			StringBuilder srcSinkData = new StringBuilder();
			String srcSinkDataStr;
			srcSinkData.append("{");
			srcSinkData.append("\"sources\":[");
			boolean addComa = false;
			for(String s : sources) {
				if(addComa) srcSinkData.append(",");
				srcSinkData.append("\"" + s + "\"");
				addComa = true;
			}
			srcSinkData.append("],");
			srcSinkData.append("\"sinks\":[");
			addComa = false;
			for(String s : sinks) {
				if(addComa) srcSinkData.append(",");
				srcSinkData.append("\"" + s + "\"");
				addComa = true;
			}
			srcSinkData.append("]}");
			try {
				srcSinkDataStr = new String(Base64.encodeBase64(srcSinkData.toString().getBytes("UTF-8")));
			} catch(UnsupportedEncodingException e) {
				throw new Error(e);
			}
			return "<span class='srcSinkSpan' data-stamp-srcsink='"+srcSinkDataStr+"'>";
		}
	}

	public static class KeySpanStart extends Insertion {
		private String key;

		public KeySpanStart(int position, String key) {
			super(position, 6);
			this.key = key;
		}

		@Override public String toString() {
			return "<span id='"+key+getPosition()+"' name='"+key+"'>";
		}
	}

	public static class EscapeStart extends Insertion {
		private String sequence;

		public EscapeStart(int position, String sequence) {
			super(position, -12);
			this.sequence = sequence;
		}

		@Override public String toString() {
			return this.sequence;
		}
	}
}
