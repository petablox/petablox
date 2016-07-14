package stamp.reporting;

import java.io.*;
import java.util.*;

public class ClassIndexGenerator
{
	private File rootPath;
	private File outPath;
	private List<File> srcPaths;

	public ClassIndexGenerator(String srcPath, String rootPath, String outPath)
	{
		this.srcPaths = new ArrayList();
		for(String dirName : srcPath.split(":"))
			this.srcPaths.add(new File(dirName));
		this.rootPath = new File(rootPath);
		this.outPath = new File(outPath);
		System.out.println("CallIndexGenerator "+srcPath+" "+rootPath);
	}

	private boolean isJavaDir(File dir)
	{
		for(File f : dir.listFiles()) {
			if(f.isDirectory()){
				if(isJavaDir(f))
					return true;
			} else {
				String fname = f.getName();
				if(fname.endsWith(".java") && Character.isLetter(fname.charAt(0))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isJimpleDir(File dir)
	{
		for(File f : dir.listFiles()) {
			if(f.isDirectory()){
				if(isJimpleDir(f))
					return true;
			} else {
				String fname = f.getName();
				if(fname.endsWith(".jimple") && Character.isLetter(fname.charAt(0))) {
					return true;
				}
			}
		}
		return false;
	}

	private List<String> getString(String pkgName, TreeSet<String> subPkgs, TreeSet<String> javaFiles)
	{
		List<String> result = new ArrayList();
		for(String subPkg : subPkgs){
			if(pkgName != null)
				subPkg = pkgName+"."+subPkg;
			result.add("{\"name\":\""+subPkg+"\",\"type\":\"folder\"}");
		}

		String d = pkgName == null ? "" : pkgName.replace('.','/')+"/";
		for(String jf : javaFiles){
			result.add("{\"name\":\""+jf+"\",\"type\":\"item\",\"file\":\""+d+jf+"\"}");
		}
		return result;
	}

	private void collect(File rootDir, String pkgName, TreeSet<String> subPkgs, TreeSet<String> javaFiles) 
	{
		File dir = rootDir;
		if(pkgName != null)
			dir = new File(dir, pkgName.replace('.','/'));
		if(!dir.exists())
			return;

		System.out.println("dir: "+dir);
		try {
			for(File f : dir.listFiles()) {
				if(f.isDirectory()){
					if(isJavaDir(f)){
						subPkgs.add(f.getName());
					}
			   } else {
				   String fname = f.getName();
				   if(fname.endsWith(".java") && Character.isLetter(fname.charAt(0))) {
					   javaFiles.add(fname);
				   }
			   }
			}
		} catch(Exception e) {
			throw new Error(e);
		}
	}

	private void collectJimple(File rootDir, String pkgName, TreeSet<String> subPkgs, TreeSet<String> jimpleFiles) 
	{
		File dir = rootDir;
		if(pkgName != null)
			dir = new File(dir, pkgName.replace('.','/'));
		if(!dir.exists())
			return;

		System.out.println("dir: "+dir);
		try {
			for(File f : dir.listFiles()) {
				if(f.isDirectory()){
					if(isJimpleDir(f)){
						subPkgs.add(f.getName());
					}
			   } else {
				   String fname = f.getName();
				   if(fname.endsWith(".jimple") && Character.isLetter(fname.charAt(0))) {
					   jimpleFiles.add(fname);
				   }
			   }
			}
		} catch(Exception e) {
			throw new Error(e);
		}
	}

	public String generate(String type, String pkgName)
	{
		TreeSet<String> subPkgs = new TreeSet();
		TreeSet<String> javaFiles = new TreeSet();

		if(type.equals("app")){
			for(File dir : srcPaths){
				collect(dir, pkgName, subPkgs, javaFiles);
			}
		} else if(type.equals("model")){
			File dir = new File(rootPath, "models/src");
			collect(dir, pkgName, subPkgs, javaFiles);
		} else if(type.equals("framework")){
			File dir = new File(rootPath, "models/api-16/gen");
			collect(dir, pkgName, subPkgs, javaFiles);
		} else if(type.equals("jimple")) {
			File dir = new File(outPath, "jimple");
			collectJimple(dir, pkgName, subPkgs, javaFiles);
		}
		
		List<String> result = getString(pkgName, subPkgs, javaFiles);
		
		StringBuilder builder = new StringBuilder("[");
		for(Iterator<String> it = result.iterator(); it.hasNext();){
			builder.append(it.next());
			if(it.hasNext())
				builder.append(",");
		}
		String ret = builder.append("]").toString();
		//System.out.println(ret);
		return ret;
	}
}
  
