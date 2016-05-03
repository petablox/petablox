package petablox.android.srcmap;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;
import static petablox.android.srcmap.PetabloxSigFactory.toPetabloxType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import petablox.android.missingmodels.jimplesrcmapper.CodeStructureInfo.SimpleCodeStructure;

/*
 * @author Saswat Anand
 * @author Osbert Bastani
 */
/*
public class SourceMapGenerator {
	private PrintWriter writer;
	private String topLevelClassSig;

	public SourceMapGenerator(File outputFile) {
		try {
			this.writer = new PrintWriter(outputFile);
		} catch(IOException e){
			throw new Error(e);
		}
	}

	public void start() {
		this.writer.println("<root>");
	}

	public void end() {
		this.writer.println("</root>");
		this.writer.close();
	}

	public void startVisitClass(String chordSig, int lineNum, boolean isTopLevel) {
		this.writer.println("<class chordsig=\"" + escapeXml(chordSig) + "\" line=\"" + lineNum + "\">");
		if(isTopLevel) {
			this.topLevelClassSig = chordSig;
		}
	}

	public void endVisitClass(boolean isTopLevel) {
		if(isTopLevel) {
			this.topLevelClassSig = null;
		}
		this.writer.println("</class>");
	}

	public void startVisitAnonymousClass(String chordSig, int lineNum) {
		this.writer.println("<class anonymous=\"true\" chordsig=\"" + escapeXml(chordSig) + "\" line=\"" + lineNum + "\">");
	}

	public void endVisitAnonymousClass() {
		this.writer.println("</class>");
	}

	//bodyLength, bodyStartLineNum, and bodyEndLineNum are zero for abstract methods
	public void startVisitMethod(String chordSig, String sig, Set<String> aliasDescriptions,
			List<SimpleCodeStructure> parameters, List<String> parameterNames, List<String> parameterTypes,
			int startPos, int endPos, int lineNum, int bodyLength, int bodyStartLineNum, int bodyEndLineNum) {
		
		this.writer.println("<method");
		this.writer.println(" chordsig=\"" + escapeXml(chordSig) + "\"");
		this.writer.println(" sig=\"" + escapeXml(sig) + "\"");
		this.writer.println(" line=\"" + lineNum + "\"");
		this.writer.println(" startpos=\"" + startPos + "\"");
		this.writer.println(" endpos=\"" + endPos + "\"");
		this.writer.println(" bodyStartLn=\"" + bodyStartLineNum + "\"");
		this.writer.println(" bodyEndLn=\"" + bodyEndLineNum + "\"");
		this.writer.println(">");

		for(String desc : aliasDescriptions) {
			this.writer.println("\t<alias>"+escapeXml(desc)+"</alias>");
		}

		//parameter names
		if(parameters != null) {
			for(int i=0; i<parameters.size(); i++) {
				writeSimpleMarker("param", null, -1, parameters.get(i), parameterNames.get(i), parameterTypes.get(i));
			}
		}
	}

	public void endVisitMethod() {
		this.writer.println("</method>");
	}

	public void startVisitInitializer(String chordSig, int lineNum, int bodyLength, int bodyStartLineNum, int bodyEndLineNum) {
		this.writer.println("<method");
		this.writer.println(" chordsig=\"" + escapeXml(chordSig) + "\"");
		this.writer.println(" line=\"" + lineNum + "\"");
		this.writer.println(" bodyStartLn=\"" + bodyStartLineNum + "\"");
		this.writer.println(" bodyEndLn=\"" + bodyEndLineNum + "\"");
		this.writer.println(">");
	}

	public void endVisitInitializer() {
		this.writer.println("</method>");
	}
	
	public void visitInvokeExpression(int lineNum, String calleePetabloxSig, int calleeStartPos, int calleeEndPos, String expression,
			List<SimpleCodeStructure> arguments, List<String> argumentNames, List<String> argumentTypes,
			String tag, String specialReceiver) {
		
		this.writer.println("\t<" + tag + " line=\"" + lineNum + "\" chordsig=\"" + escapeXml(calleePetabloxSig) + "\""
				+ " start=\"" + calleeStartPos + "\" length=\"" + (calleeEndPos-calleeStartPos) + "\" type=\"invoke\">");
		this.writer.println("\t\t\t<expr><![CDATA[" + expression + "]]></expr>");
		for(int i=0; i<arguments.size(); i++) {
			SimpleCodeStructure argument = arguments.get(i);
			this.writer.println("\t\t<param "+ getLocationStr(argument) + ">");
			
			if(argumentNames != null) {
				String argumentName = argumentNames.get(i);
				if(argumentTypes != null) {
					String argumentType = argumentTypes.get(i);
					this.writer.println("\t\t\t<expr type=\"" + argumentType + "\"><![CDATA[" + argumentName + "]]></expr>");
				}
				else {
					this.writer.println("\t\t\t<expr><![CDATA[" + argumentName + "]]></expr>");
				}
			} else {
				this.writer.println("\t\t\t<expr><![CDATA[" + specialReceiver + "]]></expr>");
			}
			this.writer.println("\t\t</param>");
		}
		this.writer.println("\t</"+tag+">");
	}

	public void visit(FieldAccess fa) {
		IVariableBinding v = fa.resolveFieldBinding();
		
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

	public boolean visit(SwitchStatement ss)
	{
		Expression expr = ss.getExpression();
		if(expr != null){
			writeSimpleMarker("switch", null, ss, expr);
		}
		return true;
	}


	public boolean visit(SimpleType st) {
		ITypeBinding classType = st.resolveBinding();
		String chordSig = chordClassName(classType);
		writer.println("\t<type "+getLocationStr(st)+" chordsig=\""+chordSig+"\"/>");
		return true;
	}

	private void writeSimpleMarker(String markerType, String chordSig, int lineNum,
			SimpleCodeStructure codeStructure, String expressionName, String expressionType) {
		
		this.writer.println("\t<marker line=\""+lineNum+"\" type=\""+markerType+"\"" + (chordSig != null ? " chordsig=\""+chordSig+"\"" : "") + ">");
		this.writer.println("\t\t<operand " + getLocationStr(codeStructure) + ">");

		if(expressionType != null) {
			this.writer.println("\t\t\t<expr" + " type=\"" + expressionType+"\"><![CDATA[" + expressionName + "]]></expr>");
		} else {
			this.writer.println("\t\t\t<expr><![CDATA[" + expressionName + "]]></expr>");
		}
		this.writer.println("\t\t</operand>");
		this.writer.println("\t</marker>");
	}

	private String getLocationStr(SimpleCodeStructure codeStructure) {
		int start, length, line;
		if(codeStructure == null){
			start = length = line = -1;
		} else {
			start = codeStructure.start;
			length = codeStructure.end - codeStructure.start;
			line = codeStructure.lineNum;
		}
		return " start=\""+start+ "\" length=\""+length+ "\" line=\""+line+"\"";
	}
}
*/
