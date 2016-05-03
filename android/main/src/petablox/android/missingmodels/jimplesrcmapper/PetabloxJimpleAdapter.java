package petablox.android.missingmodels.jimplesrcmapper;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.StringEscapeUtils;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import petablox.android.missingmodels.jimplesrcmapper.CodeStructureInfo.CodeStructure;
import petablox.android.missingmodels.jimplesrcmapper.CodeStructureInfo.SimpleCodeStructure;
import petablox.android.missingmodels.util.xml.XMLObject.XMLContainerObject;
import petablox.android.srcmap.sourceinfo.jimpleinfo.JimpleSourceInfo;

public class PetabloxJimpleAdapter extends JimpleVisitorWithStructure {
	/** Information about the Java source files */
	private JimpleSourceInfo sourceInfo;
	
	/** The current stack of objects in the XML tree */
	private Stack<XMLContainerObject> openObjects;
	
	/** Stores the XML objects generated for each soot class */
	private Map<SootClass,XMLContainerObject> newObjects;
	
	public PetabloxJimpleAdapter(JimpleSourceInfo sourceInfo) {
		this.sourceInfo = sourceInfo;
		//this.objects = objects;
		this.newObjects = new HashMap<SootClass,XMLContainerObject>();
	}
	
	/** Returns the resulting XML objects */
	public Map<SootClass,XMLContainerObject> getResults() {
		return this.newObjects;
	}
	
	/** Adds a new child to the current leaf of the tree */
	/*
	private void addObject(XMLObject object) {
		this.openObjects.peek().addChild(object);
	}
	*/
	
	/** Adds a new child to the current leaf of the tree and makes it the new leaf */
	private void startObject(XMLContainerObject object) {
		this.openObjects.peek().addChild(object);
		this.openObjects.push(object);
	}
	
	/** Ends the current leaf */
	private void endObject() {
		this.openObjects.pop();
	}
	
	/** Visit the file */
	@Override
	public void start(SootClass cl, SimpleCodeStructure fileInfo) {
		this.openObjects = new Stack<XMLContainerObject>();
		this.openObjects.push(new XMLContainerObject("root"));
	}
	
	@Override
	public void end(SootClass cl) {
		if(this.openObjects.size() != 1) {
			throw new RuntimeException("XML object tree not properly closed!");
		}
		this.newObjects.put(cl, this.openObjects.pop());
	}
	
	/** Visits a type declaration */
	// See PetabloxAdapter.visit(TypeDeclaration)
	@Override
	public void visit(SootClass cl, CodeStructure classInfo) {
		XMLContainerObject newObject = new XMLContainerObject("class");
		this.startObject(newObject);
		
		String chordSig = StringEscapeUtils.escapeXml(this.sourceInfo.srcClassName(cl));
		// guarantee that the node has a chord sig (causes crash if it doesn't)
		newObject.putAttribute("chordsig", escapeXml(chordSig));
		// remark: also needs linenum to avoid crash, but this is also needed to overwrite the java linenum with the jimple line num
		newObject.putAttribute("line", Integer.toString(classInfo.declarationLineNum)); 
	}
	
	@Override
	public void endVisit(SootClass cl) {
		this.endObject();
	}
	
	/** Visits the method declaration */
	@Override
	public void visit(SootMethod m, CodeStructure methodInfo) {
		// TODO: get the parameters, etc.
		XMLContainerObject newObject = new XMLContainerObject("method");
		this.startObject(newObject);
		
		String chordSig = StringEscapeUtils.escapeXml(this.sourceInfo.chordSigFor(m));		
		// guarantee that the node has a chord sig (causes crash if it doesn't)
		newObject.putAttribute("chordsig", chordSig);
		newObject.putAttribute("line", Integer.toString(methodInfo.declarationLineNum));
		newObject.putAttribute("startpos", Integer.toString(methodInfo.declarationStart));
		newObject.putAttribute("endpos", Integer.toString(methodInfo.declarationEnd));
		newObject.putAttribute("bodyStartLn", Integer.toString(methodInfo.bodyStartLineNum));
		newObject.putAttribute("bodyEndLn", Integer.toString(methodInfo.bodyEndLineNum));
	}
	
	@Override
	public void endVisit(SootMethod m) {
		this.endObject();
	}
	
	/** Starts a method local variable declaration visit */
	public void visit(Local local, SimpleCodeStructure localStructure) {
		// TODO: fill this in
	}

	/** Starts a unit graph statement visit */
	public void visit(Unit stmt, SimpleCodeStructure unitStructure) {
		// TODO: fill this in
		if(stmt instanceof Stmt) {
			Stmt s = (Stmt)stmt;
			if(s.containsInvokeExpr()) {

				InvokeExpr ie = s.getInvokeExpr();
				for(Value value : ie.getArgs()) {
					
				}
			} else if(s.containsFieldRef()){
			} else if(s.containsArrayRef()) {
			} else if(s instanceof AssignStmt) {
			} else if(s instanceof ReturnStmt){
			} else if(s instanceof IdentityStmt){
			} else if(s instanceof TableSwitchStmt){
			} else if(s instanceof LookupSwitchStmt){
			}
		}
	}
}
