package petablox.android;

import javax.xml.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Commandline;

import java.util.*;
import java.io.*;
import java.util.zip.*;

public class StampBuildTask extends Task
{
	private String appDirPropName;
	private String absoluteAppPath;
	private String stampOutDir;
	private String libDirs;
	private String srcPathPropName;
	private String genPathPropName;
	private String classesPathPropName;
	private String libJarsPathPropName;
	private String apkPathPropName;
	private String annotJar;

	private static final String PROPS_FILE_NAME = "stamp.app.properties";

	public void execute() throws BuildException
	{
		File dir = appDir();
		if(!dir.exists() || !dir.isDirectory())
			throw new BuildException("app dir path "+dir.getAbsolutePath()+" does not exists+");
		
		readProjectProperties(dir);
		updateProjects(dir);
		writeCustomRulesFiles(dir);
		buildApp(dir);
		postProcessAppConfigFile(dir);
	}

	private File appDir()
	{
		File appDir = null;
		if(absoluteAppPath.endsWith(".zip")){
			File outDir = new File(stampOutDir);

			Set<String> existingFiles = new HashSet();
			for(File f : outDir.listFiles())
				existingFiles.add(f.getAbsolutePath());

			try{
				int DEFAULT_BUFFER_SIZE = 10240; // 10KB.
				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				ZipInputStream zis = 
					new ZipInputStream(new BufferedInputStream(new FileInputStream(absoluteAppPath), DEFAULT_BUFFER_SIZE));
				ZipEntry ze = zis.getNextEntry();
				if(ze == null)
					throw new BuildException(absoluteAppPath + " is empty.");
				
				while(ze != null){
					String fileName = ze.getName();
					File newFile = new File(outDir, fileName);
					newFile.delete();
					newFile.getParentFile().mkdirs();
					if(ze.isDirectory())
						newFile.mkdirs();
					else{
						OutputStream output = new BufferedOutputStream(new FileOutputStream(newFile), DEFAULT_BUFFER_SIZE);   
						for (int length = 0; ((length = zis.read(buffer)) > 0);) 
							output.write(buffer, 0, length);
						output.close();   
					}
					ze = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();				
			}catch(IOException e){
				throw new BuildException(e);
			}
			
			for(File f : outDir.listFiles()){
				System.out.println(f.getAbsolutePath());
				if(existingFiles.contains(f.getAbsolutePath()))
					continue;
				if(appDir == null){
					appDir = f;
				} else {
					throw new BuildException(absoluteAppPath + " is expected to contain only one dir.");
				}
			}
		} else {
			appDir = new File(absoluteAppPath);
		}
		if(!appDir.isDirectory())
			throw new BuildException(absoluteAppPath + " is neither a zip file nor a directory");
		String appDirPath;
		try{
			appDirPath = appDir.getCanonicalPath();
		}catch(IOException e){
			throw new BuildException(e.getMessage());
		}
		getProject().setProperty(appDirPropName, appDirPath);
		return appDir;
	}

	private void updateProjects(File appDirectory) throws BuildException
	{
		updateProject(appDirectory, true);
		
		//update library projects
		if(libDirs == null)
			return;
		for(String ld : libDirs.split(":")){
			File d = new File(ld);
			if(!d.exists() || !d.isDirectory())
				continue;
			updateProject(d, false);
		}
	}

	private void updateProject(File dir, boolean isApp) throws BuildException
	{
		ExecTask exe = new ExecTask(this);
		exe.setExecutable("android");
		exe.setDir(dir);
		exe.setSpawn(false);
		Commandline.Argument arg = exe.createArg();
		arg.setLine("update "+(isApp ? "" : "lib-")+"project --path . --target "+getProject().getProperty("stamp.androidtarget"));
		exe.execute();		

		Copy copy = new Copy();
		copy.setFile(new File(annotJar));
		copy.setTodir(new File(dir.toString()+"/libs")); //TODO: remove hard-coded libs
		copy.setOverwrite(true);
		copy.execute();
	}

	/*
	  reads the project.properties in the app dir (if it exists) to collect
	  paths to library projects
	 */
	private void readProjectProperties(File appDirectory) throws BuildException
	{
		if(this.libDirs != null){
			//user has explicitly set the lib paths through "libDirs" property 
			return;
		}

		File propsFile = new File(appDirectory, "project.properties");
		if(!propsFile.exists())
			return;

		Properties props = new Properties();
		try{
			props.load(new FileInputStream(propsFile));
		}catch(IOException e){
			throw new BuildException(e);
		}

		int i1 = "android.library.reference.".length();
		SortedMap<Integer,String> libPaths = new TreeMap();
		for(String key : props.stringPropertyNames()){
			if(!key.startsWith("android.library.reference."))
				continue;
			File libDir = new File(appDirectory, props.getProperty(key));
			if(!libDir.exists() || !libDir.isDirectory())
				continue;
			String absPath;
			try{
				absPath = libDir.getCanonicalPath();
			}catch(IOException e){
				throw new BuildException(e);
			}
			int id = Integer.parseInt(key.substring(i1));
			libPaths.put(id, absPath);
		}
		this.libDirs = join(libPaths.values());
	}

	private void postProcessAppConfigFile(File appDirectory) throws BuildException
	{
		Properties props = new Properties();
		try{
			props.load(new FileInputStream(new File(appDirectory, PROPS_FILE_NAME)));
		}catch(IOException e){
			throw new BuildException(e);
		}
		
		String sdkDir = props.getProperty("stamp.sdk.dir");
		String[] libJars1 = props.getProperty("stamp.lib.jars").split(":");
		List<String> libJars2 = new ArrayList();
		if(libJars1.length > 0){
			for(String libJar : libJars1){
				if(libJar.startsWith(sdkDir)){
					//we will have stubs for these
					continue;
				}
				libJars2.add(libJar);
			}
		}

		getProject().setProperty(libJarsPathPropName, join(libJars2));

		getProject().setProperty(classesPathPropName, props.getProperty("stamp.classes.path"));
		getProject().setProperty(apkPathPropName, props.getProperty("stamp.apk.path"));

		SortedMap<Integer,String> srcPath = new TreeMap();
		SortedMap<Integer,String> genPath = new TreeMap();
		int i1 = "stamp.src.path.".length();
		int i2 = "stamp.gen.path.".length();
		for(String key : props.stringPropertyNames()){
			//System.out.println("DEBUG "+key+ " "+props.getProperty(key));
			if(key.startsWith("stamp.src.path.")){
				int id = Integer.parseInt(key.substring(i1));
				srcPath.put(id, props.getProperty(key));
			} else if(key.startsWith("stamp.gen.path.")){
				int id = Integer.parseInt(key.substring(i2));
				genPath.put(id, props.getProperty(key));
			}
		}
		
		getProject().setProperty(srcPathPropName, join(srcPath.values()));
		getProject().setProperty(genPathPropName, join(genPath.values()));
	}

	private void buildApp(File appDirectory) throws BuildException
	{
		/*
		ExecTask exe = new ExecTask(this);
		exe.setExecutable("ant");
		exe.setDir(new File(appDir));
		exe.setSpawn(true);
		Commandline.Argument arg = exe.createArg();
		arg.setLine("debug");
		exe.execute();
		*/
		
		Ant ant = new Ant();
		ant.setProject(getProject());
		ant.setDir(appDirectory);
		ant.setAntfile("build.xml");
		ant.setTarget("debug");
		ant.setInheritAll(false);
		ant.execute();

	}

	private void writeCustomRulesFiles(File appDirectory) throws BuildException
	{
		String appAbsPath = null;
		File f = new File(appDirectory, PROPS_FILE_NAME);
		try{
            appAbsPath = f.getCanonicalPath();
        }catch(IOException e){
			throw new BuildException("error getting absolute app path!");
        }
		f.delete();

		writeCustomRulesFile(appDirectory, appAbsPath, 0);

		if(libDirs != null && (libDirs = libDirs.trim()).length() > 0){
			String[] paths = libDirs.split(":");
			int i = 1;
			for(String path : paths){
				File dir = new File(path);
				if(!dir.exists() || !dir.isDirectory())
					continue;
				writeCustomRulesFile(dir, appAbsPath, i++);
			}
		}
	}

	private void writeCustomRulesFile(File dir, String propsFilePath, int id) throws BuildException
	{
		File f = new File(dir, "custom_rules.xml");
		if(f.exists()){
			if(!isStampFile(f))
				throw new BuildException("non-STAMP custom_rules.xml found in "+dir);
		}
		try{
			String echoStr = "\t\t<echo append=\"true\" file=\""+propsFilePath+"\">\n";
			PrintWriter writer = new PrintWriter(new FileWriter(f));
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<project name=\"stamp_custom_rules"+id+"\">");
			writer.println("\t<target name=\"-pre-compile\">");
			writer.println(echoStr+"stamp.gen.path."+id+"=${gen.absolute.dir}</echo>");
			writer.println(echoStr+"stamp.src.path."+id+"=${source.absolute.dir}</echo>");
			if(id == 0){
				//app code
				writer.println(echoStr+"stamp.classes.path=${out.classes.absolute.dir}</echo>");
				writer.println("\t\t<pathconvert refid=\"project.all.jars.path\" property=\"stamp.lib.jars\"/>");
				writer.println(echoStr+"stamp.lib.jars=${stamp.lib.jars}</echo>");
				writer.println(echoStr+"stamp.sdk.dir=${sdk.dir}</echo>");
				writer.println(echoStr+"stamp.apk.path=${out.final.file}</echo>");
			}
			//writer.println("\t\t<echoproperties destfile=\""+appAbsPath+"\" prefix=\"stamp.\"/>");
			writer.println("\t</target>");
			writer.println("</project>");
			writer.close();
		}catch(IOException e){
			throw new BuildException(e.getMessage());
		}
	}

