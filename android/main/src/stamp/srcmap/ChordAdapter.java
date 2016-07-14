package stamp.srcmap;

import java.io.*;
import java.util.*;
 
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;

import static stamp.srcmap.ChordSigFactory.toChordType;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

/*
 * @author Saswat Anand
 */
public class ChordAdapter extends ASTVisitor
{
	private PrintWriter writer;
	private CompilationUnit cu;

	private String topLevelClassSig;

	public ChordAdapter(CompilationUnit cu, File outputFile)
	{
		try{
			this.writer = new PrintWriter(outputFile);
		}catch(IOException e){
			throw new Error(e);
		}
		this.cu = cu;
		writer.println("<root>");
	}
	
	public void finish()
	{
		writer.println("</root>");
		writer.close();
	}
	
	public boolean visit(TypeDeclaration node)
	{
		ITypeBinding classType = node.resolveBinding();
		String chordSig = chordClassName(classType);
		int lineNum = cu.getLineNumber(node.getName().getStartPosition());
		writer.println("<class" +
					   " chordsig=\""+escapeXml(chordSig)+"\""+
					   " line=\""+lineNum+"\""+
					   ">");
		if(classType.isTopLevel())
			topLevelClassSig = chordSig;
		return true;
	}
	
	public void endVisit(TypeDeclaration node)
	{
		ITypeBinding classType = node.resolveBinding().getErasure();
		if(classType.isTopLevel())
			topLevelClassSig = null;
		writer.println("</class>");
	}

	public boolean visit(EnumDeclaration node)
	{
		ITypeBinding classType = node.resolveBinding();
		String chordSig = chordClassName(classType);
		int lineNum = cu.getLineNumber(node.getName().getStartPosition());
		writer.println("<class" +
					   " chordsig=\""+escapeXml(chordSig)+"\""+
					   " line=\""+lineNum+"\""+
					   ">");
		if(classType.isTopLevel())
			topLevelClassSig = chordSig;
		return true;
	}

	public boolean visit(EnumConstantDeclaration node)
	{
		AnonymousClassDeclaration anonymDecl = node.getAnonymousClassDeclaration();
		if(anonymDecl != null){
			assert topLevelClassSig != null : node.toString();
			ITypeBinding anonymType = anonymDecl.resolveBinding();
			int lineNum = cu.getLineNumber(node.getStartPosition());
			String name = topLevelClassSig+"#"+chordClassName(anonymType.getSuperclass())+"#"+lineNum;
			ITypeBinding anonymTypeBinding = anonymType.getErasure();
			ChordSigFactory.newAnonymousClass(anonymTypeBinding, name);
		}		
		return true;
	}

	public void endVisit(EnumDeclaration node)
	{
		ITypeBinding classType = node.resolveBinding().getErasure();
		if(classType.isTopLevel())
			topLevelClassSig = null;
		writer.println("</class>");
	}
	
	public boolean visit(AnonymousClassDeclaration node)
	{
		ITypeBinding classType = node.resolveBinding();
		String chordSig = chordClassName(classType);
		int lineNum = cu.getLineNumber(node.getStartPosition());
		writer.println("<class" +
					   " anonymous=\"true\""+
					   " chordsig=\""+escapeXml(chordSig)+"\""+
					   " line=\""+lineNum+"\""+
					   ">");
		return true;
	}

	public void endVisit(AnonymousClassDeclaration node)
	{
		writer.println("</class>");
	}

	public void endVisit(MethodDeclaration node) 
	{
		writer.println("</method>");
	}
	
