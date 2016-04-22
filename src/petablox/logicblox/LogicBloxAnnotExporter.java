package petablox.logicblox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.Config.DatalogEngineType;
import petablox.util.ArraySet;
import petablox.util.Utils;
import petablox.util.tuple.object.Pair;
import petablox.util.tuple.object.Quad;
import petablox.util.tuple.object.Trio;

/**
 * A class for exporting domains and relations containing annotations to LogicBlox. 
 *
 */
public class LogicBloxAnnotExporter extends LogicBloxIOBase {
 
	public static ArraySet<String> annotationName = new ArraySet<String>();
	public static Map<Integer, List<Trio<String, String, String>>> fieldAnnot = new HashMap<Integer, List<Trio<String, String, String>>>();
	public static Map<Integer, List<Quad<String, Integer, String, String>>> methParamAnnot = new HashMap<Integer, List<Quad<String, Integer, String, String>>>();
	public static Map<Integer, List<Trio<String, String, String>>> methRetAnnot = new HashMap<Integer, List<Trio<String, String, String>>>();
	public static Map<Integer, List<Trio<String, String, String>>> pgmPtAnnot = new HashMap<Integer, List<Trio<String, String, String>>>();
	
	
    public LogicBloxAnnotExporter() {
        super();
        ArraySet<String> oldAnnots = LogicBloxUtils.getAnnotASet();
        annotationName.addAll(oldAnnots);
    }
    
    public LogicBloxAnnotExporter(DatalogEngineType engineType) {
        super(engineType);
        ArraySet<String> oldAnnots = LogicBloxUtils.getAnnotASet();
        annotationName.addAll(oldAnnots);
    }
     
    public void saveAnnotationNameDomain() {
    	if (!Config.populate) return;
    	String annotName = ANNOT_NAME;
    	File factsFile = new File(workDir, annotName + ".csv");
    	final String DELIM = this.delim;

    	ArraySet<String> oldAnnots = LogicBloxUtils.getAnnotASet();

    	//saveDomainData
    	PrintWriter out = createPrintWriter(factsFile);
    	int i = 0;
    	for (String nm : annotationName) {
    		if (!oldAnnots.contains(nm)) {
    			out.print(i);
    			out.print(DELIM);
    			out.println(nm);
    		}
    		i++;
    	}
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("Error writing " + annotName
    				+ " domain facts to " + factsFile.getAbsolutePath());
    	}

    	File typeFile = new File(workDir, annotName + ".type");
    	saveDomainType(annotName, LB3_ANNOT_NAME_LIM, typeFile);

    	File importFile = new File(workDir, annotName + ".import");
    	saveDomainImport(annotName, importFile, factsFile);

