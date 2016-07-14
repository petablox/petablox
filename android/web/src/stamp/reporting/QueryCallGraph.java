package stamp.reporting;

import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

import java.util.*;
import java.io.*;

public class QueryCallGraph
{
	private Document document;
	private XPath xpath;

	public QueryCallGraph(String outPath)
	{
		try{
			System.out.println("QueryCallGraph: outPath = "+outPath);
			File xmlResult = new File(outPath+"/results/IM.xml");
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlResult);
			xpath = XPathFactory.newInstance().newXPath();
		}catch(Exception e){
			throw new Error(e);
		}			
	}

	private String methodName(Element methodElem)
	{
		Element parent = (Element) methodElem.getParentNode();
		StringBuilder msig = new StringBuilder();
		while(parent.getNodeName().equals("category")) {
			msig.insert(0,".").insert(0,Common.getLabel(Common.getFirstChildByTagName(parent, "value")).trim());
			parent = (Element) parent.getParentNode();
		}
		Element value = Common.getFirstChildByTagName(methodElem, "value");
		msig.append(Common.getLabel(value));
		return Common.linkToSrc(msig.toString(), value);
	}

	public String getCallers(String chordSig, List<String> callers)
	{
		Element methodElem;
		try{
			String query = "//category[@type=\"method\" and value[@chordsig=\""+chordSig+"\"]]";
			System.out.println("getCallers_query: "+query);
			methodElem = (Element) xpath.evaluate(query, document, XPathConstants.NODE); 
		}catch(Exception e){
			throw new Error(e);
		}
		if(methodElem == null)
			return null;
		List<Element> tuples = Common.getChildrenByTagName(methodElem, "tuple");
		for(Element tuple: tuples){
			Element value = Common.getFirstChildByTagName(tuple, "value");
			String vlab = Common.linkToSrc(Common.getLabel(value), value);
			callers.add(vlab);
		}
		return methodName(methodElem);
	}

	public String getCallees(String chordSig, String filePath, String lineNum, List<String> callees)
	{
		NodeList methodElems;
		try{
			//String query = "//category[tuple/value[@chordsig=\""+chordSig+"\"]]";
			//String query = "//category[@type=\"method\" and tuple/value[@chordsig=\""+chordSig+"\"]]"; // and @filePath=\""+filePath+"\"
			//String query = "//category[@type=\"method\" and tuple/value[@chordsig=\""+chordSig+"\" and @lineNum=\""+lineNum+"\"]]";
			String query = "//category[@type=\"method\" and tuple/value[@chordsig=\""+chordSig+"\" and @lineNum=\""+lineNum+"\" and @srcFile=\""+filePath+"\"]]";
			System.out.println("getCallees_query: "+query);
			methodElems = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);
		}catch(Exception e){
			throw new Error(e);
		}

		int numCallees = methodElems.getLength();
		System.out.println("numCallees = "+ numCallees);
		
		for(int i = 0; i < numCallees; i++){
			Element methElem = (Element) methodElems.item(i);
			callees.add(methodName(methElem));
		}
		
		String callsite;
		try{
			String query = "//category[@type=\"method\"]/tuple/value[@chordsig=\""+chordSig+"\" and @lineNum=\""+lineNum+"\" and @srcFile=\""+filePath+"\"]";
			callsite = Common.getLabel((Element) xpath.evaluate(query, document, XPathConstants.NODE));
			System.out.println("callsite: "+callsite);
		}catch(Exception e){
			throw new Error(e);
		}
		return Common.linkToSrc(callsite, filePath, lineNum);
	}

	public static void main(String[] args)
	{
		String appPath = "/home/saswat/work/stamp9/apache-tomcat-6.0.35/webapps/ROOT/stamp_output";
		String chordSig = "getDhcpInfo:()Landroid/net/DhcpInfo;@android.net.wifi.WifiManager";
		//String chordSig = "execute:(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;@org.apache.http.impl.client.AbstractHttpClient";
		String lineNum = "385";

		List<String> callees = new ArrayList();
		new QueryCallGraph(appPath).
		getCallees(chordSig,
				   "com/wilers/Wifinder/WiFinder.java",
				   lineNum,
				   callees);
		for(String callee : callees)
			System.out.println(callee);
	}
}
