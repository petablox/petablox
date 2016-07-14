package stamp.missingmodels.jimplesrcmapper;

import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;

public class JimpleVisitor {
	/** Visited before writing the class */
	public void start(SootClass cl, int pos, int lineNum) {}
	
	/** Visited after writing the class */
	public void end(SootClass cl, int pos, int lineNum) {}

	/** Starts a class visit */
	public void startVisit(SootClass cl, int pos, int lineNum) {}

	/** Ends a class visit */
	public void endVisit(SootClass cl, int pos, int lineNum) {}
	
	/** Starts a class declaration visit */
	public void startVisitDeclaration(SootClass cl, int pos, int lineNum) {}
	
	/** Ends a class declaration visit */
	public void endVisitDeclaration(SootClass cl, int pos, int lineNum) {}
	
	/** Starts a class body visit */
	public void startVisitBody(SootClass cl, int pos, int lineNum) {}

	/** Ends a class body visit */
	public void endVisitBody(SootClass cl, int pos, int lineNum) {}
	
	/** Starts a class field declaration visit */
	public void startVisit(SootField f, int pos, int lineNum) {}
	
	/** Ends a class field declaration visit */
	public void endVisit(SootField f, int pos, int lineNum) {}
	
	/** Starts a method visit */
	public void startVisit(SootMethod m, int pos, int lineNum) {}
	
	/** Ends a method visit */
	public void endVisit(SootMethod m, int pos, int lineNum) {}
	
	/** Starts a method declaration visit */
	public void startVisitDeclaration(SootMethod m, int pos, int lineNum) {}
	
	/** Ends a method declaration visit */
	public void endVisitDeclaration(SootMethod m, int pos, int lineNum) {}
	
	/** Starts a method body visit */
	public void startVisitBody(SootMethod m, int pos, int lineNum) {}
	
	/** Ends a method body visit */
	public void endVisitBody(SootMethod m, int pos, int lineNum) {}
	
	/** Starts a method local variable declaration visit */
	public void startVisit(Local local, int pos, int lineNum) {}

	/** Ends a method local variable declaration visit */
	public void endVisit(Local local, int pos, int lineNum) {}

	/** Starts a unit graph statement visit */
	public void startVisit(Unit stmt, int pos, int lineNum) {}
	
	/** Ends a unit graph statement visit */
	public void endVisit(Unit stmt, int pos, int lineNum) {}
	
	/*
	public void finish() {}
	
	public boolean visit(TypeDeclaration node) {}
	public void endVisit(TypeDeclaration node) {}
	
	public boolean visit(EnumConstantDeclaration node) {}
	public void endVisit(EnumConstantDeclaration node) {}
	
	public boolean visit(EnumDeclaration node) {}
	public void endVisit(EnumDeclaration node) {}
	
	public boolean visit(AnonymousClassDeclaration node) {}
	public void endVisit(AnonymousClassDeclaration node) {}
	
	public boolean visit(MethodDeclaration node) {}
	public void endVisit(MethodDeclaration node) {} 

	public boolean visit(Initializer node) {}
	public void endVisit(Initializer node) {}
	
	public boolean visit(ClassInstanceCreation cic) {}
	public boolean visit(MethodInvocation mi) {}

	public boolean visit(SuperMethodInvocation mi) {}
	public void endVisit(SuperMethodInvocation mi) {}
	
	public boolean visit(FieldAccess fa) {}
	public void endVisit(FieldAccess fa) {}
	
	public boolean visit(ExpressionStatement stmt) {}
	public void endVisit(ExpressionStatement stmt) {}
	
	public boolean visit(VariableDeclarationStatement stmt) {}
	public void endVisit(VariableDeclarationStatement stmt) {}
	
	public boolean visit(Assignment as) {}
	public void endVisit(Assignment as) {}
	
	public boolean visit(ReturnStatement rs) {}
	public void endVisit(ReturnStatement rs) {}
	
	public boolean visit(SwitchStatement ss) {}
	public void endVisit(SwitchStatement ss) {}

	public boolean visit(SimpleType st) {}
	public void endVisit(SimpleType st) {}
	*/
}
