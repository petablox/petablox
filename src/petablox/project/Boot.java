package petablox.project;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.net.URL;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import edu.berkeley.confspell.*;
import petablox.util.ProcessExecutor;
import petablox.util.Utils;

/**
 * Entry point of Petablox before JVM settings are resolved.
 *
 * Resolves JVM settings and spawns Petablox in a fresh JVM process with those settings.
 *
 * The system properties in the current JVM are altered as follows (in order):
 *
 * 1. Property petablox.main.dir is set to the directory containing file petablox.jar
 *    from which this class is loaded.
 *
 * 2. All properties from file "[petablox.main.dir]/petablox.properties" are loaded, if the
 *    file exists.
 *
 * 3. Property petablox.work.dir is set to "[user.dir]" unless the user has defined it;
 *    in either case, its value is canonicalized, and Petablox exits if it is not a
 *    valid existing directory.
 *
 * 4. All properties from file "[petablox.work.dir]/petablox.properties" are loaded, if the
 *    file exists, unless the user has defined property petablox.props.file, in which
 *    case all properties from the file specified by that property are loaded; in the
 *    latter case, Petablox exits if the file cannot be read.
 *
 * 5. The following properties are set to the following values unless the user has
 *    already defined them:
 *
 *    Property name    Default value
 *
 *    petablox.max.heap  "2048m"
 *    petablox.max.stack "32m"
 *    petablox.jvmargs   "-ea -Xmx[petablox.max.heap] -Xss[petablox.max.stack]"
 *    petablox.classic   "true"
 *
 *    petablox.std.java.analysis.path "[petablox.main.dir]/petablox.jar"
 *    petablox.ext.java.analysis.path ""
 *    petablox.java.analysis.path     "[petablox.std.java.analysis.path]:[petablox.ext.java.analysis.path]"
 *
 *    petablox.std.dlog.analysis.path "[petablox.main.dir]/petablox.jar"
 *    petablox.ext.dlog.analysis.path ""
 *    petablox.dlog.analysis.path     "[petablox.std.dlog.analysis.path]:[petablox.ext.dlog.analysis.path]"
 *   
 *    petablox.class.path ""
 *
 * 6. Property user.dir is set to "[petablox.work.dir]".
 *
 * 7. Property java.class.path is set to
 *    "[petablox.main.dir]/petablox.jar:[petablox.java.analysis.path]:[petablox.dlog.analysis.path]:[petablox.class.path]".
 *
 * The above altered properties plus all other system properties in the current JVM
 * are passed on to the new JVM.
 *
 * Note: Do not refer to any properties defined in class petablox.project.Config here.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Boot {
    private static final String WARN_DUPLICATE_SYSPROP =
        "WARN: Property '%s' defined multiple times; assuming value '%s' instead of '%s'.";
    private static final String CHORD_JAR_NOT_FOUND =
        "ERROR: Boot: Expected Petablox to be loaded from petablox.jar instead of from '%s'.";
    private static final String USER_DIR_AS_CHORD_WORK_DIR =
        "WARN: Boot: Property petablox.work.dir not set; using value of user.dir '%s' instead.";
    private static final String CHORD_MAIN_DIR_UNDEFINED =
        "ERROR: Boot: Property petablox.main.dir must be set to location of directory named 'main' in your Petablox installation.";
    private static final String CHORD_MAIN_DIR_NOT_FOUND =
        "ERROR: Boot: Directory '%s' specified by property petablox.main.dir not found.";
    private static final String CHORD_WORK_DIR_UNDEFINED =
        "ERROR: Boot: Property petablox.work.dir must be set to location of working directory desired during Petablox's execution.";
    private static final String CHORD_WORK_DIR_NOT_FOUND =
        "ERROR: Boot: Directory '%s' specified by property petablox.work.dir not found.";

    public static boolean SPELLCHECK_ON = Utils.buildBoolProperty("petablox.useSpellcheck", false);

    static String mainDirName;

    public static void main(String[] args) throws Throwable {
        String chordJarFile = getPetabloxJarFile();

        // resolve Petablox's main dir

        mainDirName = (new File(chordJarFile)).getParent();
        if (mainDirName == null)
            Messages.fatal(CHORD_MAIN_DIR_UNDEFINED);
        System.setProperty("petablox.main.dir", mainDirName);

        if (SPELLCHECK_ON) {
            OptionSet optSet = new OptionSet(getPetabloxSysProps());
            optSet.enableSubstitution();
            Checker.checkConf(new OptDictionary(Boot.class.getResourceAsStream("/options.dict") ),
                optSet);
        }

        // resolve Petablox's work dir

        String workDirName = System.getProperty("petablox.work.dir");
        if (workDirName == null) {
            workDirName = System.getProperty("user.dir");
            if (workDirName == null)
                Messages.fatal(CHORD_WORK_DIR_UNDEFINED);
            Messages.log(USER_DIR_AS_CHORD_WORK_DIR, workDirName);
        }
        try {
            workDirName = (new File(workDirName)).getCanonicalPath();
        } catch (IOException ex) {
            Messages.fatal(ex);
        }
        if (!(new File(workDirName)).isDirectory()) {
            Messages.fatal(CHORD_WORK_DIR_NOT_FOUND, workDirName);
        }
        System.setProperty("petablox.work.dir", workDirName);

        // load program-specific Petablox properties, if any

        String propsFileName = System.getProperty("petablox.props.file");
        if (propsFileName != null) {
            try {
                readProps(propsFileName);
            } catch (IOException ex) {
                Messages.fatal(ex);
            }
        } else {
            try {
                propsFileName = workDirName + File.separator + "petablox.properties";
                readProps(propsFileName);
            } catch (IOException ex) {
                // ignore silently; user did not provide this file
            }
        }

        // load system-wide Petablox properties, if any

        try {
            readProps(mainDirName + File.separator + "petablox.properties");
        } catch (IOException ex) {
            // ignore silently; user is not required to provide this file
        }

        // process other JVM settings (maximum runtime heap size, classpath, etc.)

        String maxHeap = getOrSetProperty("petablox.max.heap", "2048m");
        String maxStack = getOrSetProperty("petablox.max.stack", "32m");
        String jvmargs = getOrSetProperty("petablox.jvmargs", "-ea -Xmx" + maxHeap + " -Xss" + maxStack);
        boolean isClassic = getOrSetProperty("petablox.classic", "true").equals("true");
        String stdJavaAnalysisPath = getOrSetProperty("petablox.std.java.analysis.path", chordJarFile);
        String extJavaAnalysisPath = getOrSetProperty("petablox.ext.java.analysis.path", "");
        String javaAnalysisPath = getOrSetProperty("petablox.java.analysis.path",
            Utils.concat(stdJavaAnalysisPath, File.pathSeparator, extJavaAnalysisPath));
        String stdDlogAnalysisPath = getOrSetProperty("petablox.std.dlog.analysis.path", chordJarFile);
        String extDlogAnalysisPath = getOrSetProperty("petablox.ext.dlog.analysis.path", "");
        String dlogAnalysisPath = getOrSetProperty("petablox.dlog.analysis.path",
            Utils.concat(stdDlogAnalysisPath, File.pathSeparator, extDlogAnalysisPath));
        String userClassPath = getOrSetProperty("petablox.class.path", "");
        boolean isFixedCPU = getOrSetProperty("petablox.fixCPU", "false").equals("true");
        String CPUID = getOrSetProperty("petablox.CPUID", "0");

        System.setProperty("user.dir", workDirName);

        List<String> cpList = new ArrayList<String>(10);
        cpList.add(chordJarFile);
        if (!javaAnalysisPath.equals("")) {
            String[] a = javaAnalysisPath.split(Utils.PATH_SEPARATOR);
            for (String s : a) {
                if (!cpList.contains(s))
                    cpList.add(s);
            }
        }
        if (!dlogAnalysisPath.equals("")) {
            String[] a = dlogAnalysisPath.split(Utils.PATH_SEPARATOR);
            for (String s : a) {
                if (!cpList.contains(s))
                    cpList.add(s);
            }
        }
        if (!userClassPath.equals("")) {
            userClassPath= userClassPath.replace(';', File.pathSeparatorChar);//normalize
            System.setProperty("petablox.class.path", userClassPath);//write back new value

            String[] a = userClassPath.split(Utils.PATH_SEPARATOR);
            for (String s : a) {
                if (!cpList.contains(s))
                    cpList.add(s);
            }
        }
        String cp = cpList.get(0);
        for (int i = 1; i < cpList.size(); i++)
            cp += File.pathSeparator + cpList.get(i);
        System.setProperty("java.class.path", cp);

        // build command line arguments of fresh JVM process to run Petablox

        List<String> cmdList = new ArrayList<String>();
        if (isFixedCPU) {
        	cmdList.add("taskset");
        	cmdList.add("-c");
        	cmdList.add(CPUID);
        }
        cmdList.add("java");
        for (String s : jvmargs.split(" "))
            cmdList.add(s);
        for (Map.Entry e : System.getProperties().entrySet()) {
            String k = (String) e.getKey();
            String v = (String) e.getValue();
            // no need to pass standard params
            if (k.startsWith("sun") || k.startsWith("jikes"))
                continue; 
            cmdList.add("-D" + k + "=" + v);
        }
        if (!isClassic) {
            cmdList.add("hj.lang.Runtime");
            cmdList.add("-INIT_THREADS_PER_PLACE=1");
            cmdList.add("-NUMBER_OF_LOCAL_PLACES=1");
            cmdList.add("-rt=wsh CnCHJ.runtime.CnCRuntime");
            cmdList.add("-policy=BlockingCoarse");
        }
        cmdList.add("petablox.project.Main");
        String[] cmdAry = new String[cmdList.size()];
        cmdList.toArray(cmdAry);
        
        if (Utils.buildBoolProperty("showMainArgs", false))
            showArgsToMain(cmdAry);
        
        int result = ProcessExecutor.execute(cmdAry, null, new File(workDirName), -1);
        System.exit(result);
    }

    private static void showArgsToMain(String[] cmdAry) {
        StringBuilder cmdLine = new StringBuilder();
        for(String s: cmdAry) {
            cmdLine.append(s);
            cmdLine.append(" ");
        }
        System.out.println("Boot spawning subprocess with command line: " + cmdLine.toString());
    }

    private static Properties getPetabloxSysProps() {
        Properties p = new Properties();
        for (Map.Entry e : System.getProperties().entrySet()) {
            if (e.getKey().toString().startsWith("petablox"))
                p.setProperty(e.getKey().toString(), e.getValue().toString());
        }
        return p;
    }

    private static String getPetabloxJarFile() {
        String cname = Boot.class.getName().replace('.', '/') + ".class";
        URL url = Boot.class.getClassLoader().getResource(cname);
        if (!url.getProtocol().equals("jar"))
            Messages.fatal(CHORD_JAR_NOT_FOUND, url.toString());
        String file = url.getFile();
        return file.substring(file.indexOf(':') + 1, file.indexOf('!'));
    }

    private static String getOrSetProperty(String key, String defVal) {
        String val = System.getProperty(key);
        if (val != null)
            return val;
        System.setProperty(key, defVal);
        return defVal;
    }

    private static void readProps(String fileName) throws IOException {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(fileName);
        props.load(in);
        in.close();

        if (SPELLCHECK_ON) { //Check the params we just read in
            OptionSet optSet = new OptionSet(props);
            optSet.enableSubstitution();
            Checker.checkConf(new OptDictionary(Boot.class.getResourceAsStream("/options.dict")), optSet);
        }

        Properties sysprops = System.getProperties();
        for (Map.Entry e : props.entrySet()) {
            String key = (String) e.getKey();
            String val = (String) e.getValue();
            val = substituteVars(props, val);
            String oldVal = (String) sysprops.get(key);
            if (oldVal == null)
                sysprops.setProperty(key, val);
            else if (!oldVal.equals(val))
                Messages.log(WARN_DUPLICATE_SYSPROP, key, oldVal, val);
        }
    }
    
    // ${} substitution code from Hadoop's Configuration class.
    // [under Apache license]
    private static Pattern varPat = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");
    private static int MAX_SUBST = 50;
    private static String substituteVars(Properties props, String expr) {
        if (expr == null) {
            return null;
        }
        Matcher match = varPat.matcher("");
        String eval = expr;
        for (int s = 0; s < MAX_SUBST; s++) {
            match.reset(eval);
            if (!match.find()) {
                return eval;
            }
            String variableName = match.group();
            variableName = variableName.substring(2, variableName.length() - 1); // remove ${ .. }
            String val = props.getProperty(variableName, System.getProperty(variableName));
            if (val == null) {
                return eval; // return literal ${var}: var is unbound
            }
            // substitute
            eval = eval.substring(0, match.start())+val+eval.substring(match.end());
        }
        Messages.fatal("Variable substitution depth too large: " + MAX_SUBST + " " + expr);
        return null;
    }
}
