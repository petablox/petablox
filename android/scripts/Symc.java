import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.text.*;

public class Symc
{
    public static void main(String[] args) throws IOException
    {
		File stamp_output = new File(args[0]);
		final String prefix = new File(stamp_output, new File(args[1]).getCanonicalPath().replace('/', '_')).getCanonicalPath();
		System.out.println("prefix: "+prefix);
		File[] apkDirs = stamp_output.listFiles(new FileFilter(){
				public boolean accept(File dir){
					if(dir.getAbsolutePath().startsWith(prefix) && dir.isDirectory())
						return true;
					//System.out.println(dir);
					return false;
				}
			});
		
		final File f = new File("results-"+new SimpleDateFormat("MM.dd-HH.mm").format(new Date())+".zip");
		final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
		
		int successCount = 0;
		List<File> failedApps = new ArrayList();
		for(File apkDir : apkDirs){
			File resultsDir = new File(apkDir, "results");
			File flowFile = new File(resultsDir, "SrcSinkFlow.xml");
			if(!flowFile.exists()){
				failedApps.add(apkDir);
				continue;
			}
			
			successCount++;
			String apkName = flowFile.getAbsolutePath().replace(prefix,"").substring(1);
			apkName = apkName.substring(0, apkName.indexOf('/'));
			System.out.println(apkName);
			System.out.println(querySrcSinkFlows(flowFile));
	    
			ZipEntry e = new ZipEntry(apkName+"/SrcSinkFlow.xml");
			out.putNextEntry(e);
			
			InputStream input = new BufferedInputStream(new FileInputStream(flowFile), 10240);
			byte[] buffer = new byte[10240];
			for (int length = 0; ((length = input.read(buffer)) > 0);) {
				out.write(buffer, 0, length);
			}
			out.closeEntry();
			
			File logFile = new File(apkDir, "chord_output/log.txt");
			if(logFile.exists()){
				BufferedReader reader = new BufferedReader(new FileReader(logFile));
				String line;
				String lastLine = null;
				while((line = reader.readLine()) != null){
					lastLine = line;
				}
				reader.close();
				if(lastLine != null && lastLine.startsWith("Total time: ")){
					System.out.println("Time " + apkName + " " + lastLine.replace("Total time: ", ""));
				}
			}
		}
		out.close();
		
		System.out.println("Successfully analyzed "+successCount+" out of "+ apkDirs.length +" apks");
		for(File fa : failedApps)
			System.out.println("failed app: "+fa.getName()); 
		
    }
    
    static String querySrcSinkFlows(File xmlResult)
    {
        NodeList tuples;
        try{
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlResult);
            XPath xpath = XPathFactory.newInstance().newXPath();
            tuples = (NodeList) xpath.evaluate("/root/tuple", doc, XPathConstants.NODESET);
        }catch(Exception e){
            throw new Error(e);
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < tuples.getLength(); i++){
            Element tuple = (Element) tuples.item(i);
            Element srcElem = getFirstChildByTagName(tuple, "source");
            Element sinkElem = getFirstChildByTagName(tuple, "sink");
            String src = srcElem.getFirstChild().getNodeValue();
	    String sink = sinkElem.getFirstChild().getNodeValue();
            builder.append(src).append("\t\t").append(sink).append("\n");
        }
        return builder.toString();
    }

    public static Element getFirstChildByTagName(Element parent, String name)
    {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
		name.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }
}