	private String join(Collection<String> tokens)
	{
		if(tokens.isEmpty()) return "";
		Iterator<String> iter = tokens.iterator();
		StringBuilder builder = new StringBuilder(iter.next());
		while(iter.hasNext())
			builder.append(':').append(iter.next());
		return builder.toString();
	}

	private boolean isStampFile(File f) throws BuildException
	{
		try{
			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(f);
			XPath xpath = XPathFactory.newInstance().newXPath();
			Element projNode = (Element) xpath.evaluate("//project", document, XPathConstants.NODE);
			if(projNode == null)
				return false;
			String projName = projNode.getAttribute("name");
			if(projName == null)
				return false;
			return projName.startsWith("stamp_custom_rules");
		}catch(Exception e){ 
			throw new BuildException(e.getMessage());
		}
	}

	public void setAbsoluteAppPath(String p)
	{
		this.absoluteAppPath = p;
	}

	public void setStampOutDir(String p)
	{
		this.stampOutDir = p;
	}

	public void setAppDirPropName(String p)
	{
		this.appDirPropName = p;
	}
	
	public void setSrcPathPropName(String p)
	{
		this.srcPathPropName = p;
	}

	public void setGenPathPropName(String p)
	{
		this.genPathPropName = p;
	}

	public void setClassesPathPropName(String p)
	{
		this.classesPathPropName = p;
	}

	public void setLibJarsPathPropName(String p)
	{
		this.libJarsPathPropName = p;
	}

	public void setApkPathPropName(String p)
	{
		apkPathPropName = p;
	}
	
	public void setAnnotJar(String p)
	{
		this.annotJar = p;
	}
}