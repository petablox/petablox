package stamp.reporting;

import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

import java.util.*;
import java.io.*;


public class QueryObservedParams{

	private Document document;
	private XPath xpath;

	public QueryObservedParams(String path){
		if(path == ""){
			return;
		}

		try{
			File xml = new File(path);
			if(!xml.isFile())
				return;

			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
			xpath = XPathFactory.newInstance().newXPath();
		}catch(Exception e){
			return;
		}		
	}

	// Result contains a mapping from Integer index to a list of Parameter values
	public SortedMap<Integer, ParameterValueSet> getParams(String chordSig){
		if(document == null){
			return null;
		}

		SortedMap<Integer, ParameterValueSet> params = new TreeMap<Integer, ParameterValueSet>();

		// Get the <inspected_method> element that matches the signature
		String query = "//inspected_method[callee/chord_sig=\""+chordSig+"\"]";
		try{
			NodeList nodes = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET);
			int n = nodes.getLength();
			for(int i = 0; i < n; i++){
				Element e = (Element) nodes.item(i);
				getParamsFromElement(e, params);
			}
		} catch(Exception e){
			return null;
		}

		return params;
	}

	public SortedMap<Integer, ParameterValueSet> getParamsFromElement(Element e, SortedMap<Integer, ParameterValueSet> params){
		
		// Get the set of param tags below e
		NodeList nodes = e.getElementsByTagName("param");
		int n = nodes.getLength();
		for(int i = 0; i < n; i++){
			
			Element paramElement = (Element) nodes.item(i);

			// Get the index tag
			NodeList indexes = paramElement.getElementsByTagName("index");
			if(indexes.getLength() != 1)
				continue;

			Node index = indexes.item(0);
			Node indexValueNode = index.getFirstChild();

			if(indexValueNode == null)
				continue;

			Integer indexValue = new Integer(indexValueNode.getNodeValue());

			// Get the parameter value
			NodeList values = paramElement.getElementsByTagName("value");
			if(values.getLength() != 1)
				continue;

			Node value = values.item(0);
			Node valueValueNode = value.getFirstChild();

			if(valueValueNode == null)
				continue;

			String paramValue = valueValueNode.getNodeValue();

			// Add to our data structure
			if(!params.containsKey(indexValue)){
				params.put(indexValue, new ParameterValueSet());
			} 
			params.get(indexValue).addValue(paramValue);
		}

		return params;
	}
}


