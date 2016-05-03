package petablox.android.missingmodels.jimplesrcmapper;

import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import petablox.android.missingmodels.jimplesrcmapper.CodeStructureInfo.CodeStructure;
import petablox.android.missingmodels.jimplesrcmapper.CodeStructureInfo.SimpleCodeStructure;

public class JimpleVisitorWithStructure {
	/** Visited before writing the class */
	public void start(SootClass cl, SimpleCodeStructure fileStructure) {}
	
	/** Visited after writing the class */
	public void end(SootClass cl) {}

	/** Starts a class visit */
	public void visit(SootClass cl, CodeStructure classStructure) {}

	/** Ends a class visit */
	public void endVisit(SootClass cl) {}
	
	/** Starts a class field declaration visit */
	public void visit(SootField f, SimpleCodeStructure fieldStructure) {}
	
	/** Ends a class field declaration visit */
	public void endVisit(SootField f) {}
	
	/** Starts a method visit */
	public void visit(SootMethod m, CodeStructure methodStructure) {}
	
	/** Ends a method visit */
	public void endVisit(SootMethod m) {}
	
	/** Starts a method local variable declaration visit */
	public void visit(Local local, SimpleCodeStructure localStructure) {}

	/** Ends a method local variable declaration visit */
	public void endVisit(Local local) {}

	/** Starts a unit graph statement visit */
	public void visit(Unit stmt, SimpleCodeStructure unitStructure) {}
	
	/** Ends a unit graph statement visit */
	public void endVisit(Unit stmt) {}
	
	/** Returns a Jimple visitor that wraps around the structured reader */
	public JimpleVisitor toJimpleVisitor(final CodeStructureInfo codeStructureInfo) {
		return new JimpleVisitor() {
			@Override
			public void start(SootClass cl, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.start(cl, codeStructureInfo.getFileInfo());
			}
			
			@Override
			public void end(SootClass cl, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.end(cl);
			}

			@Override
			public void startVisit(SootClass cl, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.visit(cl, codeStructureInfo.getClassInfo(cl));
			}

			@Override
			public void endVisit(SootClass cl, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.endVisit(cl);
			}
			
			@Override
			public void startVisit(SootField f, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.visit(f, codeStructureInfo.getFieldInfo(f));
			}
			
			@Override
			public void endVisit(SootField f, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.endVisit(f);
			}
			
			@Override
			public void startVisit(SootMethod m, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.visit(m, codeStructureInfo.getMethodInfo(m));
			}
			
			@Override
			public void endVisit(SootMethod m, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.endVisit(m);
			}
			
			@Override
			public void startVisit(Local local, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.visit(local, codeStructureInfo.getLocalInfo(local));
			}

			@Override
			public void endVisit(Local local, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.endVisit(local);
			}

			@Override
			public void startVisit(Unit stmt, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.visit(stmt, codeStructureInfo.getUnitInfo(stmt));
			}
			
			@Override
			public void endVisit(Unit stmt, int pos, int lineNum) {
				JimpleVisitorWithStructure.this.endVisit(stmt);
			}			
		};
	}
}
