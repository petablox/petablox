package petablox.android.analyses;
import soot.jimple.internal.JimpleLocal;

public abstract class VarNode extends JimpleLocal
{
	VarNode(){
		super(null,null);
	}
}