	public boolean visit(MethodDeclaration node) 
	{
		int startPos = node.getName().getStartPosition();
		int endPos = startPos+node.getName().getLength();
		int lineNum = cu.getLineNumber(startPos);
        Block body = node.getBody();
		int bodyLen = body == null ? 0 : body.getLength();
        int bodyStartLn = body == null ? 0 : cu.getLineNumber(body.getStartPosition());
        int bodyEndLn = body == null ? 0 : cu.getLineNumber(body.getStartPosition() + bodyLen);
		String chordSig = chordSigFor(node);
		String className = chordSig.substring(chordSig.indexOf('@')+1);
		String sig = sigFor(node);
		writer.println("<method");
		writer.println(" chordsig=\""+escapeXml(chordSig)+"\"");
		writer.println(" sig=\""+escapeXml(sig)+"\"");
		writer.println(" line=\""+lineNum+"\"");
		writer.println(" startpos=\""+startPos+"\"");
		writer.println(" endpos=\""+endPos+"\"");
    	writer.println(" bodyStartLn=\""+bodyStartLn+"\"");
		writer.println(" bodyEndLn=\""+bodyEndLn+"\"");
		writer.println(">");
		Set<String> aliasDescs = new HashSet();
		String methDesc = chordSig.substring(chordSig.indexOf(':')+1, chordSig.indexOf('@'));
		List<IMethodBinding> overriddenMeths = EclipseUtil.findOverridenMethods(node.resolveBinding());
		for(IMethodBinding m : overriddenMeths){
			String aliasSig = chordSigFor(m.getMethodDeclaration());
			aliasSig = aliasSig.substring(aliasSig.indexOf(':')+1, aliasSig.indexOf('@'));
			if(!aliasSig.equals(methDesc))
				aliasDescs.add(aliasSig);
		}
		for(String desc : aliasDescs)
			writer.println("\t<alias>"+escapeXml(desc)+"</alias>");

		//parameter names
		List params = node.parameters();
		int psize = params.size();
		for(int i = 0; i < psize; i++){
			SingleVariableDeclaration p = (SingleVariableDeclaration) params.get(i);
			writeSimpleMarker("param", null, -1, p.getName());
		}
		
		return true;
	}
	
	public boolean visit(Initializer node)
	{
		int lineNum = cu.getLineNumber(node.getStartPosition());
		Block body = node.getBody();
        int bodyLen = body.getLength();
        int bodyStartLn = cu.getLineNumber(body.getStartPosition());
        int bodyEndLn = cu.getLineNumber(body.getStartPosition() + bodyLen);
		ASTNode parent = node.getParent();
		ITypeBinding containerType = null;
		if(parent instanceof AbstractTypeDeclaration){
			AbstractTypeDeclaration td = (AbstractTypeDeclaration) parent;
			containerType = td.resolveBinding();
		}
		else if(parent instanceof AnonymousClassDeclaration){
			containerType = ((AnonymousClassDeclaration) parent).resolveBinding();
		}
		else
			throw new RuntimeException(parent.getClass().toString());
		String className = chordClassName(containerType.getErasure());
		String chordSig = "<clinit>:()V@"+className;
		writer.println("<method");
		writer.println(" chordsig=\""+escapeXml(chordSig)+"\"");
		writer.println(" line=\""+lineNum+"\"");
    	writer.println(" bodyStartLn=\""+bodyStartLn+"\"");
		writer.println(" bodyEndLn=\""+bodyEndLn+"\"");
		writer.println(">");
		return true;
	}
	
	public void endVisit(Initializer node)
	{
		writer.println("</method>");
	}

	public boolean visit(ClassInstanceCreation cic)
	{
		int lineNum = cu.getLineNumber(cic.getStartPosition());

		AnonymousClassDeclaration anonymDecl = cic.getAnonymousClassDeclaration();
		if(anonymDecl != null){
			assert topLevelClassSig != null : cic.toString();
			ITypeBinding anonymType = anonymDecl.resolveBinding();
			String name = topLevelClassSig+"#"+chordClassName(anonymType.getSuperclass())+"#"+lineNum;
			ITypeBinding anonymTypeBinding = anonymType.getErasure();
			ChordSigFactory.newAnonymousClass(anonymTypeBinding, name);
		}

		IMethodBinding callee = cic.resolveConstructorBinding().getMethodDeclaration();
		List args = cic.arguments();
		args = new ArrayList(args);
		args.add(0, cic);
		processInvkExpr(lineNum, cic.getType(), cic.toString(), callee, args, "newexpr", null);
		//System.out.println("** " + cic + " ##");
		return true;
	}

