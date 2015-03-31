package chord.analyses.typestate;

import java.util.Collections;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Class.jq_Field;

public class RegisterAccessPath extends AccessPath {
    public final Register var; // non-null
    
    public boolean isRet = false;

    public RegisterAccessPath(Register v, List<jq_Field> fields) {
        super(fields);
        assert (v != null);
        this.var = v;
    }

    public RegisterAccessPath(Register v) {
        super(Collections.EMPTY_LIST);
        this.var = v;
    }

    @Override
    public int hashCode() {
        return 31 * var.hashCode() + super.hashCode()+(this.isRet?17:0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof RegisterAccessPath) {
            RegisterAccessPath that = (RegisterAccessPath) obj;
            return var == that.var && fields.equals(that.fields)&&this.isRet == that.isRet;
        }
        return false;
    }

    @Override
    public String toString() {
        return var + super.toString()+"(isRet = "+isRet+")";
    }
}
