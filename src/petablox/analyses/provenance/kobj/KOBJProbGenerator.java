package petablox.analyses.provenance.kobj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.analyses.alloc.DomH;
import petablox.analyses.argret.DomK;
import petablox.analyses.invk.DomI;
import petablox.project.ClassicProject;
import petablox.project.ITask;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.project.analyses.provenance.Tuple;
import soot.Unit;

@Petablox(name = "kobj-gen")
public class KOBJProbGenerator extends petablox.project.analyses.provenance.RefineProblemGenerator {
	String client;

	ProgramRel IKRel;
	ProgramRel HKRel;
	ProgramRel OKRel;
	ProgramRel allowHRel;
	ProgramRel denyHRel;
	ProgramRel allowORel;
	ProgramRel denyORel;

	DomI domI;
	DomH domH;
	DomK domK;
	
	int maxK = 10;
	
	@Override
	public void run() {
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domK = (DomK) ClassicProject.g().getTrgt("K");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		client = System.getProperty("chord.provenance.client", "polysite");
		IKRel = (ProgramRel) ClassicProject.g().getTrgt("IK");
	
		HKRel = (ProgramRel) ClassicProject.g().getTrgt("HK");
		OKRel = (ProgramRel) ClassicProject.g().getTrgt("OK");
		allowHRel = (ProgramRel) ClassicProject.g().getTrgt("AllowH");
		denyHRel = (ProgramRel) ClassicProject.g().getTrgt("DenyH");
		allowORel = (ProgramRel) ClassicProject.g().getTrgt("AllowO");
		denyORel = (ProgramRel) ClassicProject.g().getTrgt("DenyO");
		this.setParameter(0);
		super.run();
	}

	private void setParameter(int k){
		Map<Unit,Integer> abs = new HashMap<Unit,Integer>();
		for (int i = 0; i < domH.size(); i++) {
			Unit H = (Unit) domH.get(i);
			abs.put(H, 0);
		}
		this.setParameter(abs);
	}
	
	private void setParameter(Map<Unit,Integer> abs){
		HKRel.zero();
		allowHRel.zero();
		denyHRel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Unit H = (Unit) domH.get(i);
			Integer k = abs.get(H);
			if (k == null||k==0)
				k = 1;
			setHK(H,k);
		}
		HKRel.save();
		allowHRel.save();
		denyHRel.save();
		ClassicProject.g().setTaskDone(HKRel);
		ClassicProject.g().setTaskDone(allowHRel);
		ClassicProject.g().setTaskDone(denyHRel);

		OKRel.zero();
		allowORel.zero();
		denyORel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Unit H = (Unit) domH.get(i);
			Integer k = abs.get(H);
			if (k == null)
				k = 0;
			setOK(H,k);
		}
		OKRel.save();
		allowORel.save();
		denyORel.save();
		ClassicProject.g().setTaskDone(OKRel);
		ClassicProject.g().setTaskDone(allowORel);
		ClassicProject.g().setTaskDone(denyORel);
	}
	private void setHK(Unit q, int k){
		HKRel.add(q,k);
		for(int i = 0; i <= k; i++){
			allowHRel.add(q,i);
		}
		for(int i = k+1; i <= maxK; i++){
			denyHRel.add(q,i);
		}
	}
	
	private void setOK(Unit q, int k){
		OKRel.add(q,k);
		for(int i = 0; i <= k; i++){
			allowORel.add(q,i);
		}
		for(int i = k+1; i <= maxK; i++){
			denyORel.add(q,i);
		}	
	}
	@Override
	public Set<String> getInputRelations() {
		Set<String> ret = new HashSet<String>();
		//input relations from kobj-bit-init.dlog
		ret.add("initCOC");
		ret.add("initCHC");
		ret.add("truncCKC");
//		ret.add("HK");
//		ret.add("OK");
		ret.add("roots");
		ret.add("IM");
		ret.add("VH");
		ret.add("MI");
		ret.add("MH");
		ret.add("CL");
		ret.add("IinvkArg0");
		ret.add("statM");
		ret.add("AllowH");
		ret.add("DenyH");
		ret.add("AllowO");
		ret.add("DenyO");
		ret.add("thisMV");
		
		//input relations from cspa-kobj.dlog
		ret.add("HT");
		ret.add("cha");
		ret.add("sub");
		ret.add("MI");
		ret.add("statIM");
		ret.add("specIM");
		ret.add("virtIM");
		ret.add("MobjValAsgnInst");
		ret.add("MobjVarAsgnInst");
		ret.add("MgetInstFldInst");
		ret.add("MputInstFldInst");
		ret.add("MgetStatFldInst");
		ret.add("MputStatFldInst");
		ret.add("clsForNameIT");
		ret.add("objNewInstIH");
		ret.add("objNewInstIM");
		ret.add("conNewInstIH");
		ret.add("conNewInstIM");
		ret.add("aryNewInstIH");
		ret.add("classT");
		ret.add("staticTM");
		ret.add("staticTF");
		ret.add("clinitTM");
		ret.add("MmethArg");
		ret.add("MspcMethArg");
		ret.add("IinvkArg");
		ret.add("IinvkArg0");
		ret.add("IinvkRet");
		ret.add("argCopy");
		ret.add("retCopy");
		ret.add("VCfilter");
		ret.add("CH");
		ret.add("epsilonM");
		ret.add("kobjSenM");
		ret.add("ctxtCpyM");
		
		//input relations from the client
		if(client.equals("polysite")){
			ret.add("virtIM");
			ret.add("checkExcludedI");
		}
		else
			if(client.equals("downcast")){
				ret.add("checkExcludedM");
				ret.add("McheckCastInst");
			}
			else
				throw new RuntimeException("Unknown client type: "+client);
		return ret;
	}

	@Override
	public String getQueryRelation() {
//		if(client.equals("polysite"))
//			return "polySite";
//		else
//			throw new RuntimeException("Unknown client type: "+client);
		return null;
	}

	@Override
	public int getWeight(Tuple t) {
//		String relName = t.getRelName();
//		if(relName.equals("DenyO")||relName.equals("DenyH"))
//			 return 1;
		return 1;
	}

	@Override
	public List<ITask> getTasks() {
		List<ITask> tasks = new ArrayList<ITask>();
		tasks.add(ClassicProject.g().getTask("cipa-0cfa-dlog"));
		tasks.add(ClassicProject.g().getTask("simple-pro-ctxts-java"));
		tasks.add(ClassicProject.g().getTask("pro-argCopy-dlog"));
//		tasks.add(ClassicProject.g().getTask("objNewInstIM-java"));
//		tasks.add(ClassicProject.g().getTask("kobj-bit-init-dlog"));
//		tasks.add(ClassicProject.g().getTask("pro-cspa-kobj-dlog"));
//		if(client.equals("polysite"))
//			tasks.add(ClassicProject.g().getTask("polysite-dlog"));
//		else
//			throw new RuntimeException("Unknown client "+client);
		return tasks;
	}

	@Override
	public Set<String> getDoms() {
		Set<String> ret = new HashSet<String>();
		ret.add("I");
		ret.add("H");
		ret.add("M");		
		ret.add("K");
		ret.add("V");
		ret.add("C");

		ret.add("T");
		ret.add("F");
		ret.add("Z");
		return ret;
	}

}
