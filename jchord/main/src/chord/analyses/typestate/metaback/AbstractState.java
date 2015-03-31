package chord.analyses.typestate.metaback;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.util.ArraySet;

/**
 * The abstract state tracked by type-state analysis for an object.
 * 
 * @author xin
 */
public class AbstractState {
	public boolean isError;
	public final ArraySet<Register> ms;
	public final boolean canReturn;
	
	public AbstractState(boolean isError,ArraySet<Register> ms) {
		this(isError, ms, false);
	}

	public AbstractState(boolean isError,ArraySet<Register> ms, boolean ret) {
		this.isError = isError;
		assert (ms != null);
		this.ms = ms;
		canReturn = ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (canReturn ? 1231 : 1237);
		result = prime * result + (isError ? 1231 : 1237);
		result = prime * result + ((ms == null) ? 0 : ms.hashCode());
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
		AbstractState other = (AbstractState) obj;
		if (canReturn != other.canReturn)
			return false;
		if (isError != other.isError)
			return false;
		if (ms == null) {
			if (other.ms != null)
				return false;
		} else if (!ms.equals(other.ms))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AbstractState [isError=" + isError + ", ms=" + ms
				+ ", canReturn=" + canReturn + "]";
	}
	

}
