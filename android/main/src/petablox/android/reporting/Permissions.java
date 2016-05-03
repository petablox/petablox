package petablox.reporting;

import java.io.*;
import java.util.*;
import java.sql.*;
import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import petablox.android.util.SHAFileChecksum;
import petablox.android.harnessgen.PersonalNamespaceContext;
import petablox.android.harnessgen.UTF8ToAnsiUtils;

/*
 * @author Saswat Anand
 */
public class Permissions
{
	public static void main(String[] args)
	{
		String apkPath = args[0];
		String dbPath = args[1];
		String apkToolOutPath = args[2];

		File manifestFile = new File(apkToolOutPath, "AndroidManifest.xml");
		List<String> perms = null;
		if(!manifestFile.exists())
			return;
		try{
			perms = readManifest(manifestFile);
		}catch(Exception e){
			System.out.println("Error encountered while reading manifest file. "+e.getMessage());
			return;
		}
		
		String sha256;
		try{
			sha256 = SHAFileChecksum.compute(apkPath);
		}catch(Exception e){
			throw new Error(e);
		}			
		
		Connection c = initDB(dbPath);
		String apkName = new File(apkPath).getName();
		
		try{
			writeToDB(c, apkName, sha256, perms);
			c.close();
		}catch(Exception e){
			throw new Error(e);
		}
	}

	private static void writeToDB(Connection c, String apkName, String sha256, List<String> perms) throws Exception
	{
		Statement stmt = c.createStatement();
		stmt.executeUpdate("DELETE FROM perms WHERE sha256 = '"+sha256+"';");
		stmt.close();
		
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < perms.size()-1; i++){
			String perm = perms.get(i);
			builder.append(perm+",");
		}
		if(perms.size() > 0)
			builder.append(perms.get(perms.size()-1));
		String permsStr = builder.toString();

		stmt = c.createStatement();
		String sql = "INSERT INTO perms (sha256,appName,perms) VALUES (" +
			"'"+sha256+"',"+ 
			"'"+apkName+"',"+ 
			"'"+permsStr+"'"+ 
			");";
		stmt.executeUpdate(sql);
		stmt.close();
	}
	
	private static List<String> readManifest(File manifestFile) throws Exception
	{
		File tmpFile = File.createTempFile("stamp_android_manifest", null, null);
		tmpFile.deleteOnExit();
		UTF8ToAnsiUtils.main(new String[]{manifestFile.getAbsolutePath(), tmpFile.getAbsolutePath()});
		manifestFile = tmpFile;
		
		DocumentBuilder builder =
			DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(manifestFile);
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new PersonalNamespaceContext());

		List<String> perms = new ArrayList();
		NodeList nodes = (NodeList)
			xpath.evaluate("/manifest/uses-permission", document, XPathConstants.NODESET);
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
			System.out.println(name);
			perms.add(name);
		}
		return perms;
	}

	private static Connection initDB(String dbPath)
	{
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");

			File f = new File(dbPath);
			boolean exists = false;
			if(f.exists()){
				c = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
				String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='perms';";
				stmt = c.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					exists = true;
				}
			} else {
				c = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			}

			if(!exists){
				stmt = c.createStatement();
				String sql = "CREATE TABLE perms " +
					"(sha256 TEXT NOT NULL," +
					" appName        TEXT    NOT NULL, " + 
					" perms          TEXT    NOT NULL, " + 
                    " UNIQUE(sha256, appName, perms) ON CONFLICT IGNORE)";
				stmt.executeUpdate(sql);
				stmt.close();
			}

			System.out.println("Opened database "+dbPath+" successfully");
			return c;
		} catch (Exception e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			throw new Error(e);
		}
	}

}
