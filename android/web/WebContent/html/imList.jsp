<%!
	String createElement(String s, String klass){
		String[] split = s.split("#");
		if(split.length < 3){
			return s;
		}
		
		String text = split[0];
		String srcLoc = split[1] + "#" + split[2]; 
		
		String chordSig = "";
		if(split.length == 4){
			chordSig = split[3]; 
		}
		
		// Hack to fix duplication in text. This should be handled elsewhere
		// but I can't figure out what is causing this -Patrick
		// stamp.stanford.malware.stamp.stanford.malware.Foo.doit =>
		// stamp.stanford.malware.Foo.doit
		
		// Remove the first (n-2)/2 elements 
		String[] splitText = text.split("\\.");
		if(splitText.length > 2){
			int cutoff = (splitText.length - 2)/2;
			text = "";
			
			for(int i = cutoff; i < splitText.length; i++){
				text += splitText[i] + ".";
			}
			text = text.substring(0, text.length()-1);
		}		
		
		return "<a href='#' class='" + klass + "' id='SrcLoc" + srcLoc + "' chordSig='" + chordSig + "'>" 
			+ text + "</a>"; 
	}
%>
<%@ page import="java.io.*,java.util.*,stamp.reporting.QueryCallGraph, stamp.reporting.*"%>
<%
	String outPath = (String)session.getAttribute("outPath");
	String chordSig = request.getParameter("chordSig");
	boolean useJimple = (Boolean)session.getAttribute("useJimple");
    String type = request.getParameter("type");

    QueryCallGraph qcg = (QueryCallGraph) session.getAttribute("qcg");
    if(qcg == null){
    	qcg = new QueryCallGraph(outPath);
    	session.setAttribute("qcg", qcg);
    }

    QueryObservedParams qop = (QueryObservedParams) session.getAttribute("qop");
    if(qop == null){
    	qop = new QueryObservedParams(outPath + "/results/observedparams.xml");
    	session.setAttribute("qop", qop);
    }


	if(type == null){
	%>
		No type found!
	<%		
	}
	else if(type.equals("method")){
		List<String> callers = new ArrayList();
		String methodName = qcg.getCallers(chordSig, callers);
		if(callers.size() == 0) {
			//no callers found
%>
			<b>This method has no known callers.</b>
<%
		} else {	 			
%>
			<span class="label label-info">method</span><br/><%=createElement(methodName, "method-link")%>
			<br/>
			<span class="label label-info">callers</span>
			<table class='table'>
<%
			for(String caller : callers){
%>
				<tr><td><%=createElement(caller, "caller-link")%></td></tr>
<%
			}
%>
			</table>

			
<% 		}
	}//end of if(type.equals("method"))
	else {
		String filePath = request.getParameter("filePath");
		String lineNum = request.getParameter("lineNum");
  		
		List<String> callees = new ArrayList();
		String callsite = qcg.getCallees(chordSig, filePath, lineNum, callees);
		if(callees.size() == 0){
			//no callees found
%>
			<b>This call-site has no known callees.</b>
<%
		} else {
%>
			<span class="label label-info">call site</span><br/><span class='value' type='method'>
				<%=createElement(callsite, "callsite-link")%>
			</span>
			<br/>
			<span class="label label-info">callees</span>
			<table class='table'>
<%		
			for(String callee : callees){
%>
				<tr><td><%=createElement(callee, "callee-link")%></td></tr>
<%			
			}
%>
			</table>	
<%		}

%>
		<span class="label label-info">Runtime parameters</span>
		<div class="droidrecord-runtime-parameters">
<%
		SortedMap<Integer, ParameterValueSet> params = new TreeMap<Integer, ParameterValueSet>();
		if(qop != null){
			params = qop.getParams(chordSig);
		}
		if(params == null || params.isEmpty()){
%>
			<p>No observed runtime params for this method</p>
<%
		}
%>
		<table class='table' id='params'>
<%
		if(params != null){
			for(Integer key : params.keySet()){
				ParameterValueSet param = params.get(key);
%>
				<tr><td><b>Values for parameter <%=key.intValue()%>:</b></td></tr> 
<%
				for(String value : param.getValues()){
%>
					<tr><td><%=value%></td></tr>
<%
				}
			}
		}
	}// end of else
%>
		</div>

<script>
	$('a[id^="SrcLoc"]').on("click",  function(event){
		event.preventDefault();	
		var srcLoc = $(this).attr("id").substring(6);
		var index = srcLoc.indexOf('#');
		var fileName = srcLoc.substring(0,index);
		var lineNum = srcLoc.substring(index+1);
		showSource(fileName, 'false', lineNum, <%=useJimple%>);
	});
</script>
