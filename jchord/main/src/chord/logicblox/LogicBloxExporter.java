package chord.logicblox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import chord.bddbddb.Dom;
import chord.bddbddb.Rel;
import chord.project.ChordException;
import chord.project.Config;
import chord.project.Config.DatalogEngineType;
import chord.util.Utils;

/**
 * A class for exporting domains and relations to LogicBlox in a generic way. 
 *
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxExporter {
    // configuration variables
    private String delim = Config.logicbloxInputDelim;
    private String workDir = Config.logicbloxWorkDirName;
    private String workspace = Config.logicbloxWorkspace;
    private DatalogEngineType engineType;

    public LogicBloxExporter() {
        this(Config.datalogEngine);
    }
    
    public LogicBloxExporter(DatalogEngineType engineType) {
        setEngineType(engineType);
    }
    
    /**
     * Saves a given relation to file and loads it into the LB workspace.
     * 
     * @param relation the relation to save
     */
    public void saveRelation(Rel relation) {
        String relName = relation.getName();
        File factsFile = new File(workDir, relName + ".csv");
        saveRelationData(relation, factsFile);
        
        File typeFile = new File(workDir, relName + ".type");
        saveRelationType(relation, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveRelationImport(relation, importFile, factsFile);
        
        LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }

    /**
     * Creates the type signature for <code>relation</code> and saves it 
     * to <code>typeFile</code>.
     * 
     * @param relation the relation to save
     * @param typeFile the output file
     * @throws ChordException if an error occurs writing the file
     */
    private void saveRelationType(Rel relation, File typeFile) {
        String relName           = relation.getName();
        String domainConstraints = getDomainConstraints(relation);
        String domainVars        = getRelationVariablesList(relation);
        
        PrintWriter out = null;
        try {
            out = new PrintWriter(typeFile, "UTF-8");
        } catch (IOException e) {
            throw new ChordException(e);
        }
        out.println(relName + "(" + domainVars + ") -> " + domainConstraints + ".");
        Utils.close(out);
        if (out.checkError()) {
            throw new ChordException("An error occurred writing relation type for " + 
                relName + " to " + typeFile.getAbsolutePath());
        }
    }
    
    /**
     * Saves relation data to a delimited file suitable for use by 
     * the LB file-predicate import mechanism.
     *  
     * @param relation      the relation data to export
     * @param destination   the output file
     * @throws ChordException if an error occurs writing the file
     */
    private void saveRelationData(Rel relation, File destination) {
        final String DELIM = this.delim;
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(destination), "UTF-8"), false);
        } catch (IOException e) {
            throw new ChordException(e);
        }

        StringBuilder sb = new StringBuilder();
        for (int[] row: relation.getAryNIntTuples()) {
            sb.setLength(0);
            for (int col: row)
                sb.append(col).append(DELIM);
            sb.setLength(sb.length() - DELIM.length()); // remove trailing delim
            out.println(sb.toString());
        }
        
        out.flush();
        Utils.close(out);
        if (out.checkError()) {
            throw new ChordException("An error occurred writing relation " + 
                relation.getName() + " data to " + destination.getAbsolutePath());
        }
    }
    
    /**
     * Saves a set of import commands suitable for use with <tt>lb exec</tt> to 
     * import the relation data into the LB workspace.
     * 
     * @param relation    the relation to import
     * @param importFile  the output file
     * @param factsFile   the delimited file containing the actual data
     * @throws ChordException if an error occurs writing the file
     */
    private void saveRelationImport(Rel relation, File importFile, File factsFile) {
        Dom<?>[] doms            = relation.getDoms();
        String relName           = relation.getName();
        String domainVars        = getRelationVariablesList(relation);
        boolean isLB3 = isLB3();
        
        try {
            PrintWriter out = new PrintWriter(importFile, "UTF-8");
            String idList = makeVarList("id", doms.length);
            String intType = getIntType();
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0, size = doms.length; i < size; ++i)
                sb.append(intType).append("(id").append(i).append("),");
            sb.setLength(sb.length() - 1);
            String idConstraints = sb.toString();
            
            out.println(createImportRelation(idList, idConstraints, factsFile));
            
            sb = new StringBuilder("+");
            sb.append(relName).append('(').append(domainVars).append(") <- _in(");
            if (!isLB3)
                sb.append("_; ");
            sb.append(idList);
            sb.append("), ");
            for (int i = 0, size = doms.length; i < size; ++i) {
                Dom<?> dom = doms[i];
                String domName = dom.getName();
                sb.append(domName).append("(d").append(i).append("), ")
                    .append(domName).append("_values[id").append(i).append(", _] = d")
                    .append(i).append(',');
            }
            sb.setLength(sb.length() - 1);
            sb.append(".\n");
            out.println(sb.toString());
            
            Utils.close(out);
            if (out.checkError()) {
                throw new ChordException("There as an error writing the import command for " 
                    + relName + " to " + importFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new ChordException(e);
        }
    }
    
    
    /**
     * Builds the import relation definitions used to import delimited-file data to LogicBlox.
     * <p>
     * This declares a predicate <code>_in</code> using the variable list and type constraints 
     * given.  It also handles LB 3 vs LB 4 differences and various <code>lang:physical:*</code> 
     * declarations that are required.
     * <p>
     * For example, use <code>varList</code> = "v1, v2" and <code>typeConstraints</code> = 
     * "int(v1), string(v2)".  
     * 
     * @param varList           the list of input predicate variables
     * @param typeConstraints   the type constraints of these variables
     * @param factsFile         the delimited file containing the data to import
     * @return the import relation definitions
     */
    private String createImportRelation(String varList, String typeConstraints, File factsFile) {
        StringBuilder sb = new StringBuilder("_in(");
        final boolean isLB3 = isLB3();
        if (!isLB3)
            sb.append("offset; ");
        sb.append(varList).append(") -> ");
        if (!isLB3)
            sb.append(getIntType()).append("(offset),");
        sb.append(typeConstraints).append(".\n");
        sb.append("lang:physical:filePath[`_in] = \"").append(factsFile.getAbsolutePath()).append("\".\n");
        if (isLB3)
            sb.append("lang:physical:storageModel[`_in] = \"DelimitedFile\".\n");
        else
            sb.append("lang:physical:fileMode[`_in] = \"import\".\n");
        sb.append("lang:physical:delimiter[`_in] = \"").append(this.delim).append("\".\n");
        return sb.toString();
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
    private String getDomainConstraints(Rel relation) {
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
    private String makeVarList(String varPrefix, int size) {
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
    private String getRelationVariablesList(Rel relation) {
        return makeVarList("d", relation.getDoms().length);
    }
    
    /**
     * Returns the integer type depending on the LB version.
     * @return the int type
     */
    private String getIntType() {
        return engineType == DatalogEngineType.LOGICBLOX3 ? "uint[64]" : "int";
    }
    
    private boolean isLB3() { return engineType == DatalogEngineType.LOGICBLOX3; }
    
    public String getDelim() {
        return delim;
    }

    public void setDelim(String delim) {
        this.delim = delim;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

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
