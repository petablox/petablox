package stamp;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintStream;

import shord.program.Program;
import shord.program.ProgramScope;
import shord.program.DefaultProgramScope;
import shord.project.Config;

/*
 * @author Saswat Anand
 */
public class Main
{
	private static PrintStream outStream = null;
    private static PrintStream errStream = null;

    private static final String outFileName = System.getProperty("stamp.out.file");
    private static final String errFileName = System.getProperty("stamp.err.file");    

	public static void main(String[] args) throws Exception
	{
		String mainListFile = System.getProperty("stamp.harnesslist.file");
		List<String> mainClasses = new ArrayList();
		BufferedReader reader = new BufferedReader(new FileReader(new File(mainListFile)));
		String line = null;
		while((line = reader.readLine()) != null){
			mainClasses.add(line);
		}
		reader.close();

		String widgetsFile = System.getProperty("stamp.widgets.file");
		reader = new BufferedReader(new FileReader(new File(widgetsFile)));
		String widgetClass = reader.readLine();

		//harnessClasses.add(System.getProperty("chord.main.class"));
		String outDir = System.getProperty("stamp.out.dir");

		Program prog = Program.g();
		Set<String> harnessClasses = new HashSet();
		//harnessClasses will include mainClasses
		findHarnessClasses(new File(System.getProperty("stamp.driver.dir")), null, harnessClasses);
		prog.build(harnessClasses, widgetClass);
		ProgramScope ps = new DefaultProgramScope(prog);
		prog.setScope(ps);

		setup();

		if(mainClasses.size() == 1){
			System.setProperty("chord.work.dir", outDir);
			prog.setMainClass(mainClasses.get(0));
			shord.project.Main.main(null);
		} else {
			int count = 0;
			for(String h : mainClasses){
				count++;
				String chordWorkDir = outDir+File.separator+count;
				System.setProperty("chord.work.dir", chordWorkDir);
				prog.setMainClass(h);
				shord.project.Main.main(null);
			}
		}
		
		finish();
	}


	private static void findHarnessClasses(File dir, String prefix, Set<String> classNames)
	{
		for(File f : dir.listFiles()){
			if(f.isDirectory()){
				String newPrefix = prefix == null ? "" : prefix+".";
				newPrefix += f.getName();
				findHarnessClasses(f, newPrefix, classNames);
			} else {
				String fname = f.getName();
				if(fname.endsWith(".class")){
					String cname = (prefix == null ? "" : prefix+".") + fname.substring(0, fname.length()-6);
					classNames.add(cname);
				}
			}
		}
	}
	
	private static void finish()
	{
		if (outStream != null)
            outStream.close();
        if (errStream != null && errStream != outStream)
            errStream.close();
	}

	private static void setup() throws Exception
	{
        File outFile;
        {
            if (outFileName == null)
                outFile = null;
            else {
                outFile = new File(outFileName);
                System.out.println("Redirecting stdout to file: " + outFile);
            }
        }
        File errFile;
        {
            if (errFileName == null)
                errFile = null;
            else {
                errFile = new File(errFileName);
                System.out.println("Redirecting stderr to file: " + errFile);
            }
        }

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
	}
}