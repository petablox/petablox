package stamp.scanner;

import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import chord.bddbddb.Rel.RelView;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.Chord;

@Chord(name = "scanner-java",
       consumes = { "chaIM", "out_reachableM" }
)
public class Scanner extends JavaAnalysis {
	private static final String[] dangerClassNames = {
		/* Code Invocation */
		"dalvik.system.DexClassLoader",
		"dalvik.system.BaseDexClassLoader"//,
		/* Storage */
		// "java.io.File",
		// "java.io.FileDescriptor",
		// "java.io.FileInputStream",
		// "java.io.FileOutputStream",
		// "java.io.FileReader",
		// "java.io.FileWriter",
		// "java.io.RandomAccessFile"
	};

	/* Method signature format: <methodname>:<descriptor>@<fully-qualified-class-name> */
	private static final String[] dangerMethSigs = {
		/* Calendar */
	    "getInstance:()Ljava/util/Calendar;@java.util.Calendar",
	    "getTime:()Ljava/util/Date;@java.util.Calendar",
	    "getTimeInMillis:()J@java.util.Calendar",
	    "<init>:()V@java.util.Calendar",
	    "<init>:()V@java.util.GregorianCalendar",
	    "<init>:(III)V@java.util.GregorianCalendar",
	    "add:(II)V@java.util.GregorianCalendar",
	    "equals:(Ljava/lang/Object;)Z@java.util.GregorianCalendar",
		/* Code Invocation */
	    "getRuntime:()Ljava/lang/Runtime;@java.lang.Runtime",
		"exec:([Ljava/lang/String;)Ljava/lang/Process;@java.lang.Runtime",
		"exec:([Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Process;@java.lang.Runtime",
		"exec:([Ljava/lang/String;[Ljava/lang/String;Ljava/io/File;)Ljava/lang/Process;@java.lang.Runtime",
		"exec:(Ljava/lang/String;)Ljava/lang/Process;@java.lang.Runtime",
		"exec:(Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/Process;@java.lang.Runtime",
		"exec:(Ljava/lang/String;[Ljava/lang/String;Ljava/io/File;)Ljava/lang/Process;@java.lang.Runtime",
		/* Messaging */
	    "abortBroadcast:()V@android.content.BroadcastReceiver",
		/* Native Code */
	    "loadLibrary:(Ljava/lang/String;)V@java.lang.System",
		/* Randomness */
	    "<init>:()V@java.util.Random",
	    "nextInt:()I@java.util.Random",
		/* Reflection */
	    "getClassLoader:()Ljava/lang/ClassLoader;@android.content.ContextWrapper",
	    "getClassLoader:()Ljava/lang/ClassLoader;@android.test.mock.MockContext",
	    "getClassLoader:()Ljava/lang/ClassLoader;@java.lang.Class",
	    "getClass:()Ljava/lang/Class;@java.lang.Object",
		/* Alarms */
		"set:(IJLandroid/app/PendingIntent;)V@android.app.AlarmManager",
		"setRepeating:(IJJLandroid/app/PendingIntent;)V@android.app.AlarmManager",
		"setInexactRepeating:(IJJLandroid/app/PendingIntent;)V@android.app.AlarmManager",
		/* Vibrator */
		// "vibrate:(J)V@android.os.Vibrator",
		// "vibrate:([JI)V@android.os.Vibrator",
	    /* Storage (xxx) */
	    /* Unsupported annotation types */
	    "requestLocationUpdates:(Ljava/lang/String;JFLandroid/app/PendingIntent;)V@android.location.LocationManager",
	    "requestLocationUpdates:(JFLandroid/location/Criteria;Landroid/app/PendingIntent;)V@android.location.LocationManager",
	    "requestSingleUpdate:(Ljava/lang/String;Landroid/app/PendingIntent;)V@android.location.LocationManager",
	    "requestSingleUpdate:(Landroid/location/Criteria;Landroid/app/PendingIntent;)V@android.location.LocationManager",
	    "addProximityAlert:(DDFJLandroid/app/PendingIntent;)V@android.location.LocationManager",
	    "setOutputFile:(Ljava/io/FileDescriptor;)V@android.media.MediaRecorder",
	    "setOutputFile:(Ljava/lang/String;)V@android.media.MediaRecorder",
	    "deleteFromHistory:(Landroid/content/ContentResolver;Ljava/lang/String;)V@android.provider.Browser",
	    "clearSearches:(Landroid/content/ContentResolver;)V@android.provider.Browser",
	    "deleteHistoryTimeFrame:(Landroid/content/ContentResolver;JJ)V@android.provider.Browser",
	    "truncateHistory:(Landroid/content/ContentResolver;)V@android.provider.Browser",
	    "clearHistory:(Landroid/content/ContentResolver;)V@android.provider.Browser",
	    "updateVisitedHistory:(Landroid/content/ContentResolver;Ljava/lang/String;Z)V@android.provider.Browser"
	};

	private ProgramRel relIM;

	public void run() {
		relIM = (ProgramRel) ClassicProject.g().getTrgt("chaIM");
		relIM.load();
		ProgramRel relReachableM =
			(ProgramRel) ClassicProject.g().getTrgt("out_reachableM");
		relReachableM.load();

		Program prog = Program.g();

		Set<jq_Class> dangerClasses = new HashSet<jq_Class>();
		Set<jq_Method> dangerMeths = new HashSet<jq_Method>();
		for (String cName : dangerClassNames) {
			dangerClasses.add((jq_Class) prog.getClass(cName));
		}
		for (String sig : dangerMethSigs) {
			dangerMeths.add(prog.getMethod(sig));
		}

		Iterable<jq_Method> reachableMeths = relReachableM.getAry1ValTuples();
		for (jq_Method m : reachableMeths) {
			jq_Class c = m.getDeclaringClass();
			if (!dangerClasses.contains(c) && !dangerMeths.contains(m)) {
				continue;
			}
			for (Quad quad : getCallers(m)) {
				System.out.println("DANGER_METHOD: " + m.toString() + " " +
								   quad.toJavaLocStr());
			}
		}

		relIM.close();
		relReachableM.close();
	}

	private Iterable<Quad> getCallers(jq_Method meth) {
		RelView view = relIM.getView();
		view.selectAndDelete(1, meth);
		return view.getAry1ValTuples();
	}
}
