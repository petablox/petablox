package petablox.android.srcmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;


public class Main { 
	private static String[] classpathEntries;
	private static String[] srcpathEntries;
	private static File srcMapDir;
	private static PrintWriter writer;
	private static Set<String> oldAnnots = new HashSet();

	public static void process(String srcRootPath, File javaFile) throws IOException {
		String canonicalPath = javaFile.getCanonicalPath();
		String relSrcFilePath = canonicalPath.substring(srcRootPath.length()+1);
		File infoFile = new File(srcMapDir, relSrcFilePath.replace(".java", ".xml"));

		if(javaFile.lastModified() < infoFile.lastModified()) 
			return;

		infoFile.getParentFile().mkdirs();

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);
		parser.setEnvironment(classpathEntries, srcpathEntries, null, true);
		parser.setUnitName(canonicalPath);		
		parser.setSource(toCharArray(canonicalPath));

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		System.out.println("generating srcmap file for "+canonicalPath);
		PetabloxAdapterNew visitor = new PetabloxAdapterNew(cu);
		try {
			cu.accept(visitor);
			visitor.writeXML(infoFile);
		} catch(Exception e) {
			System.out.println("Failed to generate srcmap file for "+relSrcFilePath);
			e.printStackTrace();
			infoFile.delete();
		}
		
		/*
		try {
			XMLContainerObject object = visitor.getXMLObject();
			File objectFile = new File(srcMapDir, relSrcFilePath.replace(".java", ".obj"));
			System.out.println("PRINTING XML OBJECT FOR " + javaFile.getName() + " TO " + objectFile.getCanonicalPath());
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(objectFile));
			oos.writeObject(object);
			oos.close();
		} catch(IOException e) {
			System.out.println("FAILED TO PRINT XML OBJECT FOR " + javaFile.getName() + "!");
			e.printStackTrace();
		}
		*/

		AnnotationReader annotReader = new AnnotationReader(canonicalPath);		
		cu.accept(annotReader);	
	}

	public static char[] toCharArray(String filePath) throws IOException {
		File file = new File(filePath);
		int length = (int) file.length();
		char[] array = new char[length+1];
		BufferedReader reader = new BufferedReader(new FileReader(file));
		int offset = 0;
		while(true){
			int count = reader.read(array, offset, length-offset+1);
			if(count == -1)
				break;
			offset += count;
		}
		reader.close();
		char[] ret = new char[offset];
		System.arraycopy(array, 0, ret, 0, offset);
		return ret;
	}

	public Main(File javaFile, File infoFile) throws IOException {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);

		parser.setEnvironment(classpathEntries, srcpathEntries, null, true);

		String canonicalPath = javaFile.getCanonicalPath();
		System.out.println(canonicalPath);
		parser.setUnitName(canonicalPath);
		parser.setSource(toCharArray(canonicalPath));

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		infoFile.getParentFile().mkdirs();
		/*
		PetabloxAdapterNew visitor = new PetabloxAdapterNew(cu);
		cu.accept(visitor);
		visitor.writeXml(infoFile);
		*/
	}

	/*
	   args[0] - ":" separated directories containing Java source code
	   args[1] - ":" separated jars files (third-party libs, android.jar)
	   args[2] - path of the srcmap directory
	 */
	public static void main(String[] args) throws Exception {
		String srcPath = args[0];
		String[] paths = srcPath.split(":");
		srcpathEntries = new String[paths.length];
		int i = 0;
		for(String sp : paths){
			srcpathEntries[i++] = new File(sp).getCanonicalPath();
		}

		String classPath = args[1];
		classpathEntries = classPath.split(":");

		srcMapDir = new File(args[2]); //System.out.println("DEBUG "+ args[2]);
		srcMapDir.mkdirs();

		for(String p : srcpathEntries)
			System.out.println("srcpath: " + p);
		for(String p : classpathEntries)
			System.out.println("classpath: " + p);

		String androidDir = null;
		openWriter();
		
		try{
			for(String srcRootPath : srcpathEntries)
				processDir(srcRootPath, new File(srcRootPath));
		} catch(Exception e){
			e.printStackTrace();
			throw new Error(e);
		}
		writer.close();
	}	

	private static void processDir(String srcRootPath, File dir) throws Exception {
		//System.out.println("** " + dir);
		for(File f : dir.listFiles()){
			if(f.isDirectory()){
				processDir(srcRootPath, f);
			}
			else{
				String name = f.getName();
				if(name.endsWith(".java") && name.indexOf('#') < 0)
					process(srcRootPath, f);
			}
		}
	}

	private static void openWriter() throws IOException {
		//processing android classes
		//open in append mode
		File f = new File("stamp_annotations.txt");
		if(f.exists()){
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			while((line = reader.readLine()) != null){
				oldAnnots.add(line);
			}
			reader.close();
		}
		writer = new PrintWriter(new FileWriter(f, true));
	}

	static void writeAnnot(String annot) {
		if(oldAnnots.contains(annot))
			return;
		writer.println(annot);
	}
}
