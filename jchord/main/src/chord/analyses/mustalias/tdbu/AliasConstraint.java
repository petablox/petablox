package chord.analyses.mustalias.tdbu;

import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.GlobalAccessPath;
import chord.analyses.typestate.RegisterAccessPath;
import chord.project.analyses.tdbu.Constraint;
import chord.project.analyses.tdbu.Utilities;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

/**
 * Immutable. Only contains conjunction
 * 
 * @author xin
 * 
 */
public class AliasConstraint implements Constraint, Cloneable {
	private boolean isTrue;
	private boolean isFalse;

//	 Constraint over the allocation site. It can has the form alpha_h = h or
//	 alpha_h != h1 /\ alpha_h!=h2.
//	 (equalSite = null) and (notEqualSite.size()=0) = 0. If equalSite =null &&
//	 notEqual.size() =0, it means we have no constraint
//	 over the allocation site
	// private Quad equalSite = null;
	// private Set<Quad> notEqualSite = new ArraySet<Quad>();

	// Constraint over the must access path set. It can has the form v \in
	// accessSet or v \notin accessSet. If the sizes of the all set
	// are 0, it means we don't have constraint over the access path set.
	private Set<Variable> inVSet = new ArraySet<Variable>();
	private Set<Variable> notInVSet = new ArraySet<Variable>();
	private FieldBitSet notInFSet = new FieldBitSet();

	// Constraint over the edge type. isFull and isTrue = false. If both are
	// false, it means we don't have constraints over the edge type
	// private boolean isFull = false;
	// private boolean isNull = false;

	public AliasConstraint() {
		isTrue = false;
		isFalse = false;
	}

	public AliasConstraint(boolean init) {
		if (init) {
			isTrue = true;
			isFalse = false;
		} else {
			isTrue = false;
			isFalse = true;
		}
	}

	@Override
	public boolean isFalse() {
		return isFalse;
	}

	@Override
	public boolean isTrue() {
		return isTrue;
	}

	public boolean statisfy(ArraySet<AccessPath> msset) {
		// if(this.isNull)//Something locally allocated inside the current
		// function, can satisfy anything
		// return true;
		// //Check the allocation site. I guess it's not necessary, since it
		// mainly affects the null edge ones
		// if(equalSite !=null && !equalSite.equals(alloc))
		// return false;
		// if(!notEqualSite.isEmpty() && !notEqualSite.contains(alloc))
		// return false;
		if (this.isTrue())
			return true;
		if (this.isFalse())
			return false;

		// Check the access path set

		for (Variable v : inVSet) {
			boolean containV = false;
			for (AccessPath ap : msset) {
				if (ap instanceof RegisterAccessPath && v.isLocal()) {
					RegisterAccessPath rap = (RegisterAccessPath) ap;
					if (rap.var.equals(v.getLocal()))
						containV = true;
				}
				if (ap instanceof GlobalAccessPath && v.isGlobal()) {
					GlobalAccessPath gap = (GlobalAccessPath) ap;
					if (gap.global.equals(v.getGlobal()))
						containV = true;
				}
			}
			if (!containV)
				return false;
		}

		for (AccessPath ap : msset) {
			if (ap instanceof RegisterAccessPath) {
				RegisterAccessPath rap = (RegisterAccessPath) ap;
				if (notInVSet.contains(new Variable(rap.var)))
					return false;
			} else if (ap instanceof GlobalAccessPath) {
				GlobalAccessPath gap = (GlobalAccessPath) ap;
				if (notInVSet.contains(new Variable(gap.global)))
					return false;
			}
			for (jq_Field f : ap.fields)
				if (notInFSet.contains(f))
					return false;
		}
		return true;
	}

	/**
	 * Check whether the BUEdge specified by the parameters satisfy current
	 * constraint. If so, return the combined the constraint; else return False
	 * 
	 * @param minusSet
	 * @param unionSet
	 * @param cons
	 * @return
	 */
	public AliasConstraint statisfy(Set<Variable> minusSet,
			Set<Pair<Variable, Variable>> unionSet, AliasConstraint cons) {
		if (this.isTrue())
			return cons;
		if (this.isFalse() || cons.isFalse())
			return new AliasConstraint(false);
		AliasConstraint ret = new AliasConstraint();
		for(Variable v: this.inVSet){
			if(AliasUtilities.firstContain(unionSet, v))
				continue;
			if(minusSet.contains(v))
				return new AliasConstraint(false);
			if(cons.notInVSet.contains(v))
				return new AliasConstraint(false);
			ret.inVSet.add(v);
		}
		ret.inVSet.addAll(cons.inVSet);
		for(Variable v: this.notInVSet){
			if(AliasUtilities.firstContain(unionSet, v))
				return new AliasConstraint(false);
			if(minusSet.contains(v))
				continue;
			if(cons.inVSet.contains(v))
				return new AliasConstraint(false);
			ret.notInVSet.add(v);
		}
		ret.notInVSet.addAll(cons.notInVSet);
		ret.notInFSet.addAll(this.notInFSet);
		ret.notInFSet.addAll(cons.notInFSet);
		return ret;
	}

