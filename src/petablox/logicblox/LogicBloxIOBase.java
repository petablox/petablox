package petablox.logicblox;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.Config;
import petablox.project.Config.DatalogEngineType;

/**
 * Convenience base class for {@link LogicBloxImporter} and {@link LogicBloxExporter}.
 */
public abstract class LogicBloxIOBase {

    protected String workspace = Config.logicbloxWorkspace;
    protected DatalogEngineType engineType;

    public LogicBloxIOBase() {
        this(Config.datalogEngine);
    }
    
    public LogicBloxIOBase(DatalogEngineType engineType) {
        setEngineType(engineType);
    }

    /**
     * Builds the type constraints for the domains of a relation.
     * 
     * Return values look like:<br />
     * <code>D0(d0), D1(d1), ...</code>
     * 
     * @param relation the relation to generate a type constraint string for
     * @return the type constraints
     */
    protected String getDomainConstraints(Rel relation) {
        StringBuilder sb = new StringBuilder();
        Dom<?>[] doms = relation.getDoms();
        for (int i = 0, size = doms.length; i < size; ++i) {
            Dom<?> dom = doms[i];
            sb.append(dom.getName()).append("(d").append(i).append("),");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Creates a list of generic variables numbered from 0, e.g. 
     * "v0, v1, ..." if <code>varPrefix</code> is "v".
     * 
     * @param varPrefix the variable prefix
     * @param size      the length of the variable sequence
     * @return the variable list
     */
    protected String makeVarList(String varPrefix, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; ++i)
            sb.append(varPrefix).append(i).append(',');
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Returns the variables for a generic relation, which is 
     * of the form "d0, d1, ...".
     * 
     * @param relation the relation to generate the list for
     * @return the variable list
     */
    protected String getRelationVariablesList(Rel relation) {
        return makeVarList("d", relation.getDoms().length);
    }

    /**
     * Returns the integer type depending on the LB version.
     * @return the int type
     */
    protected String getIntType() {
        return engineType == DatalogEngineType.LOGICBLOX3 ? "uint[64]" : "int";
    }

    protected boolean isLB3() { return engineType == DatalogEngineType.LOGICBLOX3; }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public DatalogEngineType getEngineType() {
        return engineType;
    }

    /**
     * Sets the engine type to use.
     * 
     * @param engineType the engine type
     * @throws IllegalArgumentException if <code>engineType</code> is not a LogicBlox engine
     */
    public void setEngineType(DatalogEngineType engineType) {
        if( engineType == null ) throw new NullPointerException("engineType is null");
        switch (engineType) {
        case LOGICBLOX3:
        case LOGICBLOX4:
            this.engineType = engineType;
            break;
        default:
            throw new IllegalArgumentException("Not a LogicBlox engine type: " + engineType);
        }
    }

}