	public boolean visit(MethodInvocation mi)
	{
		IMethodBinding callee = mi.resolveMethodBinding().getMethodDeclaration();
		if(callee == null){
			System.out.println("Could not resolve call site " + mi);
			return true;
		}
		List args = mi.arguments();
		if(!Modifier.isStatic(callee.getModifiers())){
			Expression rcvr = mi.getExpression(); //rcvr can be null if it is implicit
			args = new ArrayList(args);
			args.add(0, rcvr);
		}
		int lineNum = cu.getLineNumber(mi.getStartPosition());
		processInvkExpr(lineNum, mi.getName(), mi.toString(), callee, args, "invkexpr", "this");
		return true;
	}

	public boolean visit(SuperMethodInvocation mi)
	{
		IMethodBinding callee = mi.resolveMethodBinding().getMethodDeclaration();		
		List args = mi.arguments();
		args = new ArrayList(args);
		args.add(0, null);
		int lineNum = cu.getLineNumber(mi.getStartPosition());
		processInvkExpr(lineNum, mi.getName(), mi.toString(), callee, args, "invkexpr", "super");
		return true;
	}
	
	private void processInvkExpr(int lineNum, ASTNode methodName, String expr, IMethodBinding callee, List args, String tag, String specialRcvr)
	{
		String chordSig = chordSigFor(callee);
		writer.println("\t<"+tag+
					   " line=\""+lineNum+"\""+
					   " chordsig=\""+escapeXml(chordSig)+"\""+ 
					   " start=\""+methodName.getStartPosition()+"\""+
					   " length=\""+methodName.getLength()+"\""+
					   " type=\"invoke\""+
					   ">");
		writer.println("\t\t\t<expr><![CDATA["+expr+"]]></expr>");
		for(Iterator it = args.iterator(); it.hasNext();){
			Expression arg = (Expression) it.next();
			writer.println("\t\t<param "+ getLocationStr(arg) + ">");
			if(arg != null){
				ITypeBinding argType = arg.resolveTypeBinding();
				if(argType.isAnonymous()){
					writer.println("\t\t\t<expr"+
								   " type=\""+ChordSigFactory.getSyntheticName(argType)+"\""+
								   "><![CDATA["+arg+"]]></expr>");
				}
				else
					writer.println("\t\t\t<expr><![CDATA["+arg+"]]></expr>");
			}
			else{
				assert specialRcvr != null;
				writer.println("\t\t\t<expr><![CDATA["+specialRcvr+"]]></expr>");
			}
			writer.println("\t\t</param>");
		}
		writer.println("\t</"+tag+">");
	}

	public boolean visit(FieldAccess fa)
	{
		IVariableBinding v = fa.resolveFieldBinding();
		if(Modifier.isStatic(v.getModifiers()))
			return true;

		Expression base = fa.getExpression();
		ITypeBinding baseType = base.resolveTypeBinding();

		String chordSig, markerType;
		if(baseType.isArray()){
			assert fa.getName().getIdentifier().equals("length");
			markerType = "arraylen";
			chordSig = toChordType(baseType);
		} else{
			markerType = "fieldexpr";
			chordSig =  chordSigFor(v);
		}
		writeSimpleMarker(markerType, chordSig, fa, base);

		return true;
	}
	
	public boolean visit(ExpressionStatement stmt)
	{
		//System.out.println("ExpressionStatement " + stmt);
		return true;
	}

