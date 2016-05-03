package stamp.submitting;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.sql.*;
import java.util.Set;
import java.util.HashSet;
import java.io.StringReader;
import java.io.BufferedReader;

@WebServlet(name="PolicyServlet", urlPatterns={"/policyServlet"})
public class PolicyServlet extends HttpServlet 
{

	private static Logger logger = Logger.getLogger(PolicyServlet.class.getName());

	public PolicyServlet() 
	{
		super();
	}

	protected String select()
	{
		Connection c = null;
		Statement stmt = null;
		try {
			String path = getServletContext().getRealPath("/");
			path += "../../../stamp_output/";
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:"+path+"policy.db");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT DISTINCT policyName FROM policies;" );
			Set<String> policyNames = new HashSet<String>();
			while ( rs.next() ) {
				String policyName = rs.getString("policyName");
				policyNames.add(policyName);
				System.out.println( "POLICYNAME = " + policyName );
			}
			rs.close();
			stmt.close();
			c.close();
			String result = "";
			for (String s : policyNames) {
				result += s+'\n';
			}
			return result;
		} catch ( Exception e ) {
			System.out.println("FAILED database open");
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		return "";
	}

	protected String policy(String policyName) 
	{
		Connection c = null;
		Statement stmt = null;
		try {
			String path = getServletContext().getRealPath("/");
			path += "../../../stamp_output/";
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:"+path+"policy.db");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT * FROM policies WHERE policyName='"+policyName+"'" );
			String param = "";
			StringBuilder sb = new StringBuilder();
			while ( rs.next() ) {
				String active = Integer.toString(rs.getInt("active"));
				String sourceName = rs.getString("sourceName");
				String sourceParamName = rs.getString("sourceParamRaw");
				String sinkName = rs.getString("sinkName");
				String sinkParamName = rs.getString("sinkParamRaw");
				param = active+' '+sourceName+' '+sourceParamName+' '+sinkName+' '+sinkParamName;
				System.out.println("PARAM "+param);
				sb.append(param);
				sb.append('\n');
			}
			rs.close();
			stmt.close();
			c.close();
			return sb.toString();
		} catch ( Exception e ) {
			System.out.println("FAILED database open");
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
		System.out.println("Operation done successfully");
		return "";
	}

	/**
	 * GET requests expected include:
	 *  (1) Request for list of known srcs or sinks
	 */
	protected void doGet (HttpServletRequest request,
		HttpServletResponse response) 
	throws ServletException, IOException
	{
		String path = getServletContext().getRealPath("/scripts/");
		if (request.getParameter("annot") != null) {
			String filename = "";
			if (request.getParameter("annot").equals("Sources")) {
				filename = path+"/srcClass.xml";
			} else if (request.getParameter("annot").equals("Sinks")) {
				filename = path+"/sinkClass.xml";
			} else {
				return;
			}
			FileInputStream in = new FileInputStream(filename);
			OutputStream out = response.getOutputStream();
			byte[] buffer = new byte[4096];
			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}
			in.close();
			out.flush();

		} else if (request.getParameter("policies") != null) {
			if (request.getParameter("policies").equals("all")) {
				response.setContentType("text/plain");
				OutputStream out = response.getOutputStream();
				char[] buffer = new char[4096];
				int length;
				StringReader in = new StringReader(select());
				while ((length = in.read(buffer)) > 0) {
					out.write(new String(buffer).getBytes(), 0, length);
				}
				in.close();
				out.flush();
			} 
		} else if (request.getParameter("policyName") != null) {
			String policyName = request.getParameter("policyName");
			response.setContentType("text/plain");
			StringReader in = new StringReader(policy(policyName));
			OutputStream out = response.getOutputStream();
			char[] buffer = new char[4096];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(new String(buffer).getBytes(), 0, length);
			}
			in.close();
			out.flush();
		} 
	}

	/**
	 *
	 */
	protected void doPost (HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("text/plain");
		System.out.println(request.getParameter("sourceName"));
		Connection c = null;
		try {
			String path = getServletContext().getRealPath("/");
			path += "../../../stamp_output/";
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:"+path+"policy.db");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			String policyName = request.getParameter("policyName");
			String delete = request.getParameter("delete");
			if (delete != null && (delete.equals("before_update") || delete.equals("just_delete"))) {
				PreparedStatement stmt = c.prepareStatement("DELETE FROM policies WHERE policyName= ? ");
				stmt.setString(1, policyName);
				stmt.executeUpdate();
				stmt.close();
				c.commit();
				if (delete.equals("just_delete")) {
					c.close();
					return;
				}
			}

			PreparedStatement stmt2 = c.prepareStatement("INSERT INTO policies (policyName,active,sourceName,sourceParamRaw,sinkName,sinkParamRaw) "+
															"VALUES (?,?,?,?,?,?);");
			String active = request.getParameter("active");
			String sourceName = request.getParameter("sourceName");
			String sourceParamRaw = request.getParameter("sourceParamRaw");
			String sinkName = request.getParameter("sinkName");
			String sinkParamRaw = request.getParameter("sinkParamRaw");
			stmt2.setString(1,policyName);
			stmt2.setInt(2,Integer.parseInt(active));
			stmt2.setString(3,sourceName);
			stmt2.setString(4,sourceParamRaw);
			stmt2.setString(5,sinkName);
			stmt2.setString(6,sinkParamRaw);
			stmt2.addBatch();
			stmt2.executeBatch();
			stmt2.close();
			c.commit();
			c.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);
		}
	}
}
