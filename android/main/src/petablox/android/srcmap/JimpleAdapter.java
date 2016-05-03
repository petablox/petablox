package petablox.android.srcmap;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.parser.node.*;
import soot.jimple.parser.analysis.*;

import java.util.*;
import java.io.*;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

public class JimpleAdapter extends DepthFirstAdapter
{
	private String className;
    private PrintWriter writer;

    public JimpleAdapter(File outputFile) 
    {        
        try{
            this.writer = new PrintWriter(outputFile);
        }catch(IOException e){
            throw new Error(e);
        }
        writer.println("<root>");
    }
    
    public void finish()
    {
        writer.println("</root>");
        writer.close();
    }

    /*
      file = 
      modifier* file_type class_name extends_clause? implements_clause? file_body; 
    */
    public void inAFile(AFile node)
    {
		Token tok = ((AFullIdentClassName) node.getClassName()).getFullIdentifier();
		className = tok.getText();

        writer.println("<class" +
					   " chordsig=\""+escapeXml(className)+"\""+
                       " line=\""+tok.getLine()+"\""+
                       ">");
    } 

    public void outAFile(AFile node)
    {
		writer.println("</class>");
	}

    public void outAStaticInvokeExpr(AStaticInvokeExpr node)
	{
		AMethodSignature methSig = (AMethodSignature) node.getMethodSignature(); 
		String sig = getSignature(methSig);

		List<Token> args = getArgs(node.getArgList());
		
		StringBuilder expr = new StringBuilder();
		expr.append(sig).append('(');
		int count = args.size();
		for(int i=0; i < (count-1); i++)
			expr.append(args.get(i).getText()).append(", ");
		if(count > 0)
			expr.append(args.get(count-1).getText());
		expr.append(");");

		processInvkExpr(methSig, expr.toString(), chordSig(sig), args);
	}

	public void outANonstaticInvokeExpr(ANonstaticInvokeExpr node)
	{
		AMethodSignature methSig = (AMethodSignature) node.getMethodSignature(); 
		String sig = getSignature(methSig);
		Token base = ((AIdentName) ((ALocalName) node.getLocalName()).getName()).getIdentifier();
		List<Token> args = getArgs(node.getArgList());

		StringBuilder expr = new StringBuilder();
		expr.append(base.getText()).append('.').append(sig).append('(');
		int count = args.size();
		for(int i=0; i < (count-1); i++)
			expr.append(args.get(i).getText()).append(", ");
		if(count > 0)
			expr.append(args.get(count-1).getText());
		expr.append(");");

		args.add(0, base);

		processInvkExpr(methSig, expr.toString(), chordSig(sig), args);
	}

	private String chordSig(String sootSig)
	{
		return sootSig;
	}

	private void processInvkExpr(AMethodSignature methSig, String expr, String chordSig, List<Token> args)
    {
		Token cmpGt = methSig.getCmpgt();
		int lineNum = cmpGt.getLine();

		writer.println("\t<invkexpr"+
                       " line=\""+lineNum+"\""+
                       " chordsig=\""+escapeXml(chordSig)+"\""+
                       " start=\""+cmpGt.getPos()+"\""+
                       " length=\"1\""+
                       " type=\"invoke\""+
                       ">");
        writer.println("\t\t\t<expr><![CDATA["+expr+"]]></expr>");
		//for(Iterator it = args.iterator(); it.hasNext();){
		for(Token arg : args){	
            writer.println("\t\t<param "+ getLocationStr(arg) + ">");			
			writer.println("\t\t\t<expr><![CDATA["+arg.getText()+"]]></expr>");
            writer.println("\t\t</param>");
        }
        writer.println("\t</invkexpr>");
    }

	private List<Token> getArgs(PArgList aList)
	{
		List<Token> args = new ArrayList();
		if(aList == null)
			;//no args
		else if(aList instanceof AMultiArgList){
			do{
				PImmediate arg = (PImmediate) ((AMultiArgList) aList).getImmediate();
				Token tok = handleImmediate(arg);
				args.add(tok);
				aList = ((AMultiArgList) aList).getArgList();
			}while(aList instanceof AMultiArgList);				
			PImmediate arg = (PImmediate) ((ASingleArgList) aList).getImmediate();
			Token tok = handleImmediate(arg);
			args.add(tok);
		}
		else if(aList instanceof ASingleArgList){
			PImmediate arg = (PImmediate) ((ASingleArgList) aList).getImmediate();
			Token tok = handleImmediate(arg);
			args.add(tok);
		}
		else
			throw new RuntimeException(aList.getClass().toString());
		return args;
	}

	private Token handleImmediate(PImmediate imm)
	{
		if(imm instanceof AConstantImmediate){
			PConstant c = ((AConstantImmediate) imm).getConstant();
			if(c instanceof AClzzConstant)
				return ((AIntegerConstant) c).getIntegerConstant();
			if(c instanceof AFloatConstant)
				return ((AFloatConstant) c).getFloatConstant();
			if(c instanceof AIntegerConstant)
				return ((AIntegerConstant) c).getIntegerConstant();
			if(c instanceof ANullConstant)
				return ((ANullConstant) c).getNull();
			if(c instanceof AStringConstant)
				return ((AStringConstant) c).getStringConstant();
		}
		if(imm instanceof ALocalImmediate){
			return ((AIdentName) ((ALocalName) ((ALocalImmediate) imm).getLocalName()).getName()).getIdentifier();			
		}
		
		throw new RuntimeException(imm.getClass().toString());
	}

