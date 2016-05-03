package petablox.android.harnessgen;

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

/*
* reads AndroidManifest.xml to find out several info about the app
* @author Saswat Anand
*/
public class ParseManifest
{
	private String pkgName;

	void process(File manifestFile, Set<String> activities, Set<String> others)
	{
		try{
			File tmpFile = File.createTempFile("stamp_android_manifest", null, null);
			tmpFile.deleteOnExit();
			UTF8ToAnsiUtils.main(new String[]{manifestFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
			manifestFile = tmpFile;

			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(manifestFile);
			
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new PersonalNamespaceContext());
			
			//find package name
			Node node = (Node)
				xpath.evaluate("/manifest", document, XPathConstants.NODE);
			pkgName = node.getAttributes().getNamedItem("package").getNodeValue();
			
			findComponents(xpath, document, activities, "activity");

			for(String compType : new String[]{"service", "receiver"}){
				findComponents(xpath, document, others, compType);
			}
			
			node = (Node)
				xpath.evaluate("/manifest/application", document, XPathConstants.NODE);

			//backup agent
			Node backupAgent = node.getAttributes().getNamedItem("android:backupAgent");
			if(backupAgent != null)
				others.add(fixName(backupAgent.getNodeValue()));
			
			//application class
			Node application = node.getAttributes().getNamedItem("android:name");
			if(application != null)
				others.add(fixName(application.getNodeValue()));

		}catch(Exception e){
			throw new Error(e);
		}
	}

	private String fixName(String comp)
	{
		if(comp.startsWith("."))
			comp = pkgName + comp;
		else if(comp.indexOf('.') < 0)
			comp = pkgName + "." + comp;
		return comp;
	}


	private void findComponents(XPath xpath, Document document, Set<String> comps, String componentType) throws Exception
	{
		NodeList nodes = (NodeList)
			xpath.evaluate("/manifest/application/"+componentType, document, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			NamedNodeMap nnm = node.getAttributes();
			String name = null;
			for(int j = 0; j < nnm.getLength(); j++){
				Node n = nnm.item(j);
				if(n.getNodeName().equals("android:name")){
					name = n.getNodeValue();
					break;
				}
				//System.out.println(n.getNodeName() + " " + );
			}			
			assert name != null : node.getNodeName();
			comps.add(fixName(name));
		}
	}

	/*
	public static void main(String[] args) throws Exception
	{
		File androidManifestFile = new File(args[0]);
		String classPath = args[1];
		String androidJar = args[2];

		List<JarFile> jars = new ArrayList();
		for(String cp : classPath.split(":")){
			if(!(new File(cp).exists()))
				System.out.println("WARNING: "+cp +" does not exists!");
			else
				jars.add(new JarFile(cp));
		}

		App app = new App(androidManifestFile, classPath, androidJar);
		System.out.println(app);
		}*/
}
