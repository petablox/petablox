package stamp.app;

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;


import java.util.*;

public class StringXml
{
	private Map<String,String> idToStringValue = new HashMap();
	//new File(resDir, "values/strings.xml")

	public StringXml(File stringXmlFile)
	{
		if(!stringXmlFile.exists())
			return;
		try{
			File tmpFile = File.createTempFile("stamp_android_string_xml", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{stringXmlFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			stringXmlFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Reader reader =
				new InputStreamReader(new FileInputStream(stringXmlFile), "Cp1252");
			Document document = builder.parse(new InputSource(reader));

			XPath xpath = XPathFactory.newInstance().newXPath();

			NodeList nodes = (NodeList)
				xpath.evaluate("/resources/string", document, XPathConstants.NODESET);
			//System.out.println("nodes.size() = "+nodes.getLength());
			for(int i = 0; i < nodes.getLength(); i++) {
				Element elem = (Element) nodes.item(i);
				//System.out.println("++++ "+node.getNodeName());
				String id = elem.getAttribute("name");
				String value = elem.getTextContent();
				idToStringValue.put(id, value);
				//System.out.println("## "+id+" "+value);
			}
		}catch(Exception e){
			throw new Error(e);
		}
	}
	
	public String stringValueFor(String id)
	{
		return idToStringValue.get(id);		
	}
}