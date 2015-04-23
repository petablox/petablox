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
public class LogicBloxExporter extends LogicBloxIOBase {
    // configuration variables
    private String delim = Config.logicbloxInputDelim;
    private String workDir = Config.logicbloxWorkDirName;
    
    public LogicBloxExporter() {
        super();
    }
    
    public LogicBloxExporter(DatalogEngineType engineType) {
        super(engineType);
    }
    
    /**
     * Saves the given domain to file and loads it into the LB workspace.
     * 
     * @param dom the domain to save
     * @throws ChordException if an error occurs
     */
    public void saveDomain(Dom<?> dom) {
        String domName = dom.getName();
        File factsFile = new File(workDir, domName + ".csv");
        saveDomainData(dom, factsFile);
        
        File typeFile = new File(workDir, domName + ".type");
        saveDomainType(dom, typeFile);
        
        File importFile = new File(workDir, domName + ".import");
        saveDomainImport(dom, importFile, factsFile);
        
        LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);   
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
     * Writes domain data to a delimited file suitable for import.  
     * Data is written as pairs of (index, unique-string).
     * 
     * @param dom        the domain to save
     * @param factsFile  the destination file
     * @throws ChordException if an error occurs saving the data
     */
    private void saveDomainData(Dom<?> dom, File factsFile) {
        final String DELIM = this.delim;
        PrintWriter out = createPrintWriter(factsFile);
        for (int i = 0, size = dom.size(); i < size; ++i) {
            out.print(i);
            out.print(DELIM);
            out.println(dom.toUniqueString(i));
        }
        Utils.close(out);
        if (out.checkError()) {
            throw new ChordException("Error writing " + dom.getName() 
                + " domain facts to " + factsFile.getAbsolutePath());
        }
    }
    
    private PrintWriter createPrintWriter(File outFile) { return createPrintWriter(outFile, false); }
    private PrintWriter createPrintWriter(File outFile, boolean autoFlush) {
        try {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"), autoFlush);
        } catch (IOException e) {
            throw new ChordException(e);
        }
    }
    
    /**
     * Saves the domain's type declaration to file.
     * 
     * @param dom       the domain
     * @param typeFile  the output file
     * @throws ChordException if an error occurs writing the file
     */
    private void saveDomainType(Dom<?> dom, File typeFile) {
        String name    = dom.getName();

        // a new entity type
        StringBuilder type = new StringBuilder(name).append("(x) -> .\n");

        // with a constructor of (index, uniquestring)
        type.append(name).append("_values[id, val] = x -> ").append(name)
            .append("(x), ").append(getIntType()).append("(id), string(val).\n")
            .append("lang:constructor(`").append(name)
            .append("_values).\n");
        if (isLB3()) {
            // have to make scalable, values other than ScalableSparse are not
            // documented
            type.append("lang:physical:storageModel[`").append(name)
                .append("] = \"ScalableSparse\".\n")
                .append("lang:physical:capacity[`").append(name).append("] = ")
                .append(dom.size()).append(".\n");
        }
        
        PrintWriter out = createPrintWriter(typeFile, true);
        out.println(type.toString());
        Utils.close(out);
        if (out.checkError()) {
            throw new ChordException("Error writing " + name + " domain type to " 
                + typeFile.getAbsolutePath());
        }
    }

    
    /**
     * Saves import declarations for a domain.
     * 
     * @param dom         the domain to import
     * @param importFile  the output file
     * @param factsFile   the delimited data file that will be imported
     * @throws ChordException if an error occurs writing the file
     */
    private void saveDomainImport(Dom<?> dom, File importFile, File factsFile) {
        String name = dom.getName();
        PrintWriter out = createPrintWriter(importFile);
        out.println(createImportRelation("id, val",  getIntType() + "(id), string(val)", factsFile));
        out.println();
        out.println("+" + name + "(x), +" + name + "_values[id, val] = x <- _in(" + 
                (isLB3() ? "" : "_; ") + "id, val).");
        Utils.close(out);
        if (out.checkError()) {
            throw new ChordException("An error occurred writing import declaration of domain " 
                + name + " to " + importFile.getAbsolutePath());
        }
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
        
        PrintWriter out = createPrintWriter(typeFile, true);
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
        PrintWriter out = createPrintWriter(destination);

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
        
        PrintWriter out = createPrintWriter(importFile);
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