	public boolean visit(VariableDeclarationStatement stmt)
	{
		//System.out.println("VariableDeclarationStatement " + stmt);
		for(Iterator it = stmt.fragments().iterator(); it.hasNext();){
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) it.next();
			SimpleName leftOp = fragment.getName();
			Expression rightOp = fragment.getInitializer();
			if(rightOp instanceof MethodInvocation){
				IMethodBinding callee = ((MethodInvocation) rightOp).resolveMethodBinding().getMethodDeclaration();		
				String chordSig = chordSigFor(callee);
				writeSimpleMarker("invk.lhs", escapeXml(chordSig), stmt, leftOp);
			}
			else if(rightOp instanceof ClassInstanceCreation){
				String chordSig = toChordType(((ClassInstanceCreation) rightOp).getType().resolveBinding());
				writeSimpleMarker("new.lhs", escapeXml(chordSig), stmt, leftOp);
			}
			else if(rightOp instanceof ArrayAccess){
				String chordSig = toChordType(((ArrayAccess) rightOp).getArray().resolveTypeBinding());
				writeSimpleMarker("aload.lhs", escapeXml(chordSig), stmt, leftOp);
			}
			else if(rightOp instanceof FieldAccess){
				String chordSig = chordSigFor(((FieldAccess) rightOp).resolveFieldBinding());
				writeSimpleMarker("load.lhs", escapeXml(chordSig), stmt, leftOp);
			}
		}
		return true;
	}

	public boolean visit(Assignment as)
	{
		return true;
		/*
		//System.out.println("ass " +ass.toString());
		Expression leftOp = ass.getLeftHandSide();
		Expression rightOp = ass.getRightHandSide();
		if(leftOp instanceof FieldAccess){
			int lineNum = cu.getLineNumber(ass.getStartPosition());
			FieldAccess fa = (FieldAccess) leftOp;
			IVariableBinding field = fa.resolveFieldBinding();
			String chordSig = chordSigFor(field);
			writeSimpleMarker("storefield", chordSig, ass, rightOp);
		}
		if(leftOp instanceof SimpleName){
			if(rightOp instanceof MethodInvocation){
				invokeRet((SimpleName) leftOp, (MethodInvocation) rightOp, ass);
			}
		} else System.out.println("leftOp.class = " + leftOp.getClass());
		return true;
		*/
	}
	
	public boolean visit(ReturnStatement rs)
	{
		Expression expr = rs.getExpression();
		if(expr != null){
			writeSimpleMarker("return", null, rs, expr);
		}
		return true;
	}
	
	public boolean visit(SwitchStatement ss)
	{
		Expression expr = ss.getExpression();
		if(expr != null){
			writeSimpleMarker("switch", null, ss, expr);
		}
		return true;
	}


	public boolean visit(SimpleType st)
	{
		ITypeBinding classType = st.resolveBinding();
		String chordSig = chordClassName(classType);
		writer.println("\t<type "+getLocationStr(st)+" chordsig=\""+chordSig+"\"/>");
		return true;
	}
	
	
	private void writeSimpleMarker(String markerType, String chordSig, ASTNode markerNode, Expression expr)
	{
		int lineNum = cu.getLineNumber(markerNode.getStartPosition());
		writeSimpleMarker(markerType, chordSig, lineNum, expr);
	}

	private void writeSimpleMarker(String markerType, String chordSig, int lineNum, Expression expr)
	{
		writer.println("\t<marker"+
					   " line=\""+lineNum+"\""+
					   " type=\""+markerType+"\""+
					   (chordSig != null ? " chordsig=\""+chordSig+"\"" : "")+
					   ">");
		writer.println("\t\t<operand "+getLocationStr(expr)+">");

		ITypeBinding exprType = expr.resolveTypeBinding();
		if(exprType.isAnonymous()){
			writer.println("\t\t\t<expr"+
						   " type=\""+ChordSigFactory.getSyntheticName(exprType)+"\""+
						   "><![CDATA["+expr+"]]></expr>");
		} else {
			writer.println("\t\t\t<expr><![CDATA["+expr+"]]></expr>");
		}
		writer.println("\t\t</operand>");
		writer.println("\t</marker>");
	}

	private String getLocationStr(ASTNode node)
	{
		int start, length, line;
		if(node == null){
			start = length = line = -1;
		} else{
			start = node.getStartPosition();
			length = node.getLength();
			line = cu.getLineNumber(start);
		}
		return 
			" start=\""+start+ "\""+
			" length=\""+length+ "\""+
			" line=\""+line+"\"";
	}
	
	public static String chordSigFor(IVariableBinding v)
	{
		assert v.isField() : v.toString();
		ITypeBinding declKlass = v.getDeclaringClass();
		assert declKlass != null : v.toString();
		return v.getName()+":"+toChordType(v.getType())+"@"+chordClassName(declKlass.getErasure());
	}

	public static String chordSigFor(IMethodBinding callee)
	{
		StringBuilder builder = new StringBuilder();

		String mname;
		if(callee.isConstructor())
			mname = "<init>";
		else
			mname = callee.getName();
		builder.append(mname);
		
		//params
		builder.append(":(");
		for(ITypeBinding type : callee.getParameterTypes()){
			String ptype = toChordType(type);
			builder.append(ptype);
		}
		builder.append(")");

		//return type
		if(callee.isConstructor()){
			builder.append("V");
		} else {
			builder.append(toChordType(callee.getReturnType()));
		}

		//declaring class
		builder.append("@");		
		builder.append(chordClassName(callee.getDeclaringClass()));
		
		return builder.toString();
	}

	public static String sigFor(MethodDeclaration node)
	{
		StringBuilder builder = new StringBuilder();
		// buidling the method declaration
		if(node.getReturnType2()!=null) {
			builder.append(node.getReturnType2().resolveBinding().getName() + " ");
		}
		
		String mname = node.getName().getIdentifier();
		builder.append(mname);
		builder.append("(");
		List params = node.parameters();
		int psize = params.size();
		for(int i = 0; i < psize; i++){
			SingleVariableDeclaration p = (SingleVariableDeclaration) params.get(i);
			
			builder.append(p.getType().resolveBinding().getName());
			if(i < psize-1)
				builder.append(",");
		}
		builder.append(")");
		return builder.toString();
	}
	
	public static String chordSigFor(MethodDeclaration node)
	{	
		StringBuilder builder = new StringBuilder();

		String mname;
		if(node.isConstructor())
			mname = "<init>";
		else
			mname = node.getName().getIdentifier();
		builder.append(mname);
		builder.append(":(");
		
		List params = node.parameters();
		for(int i = 0; i < params.size(); i++){
			SingleVariableDeclaration p = (SingleVariableDeclaration) params.get(i);
			//System.out.println("** " + p.getType() + " " + ((p.getType().resolveBinding()==null) ? "null" : ""));
			ITypeBinding type = p.getType().resolveBinding();
			assert type != null : node.toString() + " param = " + i;
			String ptype = toChordType(type);
			if(p.isVarargs())
				ptype = "["+ptype;
			//System.out.println(pname + " " + ptype + " " + type.isArray() + " " + p.getType().resolveBinding().getBinaryName() + " " + p.isVarargs());
			builder.append(ptype);
		}
		builder.append(")");
		if(node.isConstructor()){
			builder.append("V");
		} else {
			builder.append(toChordType(node.getReturnType2().resolveBinding()));
		}
		builder.append("@");
		ASTNode parent = node.getParent();
		ITypeBinding containerType = null;

		if(parent instanceof AbstractTypeDeclaration){
			AbstractTypeDeclaration td = (AbstractTypeDeclaration) parent;
			containerType = td.resolveBinding();
		}
		else if(parent instanceof AnonymousClassDeclaration){
			containerType = ((AnonymousClassDeclaration) parent).resolveBinding();
		}
		else
			throw new RuntimeException(parent.getClass().toString());
				
		builder.append(chordClassName(containerType.getErasure()));
		
		String chordSig = builder.toString();
		
		//System.out.println("chordsig: " + chordSig + " " + chordSigFor(node.resolveBinding()));

		return chordSig;
	}

	
	private static String chordClassName(ITypeBinding refType)
	{
		String className = toChordType(refType);
		className = className.substring(1, className.length()-1);
		return className.replace('/','.');			
	}
}
