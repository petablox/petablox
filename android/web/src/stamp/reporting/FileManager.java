package stamp.reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import stamp.droidrecordweb.DroidrecordProxyWeb;
import stamp.reporting.processor.SourceProcessor;

public class FileManager {
    // path data
    private String rootPath;
    private String outPath;
    
	private List<File> srcDirs = new ArrayList<File>();
    //private StampVisitor srcVisitor;

    // set of files and corresponding info
    //private HashMap<String,SourceData> filePathToSourceData = new HashMap();
    private HashMap<String,String> filePathToAnnotatedSource = new HashMap<String,String>();
    private HashMap<String,String> modelFilePathToAnnotatedSource = new HashMap<String,String>();
    
    // set of jimple files and corresponding info
    private HashMap<String,String> filePathToAnnotatedJimple = new HashMap<String,String>();

    private DroidrecordProxyWeb droidrecord;

    public FileManager(String rootPath, String outPath, String libPath, String srcPath, DroidrecordProxyWeb droidrecord) throws IOException {
		this.rootPath = rootPath;
		this.outPath = outPath;
        
        this.droidrecord = droidrecord;
		
		List<String> srcpathEntries = new ArrayList<String>();
		for(String sd : srcPath.split(":")) {
			File d = new File(sd);
			if(d.exists()){
				srcDirs.add(d);
				srcpathEntries.add(d.getCanonicalPath());
			}
		}
		
		srcpathEntries.add(rootPath + "/models/api-16/gen");
		srcpathEntries.add(rootPath + "/models/src");

		List<String> classpathEntries = new ArrayList<String>();
		for(String j : libPath.split(":")) {
			File jar = new File(j);
			if(jar.exists()) {
				classpathEntries.add(j);
			}
		}
					
		//this.srcVisitor = new StampVisitor(srcpathEntries, classpathEntries);
    }
	
	private void add(String filePath, boolean isModel) throws Exception {	
		if(isModel) {
			File file = new File(rootPath+"/models/src", filePath);
			if(file.exists()){
				SourceProcessor sp = new SourceProcessor(file, droidrecord);
				String annotatedSource = sp.getSourceWithAnnotations();
				modelFilePathToAnnotatedSource.put(filePath, annotatedSource);
			}
			return;
		} 

		String srcMapDirPath = null;
		File file = new File(rootPath+"/models/api-16/gen", filePath);
		if(file.exists()) {
			srcMapDirPath = rootPath+"/models/api-16/srcmap";
		} else {
			for(File sd : srcDirs){
				file = new File(sd, filePath);
				if(file.exists()){
					srcMapDirPath = outPath+"/srcmap/";
					break;
				}
			}
		}

		//System.out.println("DEBUG: " + srcMapDirPath + " " + file.getCanonicalPath());
		
		if(srcMapDirPath == null)
			return;
		
		//SourceData data = srcVisitor.process(file);
		File taintedInfoFile = new File(outPath+"/results/TaintedVar.xml");
		File allReachableFile = new File(outPath+"/results/AllReachable.xml");
		File reachedFile = new File(outPath+"/results/reachedmethods.xml");

		//replace .java with .xml
		String fname = filePath.substring(0, filePath.length()-4).concat("xml");
		File srcMapFile = new File(srcMapDirPath+"/"+fname);

		SourceProcessor sp = new SourceProcessor(file, droidrecord, srcMapFile, taintedInfoFile, allReachableFile, reachedFile, filePath);
		String annotatedSource = sp.getSourceWithAnnotations();
		//System.out.println("FILEPATH: "+filePath+"\n"+annotatedSource);
		//filePathToSourceData.put(filePath, data);
		filePathToAnnotatedSource.put(filePath, annotatedSource);
    }
	
	private void addJimple(String filePath, boolean isModel) throws Exception {
		String jimpleFilePath = filePath;
		//String jimpleFilePath = filePath.substring(0, filePath.length()-4).concat("jimple").replace("/", ".");
		File jimpleFile = new File(outPath + "/jimple/" + jimpleFilePath);
		
		//System.out.println("DEBUG: " + filePath + " " + jimpleFile.getCanonicalPath());

		//SourceData data = srcVisitor.process(file);
		File taintedInfoFile = new File(outPath+"/results/TaintedVar.xml");
		File allReachableFile = new File(outPath+"/results/AllReachable.xml");
		File reachedFile = new File(outPath+"/results/reachedmethods.xml");

		//replace .jimple with .xml
		String fname = jimpleFilePath.substring(0, jimpleFilePath.length()-6).concat("xml");
		File srcMapFile = new File(outPath + "/jimple/" + fname);
		//System.out.println("DEBUG: srcmap file now at " + srcMapFile.getCanonicalPath());

		SourceProcessor sp = new SourceProcessor(jimpleFile, droidrecord, srcMapFile, taintedInfoFile, allReachableFile, reachedFile, filePath);
		String annotatedSource = sp.getSourceWithAnnotations();
		//System.out.println("FILEPATH: "+filePath+"\n"+annotatedSource);
		//filePathToSourceData.put(filePath, data);
		filePathToAnnotatedJimple.put(filePath, annotatedSource);
    }
	
	public String getAnnotatedJimple(String filePath, boolean isModel) {
		try {
			if(!filePathToAnnotatedJimple.containsKey(filePath)) {
				addJimple(filePath, isModel);
			}
			return filePathToAnnotatedJimple.get(filePath);
		} catch(Exception e) {
			throw new Error(e);
		}
	}
    
    public String getAnnotatedSource(String filePath, boolean isModel) {
		try {
			if(isModel) {
				if(!modelFilePathToAnnotatedSource.containsKey(filePath)) {
					add(filePath, isModel);
				}
				return modelFilePathToAnnotatedSource.get(filePath);
			} else {
				if(!filePathToAnnotatedSource.containsKey(filePath)) {
					add(filePath, isModel);
				}
				return filePathToAnnotatedSource.get(filePath);
			}
		} catch(Exception e) {
			throw new Error(e);
		}
    }

	public String getClassInfo(String chordSig) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			File fileNamesFile = new File(outPath+"/results/FileNames.xml");
			Document document = builder.parse(fileNamesFile);
			XPath xpath = XPathFactory.newInstance().newXPath();
			String query = "//tuple[@chordsig=\""+chordSig+"\"]";
			Element node = (Element) xpath.evaluate(query, document, XPathConstants.NODE);
			return node.getAttribute("srcFile")+","+node.getAttribute("lineNum");
		} catch(Exception e){
			throw new Error(e);
		}
	}

	public boolean isFrameworkFile(String filePath) {
		return new File(this.rootPath+"/models/api-16/gen", filePath).exists();
	}
	
	public static String readFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuilder fileBuilder = new StringBuilder();
		String line;
		while((line = br.readLine()) != null) {
			fileBuilder.append(line);
			fileBuilder.append("\n");
		}
		br.close();
		return fileBuilder.toString();
    }
	
    public static void writeFile(File file, String string) throws IOException  {
		file.getParentFile().mkdirs();
		PrintWriter pw = new PrintWriter(file);
		pw.print(string);
		pw.close();
    }
}
