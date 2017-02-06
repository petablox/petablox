package petablox.logicblox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.Config;
import petablox.project.PetabloxException;
import petablox.project.Config.DatalogEngineType;
import petablox.util.Utils;

/**
 * Convenience base class for {@link LogicBloxImporter} and {@link LogicBloxExporter}.
 */
public abstract class LogicBloxIOBase {

	// configuration variables
	protected int LB3_DOM_NAME_LIM = 1024;
	protected int LB3_TAG_NAME_LIM = 65536;
	protected int LB3_ANNOT_NAME_LIM = 65536;
	
	protected static String DOMS = "Doms";
	protected static String TAGS = "Tags";
	protected static String DOM_RANGES = "domRanges";
	protected static String DOM_TO_TAG_SUFFIX = "ToTag";
	protected static String TAG_TO_PGM = "TagToPgm";
	protected static String SUB_TAGS = "subTags";
	protected static String ANNOT_NAME = "AnnotationName";
	protected static String FIELD_ANNOT = "FieldAnnot";
	protected static String M_PARAM_ANNOT = "MethParamAnnot";
	protected static String M_RET_ANNOT = "MethRetAnnot";
	protected static String PGM_PT_ANNOT = "PgmPtAnnot";
		
    protected String delim = Config.logicbloxInputDelim;
    protected String workDir = Config.logicbloxWorkDirName;
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

    protected String getStringType() {
        return "string";
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

    protected PrintWriter createPrintWriter(File outFile) { return createPrintWriter(outFile, false); }
    protected PrintWriter createPrintWriter(File outFile, boolean autoFlush) {
        try {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"), autoFlush);
        } catch (IOException e) {
            throw new PetabloxException(e);
        }
    }
    
    /**
     * Saves the domain's type declaration to file.
     * 
     * @param dom       the domain
     * @param typeFile  the output file
     * @throws PetabloxException if an error occurs writing the file
     */
    protected void saveDomainType(String name, int size, File typeFile) {
        // a new entity type with ref-mode of index
        StringBuilder type = new StringBuilder(name).append("(x), ")
            .append(name).append("_index(x:index) -> ").append(getIntType()).append("(index).\n");
        
        // and a map to the string representation
        type.append(name).append("_string[x] = s -> ").append(name).append("(x), string(s).\n");
        
        if (isLB3()) {
            // have to make scalable, values other than ScalableSparse are not
            // documented
            type.append("lang:physical:storageModel[`").append(name)
                .append("] = \"ScalableSparse\".\n")
                .append("lang:physical:capacity[`").append(name).append("] = ")
                .append(size).append(".\n");
        }
        
        PrintWriter out = createPrintWriter(typeFile, true);
        out.println(type.toString());
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("Error writing " + name + " domain type to "
                + typeFile.getAbsolutePath());
        }
    }

    
    /**
     * Saves import declarations for a domain.
     * 
     * @param dom         the domain to import
     * @param importFile  the output file
     * @param factsFile   the delimited data file that will be imported
     * @throws PetabloxException if an error occurs writing the file
     */
    protected void saveDomainImport(String name, File importFile, File factsFile) {
        PrintWriter out = createPrintWriter(importFile);
        out.println(createImportRelation("id, val",  getIntType() + "(id), string(val)", factsFile));
        out.println();
        out.println("+" + name + "(x), +" + name + "_index[x] = id, +" + name + "_string[x] = val <- _in(" + 
                (isLB3() ? "" : "_; ") + "id, val).");
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("An error occurred writing import declaration of domain "
                + name + " to " + importFile.getAbsolutePath());
        }
    }
    
    protected void saveRelationType1(String relName, String str, File typeFile) {  
        PrintWriter out = createPrintWriter(typeFile, true);
        out.println(str);
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("An error occurred writing relation type for " +
                relName + " to " + typeFile.getAbsolutePath());
        }
    }
    
    protected void saveRelationImport1(String relName, String[] domNames, File importFile, File factsFile) {    
        String domainVars = makeVarList("d", domNames.length);
        boolean isLB3 = isLB3();
        
        PrintWriter out = createPrintWriter(importFile);
        String idList = makeVarList("id", domNames.length);
        String intType = getIntType();
        String strType = getStringType();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0, size = domNames.length; i < size; ++i)
        	if (domNames[i].equals("string"))
        		sb.append(strType).append("(id").append(i).append("),");
        	else
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
        for (int i = 0, size = domNames.length; i < size; ++i) {
            String domName = domNames[i];
            if (!domName.equals("int") && !domName.equals("string"))
            	sb.append(domName).append("_index[d").append(i).append("] = id").append(i).append(',');
            else
            	sb.append("d").append(i).append(" = id").append(i).append(',');
        }
        sb.setLength(sb.length() - 1);
        sb.append(".\n");
        out.println(sb.toString());
        
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("There as an error writing the import command for "
                + relName + " to " + importFile.getAbsolutePath());
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
    protected String createImportRelation(String varList, String typeConstraints, File factsFile) {
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
    
}