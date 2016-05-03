package stamp.reporting;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import java.sql.*;
import java.util.*;
import java.io.*;

@WebServlet(name="DBReader", urlPatterns={"/dbreader"})
public class DBReader extends HttpServlet
{
	private String[] dataTypes;
	private Map<String,AppData> shaToAppData = new HashMap();


	public DBReader() 
	{
		super();
	}

	/**
	 * GET requests expected include:
	 *   > Request for list of known srcs or sinks
	 *  
	 */
	protected void doGet (HttpServletRequest request,
		HttpServletResponse response) 
	throws ServletException, IOException
	{
		String dbPath = request.getParameter("dbpath");
		String formatFilePath = request.getParameter("formatfilepath");
		System.out.println("dbPath: "+dbPath);

		Connection c = null;
		try {

			readFormat(formatFilePath);

			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			System.out.println("Opened database successfully");
			Statement stmt = c.createStatement();

			StringBuilder types = new StringBuilder();
			for(int i = 0; i < dataTypes.length; i++){
				if(i > 0)
					types.append(", ");
				types.append('\'').append(dataTypes[i]).append('\'');
			}

			String query = 
				"SELECT sha256, appName, srcDesc, sinkDesc, sinkClass "+
				"FROM flows "+
				"WHERE sinkClass in ('OnDevice', 'OffDevice') and srcDesc in ("+types.toString()+");";
			System.out.println("Query: "+query); 
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				//int id = rs.getInt("flowKey");
				String sha = rs.getString("sha256");
				AppData ad = shaToAppData.get(sha);
				if(ad == null){
					String appName = rs.getString("appName");
					System.out.println(appName);
					ad = new AppData(appName);
					shaToAppData.put(sha, ad);
				}

				String srcDesc = rs.getString("srcDesc");
				String sinkDesc = rs.getString("sinkDesc");
				String sinkClass = rs.getString("sinkClass");
				ad.setFlow(srcDesc, sinkDesc, sinkClass);

				//String sinkLabel = rs.getString("sinkLabel");
				//System.out.println( appName + " " + sourceLabel +" " +sinkLabel);
				//writer.println(appName + " " + sourceLabel +" " +sinkLabel);
			}
			rs.close();
			stmt.close();
			c.close();

			StringBuilder builder = new StringBuilder("[");
			builder.append("[ "+types.toString().replace('\'', '"')+" ]");

			boolean first = true;
			for(Map.Entry<String,AppData> e : shaToAppData.entrySet()){
				String sha = e.getKey();
				AppData ad = e.getValue();
				String appName = ad.appName;
				String[] sinkDescs = ad.sinkDescs;
				String[] sinkClasses = ad.sinkClasses;

				builder.append(", ");

				builder.append("{\"sha\": \""+sha+"\", \"app\": \""+appName+"\", \"flows\": [");
				for(int i = 0; i < sinkDescs.length; i++){
					if(i > 0)
						builder.append(", ");
					builder.append("[");
					String sinkDesc = sinkDescs[i];
					String sinkClass = sinkClasses[i];
					if(sinkDesc != null){
						String[] descTokens = sinkDesc.split(",");
						String[] classTokens = sinkClass.split(",");
						for(int j = 0; j < descTokens.length; j++){
							if(j > 0)
								builder.append(",");
							builder.append("[\"" + descTokens[j] +"\",\""+classTokens[j]+"\"]");
						}
					}
					builder.append("]");
				}						
				builder.append("]}");
			}
			builder.append("]");
			System.out.println(builder.toString());

			PrintWriter writer = response.getWriter();
			writer.println(builder.toString());
			writer.flush();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		}
	}

	private class AppData
	{
		String[] sinkClasses;
		String[] sinkDescs;
		String appName;
		
		AppData(String appName){
			this.appName = appName;
			int n = DBReader.this.dataTypes.length;
			this.sinkClasses = new String[n];
			this.sinkDescs = new String[n];
		}
		
		void setFlow(String dt, String sinkDesc, String sinkClass){
			String[] dtypes = DBReader.this.dataTypes;
			for(int i = 0; i < dtypes.length; i++){
				if(dtypes[i].equals(dt)){
					if(sinkDescs[i] != null){
						if(sinkDescs[i].indexOf(sinkDesc) < 0){
							sinkDescs[i] += ","+sinkDesc;
							sinkClasses[i] += ","+sinkClass;
						}
					} else {
						sinkDescs[i] = sinkDesc;
						sinkClasses[i] = sinkClass;
					}
					break;
				}
			}
		}
	}

	private void readFormat(String formatFilePath) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(formatFilePath));
		String line;
		List<String> list = new ArrayList();
		while((line = reader.readLine()) != null){
			list.add(line.trim());
		}
		this.dataTypes = list.toArray(new String[0]);
	}
}