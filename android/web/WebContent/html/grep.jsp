<%@ page import="java.io.*, org.json.*;"%>

<%
String srcPath = (String)session.getAttribute("srcPath");
String query = request.getParameter("query");

srcPath = srcPath.substring(0, srcPath.indexOf(":"));

Process p;
StringBuffer output = new StringBuffer();
String results = "";

try{
	p = Runtime.getRuntime().exec("grep -rin " + query + " " + srcPath);
	p.waitFor();
	
	BufferedReader reader =  new BufferedReader(new InputStreamReader(
		p.getInputStream()));
	
	String line = "";			
	while ((line = reader.readLine())!= null) {
		output.append(line + "\n");
	}
	results = output.toString();
	
} catch(Exception e){
	throw new Error(e);
}

JSONArray jsonArray = new JSONArray();
String[] lines = results.split("\n");

for(int i = 0; i < lines.length; i++){
	JSONObject json = new JSONObject();
	
	String[] split = lines[i].split(":");
	if(split.length != 3){
		continue;
	}
	
	json.put("location", split[0].substring(srcPath.length()+1));
	json.put("lineNumber", split[1]);
	json.put("text", split[2]);
	jsonArray.put(json);
}

%>
	

<!-- Span for both elements exists to make them sit on the same horizontal line -->
<span style="width:100%">	
<span class='label label-info' style="float:left"> grep results </span>
<span style="float:right"> <button type="button" class="close" onclick="close_grep()">&times;</button> </span> 
</span>
<table class='table'><tbody>

<% 
try{
for(int i = 0; i < lines.length; i++){ 
	String[] split = lines[i].split(":");
	String loc = split[0].substring(srcPath.length()+1);
	String num = split[1];
	String text = split[2];
%>
<tr><td>
	<a href="#" id="grep<%=i%>" onclick="goto(<%=i%>)" loc="<%= loc %>" num="<%= num %>"> 
		<%= loc + ": " + num %>
	</a>
	</br>
	<%= text %>
</td></tr>
<% 
} 
} catch(Exception e){
    // Bad hack to address cases when no results are found
    %>
    <tr><td>
        No results found
    </td></tr>
    <%
}
%>
	
	
</tbody></table>

<script>
    // TODO - move this to grep.js
    function close_grep(){
        $("#rightbar").html("");
    }
</script>
