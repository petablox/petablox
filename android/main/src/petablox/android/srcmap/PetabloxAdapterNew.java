package petablox.android.srcmap;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;
import static petablox.android.srcmap.PetabloxSigFactory.toPetabloxType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import petablox.android.missingmodels.util.xml.XMLObject;
import petablox.android.missingmodels.util.xml.XMLObject.XMLContainerObject;
import petablox.android.missingmodels.util.xml.XMLObject.XMLEmptyObject;
import petablox.android.missingmodels.util.xml.XMLObject.XMLTextObject;

/*
 * @author Saswat Anand
 * @author Osbert Bastani
 */
public class PetabloxAdapterNew extends ASTVisitor {
	
	private CompilationUnit cu;
	
	private Stack<XMLContainerObject> openObjects;
	
	private String topLevelClassSig;
	
	public XMLContainerObject getXMLObject() {
		return this.openObjects.firstElement();
	}
	
	private void addObject(XMLObject object) {
		this.openObjects.peek().addChild(object);
	}
	
	private void startObject(XMLContainerObject object) {
		this.openObjects.peek().addChild(object);
		this.openObjects.push(object);
	}
	
	private void endObject() {
		this.openObjects.pop();
	}

	public PetabloxAdapterNew(CompilationUnit cu) {
		this.openObjects = new Stack<XMLContainerObject>();
		this.openObjects.push(new XMLContainerObject("root"));
		this.cu = cu;
	}
	
	public void writeXML(File outputFile) {
		if(!(this.openObjects.size() == 1)) {
			if(this.openObjects.size() > 1) {
				System.out.println(this.openObjects.peek().toString());
			} else {
				System.out.println("Empty!");
			}
			throw new RuntimeException("Objects not properly closed!");
		}
		try {
			PrintWriter writer = new PrintWriter(outputFile);
			writer.println(this.openObjects.peek());
			writer.close();
		} catch(IOException e) {
			throw new Error(e);
		}
	}
	
	// TODO: OVERWRITE
	public boolean visit(TypeDeclaration node) {
		ITypeBinding classType = node.resolveBinding();
		String chordSig = chordClassName(classType);
		int lineNum = cu.getLineNumber(node.getName().getStartPosition());
		
		XMLContainerObject object = new XMLContainerObject("class");
		object.putAttribute("chordsig", escapeXml(chordSig));
		object.putAttribute("line", Integer.toString(lineNum));
		this.startObject(object);
		
		if(classType.isTopLevel()) {
			this.topLevelClassSig = chordSig;
		}
		return true;
	}
	
	public void endVisit(TypeDeclaration node) {
		ITypeBinding classType = node.resolveBinding().getErasure();
		if(classType.isTopLevel()) {
			this.topLevelClassSig = null;
		}
		this.endObject();
	}

	// TODO: OVERWRITE
	public boolean visit(EnumDeclaration node) {
		ITypeBinding classType = node.resolveBinding();
		String chordSig = chordClassName(classType);
		int lineNum = cu.getLineNumber(node.getName().getStartPosition());
		
		XMLContainerObject object = new XMLContainerObject("class");
		object.putAttribute("chordsig", escapeXml(chordSig));
		object.putAttribute("line", Integer.toString(lineNum));
		this.startObject(object);
		
		if(classType.isTopLevel()) {
			this.topLevelClassSig = chordSig;
		}
		return true;
	}

	public void endVisit(EnumDeclaration node) {
		ITypeBinding classType = node.resolveBinding().getErasure();
		if(classType.isTopLevel()) {
			this.topLevelClassSig = null;
		}
		this.endObject();
	}

	public boolean visit(EnumConstantDeclaration node) {
		AnonymousClassDeclaration anonymDecl = node.getAnonymousClassDeclaration();
		if(anonymDecl != null){
			assert topLevelClassSig != null : node.toString();
			ITypeBinding anonymType = anonymDecl.resolveBinding();
			int lineNum = cu.getLineNumber(node.getStartPosition());
			String name = topLevelClassSig+"#"+chordClassName(anonymType.getSuperclass())+"#"+lineNum;
			ITypeBinding anonymTypeBinding = anonymType.getErasure();
			PetabloxSigFactory.newAnonymousClass(anonymTypeBinding, name);
		}		
		return true;
	}

