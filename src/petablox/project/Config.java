package petablox.project;

import java.io.File;
import java.io.IOException;

import petablox.logicblox.LogicBloxUtils;
import petablox.util.Utils;

/**
 * System properties recognized by Petablox.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Config {
    private static final String BAD_OPTION = "ERROR: Unknown value '%s' for system property '%s'; expected: %s";

    private Config() { }

    // properties concerning settings of the JVM running Petablox

    public final static String maxHeap = System.getProperty("petablox.max.heap");
    public final static String maxStack = System.getProperty("petablox.max.stack");
    public final static String jvmargs = System.getProperty("petablox.jvmargs");
    public final static boolean fixCPU = Utils.buildBoolProperty("petablox.fixCPU",false);
    public final static String CPUID = System.getProperty("petablox.CPUID", "0");
    public final static String bddbddbCPUID = System.getProperty("petablox.bddbddb.CPUID", "0");

    // basic properties about program being analyzed (its main class, classpath, command line args, etc.)

    public final static String workDirName = System.getProperty("petablox.work.dir");
    public final static String mainClassName = System.getProperty("petablox.main.class");
    public static String userClassPathName = System.getProperty("petablox.class.path");
    public final static String srcPathName = System.getProperty("petablox.src.path");
    public final static String runIDs = System.getProperty("petablox.run.ids", "0");
    public final static String runtimeJvmargs = System.getProperty("petablox.runtime.jvmargs", "-ea -Xmx1024m");

    // properties concerning how the program's analysis scope is constructed

    public final static String scopeKind = System.getProperty("petablox.scope.kind", "rta");
    public final static String reflectKind = System.getProperty("petablox.reflect.kind", "none");
    public final static String CHkind = System.getProperty("petablox.ch.kind", "static");
    public final static String ssaKind = System.getProperty("petablox.ssa.kind", "phi");
    public final static String cfgKind = System.getProperty("petablox.cfg.kind", "exception");
    static {
        check(CHkind, new String[] { "static", "dynamic" }, "petablox.ch.kind");
        check(reflectKind, new String[] { "none", "static", "dynamic", "static_cast", "external" }, "petablox.reflect.kind");
        check(ssaKind, new String[] { "none", "phi", "nophi", "nomove", "nomovephi" }, "petablox.ssa.kind");
        check(cfgKind, new String[] { "exception", "noexception" }, "petablox.cfg.kind");
    }
    public final static String DEFAULT_SCOPE_EXCLUDES = "";
    public final static String scopeStdExcludeStr = System.getProperty("petablox.std.scope.exclude", DEFAULT_SCOPE_EXCLUDES);
    public final static String scopeExtExcludeStr = System.getProperty("petablox.ext.scope.exclude", "");
    public static String scopeExcludeStr =
        System.getProperty("petablox.scope.exclude", Utils.concat(scopeStdExcludeStr, ",", scopeExtExcludeStr));
    public final static String DEFAULT_CHECK_EXCLUDES =
        "java.,javax.,sun.,com.sun.,com.ibm.,org.apache.harmony.";
    public final static String checkStdExcludeStr = System.getProperty("petablox.std.check.exclude", DEFAULT_CHECK_EXCLUDES);
    public final static String checkExtExcludeStr = System.getProperty("petablox.ext.check.exclude", "");
    public final static String checkExcludeStr =
        System.getProperty("petablox.check.exclude", Utils.concat(checkStdExcludeStr, ",", checkExtExcludeStr));
    public final static String DEFAULT_EXT_REFL_EXCLUDES =
    "$Proxy,ByCGLIB,GeneratedConstructorAccessor,GeneratedMethodAccessor,GeneratedSerializationConstructorAccessor,org\\.apache\\.derby\\.exe\\.";
    public final static String extReflDefExcludeStr = System.getProperty("petablox.def.extrefl.exclude", DEFAULT_EXT_REFL_EXCLUDES);
    public final static String extReflAddlExcludeStr = System.getProperty("petablox.addl.extrefl.exclude", "");
    public final static String extReflExcludeStr =
        System.getProperty("petablox.extrefl.exclude", Utils.concat(extReflDefExcludeStr, ",", extReflAddlExcludeStr));

    // properties dictating what gets computed/printed by Petablox

    public final static boolean buildScope = Utils.buildBoolProperty("petablox.build.scope", false);
    public final static String runAnalyses = System.getProperty("petablox.run.analyses", "");
    public final static String printClasses = System.getProperty("petablox.print.classes", "").replace('#', '$');
    public final static boolean printAllClasses = Utils.buildBoolProperty("petablox.print.all.classes", false);
    public final static String printRels = System.getProperty("petablox.print.rels", "");
    public final static boolean printProject = Utils.buildBoolProperty("petablox.print.project", false);
    public final static boolean printResults = Utils.buildBoolProperty("petablox.print.results", true);
    public final static boolean saveDomMaps = Utils.buildBoolProperty("petablox.save.maps", true);
    // Determines verbosity level of Petablox:
    // 0 => silent
    // 1 => print task/process enter/leave/time messages and sizes of computed doms/rels
    //      bddbddb: print sizes of relations output by solver
    // 2 => all other messages in Petablox
    //      bddbddb: print bdd node resizing messages, gc messages, and solver stats (e.g. how long each iteration took)
    // 3 => bddbddb: noisy=yes for solver
    // 4 => bddbddb: tracesolve=yes for solver
    // 5 => bddbddb: fulltravesolve=yes for solver
    public final static int verbose = Integer.getInteger("petablox.verbose", 1);

    // Petablox project properties

    public final static boolean classic = System.getProperty("petablox.classic").equals("true");
    public final static String stdJavaAnalysisPathName = System.getProperty("petablox.std.java.analysis.path");
    public final static String extJavaAnalysisPathName = System.getProperty("petablox.ext.java.analysis.path");
    public final static String javaAnalysisPathName = System.getProperty("petablox.java.analysis.path");
    public final static String stdDlogAnalysisPathName = System.getProperty("petablox.std.dlog.analysis.path");
    public final static String extDlogAnalysisPathName = System.getProperty("petablox.ext.dlog.analysis.path");
    public final static String dlogAnalysisPathName = System.getProperty("petablox.dlog.analysis.path");

    // properties specifying configuration of instrumentation and dynamic analysis

    public final static boolean useJvmti = Utils.buildBoolProperty("petablox.use.jvmti", false);
    public final static String instrKind = System.getProperty("petablox.instr.kind", "offline");
    public final static String traceKind = System.getProperty("petablox.trace.kind", "full");
    public final static int traceBlockSize = Integer.getInteger("petablox.trace.block.size", 4096);
    static {
        check(instrKind, new String[] { "offline", "online" }, "petablox.instr.kind");
        check(traceKind, new String[] { "full", "pipe" }, "petablox.trace.kind");
    }
    public final static boolean dynamicHaltOnErr = Utils.buildBoolProperty("petablox.dynamic.haltonerr", true);
    public final static int dynamicTimeout = Integer.getInteger("petablox.dynamic.timeout", -1);
    public final static int maxConsSize = Integer.getInteger("petablox.max.cons.size", 50000000);

    // properties dictating what is reused across Petablox runs

    public final static boolean reuseScope = Utils.buildBoolProperty("petablox.reuse.scope", false);
    public final static boolean reuseRels =Utils.buildBoolProperty("petablox.reuse.rels", false);
    public final static boolean reuseTraces =Utils.buildBoolProperty("petablox.reuse.traces", false);

    // datalog engine selection
    public static enum DatalogEngineType {
        BDDBDDB,
        LOGICBLOX3,
        LOGICBLOX4;
    }

    public final static DatalogEngineType datalogEngine =
        Utils.buildEnumProperty("petablox.datalog.engine", DatalogEngineType.BDDBDDB);
    
    // properties concerning LogicBlox
    
    /** <tt>petablox.logicblox.delim</tt> : the input delimiter in domain export files. */
    public final static String logicbloxInputDelim =
        System.getProperty("petablox.logicblox.delim", "\t");
    /** <tt>petablox.logicblox.command</tt> : the main logicblock command (default: <tt>lb</tt>) */
    public final static String logicbloxCommand =
        System.getProperty("petablox.logicblox.command", "lb");
    public final static String logicbloxWorkspace;
    static {
        String ws = System.getProperty("petablox.logicblox.workspace");
        if (ws == null)
            ws = LogicBloxUtils.generateWorkspaceName(new File(Config.workDirName).getAbsolutePath());
        logicbloxWorkspace = ws;
    }
    

    // properties concerning BDDs

    public final static boolean useBuddy =Utils.buildBoolProperty("petablox.use.buddy", false);
    public final static String bddbddbMaxHeap = System.getProperty("petablox.bddbddb.max.heap", "1024m");
    public final static String bddCodeFragmentFolder = System.getProperty("petablox.bddbddb.codeFragment.out", "");

    // properties specifying names of Petablox's output files and directories

    public static String outDirName = System.getProperty("petablox.out.dir", workRel2Abs("petablox_output"));
    public final static String outFileName = System.getProperty("petablox.out.file", outRel2Abs("log.txt"));
    public final static String errFileName = System.getProperty("petablox.err.file", outRel2Abs("log.txt"));    
    public final static String reflectFileName = System.getProperty("petablox.reflect.file", outRel2Abs("reflect.txt"));
    public final static String methodsFileName = System.getProperty("petablox.methods.file", outRel2Abs("methods.txt"));
    public final static String typesFileName = System.getProperty("petablox.types.file", outRel2Abs("types.txt"));
    public final static String classesFileName = System.getProperty("petablox.classes.file", outRel2Abs("classes.txt"));
    public final static String bddbddbWorkDirName = System.getProperty("petablox.bddbddb.work.dir", outRel2Abs("bddbddb"));
    public final static String bootClassesDirName = System.getProperty("petablox.boot.classes.dir", outRel2Abs("boot_classes"));
    public final static String userClassesDirName = System.getProperty("petablox.user.classes.dir", outRel2Abs("user_classes"));
    public final static String instrSchemeFileName = System.getProperty("petablox.instr.scheme.file", outRel2Abs("scheme.ser"));
    public final static String traceFileName = System.getProperty("petablox.trace.file", outRel2Abs("trace"));
    public final static String logicbloxWorkDirName = System.getProperty("petablox.logicblox.work.dir", outRel2Abs("logicblox"));

    static {
        Utils.mkdirs(outDirName);
        Utils.mkdirs(bddbddbWorkDirName);
        Utils.mkdirs(logicbloxWorkDirName);
    }

    // Properties concerning Soot
    public final static boolean sootIgnoreStatic = Utils.buildBoolProperty("soot.ignore.static", false);

    // commonly-used constants

    public final static String mainDirName = System.getProperty("petablox.main.dir");
    public final static String javaClassPathName = System.getProperty("java.class.path");
    public final static String toolClassPathName =
    		mainDirName + File.separator + "petablox.jar" + File.pathSeparator + javaAnalysisPathName;
    // This source of this agent is defined in main/agent/chord_instr_agent.cpp.
    // See the ccompile target in main/build.xml and main/agent/Makefile for how it is built.
    public final static String cInstrAgentFileName = mainDirName + File.separator + "libchord_instr_agent.so";
    // This source of this agent is defined in main/src/petablox/instr/OnlineTransformer.java.
    // See the jcompile target in main/build.xml for how it is built.
    public final static String jInstrAgentFileName = mainDirName + File.separator + "petablox.jar";
    public final static String javadocURL = "http://petablox.stanford.edu/javadoc/";

    public static String[] scopeExcludeAry = Utils.toArray(scopeExcludeStr);
    public static boolean isExcludedFromScope(String typeName) {
        for (String c : scopeExcludeAry)
            if (typeName.startsWith(c))
                return true;
        return false;
    }
    public final static String[] checkExcludeAry = Utils.toArray(checkExcludeStr);
    public static boolean isExcludedFromCheck(String typeName) {
        for (String c : checkExcludeAry)
            if (typeName.startsWith(c))
                return true;
        return false;
    }
    public final static String[] extReflExcludeAry = Utils.toArray(extReflExcludeStr);
    public static boolean isExcludedFromExtRefl(String className) {
        for (String c : extReflExcludeAry)
            if (className.indexOf(c) >= 0)
                return true;
        return false;
    }

    public static void print() {
        System.out.println("java.vendor: " + System.getProperty("java.vendor"));
        System.out.println("java.version: " + System.getProperty("java.version"));
        System.out.println("os.arch: " + System.getProperty("os.arch"));
        System.out.println("os.name: " + System.getProperty("os.name"));
        System.out.println("os.version: " + System.getProperty("os.version"));
        System.out.println("java.class.path: " + javaClassPathName);
        System.out.println("petablox.max.heap: " + maxHeap);
        System.out.println("petablox.max.stack: " + maxStack);
        System.out.println("petablox.jvmargs: " + jvmargs);
        System.out.println("petablox.fixCPU: " + fixCPU);
        System.out.println("petablox.cpuID: " + CPUID);
        System.out.println("petablox.bddbddb.CPUID: " + bddbddbCPUID);
        System.out.println("petablox.main.dir: " + mainDirName);
        System.out.println("petablox.work.dir: " + workDirName);
        System.out.println("petablox.main.class: " + mainClassName);
        System.out.println("petablox.class.path: " + userClassPathName);
        System.out.println("petablox.src.path: " + srcPathName);
        System.out.println("petablox.run.ids: " + runIDs);
        System.out.println("petablox.runtime.jvmargs: " + runtimeJvmargs);
        System.out.println("petablox.scope.kind: " + scopeKind);
        System.out.println("petablox.reflect.kind: " + reflectKind);
        System.out.println("petablox.ch.kind: " + CHkind);
        System.out.println("petablox.ssa: " + ssaKind);
        System.out.println("petablox.cfg: " + cfgKind);
        System.out.println("petablox.std.scope.exclude: " + scopeStdExcludeStr);
        System.out.println("petablox.ext.scope.exclude: " + scopeExtExcludeStr);
        System.out.println("petablox.scope.exclude: " + scopeExcludeStr);
        System.out.println("petablox.std.check.exclude: " + checkStdExcludeStr);
        System.out.println("petablox.ext.check.exclude: " + checkExtExcludeStr);
        System.out.println("petablox.check.exclude: " + checkExcludeStr);
        System.out.println("petablox.def.extrefl.exclude: " + extReflDefExcludeStr);
        System.out.println("petablox.addl.extrefl.exclude: " + extReflAddlExcludeStr);
        System.out.println("petablox.extrefl.exclude: " + extReflExcludeStr);
        System.out.println("petablox.build.scope: " + buildScope);
        System.out.println("petablox.run.analyses: " + runAnalyses);
        System.out.println("petablox.print.all.classes: " + printAllClasses);
        System.out.println("petablox.print.classes: " + printClasses);
        System.out.println("petablox.print.rels: " + printRels);
        System.out.println("petablox.print.project: " + printProject);
        System.out.println("petablox.print.results: " + printResults);
        System.out.println("petablox.save.maps: " + saveDomMaps);
        System.out.println("petablox.verbose: " + verbose);
        System.out.println("petablox.classic: " + classic);
        System.out.println("petablox.std.java.analysis.path: " + stdJavaAnalysisPathName);
        System.out.println("petablox.ext.java.analysis.path: " + extJavaAnalysisPathName);
        System.out.println("petablox.java.analysis.path: " + javaAnalysisPathName);
        System.out.println("petablox.std.dlog.analysis.path: " + stdDlogAnalysisPathName);
        System.out.println("petablox.ext.dlog.analysis.path: " + extDlogAnalysisPathName);
        System.out.println("petablox.dlog.analysis.path: " + dlogAnalysisPathName);
        System.out.println("petablox.use.jvmti: " + useJvmti);
        System.out.println("petablox.instr.kind: " + instrKind);
        System.out.println("petablox.trace.kind: " + traceKind);
        System.out.println("petablox.trace.block.size: " + traceBlockSize);
        System.out.println("petablox.dynamic.haltonerr: " + dynamicHaltOnErr);
        System.out.println("petablox.dynamic.timeout: " + dynamicTimeout);
        System.out.println("petablox.max.cons.size: " + maxConsSize);
        System.out.println("petablox.reuse.scope: " + reuseScope);
        System.out.println("petablox.reuse.rels: " + reuseRels);
        System.out.println("petablox.reuse.traces: " + reuseTraces);
        System.out.println("petablox.use.buddy: " + useBuddy);
        System.out.println("petablox.bddbddb.max.heap: " + bddbddbMaxHeap);
        System.out.println("petablox.datalog.engine: " + datalogEngine);
        System.out.println("petablox.logicblox.work.dir: " + logicbloxWorkDirName);
    }

    public static String outRel2Abs(String fileName) {
        return (fileName == null) ? null : Utils.getAbsolutePath(outDirName, fileName);
    }

    public static String workRel2Abs(String fileName) {
        return (fileName == null) ? null : Utils.getAbsolutePath(workDirName, fileName);
    }

    public static void check(String val, String[] legalVals, String key) {
        for (String s : legalVals) {
            if (val.equals(s))
                return;
        }
        String legalValsStr = "[ ";
        for (String s : legalVals)
            legalValsStr += s + " ";
        legalValsStr += "]";
        Messages.fatal(BAD_OPTION, val, key, legalValsStr);
    }
}
