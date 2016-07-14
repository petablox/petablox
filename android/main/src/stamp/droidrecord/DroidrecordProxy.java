package stamp.droidrecord;

import edu.stanford.droidrecord.logreader.BinLogReader;
import edu.stanford.droidrecord.logreader.CoverageReport;
import edu.stanford.droidrecord.logreader.EventLogStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DroidrecordProxy {
    
    private static DroidrecordProxy singleton = null;
    
    private boolean available;
    private BinLogReader logReader = null;
    private List<String> binLogFiles;
    
    private CoverageReport catchedCoverage = null;
    
    private void getBinLogFiles(String binLogFile) {
        // Go from dir/droidrecord.log.bin to gathering all 
        // dir/droidrecord.log.bin.runX.threadY files
        try {
            binLogFiles = new ArrayList<String>();
            int dirEnd = binLogFile.lastIndexOf('/');
            String dirPath = binLogFile.substring(0,dirEnd);
            File folder = new File(dirPath);
            String canonicalBinLogFile = 
                folder.getCanonicalPath() + binLogFile.substring(dirEnd);
            Pattern traceFilePattern = 
                Pattern.compile("^" + canonicalBinLogFile + "\\.run\\d*\\.thread\\d*$");
            System.out.println("Droidrecord bin log pattern: " + traceFilePattern.toString());
            File[] files = folder.listFiles();
            for(File file : files) {
                if(!file.exists()) continue;
                String filename = file.getCanonicalPath();
                System.out.println("Inspecting file: " + filename);
                Matcher m = traceFilePattern.matcher(filename);
                if(m.matches()) {
                    binLogFiles.add(filename);
                    System.out.println("Loaded binary droidrecord log: " + filename);
                }
            }
        } catch(IOException e) {
            throw new Error(e);
        }
    }

    private DroidrecordProxy() {
        String templateLogFile = System.getProperty("stamp.droidrecord.logfile.template");
        String binLogFile = System.getProperty("stamp.droidrecord.logfile.bin");
        if(templateLogFile == null || binLogFile == null || 
           templateLogFile.equals("") || binLogFile.equals("") || 
            !(new File(templateLogFile)).exists()) {
            available = false;
        } else {
            logReader = new BinLogReader(templateLogFile);
            getBinLogFiles(binLogFile);
            available = true;
        }
    }
    
    public static DroidrecordProxy g() {
        if(singleton == null) {
            singleton = new DroidrecordProxy();
        }
        return singleton;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public StampCallArgumentValueAnalysis getCallArgumentValueAnalysis() {
        if(!isAvailable()) {
            throw new Error("Droidrecord log not available!");
        }
        EventLogStream els = logReader.parseLogs(binLogFiles);
        return new StampCallArgumentValueAnalysis(els);
    }
    
    public CoverageReport getCoverage() {
        if(!isAvailable()) {
            throw new Error("Droidrecord log not available!");
        } else if(catchedCoverage != null){
            return catchedCoverage;
        }
        logReader.parseLogs(binLogFiles).readAll();
        return logReader.getCumulativeCoverageReport();
    }
}