package petablox.project;

import java.io.*;
import java.lang.Exception;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import petablox.program.Program;
import petablox.util.Timer;
import petablox.util.Utils;

/**
 * Entry point of Petablox after JVM settings are resolved.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Main {
    public static void main(String[] args) throws Exception {
        File outFile;
        {
            String outFileName = Config.outFileName;
            if (outFileName == null)
                outFile = null;
            else {
                outFile = new File(outFileName);
                System.out.println("Redirecting stdout to file: " + outFile);
            }
        }
        File errFile;
        {
            String errFileName = Config.errFileName;
            if (errFileName == null)
                errFile = null;
            else {
                errFile = new File(errFileName);
                System.out.println("Redirecting stderr to file: " + errFile);
            }
        }
        PrintStream outStream = null;
        PrintStream errStream = null;
        if (outFile != null) {
            outStream = new PrintStream(outFile);
            System.setOut(outStream);
        }
        if (errFile != null) {
            if (outFile != null && errFile.equals(outFile))
                errStream = outStream;
            else
                errStream = new PrintStream(errFile);
            System.setErr(errStream);
        }
        extractNestedJARs();
		run();
        try{
        	File tempFile = new File(Config.outDirName+ File.separator +"temp");
        	if(tempFile.exists()){
        		for (File f : tempFile.listFiles())
        			f.delete();
        		if (!tempFile.delete())
        			throw new FileNotFoundException("Failed to delete file: " + tempFile);
        	}
        }catch(Exception e){};
        if (outStream != null)
            outStream.close();
        if (errStream != null && errStream != outStream)
            errStream.close();
    }
    private static void run() {
        Timer timer = new Timer("petablox");
        timer.init();
        String initTime = timer.getInitTimeStr();
        if (Config.verbose >= 0)
            System.out.println("Petablox run initiated at: " + initTime);
        if (Config.verbose >= 2)
            Config.print();
        Program program = Program.g();
        Project project = Project.g();
        if (Config.buildScope) {
            program.build();
        }
        if (Config.printAllClasses)
            program.printAllClasses();
        String[] printClasses = Utils.toArray(Config.printClasses);
        if (printClasses.length > 0) {
            for (String className : printClasses)
                program.printClass(className);
        }
        String[] analysisNames = Utils.toArray(Config.runAnalyses);
        if (analysisNames.length > 0) {
            project.run(analysisNames);
        }
        String[] relNames = Utils.toArray(Config.printRels);
        if (relNames.length > 0) {
            project.printRels(relNames);
        }
        if (Config.printProject) {
            project.print();
        }
        timer.done();
        String doneTime = timer.getDoneTimeStr();
        if (Config.verbose >= 0) {
            System.out.println("Petablox run completed at: " + doneTime);
            System.out.println("Total time: " + timer.getInclusiveTimeStr());
        }
    }
    private static String basename(String fname) {
    	String[] parts = fname.split(File.separator);
    	return parts[parts.length - 1];
    }

    private static void extractNestedJARs(){
        String tempDir = Config.outDirName+ File.separator +"temp";
        Utils.mkdirs(tempDir);
        String classpath = Config.userClassPathName;
        String[] classPathElems = classpath.split(File.pathSeparator);
        for(String elem : classPathElems){
            if(elem.endsWith(".jar")){
               try{
                   JarFile jf = new JarFile(elem);
                   Enumeration<JarEntry> entries = jf.entries();
                   while(entries.hasMoreElements()){
                       JarEntry je = entries.nextElement();
                       if(je.getName().endsWith(".jar")) {
                           InputStream is = jf.getInputStream(je);
                           String fileName = basename(je.getName());
                           File newLoc = new File(tempDir + File.separator + fileName);
                           FileOutputStream fos = new java.io.FileOutputStream(newLoc);
                           while (is.available() > 0)
                               fos.write(is.read());
                           fos.close();
                           is.close();
                           classpath += File.pathSeparator + newLoc.getAbsolutePath();
                       }
                   }
               }catch(Exception e){
                   continue;
               }
            }
        }
        Config.userClassPathName = classpath;
    }
}
