<%@ page import="stamp.reporting.FileManager,stamp.droidrecordweb.DroidrecordProxyWeb"%>
<%
	String filepath = request.getParameter("filepath");
	boolean isModel = false;
	if(request.getParameter("isModel") != null) {
		isModel = request.getParameter("isModel").equals("true");
	} else {
		System.out.println("No flag 'isModel' set!");
	}	
	boolean useJimple = false;
	if(request.getParameter("useJimple") != null) {
	    useJimple = request.getParameter("useJimple").equals("true");
	}
	System.out.println("DEBUG: useJimple=" + request.getParameter("useJimple"));
	String lineNum = request.getParameter("lineNum");

	FileManager manager = (FileManager) session.getAttribute("manager");
	if(manager == null){
		String rootPath = (String)session.getAttribute("rootPath");
		String srcPath = (String)session.getAttribute("srcPath");
		String outPath = (String)session.getAttribute("outPath");
		String libPath = (String)session.getAttribute("libPath");
        DroidrecordProxyWeb dr = (DroidrecordProxyWeb)session.getAttribute("droidrecord");
    	manager = new FileManager(rootPath, outPath, libPath, srcPath, dr);
		session.setAttribute("manager", manager);
	}
	
	String program;
	if(useJimple) {	
		program = manager.getAnnotatedJimple(filepath, isModel);
	} else {
	        program = manager.getAnnotatedSource(filepath, isModel);
	}
%><link href="/stamp/css/viewSource.css" rel="stylesheet" /><%=program%>