    	if (!LogicBloxUtils.domsExist())
    		LogicBloxUtils.addBlock(typeFile);
    	LogicBloxUtils.execFile(importFile);  
    }
     
    public void saveAnnotRelations() {
    	saveTrioAnnotRelation(Config.multiTag + FIELD_ANNOT, fieldAnnot);
    	saveQuadAnnotRelation(Config.multiTag + M_PARAM_ANNOT, methParamAnnot);
    	saveTrioAnnotRelation(Config.multiTag + M_RET_ANNOT, methRetAnnot);
    	saveTrioAnnotRelation(Config.multiTag + PGM_PT_ANNOT, pgmPtAnnot);
    }
    
    private void saveTrioAnnotRelation(String relName, Map<Integer, List<Trio<String, String, String>>> relData) {
    	if (!Config.populate) return;
    	
        File factsFile = new File(workDir, relName + ".csv");
        saveTrioAnnotRelData(relName, factsFile, relData);
        
        File typeFile = new File(workDir, relName + ".type");
        saveTrioAnnotRelType(relName, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveTrioAnnotRelImport(relName, importFile, factsFile);
        
        if (!LogicBloxUtils.annotRelsPres.contains(relName))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveTrioAnnotRelData(String relName, File factsFile, Map<Integer, List<Trio<String, String, String>>> relData) {
    	final String DELIM = this.delim;
    	PrintWriter out = createPrintWriter(factsFile);

    	for(int ndx : relData.keySet()) {
    		List<Trio<String, String, String>> tl = relData.get(ndx);
    		for (Trio<String, String, String> t : tl) {
	    		StringBuilder sb = new StringBuilder();
	    		sb.append(ndx).append(DELIM);
	    		sb.append(annotationName.indexOf(t.val0)).append(DELIM);
	    		sb.append(t.val1).append(DELIM);
	    		sb.append(t.val2);
	    		out.println(sb.toString());
    		}
    	}
    	out.flush();
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("An error occurred writing relation " +
    				relName + " data to " + factsFile.getAbsolutePath());
    	}
    }
    
    private void saveTrioAnnotRelType(String relName, File typeFile) {  
        String str = relName + "(d0,d1,d2,d3) -> " + "int(d0)," + ANNOT_NAME + "(d1),string(d2),string(d3).";
        saveRelationType1(relName, str, typeFile);
    }
 
    private void saveTrioAnnotRelImport(String relName, File importFile, File factsFile) {
    	String[] domNames = new String[4];   
    	domNames[0] = "int";
    	domNames[1] = ANNOT_NAME;
    	domNames[2] = "string";
    	domNames[3] = "string";
    	saveRelationImport1(relName, domNames, importFile, factsFile);
    }
    
    private void saveQuadAnnotRelation(String relName, Map<Integer, List<Quad<String, Integer, String, String>>> relData) {
    	if (!Config.populate) return;
    	
        File factsFile = new File(workDir, relName + ".csv");
        saveQuadAnnotRelData(relName, factsFile, relData);
        
        File typeFile = new File(workDir, relName + ".type");
        saveQuadAnnotRelType(relName, typeFile);
        
        File importFile = new File(workDir, relName + ".import");
        saveQuadAnnotRelImport(relName, importFile, factsFile);
        
        if (!LogicBloxUtils.annotRelsPres.contains(relName))
        	LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    private void saveQuadAnnotRelData(String relName, File factsFile, Map<Integer, List<Quad<String, Integer, String, String>>> relData) {
    	final String DELIM = this.delim;
    	PrintWriter out = createPrintWriter(factsFile);

    	for(int ndx : relData.keySet()) {
    		List<Quad<String, Integer, String, String>> tl = relData.get(ndx);
    		for (Quad<String, Integer, String, String> t : tl) {
	    		StringBuilder sb = new StringBuilder();
	    		sb.append(ndx).append(DELIM);
	    		sb.append(annotationName.indexOf(t.val0)).append(DELIM);
	    		sb.append(t.val1).append(DELIM);
	    		sb.append(t.val2).append(DELIM);
	    		sb.append(t.val3);
	    		out.println(sb.toString());
    		}
    	}
    	out.flush();
    	Utils.close(out);
    	if (out.checkError()) {
    		throw new PetabloxException("An error occurred writing relation " +
    				relName + " data to " + factsFile.getAbsolutePath());
    	}
    }
    
    private void saveQuadAnnotRelType(String relName, File typeFile) {  
        String str = relName + "(d0,d1,d2,d3,d4) -> " + "int(d0)," + ANNOT_NAME + "(d1),int(d2),string(d3),string(d4).";
        saveRelationType1(relName, str, typeFile);
    }
 
    private void saveQuadAnnotRelImport(String relName, File importFile, File factsFile) {
    	String[] domNames = new String[5];   
    	domNames[0] = "int";
    	domNames[1] = ANNOT_NAME;
    	domNames[2] = "int";
    	domNames[3] = "string";
    	domNames[4] = "string";
    	saveRelationImport1(relName, domNames, importFile, factsFile);
    }
}
