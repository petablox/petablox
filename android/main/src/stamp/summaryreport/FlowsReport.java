package stamp.summaryreport;

import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import java.io.*;
import java.util.*;

public class FlowsReport
{
	private Main main;
	private LabelReader srcLabelReader;
	private LabelReader sinkLabelReader;

	FlowsReport(Main main)
	{
		this.main = main;
	}

	void generate()
	{
		File xmlResult = new File(System.getProperty("stamp.out.dir"), "results/SrcSinkFlow.xml");
		if(!xmlResult.exists())
			return;

		NodeList flowElems;
		try{ 
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlResult);
			XPath xpath = XPathFactory.newInstance().newXPath();
			String query = "/root/tuple";
			//System.out.println("query: "+query);
			flowElems = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET); 
		}catch(Exception e){
			throw new Error(e);
		}

		File srcClassFile = new File(System.getProperty("stamp.dir"), "assets/srcClass.xml");
		File sinkClassFile = new File(System.getProperty("stamp.dir"), "assets/sinkClass.xml");

		srcLabelReader = new LabelReader(srcClassFile);
		sinkLabelReader = new LabelReader(sinkClassFile);

		try{
			File formatFile = new File(System.getProperty("stamp.dir"), "assets/summary.format");
			Set<String> dataTypes = readFormat(formatFile);
			write(flowElems, dataTypes);
		}catch(Exception e){
			throw new Error(e);
		}
	}

	private void write(NodeList flowElems, Set<String> dataTypes)
	{
		int numFlows = flowElems.getLength();
		System.out.println("numFlows = "+ numFlows);

		Map<String,String> sinkToIcon = new HashMap();
		sinkToIcon.put("HTTP", "glyphicon-cloud-upload");
		sinkToIcon.put("Internet", "glyphicon-cloud-upload");
		sinkToIcon.put("SMS", "glyphicon-envelope");
		sinkToIcon.put("File", "glyphicon-floppy-save");

		Map<String,Double> sinkToWeight = new HashMap();
		sinkToWeight.put("HTTP", 1.0);
		sinkToWeight.put("Internet", 1.0);
		sinkToWeight.put("SMS", 1.0);
		sinkToWeight.put("File", 0.5);
		
		Map<String,Set<String>> flows = new HashMap();
		double pvi = 0.0;
		for(int i = 0; i < numFlows; i++){
			Element flowElem = (Element) flowElems.item(i);
			String src = getLabel(getNthChildByTagName(flowElem, "value", 1));
			String sink = getLabel(getNthChildByTagName(flowElem, "value", 2));
			
			Element srcElem = srcLabelReader.findElem(src);
			Element sinkElem = sinkLabelReader.findElem(sink);
			
			System.out.println(src+" --> "+sink);

			String srcDesc;
			String srcClass;
			if(srcElem == null){
				srcDesc = "";
				srcClass = "";
				System.out.println("src not found: "+src);
			} else {
				srcDesc = srcElem.getAttribute("desc");
				srcClass = srcElem.getAttribute("class");
				//String srcPriority = srcElem.getAttribute("priority");
			}
			
			String sinkDesc; 
			String sinkClass; 
			
			if(sinkElem == null){
				sinkDesc = "";
				sinkClass = "";
				System.out.println("sink not found: "+sink);
			} else {
				sinkDesc = sinkElem.getAttribute("desc");
				sinkClass = sinkElem.getAttribute("class");
			}
			//System.out.println("src: "+src+" "+srcDesc+" "+srcClass+" "+srcPriority);
			//System.out.println("sink: "+sink+" "+sinkDesc+" "+sinkClass);

			if(sinkClass.equals("OnDevice") || sinkClass.equals("OffDevice")){
				if(dataTypes.contains(srcDesc) && !sinkDesc.equals("Internal Message")){

					Set<String> fs = flows.get(srcDesc);
					if(fs == null){
						fs = new HashSet();
						flows.put(srcDesc, fs);
					}
					String icon = sinkToIcon.get(sinkDesc);
					fs.add(icon);
					
					Double f = sinkToWeight.get(sinkDesc);
					if(f != null)
						pvi += f;
				}
			}
		}

		main.startPanel(String.format("Privacy Risk <span class=\"badge badge-important\">%.1f</span>",pvi));
		main.println("<table class=\"table table-striped table-condensed\">");
		for(Map.Entry<String,Set<String>> e : flows.entrySet()){
			StringBuilder builder = new StringBuilder("<tr>");
			String srcDesc = e.getKey();
			Set<String> icons = e.getValue();
			builder.append(String.format("<td>%s</td>", srcDesc));
			builder.append("<td>");
			for(String i : icons){
				builder.append(String.format("<span class=\"glyphicon %s\"></span>&nbsp;&nbsp;", i));
			}
			builder.append("</td>");
			builder.append("</tr>");
			main.println(builder.toString());
		}
		main.println("</table>");
		main.endPanel();
	}

	private Set<String> readFormat(File formatFile) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(formatFile));
		String line;
		Set<String> dataTypes = new HashSet();
		while((line = reader.readLine()) != null){
			dataTypes.add(line.trim());
		}
		return dataTypes;
	}

	public static String getLabel(Element value)
	{
	    Element labelElem = getNthChildByTagName(value, "label", 1);
		return escapeHtml4(labelElem.getFirstChild().getNodeValue());
	}

	public static Element getNthChildByTagName(Element parent, String name, int n) 
	{
		int count = 0;
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && 
				name.equals(child.getNodeName())) {
				count++;
				if(n == count){
					return (Element) child;
				}
			}
		}
		return null;
    }

	static class LabelReader 
	{
		private XPath xpath;
		private Document doc;
		
		LabelReader(File classFile)
		{
			try{ 
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(classFile);
				xpath = XPathFactory.newInstance().newXPath();
			}catch(Exception e){
				throw new Error(e);
			}
		}
		
		Element findElem(String label)
		{
			try{
				String query = "/root/*[contains(.,\""+label.toLowerCase()+"\")]";
				//System.out.println("query: "+query);
				return (Element) xpath.evaluate(query, doc, XPathConstants.NODE); 
			}catch(Exception e){
				throw new Error(e);
			}		
		}
	}

}