package petablox.logicblox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.Config.DatalogEngineType;
import petablox.util.ArraySet;
import petablox.util.Utils;
import petablox.util.tuple.object.Pair;

/**
 * A class for exporting domains and relations to LogicBlox in a generic way. 
 *
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxExporter extends LogicBloxIOBase {
	
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
          String nm = dom.getName().substring(Config.multiTag.length());
          if (domNdxMap.containsKey(nm))  
            sz = domNdxMap.get(nm);
          else
            domNdxMap.put(nm,0);
          newDomNdxMap.put(nm, sz + dom.size());
        }

        String domName = dom.getName();
        File factsFile = new File(workDir, domName + ".csv");
        saveDomainData(dom, factsFile, sz);
        
        File typeFile = new File(workDir, domName + ".type");
        saveDomainType(dom.getName(), dom.size(), typeFile);
        
        File importFile = new File(workDir, domName + ".import");
        saveDomainImport(dom.getName(), importFile, factsFile);
        
        if (!(Config.populate && LogicBloxUtils.domsExist() && LogicBloxUtils.domContains(dom.getName())))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);  
        
        if (Config.populate) saveDomToTagRelation(dom, sz);
        if (Config.populate && !LogicBloxUtils.domContains(dom.getName())) newDomASet.add(dom.getName());
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
        		String nm = d.getName().substring(Config.multiTag.length());
	        	if (domNdxMap.containsKey(nm))
	        		domSz[i++] = domNdxMap.get(nm);
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
        
        String str = relName + "(" + domainVars + ") -> " + domainConstraints + ".";
        saveRelationType1(relName, str, typeFile);
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
        String[] domNames        = new String[doms.length];
        
        for(int i = 0; i < doms.length; i++)
        	domNames[i] = doms[i].getName();
        
        saveRelationImport1(relName, domNames, importFile, factsFile);
    }
             
    public void saveDomsDomain() {	
    	if (newDomASet.size() == 0) return;
    	if (!Config.populate) return;
    	
        String domName = DOMS;
        File factsFile = new File(workDir, domName + ".csv");
        final String DELIM = this.delim;
        
        //saveDomainData
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
        
        if (!LogicBloxUtils.domsExist())
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);  
    }
    
    public void saveTagsDomain() {
    	 String domName = TAGS;
         File factsFile = new File(workDir, domName + ".csv");
         final String DELIM = this.delim;
         
         //saveDomainData
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
         
         if (!LogicBloxUtils.domsExist())
         	LogicBloxUtils.addBlock(typeFile);
         LogicBloxUtils.execFile(importFile);  
    }
    
    public void saveDomRangeRelation() {
    	if (!Config.populate) return;
        String relName = DOM_RANGES;
        File factsFile = new File(workDir, relName + ".csv");
        saveDomRangeData(relName, factsFile);
        
        File typeFile = new File(workDir, relName + ".type");
        saveDomRangeType(relName, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveDomRangeImport(relName, importFile, factsFile);
        
        if (!LogicBloxUtils.domsExist())
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveDomRangeData(String relName, File factsFile) {
    	ArraySet<String> tagASet = LogicBloxUtils.getTagASet();
    	int tagSetSz = tagASet.size();
    	ArraySet<String> domASet = LogicBloxUtils.getDomASet();
    	int domSetSz = domASet.size();
    	final String DELIM = this.delim;
    	PrintWriter out = createPrintWriter(factsFile);

    	if (Config.populate) {
	    	for(String domName : newDomNdxMap.keySet()) {
	    		StringBuilder sb = new StringBuilder();
	    		sb.append(tagSetSz).append(DELIM);
	    		String nm = Config.multiTag + domName;
	    		if (newDomASet.contains(nm))
	    			sb.append(newDomASet.indexOf(nm)+domSetSz).append(DELIM);
	    		else
	    			sb.append(domASet.indexOf(nm)).append(DELIM);
	    		int startRange = 0;
	    		if (domNdxMap.containsKey(domName)) startRange = domNdxMap.get(domName);
	    		sb.append(startRange).append(DELIM);
	    		sb.append(newDomNdxMap.get(domName) - 1);
	    		out.println(sb.toString());
	    	}
    	} else if (Config.analyze) {
    		for(String child : Config.tagList.split(",")) {
    			HashMap<String, ArrayList<Pair<Integer, Integer>>> perTagRange = LogicBloxUtils.domRanges.get(child);
    			for(String domName : perTagRange.keySet()) {
    				ArrayList<Pair<Integer, Integer>> alist = perTagRange.get(domName);
    				for (Pair<Integer,Integer> pr : alist) {
	    	    		StringBuilder sb = new StringBuilder();
	    	    		sb.append(tagSetSz).append(DELIM);
	    	    		sb.append(domASet.indexOf(domName)).append(DELIM);	
	    	    		sb.append(pr.val0).append(DELIM);
	    	    		sb.append(pr.val1);
	    	    		out.println(sb.toString());
    				}
    	    	}
        	}
    	}
    	
    	out.flush();
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("An error occurred writing relation " +
    				relName + " data to " + factsFile.getAbsolutePath());
    	}
    }
    
    private void saveDomRangeType(String relName, File typeFile) {  
        String str = relName + "(d0,d1,d2,d3) -> " + TAGS + "(d0)," + DOMS + "(d1),int(d2),int(d3).";
        saveRelationType1(relName, str, typeFile);
    }
 
    private void saveDomRangeImport(String relName, File importFile, File factsFile) {
    	String[] domNames = new String[4];   
    	domNames[0] = TAGS;
    	domNames[1] = DOMS;
    	domNames[2] = "int";
    	domNames[3] = "int";
    	saveRelationImport1(relName, domNames, importFile, factsFile);
    }
    
    public void saveSubTagRelation() {
    	if (!Config.analyze) return;
    	
        String relName = SUB_TAGS;
        File factsFile = new File(workDir, relName + ".csv");
        saveSubTagData(relName, factsFile);
        
        File typeFile = new File(workDir, relName + ".type");
        saveSubTagType(relName, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveSubTagImport(relName, importFile, factsFile);
        
        if (LogicBloxUtils.subTags.keySet().size() == 0)
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveSubTagData(String relName, File factsFile) {
    	ArraySet<String> tagASet = LogicBloxUtils.getTagASet();
    	int sz = tagASet.size();
    	final String DELIM = this.delim;
    	PrintWriter out = createPrintWriter(factsFile);

    	for(String child : Config.tagList.split(",")) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(sz).append(DELIM);
    		sb.append(tagASet.indexOf(child));
    		out.println(sb.toString());
    	}
    	out.flush();
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("An error occurred writing relation " +
    				relName + " data to " + factsFile.getAbsolutePath());
    	}
    }
    
    private void saveSubTagType(String relName, File typeFile) {  
        String str = relName + "(d0,d1) -> " + TAGS + "(d0)," + TAGS + "(d1).";
        saveRelationType1(relName, str, typeFile);
    }
 
    private void saveSubTagImport(String relName, File importFile, File factsFile) {
    	String[] domNames = new String[2];   
    	domNames[0] = TAGS;
    	domNames[1] = TAGS;
    	saveRelationImport1(relName, domNames, importFile, factsFile);
    }
     
    public void saveTagToPgmRelation() {
    	if (!Config.populate) return;
        String relName = TAG_TO_PGM;
        File factsFile = new File(workDir, relName + ".csv");
        saveTagToPgmData(relName, factsFile);
        
        File typeFile = new File(workDir, relName + ".type");
        saveTagToPgmType(relName, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveTagToPgmImport(relName, importFile, factsFile);
        
        if (!LogicBloxUtils.domsExist())
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveTagToPgmData(String relName, File factsFile) {
    	ArraySet<String> tagASet = LogicBloxUtils.getTagASet();
    	int sz = tagASet.size();
    	final String DELIM = this.delim;
    	PrintWriter out = createPrintWriter(factsFile);

    	StringBuilder sb = new StringBuilder();
		sb.append(sz).append(DELIM);
		if (Config.populate)
			sb.append(Config.workDirName);
		else if (Config.analyze)
			sb.append(Config.tagList);
		out.println(sb.toString());
    	out.flush();
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("An error occurred writing relation " +
    				relName + " data to " + factsFile.getAbsolutePath());
    	}
    }
    
    private void saveTagToPgmType(String relName, File typeFile) {  
        String str = relName + "(d0,d1) -> " + TAGS + "(d0)," + "string(d1).";
        saveRelationType1(relName, str, typeFile);
    }
 
    private void saveTagToPgmImport(String relName, File importFile, File factsFile) {
    	String[] domNames = new String[2];   
    	domNames[0] = TAGS;
    	domNames[1] = "string";
    	saveRelationImport1(relName, domNames, importFile, factsFile);
    }
     
    public void saveDomToTagRelation(Dom<?> dom, int sz) {
    	if (!Config.populate) return;
        String relName = dom.getName() + DOM_TO_TAG_SUFFIX;
        File factsFile = new File(workDir, relName + ".csv");
        saveDomToTagData(dom, relName, factsFile, sz);
        
        File typeFile = new File(workDir, relName + ".type");
        saveDomToTagType(relName, dom.getName(), typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveDomToTagImport(relName, dom.getName(), importFile, factsFile);
        
        if (!(LogicBloxUtils.domsExist() && LogicBloxUtils.domContains(dom.getName())))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveDomToTagData(Dom<?>dom, String relName, File factsFile, int sz) {
    	ArraySet<String> tagASet = LogicBloxUtils.getTagASet();
    	int tagSetSz = tagASet.size();
    	final String DELIM = this.delim;
    	PrintWriter out = createPrintWriter(factsFile);

		for (int i = 0, size = dom.size(); i < size; ++i) {
		    out.print(i + sz);
		    out.print(DELIM);
		    out.println(tagSetSz);
		}
    	out.flush();
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("An error occurred writing relation " +
    				relName + " data to " + factsFile.getAbsolutePath());
    	}
    }
    
    private void saveDomToTagType(String relName, String domName, File typeFile) {  
        String str = relName + "(d0,d1) -> " + domName + "(d0)," + TAGS + "(d1).";
        saveRelationType1(relName, str, typeFile);
    }
 
    private void saveDomToTagImport(String relName, String domName, File importFile, File factsFile) {
    	String[] domNames = new String[2];   
    	domNames[0] = domName;
    	domNames[1] = TAGS;
    	saveRelationImport1(relName, domNames, importFile, factsFile);
    }
}
