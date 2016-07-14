package stamp.srcmap.sourceinfo.javainfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import soot.SootClass;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.SourceLineNumberTag;
import soot.tagkit.Tag;
import stamp.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

/**
 * @author Saswat Anand 
 */
public class JavaSourceInfo extends AbstractSourceInfo {
	private File frameworkSrcDir;
	private List<File> srcMapDirs = new ArrayList<File>();

	public JavaSourceInfo() {
		File frameworkDir = new File(System.getProperty("stamp.framework.dir"));

		frameworkSrcDir = new File(frameworkDir, "gen");
		if(!frameworkSrcDir.exists())
			throw new RuntimeException("Framework dir " + frameworkSrcDir + " does not exist");
 		
		File frameworkSrcMapDir = new File(frameworkDir, "srcmap");
	 	if(!frameworkSrcMapDir.exists())
			throw new RuntimeException("Framework dir " + frameworkSrcMapDir + " does not exist");
		srcMapDirs.add(frameworkSrcMapDir);

		String outDir = System.getProperty("stamp.out.dir");
		File appSrcMapDir = new File(outDir+"/srcmap");
		if(!appSrcMapDir.exists())
			throw new RuntimeException("Framework dir " + appSrcMapDir + " does not exist");
		srcMapDirs.add(appSrcMapDir);
	}
	
	/*
	public boolean isFrameworkClass(SootClass klass) {
		String srcFileName = filePath(klass);
		if(srcFileName == null){
			//System.out.println("srcFileName null for "+klass);
			//TODO: should we not return false here?
			return true;
		}
		boolean result = new File(frameworkSrcDir, srcFileName).exists();
		//System.out.println("isFrameworkClass " + srcFileName + " " + klass + " " + result);
		return result;
	}
	*/

	public int stmtLineNum(Stmt s) {
		for(Tag tag : s.getTags()){
			if(tag instanceof SourceLineNumberTag){
				return ((SourceLineNumberTag) tag).getLineNumber();
			} else if(tag instanceof LineNumberTag){
				return ((LineNumberTag) tag).getLineNumber();
			}
		}
		return 0;
	}
	
	public String filePath(SootClass klass) {		
		for(Tag tag : klass.getTags()){
			if(tag instanceof SourceFileTag){
				String fileName = ((SourceFileTag) tag).getSourceFile();
				return klass.getPackageName().replace('.','/')+"/"+fileName;
			}
		}
		return null;
	}
	
	public File srcMapFile(String srcFileName) {
		if(srcFileName != null){
			for(File dir : srcMapDirs){
				File f = new File(dir, srcFileName.replace(".java", ".xml"));
				if(f.exists())
					return f;
			}
		}
		return null;
	}
}