	// TODO: OVERWRITE
	public boolean visit(AnonymousClassDeclaration node) {
		ITypeBinding classType = node.resolveBinding();
		String chordSig = chordClassName(classType);
		int lineNum = cu.getLineNumber(node.getStartPosition());
		
		XMLContainerObject object = new XMLContainerObject("class");
		object.putAttribute("anonymous", "true");
		object.putAttribute("chordsig", escapeXml(chordSig));
		object.putAttribute("line", Integer.toString(lineNum));
		this.startObject(object);
		
		return true;
	}

	public void endVisit(AnonymousClassDeclaration node) {
		this.endObject();
	}

	// TODO: OVERWRITE
	public boolean visit(MethodDeclaration node) {
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
		
		XMLContainerObject object = new XMLContainerObject("method");
		object.putAttribute("chordsig", escapeXml(chordSig));
		object.putAttribute("sig", escapeXml(sig));
		object.putAttribute("line", Integer.toString(lineNum));
		object.putAttribute("startpos", Integer.toString(startPos));
		object.putAttribute("endpos", Integer.toString(endPos));
		object.putAttribute("bodyStartLn", Integer.toString(bodyStartLn));
		object.putAttribute("bodyEndLn", Integer.toString(bodyEndLn));
		this.startObject(object);

		Set<String> aliasDescs = new HashSet();
		String methDesc = chordSig.substring(chordSig.indexOf(':')+1, chordSig.indexOf('@'));
		List<IMethodBinding> overriddenMeths = EclipseUtil.findOverridenMethods(node.resolveBinding());
		for(IMethodBinding m : overriddenMeths){
			String aliasSig = chordSigFor(m.getMethodDeclaration());
			aliasSig = aliasSig.substring(aliasSig.indexOf(':')+1, aliasSig.indexOf('@'));
			if(!aliasSig.equals(methDesc)) {
				aliasDescs.add(aliasSig);
			}
		}
		for(String desc : aliasDescs) {
			XMLTextObject childObject = new XMLTextObject("alias");
			childObject.setInnerXML(escapeXml(desc));
			this.addObject(childObject);
		}

		//parameter names
		List params = node.parameters();
		int psize = params.size();
		for(int i = 0; i < psize; i++){
			SingleVariableDeclaration p = (SingleVariableDeclaration) params.get(i);
			writeSimpleMarker("param", null, -1, p.getName());
		}
		
		return true;
	}

	public void endVisit(MethodDeclaration node) {
		this.endObject();
	}

	// TODO: OVERWRITE
	public boolean visit(Initializer node) {
		int lineNum = cu.getLineNumber(node.getStartPosition());
		Block body = node.getBody();
        int bodyLen = body.getLength();
        int bodyStartLn = cu.getLineNumber(body.getStartPosition());
        int bodyEndLn = cu.getLineNumber(body.getStartPosition() + bodyLen);
		ASTNode parent = node.getParent();
		ITypeBinding containerType = null;
		if(parent instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration td = (AbstractTypeDeclaration) parent;
			containerType = td.resolveBinding();
		} else if(parent instanceof AnonymousClassDeclaration) {
			containerType = ((AnonymousClassDeclaration) parent).resolveBinding();
		} else {
			throw new RuntimeException(parent.getClass().toString());
		}
		
		String className = chordClassName(containerType.getErasure());
		String chordSig = "<clinit>:()V@"+className;
		XMLContainerObject object = new XMLContainerObject("method");
		object.putAttribute("chordsig", escapeXml(chordSig));
		object.putAttribute("line", Integer.toString(lineNum));
		object.putAttribute("bodyStartLn", Integer.toString(bodyStartLn));
		object.putAttribute("bodyEndLn", Integer.toString(bodyEndLn));
		this.startObject(object);
		
		return true;
	}
	
	public void endVisit(Initializer node) {
		this.endObject();
	}

