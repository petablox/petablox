package chord.logicblox;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import chord.project.ChordException;
import chord.project.Config;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.util.ProcessExecutor;
import chord.util.Timer;

/**
 * Utilities for interacting with the LogicBlox engine.
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxUtils {
    private static final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ0123456789_-".toCharArray();
    static {
        assert alphabet.length == 64 : "Alphabet is not 64 characters long.";
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
        initializeWorkspace(Config.logicbloxWorkspace); 
    }
    
    public static void initializeWorkspace(String workspace) {
        ProcessExecutor.Result result = OutDirUtils.executeCaptureWithFailOnError(
            Config.logicbloxCommand,
            "create",
            "--overwrite",
            workspace
        );
        
        Messages.log("LogicBlox workspace initialized: %s" , workspace);
    }
    
    /**
     * Generates a workspace name by hashing some input string, intended to 
     * be the absolute path of Chord's working directory.
     * 
     * @param seed a seed input
     * @return the workspace name
     */
    public static String generateWorkspaceName(String seed) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(seed.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder("chord:");
            for (int i = 0; i < hash.length; i += 2) {
                int next = (i + 1 < hash.length ? hash[i + 1] : 0) & 0xFF;
                int unsigned = ((hash[i] & 0xFF) << 8) | next;
                sb.append(toAlphabetString(unsigned));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ChordException(e);
        } catch (UnsupportedEncodingException e) {
            throw new ChordException(e);
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
