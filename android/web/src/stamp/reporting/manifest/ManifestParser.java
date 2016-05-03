// Author: Patrick Mutchler
// Handles parsing the manifest file
package stamp.reporting.manifest;

import java.io.*;
import java.util.*;
import org.json.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;

public class ManifestParser{
		
	private List<Permission> permissions;
	private List<EntryPoint> entryPoints;
	
	private Document doc;
	private XPath xpath;
	
	public ManifestParser(String appPath){
		try{
			permissions = new ArrayList<Permission>();
			entryPoints = new ArrayList<EntryPoint>();
			
		    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			doc = builder.parse(new File(appPath, "AndroidManifest.xml"));
			xpath = XPathFactory.newInstance().newXPath();
			
			parseManifest();
		} catch(Exception e){
			throw new Error(e);
		}
	}
	
	public List<Permission> getPermissions(){
		return permissions;
	}
	
	public List<EntryPoint> getEntryPoints(){
		return entryPoints;
	}	
	
	/**** Parsing Methods ****/
	private void parseManifest() throws Exception{	
		XPathExpression expr = xpath.compile("//manifest[1]");
		Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
		String pack = node.getAttributes().getNamedItem("package").getNodeValue();	
		
		parseComponent("//activity[intent-filter]", EntryPoint.ACTIVITY, pack);
		parseComponent("//service[intent-filter]", EntryPoint.SERVICE, pack);
		parseComponent("//receiver[intent-filter]", EntryPoint.RECEIVER, pack);
		parsePermissions();
	}	
	
	private void parseComponent(String query, String kind, String pack) throws Exception{
		XPathExpression expr = xpath.compile(query);
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		for(int i = 0; i < nodes.getLength(); i++){	
			String name = nodes.item(i).getAttributes().getNamedItem("android:name").getNodeValue();
			
			if(name.length() > 0 && name.charAt(0) == '.')
				name = pack + name;
			
			// Get all <intent-filter> elements and their actions under this component
			List<String> actions = new ArrayList<String>();
			expr = xpath.compile(query + "/intent-filter/action");
			NodeList intentFilters = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			for(int j = 0; j < intentFilters.getLength(); j++){
				actions.add(intentFilters.item(j).getAttributes().getNamedItem("android:name").
					getNodeValue().toLowerCase());
			}
			
			entryPoints.add(new EntryPoint(name, actions, kind));
		}
	}
	
	private void parsePermissions() throws Exception{
		XPathExpression expr = xpath.compile("//uses-permission");
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		for(int i = 0; i < nodes.getLength(); i++){
			String name = nodes.item(i).getAttributes().getNamedItem("android:name").getNodeValue();
			
			if(name.indexOf("android.permission.") != -1){
				name = name.substring(19); // "android.permission.".length()
				permissions.add(new Permission(name));
			} 
		}
	}
}
