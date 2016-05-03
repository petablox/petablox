<%@ page import="java.util.*, org.json.*, stamp.reporting.*"%>
<%
	String outPath = (String)session.getAttribute("outPath");
	
	String chordSig = request.getParameter("chordSig");
	String type = request.getParameter("type");
	String filePath = request.getParameter("filePath");
	String lineNum = request.getParameter("lineNum");
	
	List<String> callees = new ArrayList();
	QueryCallGraph qcg = (QueryCallGraph) session.getAttribute("qcg");
	if(qcg == null){
		qcg = new QueryCallGraph(outPath);
		session.setAttribute("qcg", qcg);
	}
	
	//qcg.getCallees(chordSig, filePath, lineNum, callees);
	callees = qcg.getCalleesChordSigs(chordSig, filePath, lineNum);
	JSONArray json = new JSONArray(callees);
	String jsonString = json.toString();
%>
	
<%= jsonString %>
	