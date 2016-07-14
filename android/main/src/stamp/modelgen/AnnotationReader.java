package stamp.modelgen;

import japa.parser.*;
import japa.parser.ast.*;
import java.io.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.body.*;
import japa.parser.ast.visitor.*;
import japa.parser.ast.type.*;
import java.util.*;

/**
 * reads annotations using a java source code parser
 * This code is not used right now
 * @author Saswat Anand
**/
public class AnnotationReader
{
	private static PrintWriter writer;

	public static void main(String[] args) throws Exception
	{
		writer = new PrintWriter(new FileWriter("stamp_annotations.txt"));

		File stubsDir = new File(args[0]);

		if(args.length == 2){
			BufferedReader reader = new BufferedReader(new FileReader(new File(args[1], "stamp_annotations.txt")));
			String line;
			while((line = reader.readLine()) != null){
				writer.println(line);
			}
			reader.close();
		}

		List<File> javaFiles = new ArrayList();
		listFiles(stubsDir, javaFiles);
		for(File file : javaFiles)
			process(file);

		writer.close();
	}
	
	private static void listFiles(File root, List<File> javaFiles)
	{
        for(File f : root.listFiles()){
            if(f.isDirectory()){
                listFiles(f, javaFiles);
            }
            else if(f.getName().endsWith(".java"))
				javaFiles.add(f);
		}
	}

	private static void process(File file) throws Exception
	{
        FileInputStream in = new FileInputStream(file);

        CompilationUnit cu;
        try {
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }

		//System.out.println(cu.getPackage().getName().getName());
		String pkg = "";
		if(cu.getPackage() != null)
			pkg = cu.getPackage().getName().toString();
		List<TypeDeclaration> types = cu.getTypes();
        for (TypeDeclaration type : types) {
            List<BodyDeclaration> members = type.getMembers();
			if(members == null)
				continue;
            for (BodyDeclaration member : members) {
                if (member instanceof BodyDeclaration) {
					List<AnnotationExpr> annots = ((BodyDeclaration) member).getAnnotations();
					if(annots != null){
						for(AnnotationExpr ae : annots){
							if(ae.getName().toString().equals("STAMP")){
								Map<String,Integer> nameToIndex = paramNames(member); 

								String chordMethSig = Util.chordMethodSig(member, type, pkg);
								for(MemberValuePair  pair1 : ((NormalAnnotationExpr) ae).getPairs()){
									if(!pair1.getName().equals("flows"))
										continue;
									for(Expression flowa : ((ArrayInitializerExpr) pair1.getValue()).getValues()) {
										String from = null, to = null;
										for(MemberValuePair  pair2 : ((NormalAnnotationExpr) flowa).getPairs()){
											if(pair2.getName().equals("from"))
												from = ((StringLiteralExpr) pair2.getValue()).getValue();
											else if(pair2.getName().equals("to"))
												to = ((StringLiteralExpr) pair2.getValue()).getValue();
											else
												assert false : pair2.getName();
											//System.out.println(pair2 + " " + pair2.getName() + " " + pair2.getValue() + " " + pair2.getValue().getClass());
										}
										Integer i0 = nameToIndex.get(from);
										Integer i1 = nameToIndex.get(to);
										//i0, i1 can be null if v0, v1 are source/sink labels 
										writer.println(chordMethSig + " " + (i0 == null ? from : i0.toString()) + " " + (i1 == null ? to : i1.toString()));
										//System.out.println("from = \"" + from + "\", to = \"" + to + "\"");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private static Map<String,Integer> paramNames(BodyDeclaration meth)
	{
		boolean isStatic = false;
		List<Parameter> params = null;
		Type retType = null;
		if(meth instanceof MethodDeclaration){
			MethodDeclaration method = (MethodDeclaration) meth;
			isStatic = ModifierSet.isStatic(method.getModifiers());
			params = method.getParameters();
			retType = method.getType();
		}
		else if(meth instanceof ConstructorDeclaration){
			ConstructorDeclaration method = (ConstructorDeclaration) meth;
			params = method.getParameters();
			retType = new VoidType();
		}
		else assert false : meth.toString();

		Map<String,Integer> nameToIndex = new HashMap();
		int count = 0;
		if(!isStatic){
			count = 1;
			nameToIndex.put("this", 0);
		}
		if(!(retType instanceof VoidType))
			nameToIndex.put("@return", -1);
		
		if(params != null){
			for(Parameter p : params){
				nameToIndex.put(p.getId().getName(), count++);
			}
		}
		//System.out.println(params);
		return nameToIndex;
	}
}