package stamp.modelgen;

import japa.parser.*;
import japa.parser.ast.*;
import java.io.*;
import java.util.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.body.*;
import japa.parser.ast.visitor.*;
import japa.parser.ast.type.*;
import java.util.*;

import stamp.util.Pair;

public class Main
{
	private static Map<String,BodyDeclaration> sigToMethod = new HashMap();
	private static String modelsPath;
	private static List<String> modelFiles = new ArrayList();
	private static PrintWriter log;


	public static void main(String[] args) throws Exception
	{
		log = new PrintWriter(new FileWriter("log.txt"));

		File stubsDir = new File(args[0]);
		File modelsDir = new File(args[1]);
		File bakDir = new File(args[2]);
		
		modelsPath = modelsDir.getAbsolutePath() + File.separator;

		listFiles(modelsDir);
		for(String modelFilePath : modelFiles){
			//System.out.println("processing " + modelFilePath); 
			File modelFile = new File(modelsDir, modelFilePath);
			File patchedFile = new File(stubsDir, modelFilePath);
			File bakFile = new File(bakDir, modelFilePath);

			if(!bakFile.exists() ){
				//it is OK to add a new class that does not
				//correspond to any stub class.
				System.out.println("copying " + modelFile);
				dump(patchedFile, getCU(modelFile));
			}
			else {
				process(modelFile, bakFile, patchedFile);
			}
		}
		log.close();
	}

	private static void process(File modelFile, File bakFile, File patchedFile) throws Exception
	{
		System.out.println("patching " + bakFile);
		CompilationUnit stubCU = getCU(bakFile);
 		CompilationUnit modelCU = getCU(modelFile);

		List<TypeDeclaration> stubTypes = stubCU.getTypes();
		List<TypeDeclaration> modelTypes = modelCU.getTypes();


		boolean hasStampAnnotation = patch(stubTypes, modelTypes);		

		addImports(stubCU, modelCU.getImports(), hasStampAnnotation);
		
		dump(patchedFile, stubCU);
	}

	private static boolean patch(List stubMembers, List modelMembers)
	{
		
		Map<String, TypeDeclaration> stubTypeNameToDecl = new HashMap();
        for(Object bodyDecl : stubMembers){
			if(bodyDecl instanceof TypeDeclaration){
				TypeDeclaration stubType = (TypeDeclaration) bodyDecl;
				stubTypeNameToDecl.put(stubType.getName(), stubType);
			}
		}

		boolean hasStampAnnotation = false;
		for(Object bodyDecl : modelMembers){
			if(bodyDecl instanceof TypeDeclaration){
				TypeDeclaration modelType = (TypeDeclaration) bodyDecl;
				hasStampAnnotation |= hasStampAnnotation(modelType);

				TypeDeclaration stubType = stubTypeNameToDecl.get(modelType.getName());
				if(stubType == null){
					//new non-public class that does not correspond to
					//any stub class
					if(stubMembers == null){
						stubMembers = new ArrayList();
						stubType.setMembers(stubMembers);
					}
					stubMembers.add(modelType);
				} else {
					hasStampAnnotation |= patch(modelType, stubType);
				}
			}
		}
		
		return hasStampAnnotation;
	}


	private static void addImports(CompilationUnit stubCU, List<ImportDeclaration> modelImports, boolean hasStampAnnotation)
	{
		List<ImportDeclaration> newImports = new ArrayList();		
		if(hasStampAnnotation){
			newImports.add(new ImportDeclaration(new NameExpr("edu.stanford.stamp.annotation.STAMP"), false, false));
			newImports.add(new ImportDeclaration(new NameExpr("edu.stanford.stamp.annotation.Flow"), false, false));
		}
		
		if(modelImports != null)
			newImports.addAll(modelImports);

		if(newImports.size() == 0)
			return;
		List<ImportDeclaration> imports = stubCU.getImports();
		if(imports == null){
			imports = new ArrayList();
			stubCU.setImports(imports);
		}

		for(ImportDeclaration imp : newImports)
			imports.add(imp);
	}

	private static void dump(File outFile, CompilationUnit cu) throws Exception
	{
		File parent = outFile.getParentFile();
		parent.mkdirs();
		PrintWriter writer = new PrintWriter(new FileWriter(outFile));
		writer.println(cu.toString());
		writer.close();
	}
	
	private static boolean hasStampAnnotation(TypeDeclaration modelType)
	{
		List<BodyDeclaration> modelMembers = modelType.getMembers();
		if(modelMembers == null)
			return false;
		for(BodyDeclaration modelMember : modelMembers){
			List<AnnotationExpr> annots = modelMember.getAnnotations();
			if(annots != null){
				for(AnnotationExpr ae : annots){
					if(ae.getName().toString().equals("STAMP"))
						return true;
				}
			}
		}
		return false;
	}
    
