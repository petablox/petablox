package petablox.analyses.argret;

import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.Utils;

/**
 * Domain of integers from 0 to petablox.domK.size - 1 in order.
 */
@Petablox(name = "K")
public class DomK extends ProgramDom<Integer> {
    public static final int MAXZ = Integer.getInteger("petablox.domK.size", 32);

    @Override
    public void fill() {
        for (int i = 0; i < MAXZ; i++)
            getOrAdd(new Integer(i));  
    }
    
    @Override
    public String toFIString(Integer i) {	
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(i) +")");
    	sb.append(i.toString());
    	return sb.toString();
    }
}
