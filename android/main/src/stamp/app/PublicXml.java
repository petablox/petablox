package stamp.app;

import javax.xml.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

/*
 * @author Saswat Anand
*/
public class PublicXml
{
	private final Map<String,Map<String,Integer>> map = new HashMap();

	public Integer layoutIdFor(String name)
	{
		return map.get("layout").get(name);
	}

	public Integer idIdFor(String name)
	{
		return map.get("id").get(name);
	}

	public PublicXml(File publicXmlFile)
	{
		try{
			File tmpFile = File.createTempFile("stamp_android_public_xml", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{publicXmlFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			publicXmlFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Reader reader =
				new InputStreamReader(new FileInputStream(publicXmlFile), "Cp1252");
			Document document = builder.parse(new InputSource(reader));

			XPath xpath = XPathFactory.newInstance().newXPath();

			NodeList nodes = (NodeList)
				xpath.evaluate("/resources/public", document, XPathConstants.NODESET);
			//System.out.println("nodes.size() = "+nodes.getLength());
			for(int i = 0; i < nodes.getLength(); i++) {
				Element elem = (Element) nodes.item(i);
				String type = elem.getAttribute("type");
				String name = elem.getAttribute("name");
				Integer id = Integer.decode(elem.getAttribute("id"));

				Map<String,Integer> nameToId = map.get(type);
				if(nameToId == null){
					nameToId = new HashMap();
					map.put(type, nameToId);
				}
				nameToId.put(name, id);
			}
		}catch(Exception e){
			throw new Error(e);
		}		
	}
}