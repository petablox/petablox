package petablox.android.harnessgen;

import javax.xml.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.util.*;

/*
* reads layout xml files to find out several info about the app
* @author Saswat Anand
*/
public class ParseLayout
{
	void process(File layoutDir, Set<String> callbacks, Set<String> guiElems)
	{
		File[] layoutFiles = layoutDir.listFiles(new FilenameFilter(){
				public boolean accept(File dir, String name){
					return name.endsWith(".xml");
				}
			});
		if(layoutFiles != null){
			for(File lf : layoutFiles){
				//System.out.println("processing layout "+lf);
				processLayout(lf, callbacks, guiElems);
			}
		}
	}

	private void processLayout(File layoutFile, Set<String> callbacks, Set<String> guiElems)
	{
		try{
			File tmpFile = File.createTempFile("stamp_android_layout", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{layoutFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			layoutFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Reader reader =
				new InputStreamReader(new FileInputStream(layoutFile),
									  "Cp1252");
			Document document = builder.parse(new InputSource(reader));

			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new PersonalNamespaceContext());

			findCallbacks(document, xpath, callbacks);
			
			findGuiElems(document, xpath, guiElems);
		}catch(Exception e){
			throw new Error(e);
		}
	}

	private void findGuiElems(Document document, XPath xpath, Set<String> guiElems) throws javax.xml.xpath.XPathExpressionException
	{		
		NodeList nodes = (NodeList)
			xpath.evaluate("//*", document, XPathConstants.NODESET);
		//System.out.println("nodes.size() = "+nodes.getLength());
		for(int i = 0; i < nodes.getLength(); i++) {
			//System.out.println("HELLO");
			Node node = nodes.item(i);
			//System.out.println("++++ "+node.getNodeName());
			guiElems.add(node.getNodeName());
		}
	}
	
	private void findCallbacks(Document document, XPath xpath, Set<String> callbacks) throws javax.xml.xpath.XPathExpressionException
	{
		NodeList nodes = (NodeList)
			xpath.evaluate("//*[@onClick]", document, XPathConstants.NODESET);
		
		for(int i = 0; i < nodes.getLength(); i++) {
			//System.out.println("HELLO");
			Node node = nodes.item(i);
			NamedNodeMap nnm = node.getAttributes();
			String name = null;
			for(int j = 0; j < nnm.getLength(); j++){
				Node n = nnm.item(j);
				if(n.getNodeName().equals("android:onClick")){
					callbacks.add(n.getNodeValue());
					System.out.println("CALLBACK: "+n.getNodeValue());
				}
				//System.out.println(n.getNodeName() + " " + );
			}
		}
	}
}
