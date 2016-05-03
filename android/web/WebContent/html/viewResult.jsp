<%@ page import="stamp.reporting.QueryResults"%>
<%
	String rootPath = (String) session.getAttribute("rootPath");
	String srcPath = (String) session.getAttribute("srcPath");
	
	
	QueryResults qr = (QueryResults) session.getAttribute("qr");
    if(qr == null){
    	qr = new QueryResults();
    	session.setAttribute("qr", qr);
    }

	String resultFileName = request.getParameter("resultFileName");
	String nodeId = request.getParameter("nodeId");
	String result = qr.query(resultFileName, nodeId);
	out.println(result);
%>
 
