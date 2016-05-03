<%@ page import="java.util.*, org.json.*, stamp.reporting.*, stamp.reporting.manifest.*"%>
<%
	String appPath = (String)session.getAttribute("appPath");

	ManifestParser parser = (ManifestParser) session.getAttribute("parser");
	if(parser == null){
		parser = new ManifestParser(appPath);
		session.setAttribute("parser", parser);
	}
	
	List<Permission> permissions = parser.getPermissions();
	List<EntryPoint> entryPoints = parser.getEntryPoints();
	
	JSONArray jsonPermissions = new JSONArray();
	for(Permission p : permissions){
		jsonPermissions.put(p.toJSON());
	}
	
	JSONArray jsonEntryPoints = new JSONArray();
	for(EntryPoint ep : entryPoints){
		jsonEntryPoints.put(ep.toJSON());
	}
	
	JSONObject json = new JSONObject();
	json.put("permissions", jsonPermissions);
	json.put("entry points", jsonEntryPoints);
	
	String jsonString = json.toString();
%>
	
<%= jsonString %>

