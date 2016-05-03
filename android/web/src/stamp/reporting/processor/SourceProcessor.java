package stamp.reporting.processor;

import java.util.*;
import java.io.*;

import javax.xml.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONValue;

import edu.stanford.droidrecord.logreader.CoverageReport;
import edu.stanford.droidrecord.logreader.analysis.CallValueAnalysis;
import edu.stanford.droidrecord.logreader.analysis.CallValueAnalysis.CallValueResult;
import edu.stanford.droidrecord.logreader.events.info.ParamInfo;
import edu.stanford.droidrecord.logreader.events.info.MethodInfo;
import stamp.droidrecordweb.DroidrecordProxyWeb;
import stamp.reporting.Common;

/* 
   @author Osbert Bastani
   @author Saswat Anand
   @author Lazaro Clapp
*/
public class SourceProcessor 
{

    public static abstract class Insertion implements Comparable<Insertion> {
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
		
		@Override public int compareTo(Insertion i) {
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
    }
	
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
		private String filePath; // Added by Patrick so method definitions know what file they live in

		public MethodNameStart(int position, String chordSig, boolean isReachable, boolean reached, String filePath) {
			super(position, 6);
			this.reachable = isReachable;
			this.chordSig = chordSig;
			this.reached  = reached;
			this.filePath = filePath;
		}
		
