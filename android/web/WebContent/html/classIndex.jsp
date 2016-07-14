<%@ page import="stamp.reporting.ClassIndexGenerator"%>
<%
	String rootPath = (String) session.getAttribute("rootPath");
	String srcPath = (String) session.getAttribute("srcPath");
	String outPath = (String) session.getAttribute("outPath");
	
	ClassIndexGenerator cig = (ClassIndexGenerator) session.getAttribute("cig");
    if(cig == null){
    	cig = new ClassIndexGenerator(srcPath, rootPath, outPath);
    	session.setAttribute("cig", cig);
    }

	String type = request.getParameter("type");
	String pkgName = request.getParameter("pkgName");
	String index = cig.generate(type, pkgName);
	out.println(index);
%>
 