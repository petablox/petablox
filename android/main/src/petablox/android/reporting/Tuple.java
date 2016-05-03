package petablox.reporting;

import java.io.PrintWriter;

import org.apache.commons.lang3.StringEscapeUtils;

import petablox.program.Program;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import petablox.android.srcmap.Expr;
import petablox.android.srcmap.SourceInfoSingleton;
import petablox.android.srcmap.sourceinfo.SourceInfo;
import petablox.android.srcmap.sourceinfo.jimpleinfo.JimpleSourceInfo;
import petablox.util.soot.SootUtilities;

/*
 * @author Saswat Anand
**/
public class Tuple {
	protected String str;
	protected String attrs;
	protected SourceInfo sourceInfo;
	
	public Tuple() {
		this.sourceInfo = SourceInfoSingleton.v();
	}
	
	/*
	public void setSourceInfo(SourceInfo sourceInfo) {	
		this.sourceInfo = sourceInfo;
		System.out.println("DEBUG: New tuple source info type " + (this.sourceInfo instanceof JimpleSourceInfo));	
	}
	*/

	public Tuple addValue(SootClass klass) 	{
		String line = String.valueOf(sourceInfo.classLineNum(klass));
		addValue(klass.getName(), klass, line);
		return this;
	}

	public Tuple addValue(SootMethod meth) {
		return addValue(meth, false, null);
	}

	public Tuple addValue(SootMethod meth, boolean showClassName, String type) {
		String line = String.valueOf(sourceInfo.methodLineNum(meth));
		SootClass declKlass = meth.getDeclaringClass();
		addValueWithSig((showClassName ? declKlass.getName() + "." : "") + meth.getName(), 
						declKlass, 
						line,
						(type == null ? "method" : type),
						sourceInfo.chordSigFor(meth));
		return this;
	}
	
	public Tuple addValue(Unit quad) {
		SootMethod meth = SootUtilities.getMethod((Stmt) quad);
		if(meth != null){
			String label = quad.toString();//meth.getDeclaringClass().getSourceFileName() + ":"+ quad.getLineNumber();
			addValue(label, meth.getDeclaringClass(), String.valueOf(sourceInfo.stmtLineNum((Stmt) quad)));
		}
		else
			addValue(quad.toString());
		return this;
	}
	
	public Tuple addValue(Object obj) {
		if(obj instanceof String)
			addValue((String) obj);
		else if(obj instanceof Unit)
			addValue((Unit) obj);
		else if(obj instanceof SootMethod)
			addValue((SootMethod) obj);
		else if(obj instanceof SootClass)
			addValue((SootClass) obj);
		else if(obj == null)
			return this;
		else
			throw new RuntimeException("Cannot add value of " + obj.getClass() + " type to tuple");
		return this;
	}
	
	public void write(PrintWriter writer) {
		writer.print("<tuple"+(attrs != null ? attrs : ""));
		if(str != null){
			writer.println(">");
			writer.println(str);
			writer.println("</tuple>");
		} else
			writer.println("/>");
	}

	public final Tuple setAttr(String key, String value) {
		String kvp = " "+key+"=\""+value+"\"";
		if(attrs != null)
			attrs += kvp;
		else
			attrs = kvp;
		return this;
	}

	public final Tuple addValue(String label) {
		str = (str != null ? str : "") +
			"\t<value>\n" +
			"\t\t<label><![CDATA["+label+"]]></label>\n" +
			"\t</value>\n";
		return this;
	}

	public final Tuple addValue(String label, SootClass klass, String lineNum) {
		return addValue(label, klass, lineNum, null);
	}

	public final Tuple addValue(String label, SootClass klass, String lineNum, String type) {
		return addValueWithSig(label, klass, lineNum, type, null);
 	}
	
	public final Tuple addValueWithSig(String label, SootClass klass, String lineNum, String type, String chordSig) {
		String srcFile = sourceInfo.filePath(klass);
		//System.out.println("DEBUG: Tuple source info type 1 " + (sourceInfo instanceof JimpleSourceInfo));
		//System.out.println("DEBUG: Tuple source path " + srcFile);
		str = (str != null ? str : "") +
			"\t<value"+
			(srcFile == null ? "" : (" srcFile=\""+srcFile+"\" lineNum=\""+lineNum+"\"")) +
			(chordSig == null ? "" : (" chordsig=\""+StringEscapeUtils.escapeXml(chordSig)+"\""))+
			(type == null ? "" : (" type=\""+type+"\"")) +
			">\n" 
		    + "\t\t<label><![CDATA["+label+"]]></label>\n"
		    + "\t</value>\n";
		return this;
	}

	//TODO: remove? Added to facilitate hack for Flow Viz
	public final Tuple addRawValue(String label, String srcFile, String lineNum, String type, String chordSig) {
		str = "\t<value"+
			(srcFile == null ? "" : (" srcFile=\""+srcFile+"\" lineNum=\""+lineNum+"\"")) +
			(chordSig == null ? "" : (" chordsig=\""+StringEscapeUtils.escapeXml(chordSig)+"\""))+
			(type == null ? "" : (" type=\""+type+"\"")) +
			">\n" 
		    + "\t\t<label><![CDATA["+label+"]]></label>\n"
		    + "\t</value>\n" + (str != null ? str : "");
		return this;
	}


	public final Tuple addValueWithHighlight(SootClass klass, Expr e)
	{
		String srcFile = sourceInfo.filePath(klass);
		str = (str != null ? str : "") +
			"\t<value srcFile=\""+srcFile+
			"\" lineNum=\""+e.line()+"\""+
			" type=\"expr\""+
			">\n" +
			"\t\t<highlight key=\"taintedVariable\" startpos=\""+e.start()+"\" length=\""+e.length()+"\"/>\n" +
			"\t\t<label><![CDATA["+e.text()+"]]></label>\n" +
			"\t</value>\n";
		return this;
	}

	public final Tuple addValueWithHighlight(SootClass klass, Expr e, String flows)
	{
		String srcFile = sourceInfo.filePath(klass);
		str = (str != null ? str : "") +
			"\t<value srcFile=\""+srcFile+
			"\" lineNum=\""+e.line()+"\""+
			" type=\"expr\""+
			">\n" +
			"\t\t<highlight key=\"taintedVariable\""+
			" startpos=\""+e.start()+"\""+
			" length=\""+e.length()+"\""+
			" flows=\""+flows+"\""+
			"/>\n" +
			"\t\t<label><![CDATA["+e.text()+"]]></label>\n" +
			"\t</value>\n";
		return this;
	}
		
	public int hashCode() {
		return str == null ? 0 : str.hashCode();
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof Tuple))
			return false;
		Tuple o = (Tuple) other;
		if(str == null)
			return o.str == null;
		else
			return str.equals(o.str);
	}
	
	public String toString() {
		return str;
	}
}