		@Override public String toString() {
			return "<span data-chordsig='" + StringEscapeUtils.escapeHtml4(chordSig) + 
				"' data-reachable='" + reachable + "' reached='" + reached + 
				"' reached='" + reached + "' data-filepath='" + filePath + "' name='MethodName'>";
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
	    private String flowNumbers;

	    public KeySpanStart(int position, String key, String flowNumbers) {
	        super(position, 6);
	        this.key = key;
	        this.flowNumbers = flowNumbers;
	    }

	    @Override public String toString() {
	        return "<span id='"+key+java.util.UUID.randomUUID().toString().replaceAll("-", "")+"' name='"+key+"' flows='"+flowNumbers+"'>";
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
    
    public static class TaintedVariableRecord {
        private final int startPos;
        private final int endPos;
        private final String flowNumbers;
        private final Set<String> sources;
        private final Set<String> sinks;
        
        public int getStart() { return startPos; }
        public int getEnd() { return endPos; }
        
        public TaintedVariableRecord(int startPos, int endPos, String flowNumbers) {
            //System.out.println("Created TaintedVariableRecord " + startPos + " " + endPos);
            this.startPos = startPos;
            this.endPos = endPos;
            this.flowNumbers = flowNumbers;
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
			insertions.add(new KeySpanStart(startPos, "taintedVariable", flowNumbers));
			insertions.add(new SpanEnd(endPos));
			insertions.add(new SpanEnd(endPos));
        }
        
    }

    private String source;
    private List<Insertion> insertions = new ArrayList<Insertion>();
    
    private Map<Integer, String> lineToMethodMap;
    public String getContainingMethodForLine(int lineNum)
    {
        return lineToMethodMap.get(lineNum);
    }
    
    private void populateLineToMethodMap(File srcMapFile) throws Exception {
        System.out.println("srcMapFile : " + srcMapFile);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(srcMapFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
		String query = "//method";
		System.out.println("SourceProcessor.methodNodes: "+ query);
		NodeList nodes = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);
		int n = nodes.getLength();
		Map<Integer, String> startLnToMethod = new HashMap<Integer, String>();
		Map<Integer, String> endLnToMethod = new HashMap<Integer, String>();
		int lastLn = 0;
		for(int i = 0; i < n; i++){
		    String methodSig = ((Element) nodes.item(i)).getAttribute("chordsig");
		    methodSig = DroidrecordProxyWeb.chordToSootMethodSignature(methodSig);
		    Integer bodyStartLn = new Integer(((Element) nodes.item(i)).getAttribute("bodyStartLn"));
			startLnToMethod.put(bodyStartLn, methodSig);
			Integer bodyEndLn = new Integer(((Element) nodes.item(i)).getAttribute("bodyEndLn"));
			endLnToMethod.put(bodyEndLn, methodSig);
			if(bodyEndLn > lastLn) lastLn = bodyEndLn;
		    //System.out.println(String.format("Detected method: %s [%d,%d]", methodSig, bodyStartLn, bodyEndLn));
		}
		Stack<String> currentMethodStack = new Stack<String>();
		String currentMethod = null;
		for(int i = 0; i < lastLn; i++) {
		    if(startLnToMethod.get(i) != null) {
		        currentMethodStack.push(currentMethod);
		        currentMethod = startLnToMethod.get(i);
		    }
		    if(endLnToMethod.get(i) != null) {
		        assert currentMethod.equals(endLnToMethod.get(i));
		        currentMethod = currentMethodStack.pop();
		    }
		    if(currentMethod != null) {
		        lineToMethodMap.put(i, currentMethod);
		    }
		    //System.out.println(String.format("Line: %d ===> Method: %s", i, currentMethod));
		}
    }

	public SourceProcessor(File sourceFile, DroidrecordProxyWeb droidrecord, File srcMapFile) throws Exception
	{
	    lineToMethodMap = new HashMap<Integer, String>();
	    if(srcMapFile != null) {
	        populateLineToMethodMap(srcMapFile);
	    }
	    
		// read file and add line insertions
		BufferedReader br = new BufferedReader(new FileReader(sourceFile));
		StringBuilder sourceBuilder = new StringBuilder();
		String line;
		int lineNum = 1;
		int pos=0;
		while((line = br.readLine()) != null) {
		    String methodSig = getContainingMethodForLine(lineNum);
			insertions.add(new LineBegin(pos, droidrecord, methodSig, lineNum));
			
			for(int i=0; i<line.length(); i++) {
				switch(line.charAt(i)) {
				case '<':
					insertions.add(new EscapeStart(pos+i+1, "#60;"));
					line = line.substring(0, i) + "&" + line.substring(i+1);
					break;
				case '>':
					insertions.add(new EscapeStart(pos+i+1, "#62;"));
					line = line.substring(0, i) + "&" + line.substring(i+1);
					break;
				case '&':
					insertions.add(new EscapeStart(pos+i+1, "#38;"));
					break;
				}
			}
			
			insertions.add(new LineEnd(pos+line.length()));
			
			sourceBuilder.append(line+"\n");
			
			lineNum++;
			pos+=line.length()+1;
		}
		br.close();
		this.source = sourceBuilder.toString();
	}
	
	public SourceProcessor(File sourceFile, DroidrecordProxyWeb droidrecord) throws Exception
	{
	    this(sourceFile, droidrecord, null);
	}

    public SourceProcessor(File sourceFile, DroidrecordProxyWeb droidrecord, 
                           File srcMapFile, File taintedInfo, File allReachableInfo,
						   File runtimeReachedMethods, String filePath) throws Exception 
	{
		this(sourceFile, droidrecord, srcMapFile);
		
		Set<String> reachableSigs = new HashSet<String>();
		// find reachable methods defined in this source file
		{
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(allReachableInfo);
			XPath xpath = XPathFactory.newInstance().newXPath();

			String query = "//tuple/value[@type=\"method\" and @srcFile=\""+filePath+"\"]";
			//System.out.println("SourceProcessor.reachableM: "+ query);
			NodeList nodes = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);
			int n = nodes.getLength();
			for(int i = 0; i < n; i++){
				String chordSig = ((Element) nodes.item(i)).getAttribute("chordsig");
				chordSig = StringEscapeUtils.unescapeXml(chordSig); //System.out.println("reachableMethod: "+chordSig);
				reachableSigs.add(chordSig);
			}
		}

		Set<String> reachedSigs = new HashSet<String>();  
		// Find the set of reached methods from runtime observations
		if(runtimeReachedMethods != null && runtimeReachedMethods.exists()){
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(runtimeReachedMethods);
			XPath xpath = XPathFactory.newInstance().newXPath();

			String query = "//chord_sig";
			NodeList nodes = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);
			int n = nodes.getLength();
			for(int i = 0; i < n; i++){
				Element element = (Element) nodes.item(i);
				Node node = element.getFirstChild();
				
				if(node == null)
					continue;

				String chordSig = node.getNodeValue();
				reachedSigs.add(chordSig);
			}
		}
		

		
		// add invocation insertions
		{
			//System.out.println("srcMapFile: "+srcMapFile);
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(srcMapFile);
			XPath xpath = XPathFactory.newInstance().newXPath();

			//It is quite complicated to show call targets for the "new X(..)" statements
			//when X is a nested class. So disabling this feature altogether. Instead of this
			//feature the plan is to hyperlink all types such as X in the new statement to
			//the code of the respective class.
			/*
			NodeList nodes = (NodeList)xpath.evaluate("//newexpr", document, XPathConstants.NODESET);
			for(int i=0; i<nodes.getLength(); i++) {
				Element node = (Element)nodes.item(i);
				int start = Integer.valueOf(node.getAttribute("start"));
				int length = Integer.valueOf(node.getAttribute("length"));
				String chordSig = node.getAttribute("chordsig");
				int invocationLineNum = Integer.valueOf(node.getAttribute("line"));
				insertions.add(new PreInvocation(start+length, chordSig, filePath, invocationLineNum));
				}
			*/
			
			NodeList nodes = (NodeList)xpath.evaluate("//invkexpr", document, XPathConstants.NODESET);
			for(int i=0; i<nodes.getLength(); i++) {
				Element node = (Element)nodes.item(i);
				int start = Integer.valueOf(node.getAttribute("start"));
				int length = Integer.valueOf(node.getAttribute("length"));
				String chordSig = node.getAttribute("chordsig");
				int invocationLineNum = Integer.valueOf(node.getAttribute("line"));
				String methodSig = getContainingMethodForLine(invocationLineNum);
				String callMethodSubSig = DroidrecordProxyWeb.chordToSootMethodSubSignature(chordSig);
				insertions.add(new InvocationExpressionBegin(start, droidrecord, methodSig, invocationLineNum, callMethodSubSig));
				insertions.add(new PreInvocation(start+length, chordSig, filePath, invocationLineNum));
				insertions.add(new InvocationExpressionEnd(start+length));
			}

			nodes = (NodeList)xpath.evaluate("//method", document, XPathConstants.NODESET);
			for(int i=0; i<nodes.getLength(); i++) {
				Element node = (Element)nodes.item(i);
				String chordSig = node.getAttribute("chordsig");
				if(chordSig.startsWith("<clinit>:"))
					continue;
				int start = Integer.valueOf(node.getAttribute("startpos"));
				int end = Integer.valueOf(node.getAttribute("endpos"));
				boolean reachable = reachableSigs.contains(chordSig);
				boolean reached = reachedSigs.contains(chordSig);
                
				insertions.add(new MethodNameStart(start, chordSig, reachable, reached, filePath));
				insertions.add(new SpanEnd(end));	
			}
			
			nodes = (NodeList)xpath.evaluate("//type", document, XPathConstants.NODESET);
			for(int i=0; i<nodes.getLength(); i++) {
				Element node = (Element)nodes.item(i);
				String chordSig = node.getAttribute("chordsig");
				int start = Integer.valueOf(node.getAttribute("start"));
				int end = Integer.valueOf(node.getAttribute("length"))+start;
				insertions.add(new TypeRefStart(start, chordSig));
				insertions.add(new SpanEnd(end));					
			}
		}
		
		// add taint insertions
		{
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(taintedInfo);
			XPath xpath = XPathFactory.newInstance().newXPath();

			String query = "//category[@type=\"method\"]/tuple/value[@srcFile=\""+filePath+"\"]/highlight[@key=\"taintedVariable\"]";			
			System.out.println("Tainted vars query: "+query);
			NodeList nodes = (NodeList)xpath.evaluate(query, document, XPathConstants.NODESET);
			
			Map<Integer, List<TaintedVariableRecord>> taintedVars = new HashMap<Integer, List<TaintedVariableRecord>>();
            //System.out.println("HERE 1");
			for(int i=0; i<nodes.getLength(); i++) {
				Element node = (Element)nodes.item(i);
				int start = Integer.valueOf(node.getAttribute("startpos"));
				int end = start+Integer.valueOf(node.getAttribute("length"));
				String flows = node.getAttribute("flows");
				if(!taintedVars.containsKey(start)) {
				    taintedVars.put(start, new ArrayList<TaintedVariableRecord>());
				}
				TaintedVariableRecord currentTVR = null;
				for(TaintedVariableRecord tvr : taintedVars.get(start)) {
				    if(tvr.getEnd() == end) {
				        currentTVR = tvr;
				    }
				}
				if(currentTVR == null) {
				    currentTVR = new TaintedVariableRecord(start, end, flows);
				    taintedVars.get(start).add(currentTVR);
				}
				String srcSinkQuery = "ancestor::category";
				NodeList subnodes = (NodeList)xpath.evaluate(srcSinkQuery, node, XPathConstants.NODESET);
				String s = Common.getLabel(Common.getFirstChildByTagName((Element) subnodes.item(0), "value"));
				//System.out.println("subnodes.size = "+subnodes.getLength() + " "+s);
				if(s.startsWith("$")) {
					currentTVR.addSource(s);
				} else if(s.startsWith("!")) {
					currentTVR.addSink(s);
				}
			}
			for(List<TaintedVariableRecord> l : taintedVars.values()) {
			    for(TaintedVariableRecord tvr : l) {
			        tvr.makeInsertions(insertions);
			    }
			}
		}
    }

    public String getSource() 
	{
		return this.source;
    }

    public String getSourceWithInsertions() {
	return getSourceWithAnnotations();
    }

    public String getSourceWithAnnotations() {
	String newSource = this.source;
	Collections.sort(this.insertions);
	for(Insertion i : this.insertions) {
	    if(i.getPosition() <= newSource.length()) {
		newSource = newSource.substring(0, i.getPosition()) + i.toString() + newSource.substring(i.getPosition());
	    }
	}
	return newSource;
    }

}
