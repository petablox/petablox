package stamp.reporting;

import javax.xml.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import java.util.*;
import java.io.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/*
 * @author Saswat Anand
*/
public class Main {
    private static String appHtmlPath;
    private static String modelsHtmlPath;
    private static String frameworkHtmlPath;
    private static String resultsDirPath;
    private static String outputDirPath;
    private static String docDirPath;
    private static String scriptsDirPath;
    private static String webDirPath;
    private static String appDirPath;
    private static String newAppDirPath;
    
    public static void main(String[] args) throws Exception {
	appHtmlPath = new File(args[0]).getCanonicalPath();
	modelsHtmlPath = new File(args[1]).getCanonicalPath();
	frameworkHtmlPath = new File(args[2]).getCanonicalPath();
	resultsDirPath = args[3];
	outputDirPath = args[4];
	scriptsDirPath = args[5];
	docDirPath = new File(args[6]).getCanonicalPath();
	webDirPath = args[7]+"/";
	File appDir = new File(args[8]);
	appDirPath = "apps/" + appDir.getName();

	// need to fill newAppDirPath in
	newAppDirPath = webDirPath + "apps/" + appDir.getName();

	File webDirFile = new File(webDirPath);
	File indexFile = new File(webDirPath, "index.html");
	PrintWriter pw = new PrintWriter(indexFile);
	pw.println("<form method='post' action='/web/html/index.jsp' id='loadResults'><input type='hidden' name='rootPath' value='"+webDirPath+"'/><input type='hidden' name='appPath' value='"+appDirPath+"'/></form><script>document.getElementById('loadResults').submit()</script>");
	pw.close();

	generateLeftTopFrame(new File(outputDirPath, "lefttop.html"));
    }

    private static void generateLeftTopFrame(File outputFile) throws Exception
    {
	PrintWriter pw = new PrintWriter(outputDirPath+"/reports.txt");
	for(File f : new File(resultsDirPath).listFiles()){
	    String fname = f.getName();
	    if(fname.endsWith(".xml") && Character.isLetter(fname.charAt(0))) {
		String htmlFileName = fname.replace(".xml", ".html");
		String title = generateHTMLReport(f, new File(outputDirPath, htmlFileName));
		pw.println(htmlFileName+","+title);
	    }
	}
	pw.close();
    }

    private static String generateHTMLReport(File xmlResult, File htmlFile) throws Exception {
	System.out.println("processing " + xmlResult);
	DocumentBuilder builder =
	    DocumentBuilderFactory.newInstance().newDocumentBuilder();
	Document document = null;
	//System.out.println(builder.getClass());
	document = builder.parse(xmlResult);
	XPath xpath = XPathFactory.newInstance().newXPath();

	String title = ((Node) xpath.evaluate("//title", document, XPathConstants.NODE)).getFirstChild().getNodeValue();
	PrintWriter writer = new PrintWriter(new FileWriter(htmlFile));
	writer.println("<html lang=\"eng\">");
	writer.println("<head>");
	writer.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">");
	writer.println("<script type=\"text/javascript\" src='/web/scripts/loadData.js'></script>");
	writer.println("<script type=\"text/javascript\" src='/web/scripts/mktree.js'></script>");
	writer.println("<link rel=\"stylesheet\" href='/web/css/mktree.css' TYPE=\"text/css\" MEDIA=\"screen\">");
	writer.println("</head>");
	writer.println("<body>");

	NodeList nodes = (NodeList) xpath.evaluate("//root", document, XPathConstants.NODESET);
	processCategoryNode((Element) nodes.item(0), writer);
	writer.println("</body>");
	writer.println("</html>");
	writer.close();
		
