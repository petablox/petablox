<%@ page import="java.util.*"%>
<%@ page import="java.io.*"%>
<%@ page import="stamp.reporting.controller.BaseCallReportController.*"%>
<%!
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
