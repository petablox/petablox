package chord.logicblox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import chord.project.ChordException;
import chord.project.Config;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.util.ProcessExecutor;
import chord.util.Timer;
import chord.util.Utils;

/**
 * Utilities for interacting with the LogicBlox engine.
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxUtils {
    private static final String PROCESS_STARTING = "Starting command: '%s'";
    private static final String PROCESS_FINISHED = "Finished command: '%s'";
    
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
        
        // TODO if verbose ...
        Messages.log("LogicBlox workspace initialized: %s" , workspace);
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
