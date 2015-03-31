package chord.analyses;

import java.io.PrintWriter;
import java.util.*;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Type;

import chord.program.ClassHierarchy;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.util.Utils;

/**
 * Dumps a subset classes that CHA found that weren't in-scope.
 * Subset is defined by option deadClasses.relevantPrefixes 
 * @author asrabkin
 * 
 * Output is in dead_classes.txt
 *
 */
@Chord(
		name="DeadClasses",
		consumes={"T"})
		public class DeadClasses extends JavaAnalysis {

	String[] relevantPrefixes;

	@Override
	public void run() {
		relevantPrefixes = Utils.toArray(System.getProperty("deadClasses.relevantPrefixes", ""));
		if(relevantPrefixes.length == 0) {
			System.err.println("You must specify property deadClasses.relevantPrefixes to use the DeadClasses analysis");
			System.exit(-1);
			//    	relevantPrefixes = new String[] {""};
		}
		Program program = Program.g();
		//    ClassicProject project = ClassicProject.g();

		ClassHierarchy ch = program.getClassHierarchy();

		//    DomT domT = (DomT) project.getTrgt("T");

		TreeSet<String> sortedDead = new TreeSet<String>();
		for(String s: ch.allClassNamesInPath()) {
			if(Utils.prefixMatch(s,relevantPrefixes)) {
				jq_Reference r = program.getClass(s);
				if(r == null)
					sortedDead.add(s);
			}
		}
		HashSet<String> hasMain = new HashSet<String>(50);
		for(String s: sortedDead) {
			try {
				jq_Type r = (jq_Type) jq_Type.parseType(s);
				if(r instanceof jq_Class) {
					jq_Class cl = (jq_Class) r;
					cl.prepare();
					jq_Method mainMethod = (jq_Method) cl.getDeclaredMember(
							new jq_NameAndDesc("main", "([Ljava/lang/String;)V"));
					if(mainMethod != null)
						hasMain.add(s);
				}

			}	catch(Exception e) {e.printStackTrace();}
				catch(NoClassDefFoundError e) {System.err.println(e);}//can happen if a class in-scope extends one that isn't
				catch(java.lang.ClassFormatError e) {System.err.println(e);}//not sure why this happens

				
		}


		PrintWriter writer = OutDirUtils.newPrintWriter("dead_classes.txt");

		for(String s: sortedDead)
			if(hasMain.contains(s))
				writer.println(s + "    HAS MAIN");
			else
				writer.println(s);


		writer.close();  	
	}
}
