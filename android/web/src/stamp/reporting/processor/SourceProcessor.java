package stamp.reporting.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import stamp.droidrecordweb.DroidrecordProxyWeb;
import stamp.reporting.Common;
import stamp.reporting.processor.Insertion.EscapeStart;
import stamp.reporting.processor.Insertion.InvocationExpressionBegin;
import stamp.reporting.processor.Insertion.InvocationExpressionEnd;
import stamp.reporting.processor.Insertion.LineBegin;
import stamp.reporting.processor.Insertion.LineEnd;
import stamp.reporting.processor.Insertion.MethodNameStart;
import stamp.reporting.processor.Insertion.PreInvocation;
import stamp.reporting.processor.Insertion.SpanEnd;
import stamp.reporting.processor.Insertion.TypeRefStart;

/*
 * @author Osbert Bastani
 * @author Saswat Anand
 * @author Lazaro Clapp
 */
public class SourceProcessor {
	private String source;
	private List<Insertion> insertions = new ArrayList<Insertion>();

	private Map<Integer, String> lineToMethodMap;
	public String getContainingMethodForLine(int lineNum) {
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
		System.out.println("DEBUG: Done processing query!");
		int n = nodes.getLength();
		System.out.println("Number of nodes found: " + n);
		Map<Integer, String> startLnToMethod = new HashMap<Integer, String>();
		Map<Integer, String> endLnToMethod = new HashMap<Integer, String>();
		int lastLn = 0;
		System.out.println("DEBUG: Filling in source information...");
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
		System.out.println("DEBUG: Done filling in source information!");
		Stack<String> currentMethodStack = new Stack<String>();
		String currentMethod = null;
		System.out.println("DEBUG: Building method stack...");
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
		System.out.println("DEBUG: Done building method stack!");
	}

	public SourceProcessor(File sourceFile, DroidrecordProxyWeb droidrecord, File srcMapFile) throws Exception {
		lineToMethodMap = new HashMap<Integer, String>();
		if(srcMapFile != null) {
			System.out.println("DEBUG: Populating line to method map...");
			populateLineToMethodMap(srcMapFile);
			System.out.println("DEBUG: Done populating line to method map!");
		}

		// read file and add line insertions
		System.out.println("DEBUG: Reading file at " + sourceFile.getCanonicalPath() + "...");
		BufferedReader br = new BufferedReader(new FileReader(sourceFile));
		System.out.println("DEBUG: Done reading file at " + sourceFile.getCanonicalPath() + "!");		
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
		System.out.println("DEBUG: Done processing source!");		
	}

	public SourceProcessor(File sourceFile, DroidrecordProxyWeb droidrecord) throws Exception {
		this(sourceFile, droidrecord, null);
	}

	public SourceProcessor(File sourceFile, DroidrecordProxyWeb droidrecord, File srcMapFile, File taintedInfo, File allReachableInfo, File runtimeReachedMethods, String filePath) throws Exception {
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

				insertions.add(new MethodNameStart(start, chordSig, reachable, reached));
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
					currentTVR = new TaintedVariableRecord(start, end);
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
