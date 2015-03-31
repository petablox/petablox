package chord.analyses.mustalias.tdbu;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.field.DomF;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.GlobalAccessPath;
import chord.analyses.typestate.RegisterAccessPath;
import chord.analyses.var.DomV;

public class Variable {
public static DomV domV;
public static DomF domF;
private Register local;
private jq_Field global;
private boolean isRet = false;

	public Variable(Register r){
		this.local = r;
	}

	public Variable(Register r, boolean isRet){
		this(r);
		this.isRet = isRet;
	}
	
	public Variable(jq_Field g){
		this.global = g;
	}
	
	public boolean isGlobal(){
		return global != null;
	}
	
	public boolean isLocal(){
		return local != null;
	}
	
	public Register getLocal(){
		return local;
	}
	
	public jq_Field getGlobal(){
		return global;
	}
	
	public boolean isRet() {
		return isRet;
	}

	public void setRet(boolean isRet) {
		this.isRet = isRet;
	}

	public Variable lift(){
		if(isRet == true)
			throw new RuntimeException("Variable has been lifted: "+this);
		if(this.isGlobal())
			throw new RuntimeException("Cannot lift a global variable "+this);
		Variable ret = new Variable(this.local,true);
		return ret;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((global == null) ? 0 : global.hashCode());
		result = prime * result + (isRet ? 1231 : 1237);
		result = prime * result + ((local == null) ? 0 : local.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Variable other = (Variable) obj;
		if (global == null) {
			if (other.global != null)
				return false;
		} else if (!global.equals(other.global))
			return false;
		if (isRet != other.isRet)
			return false;
		if (local == null) {
			if (other.local != null)
				return false;
		} else if (!local.equals(other.local))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Variable [local=" + local +"("+(local==null?0:local.hashCode())+")"+ ", global=" + global + ", isRet=" + isRet + "]";
	}

	public boolean matches(AccessPath ap){
		if(ap instanceof RegisterAccessPath && this.isLocal()){
			RegisterAccessPath rap = (RegisterAccessPath)ap;
			if(rap.var.equals(this.getLocal()))
				return true;
		}
		if(ap instanceof GlobalAccessPath && this.isGlobal()){
			GlobalAccessPath gap = (GlobalAccessPath)ap;
			if(gap.global.equals(this.getGlobal()))
				return true;
		}
		return false;
	}
	
	public AccessPath replacePrefix(AccessPath ap){
		if(this.isGlobal())
			return new GlobalAccessPath(global,ap.fields);
		else{
			RegisterAccessPath ret = new RegisterAccessPath(local,ap.fields);
			if(this.isRet)
				ret.isRet=true;
			return ret;
			}
	}

	public AccessPath createAccessPath(){
		if(this.isGlobal())
			return new GlobalAccessPath(global);
		else
			return new RegisterAccessPath(local);
	}
}
