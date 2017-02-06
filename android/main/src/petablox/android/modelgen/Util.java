package petablox.android.modelgen;

import japa.parser.*;
import japa.parser.ast.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.body.*;
import japa.parser.ast.visitor.*;
import japa.parser.ast.type.*;
import java.util.*;

public class Util
{
	public static String chordMethodSig(BodyDeclaration meth, TypeDeclaration klass, String pkg)
	{
		if(meth instanceof MethodDeclaration){
			MethodDeclaration method = (MethodDeclaration) meth;
			return chordSig(method.getName(), klass.getName(), method.getType(), method.getParameters(), pkg);
		}
		else if(meth instanceof ConstructorDeclaration){
			ConstructorDeclaration method = (ConstructorDeclaration) meth;
			return chordSig("<init>", klass.getName(), new VoidType(), method.getParameters(), pkg);
		}

		assert false : meth.toString();
		return null;
	}

	private static String chordSig(String name, String className, Type retType, List<Parameter> params, String pkg)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(name);
		builder.append(":(");
		
		if(params != null){
			for(Parameter p : params){
				builder.append(bytecodeTypeSig(p.getType().toString()));
			}
		}
		builder.append(")");
		builder.append(bytecodeTypeSig(retType.toString()));
		builder.append("@");
		builder.append(pkg.equals("") ? className : (pkg+"."+className));
		
		return builder.toString();
		
		//for(Statement stmt : stmts)
		//	System.out.println(stmt);
	}
	
	public static String bytecodeTypeSig(String type)
	{
		if(type.equals("void"))
			return "V";
		if(type.equals("boolean"))
			return "Z";
		if(type.equals("int"))
			return "I";
		if(type.equals("byte"))
			return "B";
		if(type.equals("short"))
			return "S";
		if(type.equals("char"))
			return "C";
		if(type.equals("long"))
			return "J";
		if(type.equals("float"))
			return "F";
		if(type.equals("double"))
			return "D";
		
		//remove generics
		int count = 0;
		int start = -1, end = -1;
		char[] cs = type.toCharArray();
		for(int i = 0; i < cs.length; i++){
			char c = cs[i];
			if(c == '<'){
				count++;
				if(count == 1)
					start = i;
			} else if(c == '>') {
				count--;
				if(count == 0)
					end = i;
			}
		}
		assert count == 0;
		if(start > 0){
			type = type.substring(0, start).concat(type.substring(end+1));
		}

		int index = type.indexOf('[');
		if(index > 0){
			//array type
			int x = type.length() - index;
			assert x % 2 == 0 : type;
			int dim = x/2;
			String elemType = bytecodeTypeSig(type.substring(0, index));
			cs = new char[dim];
			for(int i = 0; i < dim; i++)
				cs[i] = '[';
			return new String(cs).concat(elemType);
		}
		
		return "L"+type.replace('.','/')+";";
	}
}