	@Override
	public Constraint intersect(Constraint that) {
		if (that.isTrue() || this.isFalse())
			return this;
		if (that.isFalse() || this.isTrue())
			return that;
		if (!(that instanceof AliasConstraint))
			throw new RuntimeException(
					"Currently AliasConstraint doesn't support intersection with non-AliasConstraint object");

		AliasConstraint acThat = (AliasConstraint) that;

		AliasConstraint falseCons = new AliasConstraint(false);
		// Check if there's a conflict between these two constraints

		// First, check the constraints over allocation site
		// if(this.equalSite != null){
		// if(acThat.equalSite != null &&
		// !this.equalSite.equals(acThat.equalSite))
		// return falseCons;
		// if(acThat.notEqualSite.contains(this.equalSite))
		// return falseCons;
		// }
		// if(acThat.equalSite != null){
		// if(this.equalSite != null &&
		// !acThat.equalSite.equals(this.equalSite))
		// return falseCons;
		// if(this.notEqualSite.contains(acThat.equalSite))
		// return falseCons;
		// }
		// Second, check the constraints over access path set
		if (Utilities.ifIntersect(inVSet, acThat.notInVSet))
			return falseCons;
		if (Utilities.ifIntersect(notInVSet, acThat.inVSet))
			return falseCons;
		// Third, check the constraints over the edge type
		// if(this.isFull && acThat.isNull)
		// return falseCons;
		// if(this.isNull && acThat.isFull)
		// return falseCons;

		// Everything is OK, now create the intersection of these two
		// constraints
		AliasConstraint ret = new AliasConstraint();
		// if(this.equalSite!=null)
		// ret.equalSite = this.equalSite;
		// else
		// if(acThat.equalSite!=null)
		// ret.equalSite = acThat.equalSite;
		// ret.notEqualSite = Utilities.union(this.notEqualSite,
		// acThat.notEqualSite);

		ret.inVSet = Utilities.union(this.inVSet, acThat.inVSet);
		ret.notInVSet = Utilities.union(this.notInVSet, acThat.notInVSet);
		ret.notInFSet = AliasUtilities.union(this.notInFSet, acThat.notInFSet);

		// ret.isFull = this.isFull || acThat.isFull;
		// ret.isNull = this.isNull || acThat.isNull;

		return ret;
	}

	@Override
	public AliasConstraint clone() {
		AliasConstraint ret = new AliasConstraint();
		ret.isFalse = this.isFalse;
		ret.isTrue = this.isTrue;
		ret.inVSet = new ArraySet<Variable>(this.inVSet);
		ret.notInVSet = new ArraySet<Variable>(this.notInVSet);
		ret.notInFSet = new FieldBitSet(this.notInFSet);
		return ret;
	}

	public Set<Variable> getInVSet() {
		return inVSet;
	}

	public Set<Variable> getNotInVSet() {
		return notInVSet;
	}

	public Set<jq_Field> getNotInFSet() {
		return notInFSet;
	}

	@Override
	public boolean contains(Constraint that) {
		if(that instanceof AliasConstraint){
			AliasConstraint aliasThat = (AliasConstraint)that;
			if(this.isTrue())
				return true;
			if(that.isFalse())
				return true;
			if(this.isFalse() || that.isTrue())
				return false;
			return aliasThat.inVSet.containsAll(inVSet)&&
					aliasThat.notInVSet.containsAll(notInVSet)&&
					aliasThat.notInFSet.containsAll(notInFSet);
		}else
			throw new RuntimeException("Unsupported type: "+that);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inVSet == null) ? 0 : inVSet.hashCode());
		result = prime * result + (isFalse ? 1231 : 1237);
		result = prime * result + (isTrue ? 1231 : 1237);
		result = prime * result
				+ ((notInFSet == null) ? 0 : notInFSet.hashCode());
		result = prime * result
				+ ((notInVSet == null) ? 0 : notInVSet.hashCode());
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
		AliasConstraint other = (AliasConstraint) obj;
		if (inVSet == null) {
			if (other.inVSet != null)
				return false;
		} else if (!inVSet.equals(other.inVSet))
			return false;
		if (isFalse != other.isFalse)
			return false;
		if (isTrue != other.isTrue)
			return false;
		if (notInFSet == null) {
			if (other.notInFSet != null)
				return false;
		} else if (!notInFSet.equals(other.notInFSet))
			return false;
		if (notInVSet == null) {
			if (other.notInVSet != null)
				return false;
		} else if (!notInVSet.equals(other.notInVSet))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AliasConstraint [isTrue=" + isTrue + ", isFalse=" + isFalse
				+ ", inVSet=" + inVSet + ", notInVSet=" + notInVSet
				+ ", notInFSet=" + notInFSet + "]";
	}

	public int size() {
		return inVSet.size()+notInVSet.size()+notInFSet.size();
	}

	public AliasConstraint lift(Set<Register> args){
		Set<Variable> newInV = new ArraySet<Variable>();
		for(Variable v : inVSet){
			if(v.isGlobal())
				newInV.add(v);
			else{
				if(!args.contains(v.getLocal()))
					return new AliasConstraint(false);
				newInV.add(v);
			}
		}
		Set<Variable> newNotInV = new ArraySet<Variable>();
		for(Variable v : notInVSet){
			if(v.isGlobal())
				newNotInV.add(v);
			else{
				if(args.contains(v.getLocal()))
					newNotInV.add(v);
			}
		}
		FieldBitSet newNotInF = this.notInFSet;
		AliasConstraint ret = new AliasConstraint();
		ret.inVSet = newInV;
		ret.notInVSet = newNotInV;
		ret.notInFSet = newNotInF;
		return ret;
	}
	
}