	public boolean visit(ClassInstanceCreation cic) {
		int lineNum = cu.getLineNumber(cic.getStartPosition());

		AnonymousClassDeclaration anonymDecl = cic.getAnonymousClassDeclaration();
		if(anonymDecl != null){
			assert topLevelClassSig != null : cic.toString();
			ITypeBinding anonymType = anonymDecl.resolveBinding();
			String name = topLevelClassSig+"#"+chordClassName(anonymType.getSuperclass())+"#"+lineNum;
			ITypeBinding anonymTypeBinding = anonymType.getErasure();
			PetabloxSigFactory.newAnonymousClass(anonymTypeBinding, name);
		}

		IMethodBinding callee = cic.resolveConstructorBinding().getMethodDeclaration();
		List args = cic.arguments();
		args = new ArrayList(args);
		args.add(0, cic);
		processInvkExpr(lineNum, cic.getType(), cic.toString(), callee, args, "newexpr", null);
		//System.out.println("** " + cic + " ##");
		return true;
	}

	public boolean visit(MethodInvocation mi) {
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

	public boolean visit(SuperMethodInvocation mi) {
		IMethodBinding callee = mi.resolveMethodBinding().getMethodDeclaration();		
		List args = mi.arguments();
		args = new ArrayList(args);
		args.add(0, null);
		int lineNum = cu.getLineNumber(mi.getStartPosition());
		processInvkExpr(lineNum, mi.getName(), mi.toString(), callee, args, "invkexpr", "super");
		return true;
	}

	// TODO: REMOVE AND REBUILD
	private void processInvkExpr(int lineNum, ASTNode methodName, String expr, IMethodBinding callee, List args, String tag, String specialRcvr) {
		String chordSig = chordSigFor(callee);
		
		XMLContainerObject object = new XMLContainerObject(tag);
		object.putAttribute("line", Integer.toString(lineNum));
		object.putAttribute("chordsig", escapeXml(chordSig));
		object.putAttribute("start", Integer.toString(methodName.getStartPosition()));
		object.putAttribute("length", Integer.toString(methodName.getLength()));
		object.putAttribute("type", "invoke");
		this.startObject(object);
		
		XMLTextObject childObject1 = new XMLTextObject("expr");
		childObject1.setInnerXML("<![CDATA[" + expr + "]]>");
		this.addObject(childObject1);

		for(Iterator it = args.iterator(); it.hasNext();){
			Expression arg = (Expression) it.next();
			
			XMLContainerObject childObject2 = new XMLContainerObject("param");
			setLocation(childObject2, arg);
			this.startObject(childObject2);
			
			if(arg != null){
				XMLTextObject childObject3 = new XMLTextObject("expr");
				ITypeBinding argType = arg.resolveTypeBinding();
				if(argType.isAnonymous()){
					childObject3.putAttribute("type", PetabloxSigFactory.getSyntheticName(argType));
				}
				childObject3.setInnerXML("<![CDATA[" + arg + "]]>");
				this.addObject(childObject3);
			} else {
				assert specialRcvr != null;
				XMLTextObject childObject4 = new XMLTextObject("expr");
				childObject4.setInnerXML("<![CDATA["+specialRcvr+"]]>");
				this.addObject(childObject4);
			}
			this.endObject();
		}
		
		this.endObject();
	}

	public boolean visit(FieldAccess fa) {
		IVariableBinding v = fa.resolveFieldBinding();
		if(Modifier.isStatic(v.getModifiers()))
			return true;

		Expression base = fa.getExpression();
		ITypeBinding baseType = base.resolveTypeBinding();

		String chordSig, markerType;
		if(baseType.isArray()){
			assert fa.getName().getIdentifier().equals("length");
			markerType = "arraylen";
			chordSig = toPetabloxType(baseType);
		} else{
			markerType = "fieldexpr";
			chordSig =  chordSigFor(v);
		}
		writeSimpleMarker(markerType, chordSig, fa, base);

		return true;
	}
	
	public boolean visit(VariableDeclarationStatement stmt) {
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
				String chordSig = toPetabloxType(((ClassInstanceCreation) rightOp).getType().resolveBinding());
				writeSimpleMarker("new.lhs", escapeXml(chordSig), stmt, leftOp);
			}
			else if(rightOp instanceof ArrayAccess){
				String chordSig = toPetabloxType(((ArrayAccess) rightOp).getArray().resolveTypeBinding());
				writeSimpleMarker("aload.lhs", escapeXml(chordSig), stmt, leftOp);
			}
			else if(rightOp instanceof FieldAccess){
				String chordSig = chordSigFor(((FieldAccess) rightOp).resolveFieldBinding());
				writeSimpleMarker("load.lhs", escapeXml(chordSig), stmt, leftOp);
			}
		}
		return true;
	}
	
	public boolean visit(ReturnStatement rs) {
		Expression expr = rs.getExpression();
		if(expr != null){
			writeSimpleMarker("return", null, rs, expr);
		}
		return true;
	}
	
	public boolean visit(SwitchStatement ss) {
		Expression expr = ss.getExpression();
		if(expr != null){
			writeSimpleMarker("switch", null, ss, expr);
		}
		return true;
	}

	public boolean visit(SimpleType st) {
		ITypeBinding classType = st.resolveBinding();
		String chordSig = chordClassName(classType);
		XMLEmptyObject object = new XMLEmptyObject("type");
		setLocation(object, st);
		object.putAttribute("chordsig", chordSig);
		this.addObject(object);
		return true;
	}
		
	private void writeSimpleMarker(String markerType, String chordSig, ASTNode markerNode, Expression expr) {
		int lineNum = cu.getLineNumber(markerNode.getStartPosition());
		writeSimpleMarker(markerType, chordSig, lineNum, expr);
	}

	// TODO: REMOVE AND REBUILD
	private void writeSimpleMarker(String markerType, String chordSig, int lineNum, Expression expr) {
		
		XMLContainerObject object = new XMLContainerObject("marker");
		object.putAttribute("line", Integer.toString(lineNum));
		object.putAttribute("type", markerType);
		if(chordSig != null) {
			object.putAttribute("chordsig", chordSig);
		}
		this.startObject(object);
		
		XMLContainerObject childObject1 = new XMLContainerObject("operand");
		this.setLocation(childObject1, expr);
		this.startObject(childObject1);

		ITypeBinding exprType = expr.resolveTypeBinding();
		XMLTextObject childObject2 = new XMLTextObject("expr");
		if(exprType.isAnonymous()) {
			childObject2.putAttribute("type", PetabloxSigFactory.getSyntheticName(exprType));
		}
		childObject2.setInnerXML("<![CDATA["+expr+"]]>");
		this.addObject(childObject2);
		
		this.endObject();
		
		this.endObject();
	}

	private void setLocation(XMLObject object, ASTNode node) {
		int start, length, line;
		if(node == null){
			start = length = line = -1;
		} else{
			start = node.getStartPosition();
			length = node.getLength();
			line = cu.getLineNumber(start);
		}
		object.putAttribute("start", Integer.toString(start));
		object.putAttribute("length", Integer.toString(length));
		object.putAttribute("line", Integer.toString(line));
	}
	
	public static String chordSigFor(IVariableBinding v) {
		assert v.isField() : v.toString();
		ITypeBinding declKlass = v.getDeclaringClass();
		String declKlassName;
		if(declKlass == null){
			//The field length of an array type has no declaring class.
			assert v.toString().equals("public final int length") : v.toString();
			declKlassName = "[]";
			
		} else
			declKlassName = chordClassName(declKlass.getErasure());
		return v.getName()+":"+toPetabloxType(v.getType())+"@"+declKlassName;
	}

	public static String chordSigFor(IMethodBinding callee) {
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
			String ptype = toPetabloxType(type);
			builder.append(ptype);
		}
		builder.append(")");

		//return type
		if(callee.isConstructor()){
			builder.append("V");
		} else {
			builder.append(toPetabloxType(callee.getReturnType()));
		}

		//declaring class
		builder.append("@");		
		builder.append(chordClassName(callee.getDeclaringClass()));
		
		return builder.toString();
	}

	public static String sigFor(MethodDeclaration node) {
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
	
	public static String chordSigFor(MethodDeclaration node) {	
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
			String ptype = toPetabloxType(type);
			if(p.isVarargs())
				ptype = "["+ptype;
			//System.out.println(pname + " " + ptype + " " + type.isArray() + " " + p.getType().resolveBinding().getBinaryName() + " " + p.isVarargs());
			builder.append(ptype);
		}
		builder.append(")");
		if(node.isConstructor()){
			builder.append("V");
		} else {
			builder.append(toPetabloxType(node.getReturnType2().resolveBinding()));
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

	
	private static String chordClassName(ITypeBinding refType) {
		String className = toPetabloxType(refType);
		className = className.substring(1, className.length()-1);
		return className.replace('/','.');			
	}
}
