package stamp.reporting;

import javax.xml.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import java.util.*;
import java.io.*;

import static stamp.reporting.Common.*;

public class QueryResults
{
	private Map<String,Document> fileNameToDoc = new HashMap();

	public QueryResults()
	{
	}

    public String readFile(File file) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(file));
	StringBuilder sb = new StringBuilder();
	String line;
	while((line = br.readLine()) != null) {
	    sb.append(line + "\n");
	}
	return sb.toString();
    }

    public String getSrcSinkFlowBody(String src, String sink, String outpath) {
	try {
	    return readFile(new File(outpath + "/cfl/" + src.substring(1) + "2" + sink.substring(1) + ".out"));
	    //return readFile(new File("../cfl/" + src + "2" + sink + ".out"));
	} catch(IOException e) {
	    return "Error! Did you run JCFLSolverAnalysis?";
	}
    }

	public String querySrcSinkFlows(String fileName)
	{
		NodeList tuples;
		try{
			File xmlResult = new File(fileName);
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlResult);
			XPath xpath = XPathFactory.newInstance().newXPath();
			tuples = (NodeList) xpath.evaluate("/root/tuple", doc, XPathConstants.NODESET); 
		}catch(Exception e){
			throw new Error(e);
		}
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for(int i = 0; i < tuples.getLength(); i++){
			Element tuple = (Element) tuples.item(i);
			List<Element> vals = getChildrenByTagName(tuple, "value");
			if(vals.size() != 2 && vals.size() != 3) {
			    throw new RuntimeException("unexpected " + vals.size());
			}
			String src = getLabel(vals.get(0));
			String sink = getLabel(vals.get(1));
			String weight = vals.size() == 2 ? "0" : getLabel(vals.get(2));
			if(!first){
				builder.append(',');
			} else{
				first = false;
			}
			builder.append(src).append('#').append(sink).append('#').append(weight);
		}
		return builder.toString();
	}

	public String query(String fileName, String id)
	{		
		Element elemToExpand = queryForElement(fileName, id);
		List<String> result = new ArrayList();
		String nodeName = elemToExpand.getNodeName();
		if(nodeName.equals("tuple"))
			processTuple(id, elemToExpand, result);
		else
			processCategory(id, elemToExpand, result);

		StringBuilder builder = new StringBuilder("[");
		for(Iterator<String> it = result.iterator(); it.hasNext();){
			builder.append(it.next());
			if(it.hasNext())
				builder.append(",");
		}
		String ret = builder.append("]").toString();
		//System.out.println(ret);
		return ret;
	}
	
	public Element queryForElement(String fileName, String id)
	{
	    String query = formQuery(id);
		//System.out.println("QueryResults.queryForElement = "+query+" "+id);
		Document doc = fileNameToDoc.get(fileName);
		if(doc == null){
			//first time querying on this file
			try{
				File xmlResult = new File(fileName);
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlResult);
				fileNameToDoc.put(fileName, doc);
			}catch(Exception e){
				throw new Error(e);
			}
		}
		XPath xpath;
		try{
			xpath = XPathFactory.newInstance().newXPath();
		}catch(Exception e){
			throw new Error(e);
		}

		Element elem;
		try{
			elem = (Element) xpath.evaluate(query, doc, XPathConstants.NODE); 
		}catch(Exception e){
			throw new Error(e);
		}
		return elem;
	}

	private String formQuery(String id)
	{
		if(id == null || id.equals(""))
			return "/root";
		//always starts with -
		id = id.substring(1);
		
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("/root");
		for(String index : id.split("-")){
			char c = index.charAt(0);
			int i = Integer.parseInt(index.substring(1));
			switch(c){
			case 'c':
				queryBuilder.append("/category["+i+"]");
				break;
			case 't':
				queryBuilder.append("/tuple["+i+"]");
				break;
			default:
				throw new RuntimeException("Unexpected id "+id);
			}
		}
		String query = queryBuilder.toString();
		return query;
	}
	
	private String processValueNode(Element valueElem, String type, String subcatNodeId)
	{
	    StringBuilder builder = new StringBuilder();
	    String name = getLabel(valueElem);
        builder.append("{\"name\":\""+name+"\"");
        builder.append(",\"type\":\""+type+"\"");
        builder.append(",\"nodeId\":\""+subcatNodeId+"\"");

        String srcFile = valueElem.getAttribute("srcFile");
        String lineNum = valueElem.getAttribute("lineNum");
        String showReport = valueElem.getAttribute("showReport");
        String reportNodeID = valueElem.getAttribute("reportNodeID");
        String reportNodeShortName = valueElem.getAttribute("reportNodeShortName");
        if(srcFile != null && !srcFile.equals(""))
            builder.append(",\"file\":\""+srcFile+"\",\"lineNum\":\""+lineNum+"\"");
        if(showReport != null && !showReport.equals(""))
            builder.append(",\"showReport\":\""+showReport+"\"," +
			                   "\"reportNodeID\":\""+reportNodeID+"\"," +
			                   "\"reportNodeShortName\":\""+reportNodeShortName+"\"");
        builder.append('}');
        return builder.toString();
	}

	private void processCategory(String id, Element nodeToExpand, List<String> result)
	{
		if(id == null)
			id = "";
		
		List<Element> subcats = getChildrenByTagName(nodeToExpand, "category");
		for(int j = 0; j < subcats.size(); j++){
			Element subcatElem = subcats.get(j);

			Element valueElem = getFirstChildByTagName(subcatElem, "value");
			String name = getLabel(valueElem);
			String subcatNodeId = id + "-c" + (j+1);
			String srcFile = valueElem.getAttribute("srcFile");
			String lineNum = valueElem.getAttribute("lineNum");

			if(srcFile == null)
				result.add("{\"name\":\""+name+"\",\"type\":\"folder\",\"nodeId\":\""+subcatNodeId+"\"}");
			else
				result.add("{\"name\":\""+name+"\",\"type\":\"folder\",\"nodeId\":\""+subcatNodeId+"\",\"file\":\""+srcFile+"\",\"lineNum\":\""+lineNum+"\"}");
		}

		List<Element> tuples = getChildrenByTagName(nodeToExpand, "tuple");
		for(int j = 0; j < tuples.size(); j++){
			Element tupleElem = tuples.get(j);

			List<Element> valueElems = getChildrenByTagName(tupleElem, "value");
			int cardinality = valueElems.size();
			if(cardinality == 0)
				continue;
			
			Element valueElem = valueElems.get(0);
			String subcatNodeId = id + "-t" + (j+1);
			
			String nodeType;
			if(cardinality == 1){
			    nodeType = "item";
			} else {
				nodeType = "folder";
			}
			result.add(processValueNode(valueElem, nodeType, subcatNodeId));
		}
	}
	
	private void processTuple(String id, Element nodeToExpand, List<String> result)
	{
		List<Element> valueElems = getChildrenByTagName(nodeToExpand, "value");
		int cardinality = valueElems.size();
		if(cardinality <= 1)
			throw new RuntimeException("unexpected "+cardinality);
		
		for(int k = 1; k < cardinality; k++){
			Element valueElem = valueElems.get(k);
			result.add(processValueNode(valueElem, "item", id + "-t" + (k+1)));
		}
	}
}
