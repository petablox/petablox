package petablox.logicblox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.Config.DatalogEngineType;
import petablox.util.ArraySet;
import petablox.util.Utils;

/**
 * A class for exporting domains and relations to LogicBlox in a generic way. 
 *
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxExporter extends LogicBloxIOBase {
    // configuration variables
	private int LB3_DOM_NAME_LIM = 1024;
	private int LB3_TAG_NAME_LIM = 65536;
	private String DOMS = "Doms";
	private String TAGS = "Tags";
	private String DOM_RANGES = "domRanges";
	private String SUB_TAGS = "subTags";
	
    private String delim = Config.logicbloxInputDelim;
    private String workDir = Config.logicbloxWorkDirName;
    private static HashMap<String,Integer> domNdxMap = null;
    private static HashMap<String,Integer> newDomNdxMap = new HashMap<String,Integer>();
    private static ArraySet<String> newDomASet = new ArraySet<String>();
    
    public LogicBloxExporter() {
        super();
        domNdxMap = LogicBloxUtils.getDomNdxMap();
    }
    
    public LogicBloxExporter(DatalogEngineType engineType) {
        super(engineType);
        domNdxMap = LogicBloxUtils.getDomNdxMap();
    }
    
    public static HashMap<String,Integer> getNewDomNdxMap() {
    	return newDomNdxMap;
    }
    
    /**
     * Saves the given domain to file and loads it into the LB workspace.
     * 
     * @param dom the domain to save
     * @throws PetabloxException if an error occurs
     */
    public void saveDomain(Dom<?> dom) {
    	int sz = 0;
    	if (Config.populate) {
	    	if (domNdxMap.containsKey(dom.getName()))  
	    		sz = domNdxMap.get(dom.getName());
	    	newDomNdxMap.put(dom.getName(), sz + dom.size());
    	}
    	
        String domName = dom.getName();
        File factsFile = new File(workDir, domName + ".csv");
        saveDomainData(dom, factsFile, sz);
        
        File typeFile = new File(workDir, domName + ".type");
        saveDomainType(dom.getName(), dom.size(), typeFile);
        
        File importFile = new File(workDir, domName + ".import");
        saveDomainImport(dom.getName(), importFile, factsFile);
        
        if (!(Config.populate && LogicBloxUtils.domsExist()))
        //if (!(Config.populate && LogicBloxUtils.domsExist() && LogicBloxUtils.domContains(dom.getName())))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);  
        if (Config.populate && !LogicBloxUtils.domContains(dom.getName())) newDomASet.add(dom.getName());
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
     * @throws PetabloxException if an error occurs saving the data
     */
    private void saveDomainData(Dom<?> dom, File factsFile, int sz) {
        final String DELIM = this.delim;
        PrintWriter out = createPrintWriter(factsFile);
        for (int i = 0, size = dom.size(); i < size; ++i) {
            out.print(i + sz);
            out.print(DELIM);
            out.println(dom.toUniqueString(i));
        }
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("Error writing " + dom.getName()
                + " domain facts to " + factsFile.getAbsolutePath());
        }
    }
    
    private PrintWriter createPrintWriter(File outFile) { return createPrintWriter(outFile, false); }
    private PrintWriter createPrintWriter(File outFile, boolean autoFlush) {
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
    private void saveDomainType(String name, int size, File typeFile) {
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
    private void saveDomainImport(String name, File importFile, File factsFile) {
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
    
    /**
     * Creates the type signature for <code>relation</code> and saves it 
     * to <code>typeFile</code>.
     * 
     * @param relation the relation to save
     * @param typeFile the output file
     * @throws PetabloxException if an error occurs writing the file
     */
    private void saveRelationType(Rel relation, File typeFile) {
        String relName           = relation.getName();
        String domainConstraints = getDomainConstraints(relation);
        String domainVars        = getRelationVariablesList(relation);
        
        PrintWriter out = createPrintWriter(typeFile, true);
        out.println(relName + "(" + domainVars + ") -> " + domainConstraints + ".");
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("An error occurred writing relation type for " +
                relName + " to " + typeFile.getAbsolutePath());
        }
    }
    
    /**
     * Saves relation data to a delimited file suitable for use by 
     * the LB file-predicate import mechanism.
     *  
     * @param relation      the relation data to export
     * @param destination   the output file
     * @throws PetabloxException if an error occurs writing the file
     */
    private void saveRelationData(Rel relation, File destination) {
        final String DELIM = this.delim;
        PrintWriter out = createPrintWriter(destination);

        Dom<?>[] relDoms = relation.getDoms();
        int[] domSz = new int[relDoms.length];
        int i = 0;
        for (Dom<?> d : relDoms) {
        	if (Config.populate) {
	        	if (domNdxMap.containsKey(d.getName()))
	        		domSz[i++] = domNdxMap.get(d.getName());
	        	else
	        		domSz[i++] = 0;
        	} else
        		domSz[i++] = 0;
        }
        StringBuilder sb = new StringBuilder();
        for (int[] row: relation.getAryNIntTuples()) {
            sb.setLength(0);
            i = 0;
            for (int col: row)
                sb.append(col + domSz[i++]).append(DELIM);
            sb.setLength(sb.length() - DELIM.length()); // remove trailing delim
            out.println(sb.toString());
        }
        
        out.flush();
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("An error occurred writing relation " +
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
     * @throws PetabloxException if an error occurs writing the file
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
            sb.append(domName).append("_index[d").append(i).append("] = id").append(i).append(',');
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
    
    public void saveDomsDomain() {	
        String domName = DOMS;
        File factsFile = new File(workDir, domName + ".csv");
        final String DELIM = this.delim;
        PrintWriter out = createPrintWriter(factsFile);
        ArraySet<String> oldDoms = LogicBloxUtils.getDomASet();
        int sz = oldDoms.size();
        for (int i = 0, size = newDomASet.size(); i < size; ++i) {
            out.print(i + sz);
            out.print(DELIM);
            out.println(newDomASet.get(i));
        }
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("Error writing " + domName
                + " domain facts to " + factsFile.getAbsolutePath());
        }
        
        File typeFile = new File(workDir, domName + ".type");
        saveDomainType(domName, LB3_DOM_NAME_LIM, typeFile);
        
        File importFile = new File(workDir, domName + ".import");
        saveDomainImport(domName, importFile, factsFile);
        
        if (!(Config.populate && LogicBloxUtils.domsExist()))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);  
    }
    
    public void saveTagsDomain() {
    	 String domName = TAGS;
         File factsFile = new File(workDir, domName + ".csv");
         final String DELIM = this.delim;
         PrintWriter out = createPrintWriter(factsFile);
         ArraySet<String> tags = LogicBloxUtils.getTagASet();
         out.print(tags.size());
         out.print(DELIM);
         out.println(Config.multiTag);
         Utils.close(out);
         if (out.checkError()) {
             throw new PetabloxException("Error writing " + domName
                 + " domain facts to " + factsFile.getAbsolutePath());
         }
         
         File typeFile = new File(workDir, domName + ".type");
         saveDomainType(domName, LB3_TAG_NAME_LIM, typeFile);
         
         File importFile = new File(workDir, domName + ".import");
         saveDomainImport(domName, importFile, factsFile);
         
         if (!(Config.populate && LogicBloxUtils.domsExist()))
         	LogicBloxUtils.addBlock(typeFile);
         LogicBloxUtils.execFile(importFile);  
    }
    
    public void savedomRangeRelation() {
        String relName = DOM_RANGES;
        File factsFile = new File(workDir, relName + ".csv");
        saveDomRangeData(relName, factsFile);
        
        File typeFile = new File(workDir, relName + ".type");
        saveDomRangeType(relName, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveDomRangeImport(relName, importFile, factsFile);
        
        if (!(Config.populate && LogicBloxUtils.domsExist()))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveDomRangeData(String relName, File factsFile) {
    	
    }
    
    private void saveDomRangeType(String relName, File typeFile) {  
        PrintWriter out = createPrintWriter(typeFile, true);
        out.println(relName + "(d0,d1,d2,d3) -> " + TAGS + "(d0)," + DOMS + "(d1),int(d2),int(d3).");
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("An error occurred writing relation type for " +
                relName + " to " + typeFile.getAbsolutePath());
        }
    }
 
    private void saveDomRangeImport(String relName, File importFile, File factsFile) {
    	
    }
}
