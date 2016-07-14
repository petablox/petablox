package stamp.reporting;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import java.sql.*;
import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
@WebServlet(name="PermReader", urlPatterns={"/permreader"})
public class PermReader extends HttpServlet
{
	public PermReader() 
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
		System.out.println("dbPath: "+dbPath);

		Connection c = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			System.out.println("Opened database successfully");
			Statement stmt = c.createStatement();
			
			boolean first = true;
			String query = 
				"SELECT sha256, appName, perms "+
				"FROM perms;";
			System.out.println("Query: "+query); 
			ResultSet rs = stmt.executeQuery(query);
			StringBuilder builder = new StringBuilder("[");
			while (rs.next()) {
				//int id = rs.getInt("flowKey");
				String sha = rs.getString("sha256");
				String appName = rs.getString("appName");
				String[] perms = rs.getString("perms").split(",");

				if(!first)
					builder.append(", ");
				else
					first = false;
				builder.append("{\"sha\": \""+sha+"\", \"app\": \""+appName+"\", \"perms\": [");
				for(int i = 0; i < perms.length; i++){
					if(i > 0)
						builder.append(",");					
					builder.append("\""+perms[i]+"\"");
				}
				builder.append("]}");
			}
			builder.append("]");
			rs.close();
			stmt.close();
			c.close();
			System.out.println(builder.toString());

			PrintWriter writer = response.getWriter();
			writer.println(builder.toString());
			writer.flush();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		}
	}
}