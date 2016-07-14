package stamp.summaryreport;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.Scene;
import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.AssignStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import shord.program.Program;

import stamp.analyses.string.Slicer;

import java.util.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

public class HttpParamsReport
{
	private final Map<Stmt,Map<Integer,Set<String>>> callsiteToVals = new HashMap();
	private final Map<SootMethod,Set<Stmt>> methToCallsites = new HashMap();
	private final Main main;

	HttpParamsReport(Main main)
	{
		this.main = main;
	}

	private Map<String,Object> httpMeths()
	{
		Map<String,Object> httpHeaderMeths = new HashMap();

		httpHeaderMeths.put("<org.apache.http.client.methods.HttpGet: void addHeader(java.lang.String,java.lang.String)>", 
							new int[]{1, 2});

		httpHeaderMeths.put("<org.apache.http.client.methods.HttpPost: void addHeader(java.lang.String,java.lang.String)>", 
							new int[]{1, 2});

		httpHeaderMeths.put("<org.apache.http.message.BasicHeader: void <init>(java.lang.String,java.lang.String)>", 
							new int[]{1, 2});
		
		/*
		httpHeaderMeths.put("<org.apache.http.params.DefaultedHttpParams: org.apache.http.params.HttpParams setParameter(java.lang.String,java.lang.Object)>", 
							new int[]{1, 2});
		
		httpHeaderMeths.put("<org.apache.http.params.BasicHttpParams: org.apache.http.params.HttpParams setParameter(java.lang.String,java.lang.Object)>",
							new int[]{1, 2});
		*/

		httpHeaderMeths.put("<org.apache.http.params.AbstractHttpParams: org.apache.http.params.HttpParams setBooleanParameter(java.lang.String,boolean)>", 
							new int[]{1});

		httpHeaderMeths.put("<org.apache.http.params.AbstractHttpParams: org.apache.http.params.HttpParams setDoubleParameter(java.lang.String,double)>", 
							new int[]{1});

		httpHeaderMeths.put("<org.apache.http.params.AbstractHttpParams: org.apache.http.params.HttpParams setIntParameter(java.lang.String,int)>",
							new int[]{1});

		httpHeaderMeths.put("<org.apache.http.params.AbstractHttpParams: org.apache.http.params.HttpParams setLongParameter(java.lang.String,long)>",
							new int[]{1});

        		             
        httpHeaderMeths.put("<org.apache.http.params.HttpProtocolParams: void setUserAgent(org.apache.http.params.HttpParams,java.lang.String)>",
							new int[]{1});

        httpHeaderMeths.put("<org.apache.http.params.HttpProtocolParamBean: void setUserAgent(java.lang.String)>", 
							new int[]{1});

        httpHeaderMeths.put("<android.webkit.WebSettings: void setUserAgentString(java.lang.String)>", 
							new int[]{1});

		httpHeaderMeths.put("<org.apache.http.message.BasicNameValuePair: void <init>(java.lang.String,java.lang.String)>", 
							new int[]{1, 2});
		
		//URI's and URL's
		httpHeaderMeths.put("<java.net.URI: java.net.URI create(java.lang.String)>",
							new int[]{0});

		httpHeaderMeths.put("<android.net.Uri: android.net.Uri parse(java.lang.String)>",
							new int[]{0});

		httpHeaderMeths.put("<org.apache.http.client.methods.HttpGet: void <init>(java.lang.String)>",
							new int[]{1});

		httpHeaderMeths.put("<org.apache.http.client.methods.HttpPost: void <init>(java.lang.String)>",
							new int[]{1});		

		httpHeaderMeths.put("<org.apache.http.client.methods.HttpOptions: void <init>(java.lang.String)>",
							new int[]{1});
			
		httpHeaderMeths.put("<org.apache.http.client.methods.HttpOptions: void <init>(java.lang.String)>",
							new int[]{1});

		//webview
		httpHeaderMeths.put("<android.webkit.WebView: void loadUrl(java.lang.String)>",
							new int[]{1});
		
		httpHeaderMeths.put("<android.webkit.WebView: void loadUrl(java.lang.String,java.util.Mao)>",
							new int[]{1});

		httpHeaderMeths.put("<android.webkit.WebView: void postUrl(java.lang.String,byte[])>",
							new int[]{1});

		httpHeaderMeths.put("<android.webkit.WebView: void loadData(java.lang.String,java.lang.String,java.lang.String)>",
							new int[]{1,2});

		httpHeaderMeths.put("<android.webkit.WebView: void loadDataWithBaseURL(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String)>",
							new int[]{1,2,3,4});

		//sms
		httpHeaderMeths.put("<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
							new int[]{1});
		httpHeaderMeths.put("<android.telephony.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
							new int[]{1});
		httpHeaderMeths.put("<android.telephony.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>",
							new int[]{1});


		//toString
		httpHeaderMeths.put("<java.lang.StringBuffer: java.lang.String toString()>",
							new int[]{0});
		httpHeaderMeths.put("<java.lang.StringBuilder: java.lang.String toString()>",
							new int[]{0});

		return httpHeaderMeths;
	}


