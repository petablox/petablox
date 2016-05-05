package petablox.logicblox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.OutDirUtils;
import petablox.project.Config.DatalogEngineType;
import petablox.util.ArraySet;
import petablox.util.ProcessExecutor;
import petablox.util.Timer;
import petablox.util.Utils;
import petablox.util.tuple.object.Pair;

/**
 * Utilities for interacting with the LogicBlox engine.
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxUtils {
	private static final String INVALID_MULTIPGM_ENV = "ERROR: Multiple program support is only with the Logicblox backend";
	private static final String REPEATED_MULTIPGM_TAGNAME = "ERROR: This tag has already been used earlier. Please specify a different one. : ";
	private static final String INVALID_MULTIPGM_USAGE = "ERROR: Multiple program support: populate and analyze phases have to invoked separately.";
	private static final String INVALID_MULTIPGM_TAGLIST = "ERROR: Multiple program support: taglist cannot be empty.";
	private static final String INVALID_MULTIPGM_TAGNAME = "ERROR: Multiple program support: tagname can contain only letters of the alphabet.";
    private static final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ0123456789_-".toCharArray();
    private static boolean domsPres = false;
    private static HashMap<String,Integer> domNdxMap = null;
    private static HashMap<String,Integer> newDomNdxMap = null;
    private static ArraySet<String> domASet = new ArraySet<String>();
    private static ArraySet<String> tagASet = new ArraySet<String>();  
    private static ArraySet<String> annotASet = new ArraySet<String>();
	
	public static HashMap<String, HashMap<String, ArrayList<Pair<Integer, Integer>>>> domRanges =
			      new HashMap<String, HashMap<String, ArrayList<Pair<Integer, Integer>>>>();
	public static HashMap<String, String> subTags = new HashMap<String, String>();
	public static HashSet<String> annotRelsPres = new HashSet<String>();
    
    static {
    	assert alphabet.length == 64 : "Alphabet is not 64 characters long.";
    }
    
    public static HashMap<String,Integer> getDomNdxMap() {
    	return domNdxMap;
    }
    
    public static ArraySet<String> getDomASet() {
    	return domASet;
    }
    
    public static boolean addToDomASet(String s) {
    	return domASet.add(s);
    }
    
    public static boolean domContains(String s) {
    	return domASet.contains(s);
    }
    
    public static ArraySet<String> getTagASet() {
    	return tagASet;
    }
    
    public static boolean addToTagASet(String s) {
    	return tagASet.add(s);
    }
    
    public static boolean tagContains(String s) {
    	return tagASet.contains(s);
    }
    
    public static ArraySet<String> getAnnotASet() {
    	return annotASet;
    }
    
    public static boolean addToAnnotASet(String s) {
    	return annotASet.add(s);
    }
    
    public static boolean annotContains(String s) {
    	return annotASet.contains(s);
    }
    
    public static void readDomIndexFile() {
    	File ndxFile = new File(Config.logicbloxWorkDirName, "domNdx.txt");
    	if (ndxFile.exists()) {
			try {
				Scanner sc = new Scanner(ndxFile);
				while (sc.hasNext()) {
					String line = sc.nextLine().trim();
					String[] parts = line.split(" ");
					domNdxMap.put(parts[0], new Integer(parts[1]));
				}
				sc.close();
				
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}	
    }
    
    public static void writeDomIndexFile() {
    	if (Config.datalogEngine == Config.DatalogEngineType.LOGICBLOX3 ||
    		Config.datalogEngine == Config.DatalogEngineType.LOGICBLOX4) {
    		if (Config.populate) {
    			newDomNdxMap = LogicBloxExporter.getNewDomNdxMap();
		    	File ndxFile = new File(Config.logicbloxWorkDirName, "domNdx.txt");
		    	PrintWriter out;
		    	try {
						out = new PrintWriter(ndxFile);
					} catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					} catch (SecurityException s) {
						throw new RuntimeException(s);
					}
		    	for (String dname : newDomNdxMap.keySet()) {
		    		out.println(dname + " " + newDomNdxMap.get(dname));
		    	}
				Utils.close(out);
				if (out.checkError()) {
				    throw new PetabloxException("Error writing " + ndxFile.getAbsolutePath());
				}
    		}
    	}
    }
    
    private static String toAlphabetString(int data) {
        final int BOUND = alphabet.length;
        char[] buf = new char[BOUND];
        int charPos = BOUND;
        while (data != 0) {
            buf[--charPos] = alphabet[data & 63];
            data >>>= 6;
        };
        return new String(buf, charPos, (BOUND - charPos));
    }
    
    public static void initializeWorkspace() { 
    	String ws = Config.logicbloxWorkspace;
    	if (Config.multiPgmMode && existsWorkspace(ws)) {
    		return;
    	}
    	initializeWorkspace(ws); 
    }
    
    public static void initializeWorkspace(String workspace) {
    	if (Config.multiPgmMode && existsWorkspace(workspace)) {
    		return;
    	}
        ProcessExecutor.Result result = OutDirUtils.executeCaptureWithFailOnError(
            Config.logicbloxCommand,
            "create",
            "--overwrite",
            workspace
        );
        Messages.log("LogicBlox workspace initialized: %s" , workspace);
    }
    
    public static void setMultiTag() {
    	if (Config.multiPgmMode) {
	    	if (Config.multiTag.equals("")) {
				Config.multiTag = Utils.getAutoGeneratedMultiTag(tagASet);
				Messages.log("MULTIPGM: Auto-generated tag for relation names: " + Config.multiTag);
				Config.multiTag = Config.multiTag + "tag";
	    	} else {
	    		Config.multiTag = Config.multiTag + "tag";
	    		if (tagASet.contains(Config.multiTag))
	    			Messages.fatal(REPEATED_MULTIPGM_TAGNAME + Config.multiTag);
	    	}
	    	LogicBloxExporter lbe = new LogicBloxExporter();
	    	lbe.saveTagsDomain();
    	}
    }
    
    public static void validateMultiPgmOptions() {
        if (Config.multiPgmMode && Config.datalogEngine == DatalogEngineType.BDDBDDB)
        	Messages.fatal(INVALID_MULTIPGM_ENV);
        if (Config.populate && Config.analyze)
        	Messages.fatal(INVALID_MULTIPGM_USAGE);
        if (Config.analyze && Config.tagList.equals(""))
        	Messages.fatal(INVALID_MULTIPGM_TAGLIST);
        if (!Config.multiTag.equals("") && !Config.multiTag.matches("[a-zA-Z]+"))
        	Messages.fatal(INVALID_MULTIPGM_TAGNAME);
    }
    
    public static boolean existsWorkspace (String ws) {
        domsPres = false;
        domNdxMap = new HashMap<String,Integer>();
    	ProcessExecutor.Result result = OutDirUtils.executeCaptureWithFailOnError(
	            Config.logicbloxCommand,
	            "workspaces"
	        );
		if (result.getOutput().indexOf(ws + "\n") >= 0) {
			domsPres = true;
			readDomIndexFile();  
			LogicBloxImporter.loadDomainsAndRelations(domASet, tagASet, annotASet);
		} 
		return domsPres;
    }
    
    public static boolean domsExist() {
    	return domsPres;
    }
    
    public static void finalTasks() {
    	if (Config.multiPgmMode) {
	    	writeDomIndexFile();
	    	LogicBloxExporter lbe = new LogicBloxExporter();
	    	lbe.saveDomsDomain();
	    	lbe.saveDomRangeRelation();
	    	lbe.saveSubTagRelation();
	    	lbe.saveTagToPgmRelation();
	    	LogicBloxAnnotExporter lbae = new LogicBloxAnnotExporter();
	    	lbae.saveAnnotationNameDomain();
	    	lbae.saveAnnotRelations();
    	}
    }
    
    /**
     * Generates a workspace name by hashing some input string, intended to 
     * be the absolute path of Petablox's working directory.
     * 
     * @param seed a seed input
     * @return the workspace name
     */
    public static String generateWorkspaceName(String seed) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(seed.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder("petablox:");
            for (int i = 0; i < hash.length; i += 2) {
                int next = (i + 1 < hash.length ? hash[i + 1] : 0) & 0xFF;
                int unsigned = ((hash[i] & 0xFF) << 8) | next;
                sb.append(toAlphabetString(unsigned));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new PetabloxException(e);
        } catch (UnsupportedEncodingException e) {
            throw new PetabloxException(e);
        }
    }
    
    public static void addBlock(File definitionFile) {
        addBlock(Config.logicbloxWorkspace, definitionFile);
    }
    
    public static void addBlock(String workspace, File definitionFile) {
        String path = definitionFile.getAbsolutePath();
        Timer timer = new Timer("lb addblock --file " + path);
        timer.init();
        ProcessExecutor.Result result = OutDirUtils.executeCaptureWithFailOnError(
            Config.logicbloxCommand, "addblock", "--file", path, workspace
        );
        timer.done();
        
        Messages.log("Successfully added block file: %s", definitionFile);
        Messages.log("%s", timer.getInclusiveTimeStr());
    }
    
    public static void execFile(File file) {
        execFile(Config.logicbloxWorkspace, file);
    }

    public static void execFile(String workspace, File file) {
        String path = file.getAbsolutePath();
        Timer timer = new Timer("lb exec --file " + path);
        timer.init();
        ProcessExecutor.Result result = OutDirUtils.executeCaptureWithFailOnError(
            Config.logicbloxCommand, "exec", "--file", path, workspace
        );
        timer.done();
        
        Messages.log("Successfully executed logic file: %s", file);
        Messages.log("%s", timer.getInclusiveTimeStr());
    }
}
