package petablox.logicblox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Scanner;

import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.OutDirUtils;
import petablox.util.ProcessExecutor;
import petablox.util.Timer;
import petablox.util.Utils;

/**
 * Utilities for interacting with the LogicBlox engine.
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxUtils {
    private static final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ0123456789_-".toCharArray();
    private static boolean domsPres = false;
    private static HashMap<String,Integer> domNdxMap = null;
    private static HashMap<String,Integer> newDomNdxMap = null;
    
    static {
    	assert alphabet.length == 64 : "Alphabet is not 64 characters long.";
    }
    
    public static HashMap<String,Integer> getDomNdxMap() {
    	return domNdxMap;
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
    	if (Config.multiPgmMode && existsWorkspace(ws))
    		return;
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
		}
		return domsPres;
    }
    
    public static boolean domsExist() {
    	return domsPres;
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