	return title;
    }

    private static void processCategoryNode(Element catNode, PrintWriter writer) throws Exception {
	List<Element> nodes = getChildrenByTagName(catNode, "value");
	boolean isRoot = nodes.size() == 0;
	if(!isRoot){
	    assert nodes.size() == 1;
	    Element value = nodes.get(0);
	    Element labelElem = getChildrenByTagName(value, "label").get(0);
	    String label = escapeHtml4(labelElem.getFirstChild().getNodeValue());
			
	    String srcFile = value.getAttribute("srcFile");
	    String lineNum = value.getAttribute("lineNum");
	    String highlightText = value.getAttribute("highlight");
			
	    label = linkToSrc(label, srcFile, lineNum, highlightText);

		String type = catNode.getAttribute("type");
        if(!type.equals("")){
            String icon = "";
            if(type.equals("class"))
                icon = "<img src=\"/web/res/class.png\" height=\"12\" width=\"12\" alt=\"Class\"/>&#32;";
            else if(type.equals("method"))
                icon = "<img src=\"/web/res/method.png\" height=\"12\" width=\"12\" alt=\"Method\"/>&#32;";
            writer.println("<li>"+icon+label);
        }
        else
            writer.println("<li>"+label);
	} else {
	    writer.println("<ul class=\"mktree\">");
	}

	List<Element> tuples = getChildrenByTagName(catNode, "tuple");
	if(tuples.size() > 0){
	    //writer.println("<li>");
	    try {
		processTuples(tuples, writer);
	    } catch(Exception e) {
		throw e;
	    }
	    //writer.println("</li>");
	}
		
	List<Element> subcats = getChildrenByTagName(catNode, "category");
	for(int j = 0; j < subcats.size(); j++){
	    if(!isRoot)
		writer.println("<ul>");
	    Element subcat = subcats.get(j);
	    processCategoryNode(subcat, writer);
	    if(!isRoot)
		writer.println("</ul>");
	}
		
	if(!isRoot){
	    writer.println("</li>");
	} else { //root
	    writer.println("</ul>");
		if(subcats.size() == 0 && tuples.size() == 0)
			writer.println("<font color=\"#A00000\">No tuples in this relation.</font>");
	}
    }

    private static void processTuples(List<Element> nodes, PrintWriter writer) throws Exception 
	{
		writer.println("<table>");
		//writer.println("<li class=\"liOpen\"> "+title);
		//writer.println("<ul>");
		String evenColor = "#d4e3e5";
		String oddColor = "#c3dde0";

		for (int i = 0; i < nodes.size(); i++) {
			Element node = nodes.get(i);
			List<Element> ns = getChildrenByTagName(node, "value");
			if(ns.size() > 0){
				writer.println("<tr><td bgcolor=\""+(i % 2 == 0 ? evenColor : oddColor)+"\">");
				writer.println("<ul>");
				for(int j = 0; j < ns.size(); j++){
					Element value = ns.get(j);
					Element labelElem = getChildrenByTagName(value, "label").get(0);
					String label = escapeHtml4(labelElem.getFirstChild().getNodeValue());
					
					String srcFile = value.getAttribute("srcFile");
					String lineNum = value.getAttribute("lineNum");
					String highlightText = value.getAttribute("highlight");
					
					label = linkToSrc(label, srcFile, lineNum, highlightText);
					writer.println("<li>"+label+"</li>");
				}
				writer.println("</ul>");
				writer.println("</td></tr>");
			}
		}
		//writer.println("</ul>");
		//writer.println("</li>");
		
		writer.println("</table>");		
    }

    public static List<Element> getChildrenByTagName(Element parent, String name) {
		List<Element> nodeList = new ArrayList<Element>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && 
				name.equals(child.getNodeName())) {
				nodeList.add((Element) child);
			}
		}
		
		return nodeList;
    }

    private static String linkToSrc(String label, String srcFile, String lineNum, String highlightText) throws Exception
    {
	if(!srcFile.endsWith(".java"))
	    return label;
	int ln = 0;
	try{
	    ln = Integer.parseInt(lineNum);
	} catch(NumberFormatException e){
	    ln = 0;
	    System.out.println("Bad line number.");
	}
	if(ln <= 0)
	    ln = 0;
	//System.out.println("** "+srcFile+" " + lineNum);
	//srcFile = srcFile.substring(0, srcFile.length()-5).concat(".html");
							
	File f = new File(newAppDirPath + "/src/", srcFile);
	if(!f.exists())
	    f = new File("models/api-16/gen/", srcFile);
	if(!f.exists())
	    return label;

	String params;
	if(highlightText.trim().equals(""))
	    params = String.valueOf(ln);
	else
	    params = ln+"="+highlightText;

	//String action = "onclick=\"top.right.document.getElementsByTagName('li')[0].setAttribute('class','L13');\"";
	//String action = "onclick=\"top.right.location.href='"+f.getCanonicalPath()+"#"+ln+"'\"";
	//return "<button "+action+">"+label+"</button>";
	//return "<a "+action + ">"+label+"</a>";
	// TODO: modify this path
	String[] splitPath = f.getCanonicalPath().split("ROOT/");
	String filepath = splitPath[splitPath.length-1];
	//String webPath = splitPath[splitPath.length-1];
	//return "<a target=\"rightframe\" href=\""+webPath+"?"+params+"\">"+label+"</a>";
	return "<a href=\"javascript:loadSource('"+filepath+"','"+String.valueOf(ln)+"','"+highlightText+"','middleframe')\" style=\"text-decoration: none\">"+label+"</a>";
    }

}