	public void generate()
	{
		Scene scene = Program.g().scene();
		CallGraph cg = scene.getCallGraph();

		Slicer slicer = new Slicer();
		Map<String,Object> httpMeths = httpMeths();
		
		for(Map.Entry<String,Object> pair : httpMeths.entrySet()){
			String mSig = pair.getKey();
			if(!scene.containsMethod(mSig))
				continue;
			SootMethod m = scene.getMethod(mSig);
			int[] paramIndices = (int[]) pair.getValue();
			
			Set<Stmt> callsites = null;
			Iterator<Edge> edgeIt = cg.edgesInto(m);
			while(edgeIt.hasNext()){
				Edge edge = edgeIt.next();
				Stmt stmt = edge.srcStmt();
				SootMethod src = edge.src();
				InvokeExpr ie = stmt.getInvokeExpr();

				Map<Integer,Set<String>> paramIndexToVals = new HashMap();

				boolean nonEmpty = false;
				for(int paramIndex : paramIndices){
					Value arg;
					if(!m.isStatic())
						arg = paramIndex == 0 ? ((InstanceInvokeExpr) ie).getBase() : ie.getArg(paramIndex-1);
					else
						arg = ie.getArg(paramIndex);
					Set<String> vals = null;
					if(arg instanceof StringConstant){
						vals = new HashSet();
						vals.add(((StringConstant) arg).value);
						nonEmpty = true;
					} else if(arg instanceof Local){
						System.out.println("slice for: "+stmt + " in " + src.getSignature() + " for " + arg);
						vals = slicer.evaluate((Local) arg, stmt, src);
						if(vals != null){
							nonEmpty |= vals.size() > 0;
							for(String val : vals)
								System.out.println("val: "+val);
						}
					}
					if(vals == null){
						System.out.println("vals null!!");
						vals = Collections.emptySet();
					}
					paramIndexToVals.put(paramIndex, vals);
				}
				
				if(nonEmpty){
					callsiteToVals.put(stmt, paramIndexToVals);
					if(callsites == null){
						callsites = new HashSet();
						methToCallsites.put(m, callsites);
					}
					callsites.add(stmt);
				}
			}
		}
		slicer.finish();
		
		writeReport(httpMeths);
	}
	

	private void writeReport(Map<String,Object> httpMeths)
	{
		main.startPanel("HTTP Parameters");
		main.println("<div class=\"list-group\">");

		for(Map.Entry<SootMethod,Set<Stmt>> e : methToCallsites.entrySet()){
			SootMethod apiMethod = e.getKey();
			Set<Stmt> callsites = e.getValue();

			int[] paramIndices = (int[]) httpMeths.get(apiMethod.getSignature());

			main.println("<a href=\"#\" class=\"list-group-item\">");
			main.println(String.format("<h4 class=\"list-group-item-heading\">%s</h4>", escapeHtml4(apiMethod.getSignature())));
			main.println("<p class=\"list-group-item-text\">");
			
			main.println("<ul>");
			int callsiteCount = 1;
			for(Stmt callsite : callsites){
				Map<Integer,Set<String>> args = callsiteToVals.get(callsite);
				//List<Integer> paramIndices = new ArrayList(args.keySet());
				//Collections.sort(paramIndices);
				main.println(String.format("<li>Callsite %d</li>", callsiteCount++));
				main.println("<ul>");
				for(int paramIndex : paramIndices){
					Set<String> as = args.get(paramIndex);
					StringBuilder sb = new StringBuilder("[");
					boolean first = true;
					for(String a : as){
						if(!first)
							sb.append(",  &nbsp;");
						else
							first = false;
						sb.append(String.format("\"%s\"", escapeHtml4(a)));
					}
					sb.append("]");
					main.println(String.format("<li>Parameter %d: %s</li>", paramIndex, sb.toString()));
				}
				main.println("</ul>");
			}
			main.println("</ul>");

			main.println("</p>");
			main.println("</a>");
		}
		main.println("</div>");
		main.endPanel();
	}
}