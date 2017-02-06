package petablox.android.srcmap.sourceinfo.jimpleinfo;

import java.io.File;
import java.io.IOException;

import soot.SootClass;
import soot.jimple.Stmt;
import soot.tagkit.JimpleLineNumberTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.Tag;
import petablox.android.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

/**
 * @author Saswat Anand 
 * @author Osbert Bastani
 */
public class JimpleSourceInfo extends AbstractSourceInfo 
{
	//private File frameworkSrcDir;
	private File srcMapDir;;
	
	public JimpleSourceInfo() 
	{
		String outDir = System.getProperty("petablox.android.out.dir");
		srcMapDir = new File(outDir+"/jimple");
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
			if(tag instanceof JimpleLineNumberTag) {
				return ((JimpleLineNumberTag) tag).getLineNumber();
			} else if(tag instanceof LineNumberTag) {
				return ((LineNumberTag) tag).getLineNumber();
			}
		}
		return 0;
	}
	
	public String filePath(SootClass klass) {
		//System.out.println("DEBUG: Class name " + klass.getName() + ".jimple");
		return klass.getPackageName().replace('.','/') + "/" + klass.getName() + ".jimple";
	}
	
	public File srcMapFile(String srcFileName) {
		if(srcFileName != null) {
			String jimpleFileName = srcFileName.replace(".jimple", ".xml");
			File f = new File(srcMapDir, jimpleFileName);
			//try {
			//	System.out.println("DEBUG: Loading sourcemap file: " + f.getCanonicalPath());
			//} catch(IOException e) {
			//	System.out.println("DEBUG: Failed to load sourcemap file: " + jimpleFileName);
			//	e.printStackTrace();
			//}
			if(f.exists()) {
				return f;
			}
		}
		return null;
	}
}