    private static Set<Pair<String,String>> fakeStubs = null;
    private static boolean isFakeStub(String typeSig, String methSig) 
	{
		if(fakeStubs == null) {
			fakeStubs = new HashSet<Pair<String,String>>();
			try {
				BufferedReader br = new BufferedReader(new FileReader("fake_stubs.txt"));
				String line;
				while((line = br.readLine()) != null) {
					String[] tokens = line.split(" ");
					if(tokens.length != 2) continue;
					fakeStubs.add(new Pair<String,String>(tokens[0], tokens[1]));
					log.println("adding fake stub " + tokens[0] + " " + tokens[1]);
				}
			} catch(IOException e) { System.out.println("WARN: fake_stubs.txt not found"); }
		}
		return fakeStubs.contains(new Pair<String,String>(typeSig, methSig));
    }
	
    private static boolean patch(TypeDeclaration modelType, TypeDeclaration stubType) 
	{
		String typeSig = signature(modelType);
		
		List<BodyDeclaration> modelMembers = modelType.getMembers();
		if(modelMembers == null)
			return false;
		
		Map<String,BodyDeclaration> methSigToStubMethod = new HashMap();
		List<BodyDeclaration> stubMembers = stubType.getMembers();
		
		if(stubMembers != null){
			for(BodyDeclaration stubMember : stubMembers){
				if(stubMember instanceof MethodDeclaration || 
				   stubMember instanceof ConstructorDeclaration || 
				   stubMember instanceof InitializerDeclaration){
					String stubMethSig = signature(stubMember); 
					methSigToStubMethod.put(stubMethSig, stubMember);
				} 
			}
		}
		
		for(BodyDeclaration modelMember : modelMembers){
			if(modelMember instanceof TypeDeclaration)
				continue;
			if(modelMember instanceof MethodDeclaration || modelMember instanceof ConstructorDeclaration || modelMember instanceof InitializerDeclaration){
				String modelMethSig = signature(modelMember);
				BodyDeclaration stubMethod = methSigToStubMethod.get(modelMethSig);
				if(isFakeStub(typeSig, modelMethSig)) {
					if(stubMethod != null) {
						log.println("ignoring model method " + typeSig + " " + modelMethSig);
						continue;
					} else {
						log.println("refusing to ignore critical model method " + typeSig + " " + modelMethSig);
					}
				}
				if(stubMethod != null){
					log.println("replacing stub method " + typeSig + " " + modelMethSig);
					stubMembers.remove(stubMethod);
				}else{
					//here is a model method that does not correspond to any stub method
					log.println("introducing new model method " + typeSig + " " + modelMethSig);
				}
			}
			if(stubMembers == null){
				stubMembers = new ArrayList();
				stubType.setMembers(stubMembers);
			}
			//System.out.println(modelMember.getClass());
			stubMembers.add(modelMember);
		}			
		
		return patch(stubMembers, modelMembers);
    }
	
	private static void listFiles(File rootDir)
	{
        for(File f : rootDir.listFiles()){
            if(f.isDirectory()){
                listFiles(f);
            } else {
				String fname = f.getName();
				if(fname.endsWith(".java") && Character.isLetter(fname.charAt(0)))	
					modelFiles.add(f.getAbsolutePath().substring(modelsPath.length()));
			}
		}
	}

	private static CompilationUnit getCU(File file) throws Exception
	{
        FileInputStream in = new FileInputStream(file);

        CompilationUnit cu;
        try {
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }
		return cu;
	}

    private static String signature(TypeDeclaration type) 
	{
		return type.getName();
		//return type.toString();
    }

	private static String signature(BodyDeclaration meth)
	{
		if(meth instanceof MethodDeclaration){
			MethodDeclaration method = (MethodDeclaration) meth;
			return signature(method.getName(), method.getType(), method.getParameters());
		}
		else if(meth instanceof ConstructorDeclaration){
			ConstructorDeclaration method = (ConstructorDeclaration) meth;
			return signature("<init>", new VoidType(), method.getParameters());
		}
		else if(meth instanceof InitializerDeclaration){
			return "<clinit>";
		}
		throw new RuntimeException(meth.toString());
	}

	private static String signature(String name, Type retType, List<Parameter> params)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(retType.toString());
		builder.append(name);
		builder.append("(");
		
		if(params != null){
			int i = params.size();
			for(Parameter p : params){
				builder.append(p.getType().toString());
				if(i > 1)
					builder.append(",");
				i--;
			}
		}
		builder.append(")");
		return builder.toString();
	}


}