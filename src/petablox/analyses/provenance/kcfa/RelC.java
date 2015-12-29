package petablox.analyses.provenance.kcfa;

import petablox.analyses.alias.DomC;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(
	    name = "RelC",
	    consumes = { "C" },
	    sign = "C0:C0"
	)
public class RelC extends ProgramRel{

	@Override
	public void fill() {
		DomC domC = (DomC) doms[0];
        for(int i = 0;i < domC.size();i ++){
        	this.add(i);
        }
	}
	
}
