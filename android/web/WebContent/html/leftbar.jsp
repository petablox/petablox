<%!
	void makeTree(String id, JspWriter out){
		try{
			out.println("<div id=\""+id+"\" class=\"tree\">");
			out.println("<div class=\"tree-folder\" style=\"display:none;\">");
			out.println("<div class=\"tree-folder-header\">");
			out.println("<div class=\"tree-folder-icon\">");
			out.println("<i class=\"icon-plus-sign\"></i>");
			out.println("</div>");
			out.println("<div class=\"tree-folder-name\"></div>");
			out.println("</div>");
			out.println("<div class=\"tree-folder-content\"></div>");
			out.println("<div class=\"tree-loader\" style=\"display:none\"></div>");
			out.println("</div>");
			out.println("<div class=\"tree-item\" style=\"display:none;\">");
			out.println("<i class=\"tree-dot\"></i>");
			out.println("<div class=\"tree-item-name\"></div>");
			out.println("</div>");
			out.println("</div>");
		}catch(IOException e){
			throw new Error(e);
		}
	}
%> 
<%@ page import="stamp.reporting.QueryResults"%>
<%
	QueryResults qr = (QueryResults) session.getAttribute("qr");
	if(qr == null){
		qr = new QueryResults();
		session.setAttribute("qr", qr);
	}
%>
<div id="leftbartab-0">
	<span class="label label-info">
		App Classes
	</span>
	<%
	makeTree("AppHierarchy", out);
	%>
</div>
<div id="leftbartab-1">
	<span class="label label-info">
		Model Classes
	</span>
	<%
	makeTree("ModelsHierarchy", out);
	%>
</div>
<div id="leftbartab-2">
	<span class="label label-info">
		Framework Classes
	</span>
	<%
	makeTree("FrameworkHierarchy", out);
	%>
</div>
<div id="leftbartab-3">
	<span class="label label-info">
		Jimple Code
	</span>
	<%
	makeTree("JimpleHierarchy", out);
	%>
</div>
<%
	i = 4; 
	int j = 0;
	List<String> sources = new ArrayList<String>();	
	List<String> sinks = new ArrayList<String>();
	for(Map.Entry<String,String> entry : titleToFileName.entrySet()){
		String title = entry.getKey();
		String resultFileName = entry.getValue();
%>
		<div id="leftbartab-<%=i%>">
			<span class="label label-info"><%=title%></span>
			<%			
			if(title.equals("Source-to-sink Flows")){
			%>
				<div id='<%="ResultTree"+j++%>' class='result-container'>
					<%
					String result = qr.querySrcSinkFlows(resultFileName);
					if(result.trim().length() == 0){
					%>
						"No flows!"
					<%		
					} else {
					%>
						<ul class="unstyled">
					<%
						String[] flows = result.split(",");
						for(String flow : flows){
						        //int hashIndex = flow.indexOf('#');
							//String src = flow.substring(0, hashIndex);
							//String sink = flow.substring(hashIndex+1);

							String[] tokens = flow.split("#");
							String src = tokens[0];
							String sink = tokens[1];
							String weight = tokens[2];
							
							int flowVizTabId = 4 + titleToFileName.entrySet().size() + sources.size();
							sources.add(src);
							sinks.add(sink);
						%>	
							<li><%=src%> <i class='icon-arrow-right'></i> <%=sink%> (<%=weight%>) <a href="#" id="showtab-<%=flowVizTabId%>">viz</a>
						<%
						}
						%>
						</ul>
					<%
					}
					%>
				</div>
			<%
			} else {
				makeTree("ResultTree"+j++, out);
			}
			%>
		</div>
<%
		i++;
	}

	String outPath2 = props.getProperty("outPath");
	for(int k=0; k<sources.size(); k++) {
%>
		<div id="leftbartab-<%=i++%>">
		     <%=qr.getSrcSinkFlowBody(sources.get(k), sinks.get(k),outPath2)%>
		</div>
<%		
	}
%>