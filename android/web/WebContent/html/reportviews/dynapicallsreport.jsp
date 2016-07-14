<%@ page import="java.util.*"%>
<%@ page import="java.io.*"%>
<%@ page import="stamp.reporting.Common"%>
<%@ page import="stamp.reporting.QueryResults"%>
<%@ page import="stamp.reporting.controller.BaseCallReportController.*"%>
<%@ page import="stamp.reporting.controller.BaseCallReportController"%>
<%!
    //   Copied from dyncallbacksreport.jsp
    // TODO: Refactor into a .jsp include or controller or something
	void makeReportMethod(JspWriter out, Method method){
		try{
		    List<MethodParameter> params = method.getParameters();
		    MethodParameter retParam = method.getReturnParameter();
		    MethodParameter param;
		    Set<String> paramValues;
		    int keyNum = retParam.getKeyNumber();
			out.println("<span class=\"mparameter mreturnparameter ");
			out.println("mparameter-key-" + keyNum + "\">");
			out.println(retParam.getType());
			out.println("</span>");
			paramValues = params.get(0).getValues();
			out.println("<a href=\"#\" ");
			if(paramValues.size() > 0)
			    out.println("rel=\"popover\" ");
			out.println("class=\"mparameter mthisparameter ");
			param = params.get(0);
			out.println("mparameter-key-" + param.getKeyNumber() + "\">");
			out.println(param.getType());
			if(paramValues.size() > 0) {
			    out.println("<span class=\"hidden-popover\" style=\"display:none;\">");
			    out.println("<ul class=\"parameter-value-list\">");
			    for(String val : paramValues) {
			        out.println("<li>" + val + "</li>");
			    }
			    out.println("</ul>");
			    out.println("</span>");
			}
			out.println("</a>");
			String file = method.getFile();
			int linenum = method.getLineNum();
			out.println("<a class=\"mname\" href=\"#\" ");
			out.println("data-file=\"" + file + "\" data-linenum=" + linenum);
			out.println(">");
			out.println("." + method.getName());
			out.println("</a>");
			out.println("(<span class=\"mparameterlist\">");
			for(int i = 1; i < params.size(); i++) {
			    param = params.get(i);
			    paramValues = param.getValues();
			    if(paramValues.size() > 0)
			        out.println("<a href=\"#\" rel=\"popover\" class=\"mparameter ");
			    else
			        out.println("<a href=\"#\" class=\"mparameter ");
			    out.println("mparameter-key-" + param.getKeyNumber() + "\">");
			    out.println(param.getType());
			    if(paramValues.size() > 0) {
			        out.println("<span class=\"hidden-popover\" style=\"display:none;\">");
			        out.println("<ul class=\"parameter-value-list\">");
			        for(String val : paramValues) {
			            out.println("<li>" + val + "</li>");
			        }
			        out.println("</ul>");
			        out.println("</span>");
			    }
			    out.println("</a>");
			    if(i != params.size()-1) out.println(",");
			}
			out.println("</span>)");
		}catch(IOException e){
			throw new Error(e);
		}
	}
%>
<%  
    String filepath = request.getParameter("filepath");
	String id = request.getParameter("id");
	String htmlIDPrefix = Common.sha1sum(filepath + id);
	
	QueryResults qr = (QueryResults) session.getAttribute("qr");
    if(qr == null){
    	qr = new QueryResults();
    	session.setAttribute("qr", qr);
    }
    
	BaseCallReportController controller = new BaseCallReportController(qr, filepath, id);
%>
<link href="/stamp/css/reportviews/callsreports.css" rel="stylesheet" />
<script type="text/javascript" src="/stamp/scripts/reportviews/callsreports.js" ></script>
<h3 class="method-list-header">API Method:</h3>
<%
	Method method = controller.getMethod();
%>
    <div class="accordion-group">
        <div class="accordion-heading">
            <div class="button-div">
                <a href="#" class="method-plus-button btn btn-inverse btn-small" data-toggle="collapse" data-target="#<%=htmlIDPrefix%>-apimethod-collapsible">
                    <i class="icon-white icon-plus"></i>
                </a> 
            </div>
            <div class="method-container">
                <%makeReportMethod(out, method);%>
            </div>
        </div>
        <div id="<%=htmlIDPrefix%>-apimethod-collapsible" class="accordion-body collapse">
          <div class="accordion-inner">
            By call site:
<%
                Map<SrcPosition, Method> callsites = method.filterByCallsite();
                for(SrcPosition pos : callsites.keySet())
                {
                    Method m = callsites.get(pos);
%>
                    <div class="callsite">
                        <a class="callsite-position" data-file="<%=pos.getFile()%>" data-linenum="<%=pos.getLine()%>" >
                            <%=pos.toString()%>
                        </a>:
                        <div class="callsite-method-container method-container">
                            <%makeReportMethod(out, m);%>
                        </div>
                    </div>
<%
                }
%>
          </div>
        </div>
    </div>
