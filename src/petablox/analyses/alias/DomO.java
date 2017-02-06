package petablox.analyses.alias;

import java.util.Set;

import petablox.analyses.alloc.DomH;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramDom;
import petablox.util.Utils;
import soot.Unit;

/**
 * Domain of abstract objects.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomO extends ProgramDom<CIObj> {
    private DomH domH;

    @Override
    public String toFIString(CIObj oVal) {		
    	if (domH == null)
            domH = (DomH) ClassicProject.g().getTrgt("H");    	
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(oVal) +")");
    	Set<Unit> pts = oVal.pts;
    	if (pts.size() == 0)
            return sb.toString();
        for (Unit hVal : pts) 
            sb.append(domH.toFIString(hVal)+",");
    	 String s = sb.toString();
    	 return s.substring(0, s.length() - 1);
    }
    
    @Override
    public String toXMLAttrsString(CIObj oVal) {
        if (domH == null)
            domH = (DomH) ClassicProject.g().getTrgt("H");
        Set<Unit> pts = oVal.pts;
        if (pts.size() == 0)
            return "";
        String s = "Hids=\"";
        for (Unit hVal : pts) {
            int hIdx = domH.indexOf(hVal);
            s += "H" + hIdx + " ";
        }
        s = s.substring(0, s.length() - 1);
        return s + "\"";
    }
}
