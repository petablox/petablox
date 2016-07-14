package stamp.reporting;

import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import java.io.*;
import java.sql.*;
import stamp.util.SHAFileChecksum;


public class DBWriter
{
	private static LabelReader srcLabelReader;
	private static LabelReader sinkLabelReader;

	public static void main(String[] args)
	{
		String apkPath = args[0];
		String dbPath = args[1];
		String resultsPath = args[2];
		String srcClassFilePath = args[3];
		String sinkClassFilePath = args[4];

		File xmlResult = new File(resultsPath, "SrcSinkFlow.xml");
		if(!xmlResult.exists())
			return;

		NodeList flowElems;
		try{ 
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlResult);
			XPath xpath = XPathFactory.newInstance().newXPath();
			String query = "/root/category/value";
			//System.out.println("query: "+query);
			flowElems = (NodeList) xpath.evaluate(query, document, XPathConstants.NODESET); 
		}catch(Exception e){
			throw new Error(e);
		}			

		String sha256;
		try{
			sha256 = SHAFileChecksum.compute(apkPath);
		}catch(Exception e){
			throw new Error(e);
		}			

		srcLabelReader = new LabelReader(srcClassFilePath);
		sinkLabelReader = new LabelReader(sinkClassFilePath);

		Connection c = initDB(dbPath);
		String apkName = new File(apkPath).getName();

		try{
			writeToDB(c, apkName, sha256, flowElems);
			c.close();
		}catch(Exception e){
			throw new Error(e);
		}
	}

	static class LabelReader 
	{
		private XPath xpath;
		private Document doc;
		
		LabelReader(String classFilePath)
		{
			try{ 
				File classFile = new File(classFilePath);
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

	private static void writeToDB(Connection c, String apkName, String sha256, NodeList flowElems) throws Exception
	{
		int numFlows = flowElems.getLength();
		System.out.println("numFlows = "+ numFlows);
		
		Statement stmt = c.createStatement();
		stmt.executeUpdate("DELETE FROM flows WHERE sha256 = '"+sha256+"';");
		stmt.close();
		
		for(int i = 0; i < numFlows; i++){
			Element flowElem = (Element) flowElems.item(i);
			String label = getLabel(flowElem);
			String[] tokens = label.split(" ");
			String src = tokens[0];
			String sink = tokens[2];
			
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
			
			stmt = c.createStatement();
			String sql = "INSERT INTO flows (sha256,appName,srcLabel,srcDesc,srcClass,sinkLabel,sinkDesc,sinkClass) VALUES (" +
				"'"+sha256+"',"+ 
				"'"+apkName+"',"+ 
				"'"+src+"',"+ 
				"'"+srcDesc+"',"+ 
				"'"+srcClass+"',"+ 
				"'"+sink+"',"+ 
				"'"+sinkDesc+"',"+ 
				"'"+sinkClass+"'"+ 
				");";
			stmt.executeUpdate(sql);
			stmt.close();
		}

	}

	private static Connection initDB(String dbPath)
	{
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");

			File f = new File(dbPath);
			if(!f.exists()){
				c = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
				stmt = c.createStatement();
				String sql = "CREATE TABLE flows " +
					"(sha256 TEXT NOT NULL," +
					" appName        TEXT    NOT NULL, " + 
					" srcLabel       TEXT    NOT NULL, " + 
					" srcDesc        TEXT    NOT NULL, " + 
					" srcClass       TEXT    NOT NULL, " + 
					" sinkLabel      TEXT    NOT NULL, " + 
					" sinkDesc       TEXT    NOT NULL, " + 
					" sinkClass      TEXT    NOT NULL, " +
                    " UNIQUE(sha256, appName, srcLabel, srcDesc, srcClass, sinkLabel, sinkDesc, sinkClass) ON CONFLICT IGNORE)";
				stmt.executeUpdate(sql);
				stmt.close();
			} else {
				c = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			}
			System.out.println("Opened database successfully");
			return c;
		} catch (Exception e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			throw new Error(e);
		}
	}

	public static String getLabel(Element value)
	{
	    Element labelElem = getFirstChildByTagName(value, "label");
		return escapeHtml4(labelElem.getFirstChild().getNodeValue());
	}

	public static Element getFirstChildByTagName(Element parent, String name) 
	{
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && 
				name.equals(child.getNodeName())) {
				return (Element) child;
			}
		}
		return null;
    }
}