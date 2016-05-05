package petablox.android.missingmodels.jimplesrcmapper;

import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;

public class JimpleStructureExtractor extends JimpleVisitor {
	
	/** Stores the extracted code structure information */
	private CodeStructureInfo codeStructureInfo = new CodeStructureInfo();
	
	public CodeStructureInfo getCodeStructureInfo() {
		return this.codeStructureInfo;
	}
	
	/** Visited before writing the class */
	@Override
	public void start(SootClass cl, int pos, int lineNum) {
		this.codeStructureInfo.fileInfo.start = pos;
		this.codeStructureInfo.fileInfo.lineNum = lineNum;
	}
	
	/** Visited after writing the class */
	@Override
	public void end(SootClass cl, int pos, int lineNum) { this.codeStructureInfo.fileInfo.end = pos; }

	/** Starts a class visit */
	@Override
	public void startVisit(SootClass cl, int pos, int lineNum) { this.codeStructureInfo.classInfo.get(cl).start = pos; }

	/** Ends a class visit */
	@Override
	public void endVisit(SootClass cl, int pos, int lineNum) { this.codeStructureInfo.classInfo.get(cl).end = pos; }
	
	/** Starts a class declaration visit */
	@Override
	public void startVisitDeclaration(SootClass cl, int pos, int lineNum) {
		this.codeStructureInfo.classInfo.get(cl).declarationStart = pos;
		this.codeStructureInfo.classInfo.get(cl).declarationLineNum = lineNum;
	}
	
	/** Ends a class declaration visit */
	@Override
	public void endVisitDeclaration(SootClass cl, int pos, int lineNum) { this.codeStructureInfo.classInfo.get(cl).declarationEnd = pos; }
	
	/** Starts a class body visit */
	@Override
	public void startVisitBody(SootClass cl, int pos, int lineNum) {
		this.codeStructureInfo.classInfo.get(cl).bodyStart = pos;
		this.codeStructureInfo.classInfo.get(cl).bodyStartLineNum = lineNum;
	}

	/** Ends a class body visit */
	@Override
	public void endVisitBody(SootClass cl, int pos, int lineNum) {
		this.codeStructureInfo.classInfo.get(cl).bodyEnd = pos;
		this.codeStructureInfo.classInfo.get(cl).bodyEndLineNum = pos;
	}
	
	/** Starts a class field declaration visit */
	@Override
	public void startVisit(SootField f, int pos, int lineNum) {
		this.codeStructureInfo.fieldInfo.get(f).start = pos;
		this.codeStructureInfo.fieldInfo.get(f).lineNum = lineNum;
	}
	
	/** Ends a class field declaration visit */
	@Override
	public void endVisit(SootField f, int pos, int lineNum) { this.codeStructureInfo.fieldInfo.get(f).end = pos; }
	
	/** Starts a method visit */
	@Override
	public void startVisit(SootMethod m, int pos, int lineNum) { this.codeStructureInfo.methodInfo.get(m).start = pos; }
	
	/** Ends a method visit */
	@Override
	public void endVisit(SootMethod m, int pos, int lineNum) { this.codeStructureInfo.methodInfo.get(m).end = pos; }
	
	/** Starts a method declaration visit */
	@Override
	public void startVisitDeclaration(SootMethod m, int pos, int lineNum) {
		this.codeStructureInfo.methodInfo.get(m).declarationStart = pos;
		this.codeStructureInfo.methodInfo.get(m).declarationLineNum = lineNum;
	}
	
	/** Ends a method declaration visit */
	@Override
	public void endVisitDeclaration(SootMethod m, int pos, int lineNum) { this.codeStructureInfo.methodInfo.get(m).declarationEnd = pos; }
	
	/** Starts a method body visit */
	@Override
	public void startVisitBody(SootMethod m, int pos, int lineNum) {
		this.codeStructureInfo.methodInfo.get(m).bodyStart = pos;
		this.codeStructureInfo.methodInfo.get(m).bodyStartLineNum = lineNum;
	}
	
	/** Ends a method body visit */
	@Override
	public void endVisitBody(SootMethod m, int pos, int lineNum) {
		this.codeStructureInfo.methodInfo.get(m).bodyEnd = pos;
		this.codeStructureInfo.methodInfo.get(m).bodyEndLineNum = lineNum;
	}
	
	/** Starts a method local variable declaration visit */
	@Override
	public void startVisit(Local local, int pos, int lineNum) {
		this.codeStructureInfo.localInfo.get(local).start = pos;
		this.codeStructureInfo.localInfo.get(local).lineNum = lineNum;
	}

	/** Ends a method local variable declaration visit */
	@Override
	public void endVisit(Local local, int pos, int lineNum) { this.codeStructureInfo.localInfo.get(local).end = pos; }

	/** Starts a unit graph statement visit */
	@Override
	public void startVisit(Unit stmt, int pos, int lineNum) {
		this.codeStructureInfo.unitInfo.get(stmt).start = pos;
		this.codeStructureInfo.unitInfo.get(stmt).lineNum = lineNum;
	}
	
	/** Ends a unit graph statement visit */
	@Override
	public void endVisit(Unit stmt, int pos, int lineNum) { this.codeStructureInfo.unitInfo.get(stmt).end = pos; }
}
