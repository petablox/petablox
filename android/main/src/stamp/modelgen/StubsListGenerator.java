package stamp.modelgen;

import japa.parser.*;
import japa.parser.ast.*;
import java.io.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.body.*;
import japa.parser.ast.visitor.*;
import japa.parser.ast.type.*;
import java.util.*;

public class StubsListGenerator
{
	private static PrintWriter writer;
	private static int stubsCount = 0;

	public static void main(String[] args) throws Exception
	{
		writer = new PrintWriter(new FileWriter("stamp_stubs.txt"));

		File stubsDir = new File(args[0]);
		List<File> javaFiles = new ArrayList();
		listFiles(stubsDir, javaFiles);
		for(File file : javaFiles)
			process(file);

		writer.close();
		System.out.println("No of stubs = " + stubsCount);
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

	public static Set<String> process(File file) throws Exception
	{
	    Set<String> stubSigs = new HashSet<String>();
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
				BlockStmt body = null;
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
					//System.out.println("method: " + method.getName());
					body = method.getBody();
				}
                else if(member instanceof ConstructorDeclaration) {
					ConstructorDeclaration method = (ConstructorDeclaration) member;
					//System.out.println("method: " + method.getName());
					body = method.getBlock();
				}
				if(isStub(body)){
					stubsCount++;
					String sig = Util.chordMethodSig(member, type, pkg);
					if(writer != null) {
					    writer.println(sig);
					}
					stubSigs.add(sig);
					//System.out.println(chordSig(method, type, pkg));
				}
            }
        }
	return stubSigs;
	}

	private static boolean isStub(BlockStmt body)
	{
		if(body != null){
			List<Statement> stmts = body.getStmts();
			return 
				stmts != null &&
				stmts.size() == 1 && 
				stmts.get(0).toString().equals("throw new RuntimeException(\"Stub!\");");
		}
		return false;
	}

}