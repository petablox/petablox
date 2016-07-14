package stamp.injectannot;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import java.util.*;

public class WidgetAnnotation extends Visitor
{
    public WidgetAnnotation()
    {
    }
	
    protected void visit(SootClass klass)
    {
		super.visit(klass);
		if(!klass.getName().equals("stamp.harness.G"))
			return;
		visit(klass.getMethodByName("<clinit>"));
    }
	
    protected void visit(SootMethod method)
    {
		super.visit(method);

		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		Type editTextType = RefType.v("android.widget.EditText");
		Iterator<Unit> uit = units.snapshotIterator();
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
			
			if(!stmt.containsFieldRef())
				continue;

			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			Value leftOp = defStmt.getLeftOp();
			if(!(leftOp instanceof StaticFieldRef))
				continue;
			
			SootField fld = ((StaticFieldRef) leftOp).getField();
			if(!fld.getDeclaringClass().getName().equals("stamp.harness.G"))
				continue;
			
			Type fldType = fld.getType();
			if(!fh.canStoreType(fldType, editTextType))
				continue;

			String label = fld.getSubSignature();
			Local tmp = insertLabelIfNecessary((Local) defStmt.getRightOp(), stmt, label, true, false, false);

			((AssignStmt) defStmt).setRightOp(tmp);
		}
    }
}