	private String getSignature(PMethodSignature signature)
	{
		String sig = signature.toString();
		//example sig: < com.example.android.snake.SnakeView : void loadTile ( int , android.graphics.drawable.Drawable ) >
		int colonIndex = sig.indexOf(':');
		String part1 = sig.substring(2, colonIndex-1);
		int lparenIndex = sig.indexOf('(');
		String part2 = sig.substring(colonIndex, lparenIndex-1);
		int rparenIndex = sig.indexOf(')');
		String part3 = sig.substring(lparenIndex, rparenIndex+1).replace(" ","");
		sig = "<"+part1+part2+part3+">";
		System.out.println(sig);
		return sig;
	}

    public void inAMethodMember(AMethodMember node)
    {
		Token nameToken = ((AIdentName) node.getName()).getIdentifier();
		String name = nameToken.getText();
		//System.out.println("++ "+node.toString());
		//buffer.append(" " + nameToken.getLine() + " "+nameToken.getPos());

		PType retType = node.getType();
		String retTypeStr;
		if(retType instanceof AVoidType)
			retTypeStr = "void";
		else if(retType instanceof ANovoidType)
			retTypeStr = toString(((ANovoidType) retType).getNonvoidType());
		else
			throw new RuntimeException(retType.getClass().toString());

		StringBuilder buffer = new StringBuilder();
		buffer.append('<').append(className).append(": ").append(retTypeStr).append(' ').append(name).append('(');
		PParameterList pList = node.getParameterList();
        if(pList instanceof AMultiParameterList){
			do{
				AParameter param = (AParameter) ((AMultiParameterList) pList).getParameter();
				//System.out.println("*"+param.getNonvoidType().getClass().toString());
				//System.out.println("*"+toString(param.getNonvoidType()));
				buffer.append(toString(param.getNonvoidType()).toString()+",");
				pList = ((AMultiParameterList) pList).getParameterList();
			}while(pList instanceof AMultiParameterList);				
			AParameter param = (AParameter) ((ASingleParameterList) pList).getParameter();
			buffer.append(toString(param.getNonvoidType()));
			//System.out.println("*"+toString(param.getNonvoidType()));
			//System.out.println("*"+pList.toString());
        }
        else if(pList instanceof ASingleParameterList){
			AParameter param = (AParameter) ((ASingleParameterList) pList).getParameter();
			//System.out.println("*"+param.getNonvoidType().getClass().toString());
			buffer.append(toString(param.getNonvoidType()));
			//System.out.println("*"+toString(param.getNonvoidType()));
			//System.out.println("*"+pList.toString());
        } 
		buffer.append(")>");
		
		String sig = buffer.toString();
		String chordSig = chordSig(sig);
        writer.println("<method");
		writer.println(" chordsig=\""+escapeXml(chordSig)+"\"");
        writer.println(" sig=\""+escapeXml(sig)+"\"");
        writer.println(" line=\""+nameToken.getLine()+"\"");
        writer.println(" startpos=\""+nameToken.getPos()+"\"");
        writer.println(" endpos=\""+(nameToken.getPos()+name.length())+"\"");
		writer.println(">");
    }

    public void outAMethodMember(AMethodMember node)
    {
		writer.println("</method>");
	}
	
	private String toString(PNonvoidType type)
	{
		if(type instanceof AFullIdentNonvoidType){
			AFullIdentNonvoidType baseType = (AFullIdentNonvoidType) type;
			String baseTypeStr = baseType.getFullIdentifier().getText();
			return toString(baseTypeStr, baseType.getArrayBrackets().size());
		}
		if(type instanceof ABaseNonvoidType){
			ABaseNonvoidType baseType = (ABaseNonvoidType) type;
			String baseTypeStr = toString(baseType.getBaseTypeNoName());
			return toString(baseTypeStr, baseType.getArrayBrackets().size());
		}
		throw new RuntimeException(type.getClass().toString());
	}

	private String toString(String baseTypeStr, int dim)
	{
		StringBuilder builder = new StringBuilder(baseTypeStr);
		for(int i = 0; i < dim; i++)
			builder.append("[]");
		return builder.toString();
	}

	private String toString(PBaseTypeNoName baseType)
	{
		if(baseType instanceof ABooleanBaseTypeNoName)
			return "boolean";
		if(baseType instanceof AByteBaseTypeNoName)
			return "byte";
		if(baseType instanceof ACharBaseTypeNoName)
			return "char";
		if(baseType instanceof ADoubleBaseTypeNoName)
			return "double";
		if(baseType instanceof AFloatBaseTypeNoName)
			return "float";
		if(baseType instanceof AIntBaseTypeNoName)
			return "int";
		if(baseType instanceof ALongBaseTypeNoName)
			return "long";
		if(baseType instanceof ANullBaseTypeNoName)
			return "nulltype";
		if(baseType instanceof AShortBaseTypeNoName)
			return "short";
		throw new RuntimeException(baseType.getClass().toString());
	}

	private String getLocationStr(Token node)
    {
        int start, length, line;
        if(node == null){
            start = length = line = -1;
        } else{
            start = node.getPos();
            length = node.getText().length();
            line = node.getLine();
        }
        return
            " start=\""+start+ "\""+
            " length=\""+length+ "\""+
            " line=\""+line+"\"";
    }
}
