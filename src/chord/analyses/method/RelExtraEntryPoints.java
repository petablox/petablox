package chord.analyses.method;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver;
import chord.program.ClassHierarchy;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "MentryPoints",
    sign = "M0:M0"
)
/**
 * A relation over domain M containing additional entry points for the program.
 * The values of this relation are derived from the file indicated by property 
 * chord.entrypoints.file.
 * 
 * File should be a list whose entries are class names, interface names, or fully qualified method names.
 *  (A fully qualified method name is of the form <method_subsignature>@<classname>.)
 *  If a concrete class is listed, all non-private methods of that class will be added as entry points.
 *  If an interface or abstract is listed, all public declared methods of that interaface/class in
 *  all concrete subclasses will be added as entry points.
 */
public class RelExtraEntryPoints extends ProgramRel {

    public final static String extraMethodsFile = System.getProperty("chord.entrypoints.file");
    public final static String extraMethodsList = System.getProperty("chord.entrypoints");
    static LinkedHashSet<SootMethod> methods;

    @Override
    public void fill() {
        Iterable<SootMethod> publicMethods =  slurpMList();
        for (SootMethod m: publicMethods) {
            super.add(m);
        }
    }

    public static Collection<SootMethod> slurpMList() {
        if (methods != null)
            return methods;
        if (extraMethodsList == null && extraMethodsFile == null)
            return Collections.emptyList();

        methods = new LinkedHashSet<SootMethod>();
        ClassHierarchy ch = Program.g().getClassHierarchy();

        if (extraMethodsList != null) {
            String[] entries = extraMethodsList.split(",");
            for (String s: entries)
                processLine(s, ch);
        }

        try {
            if (extraMethodsFile != null) {
                String s = null;
                BufferedReader br = new BufferedReader(new FileReader(extraMethodsFile));
                while( (s = br.readLine()) != null) {
                    if (s.startsWith("#"))
                        continue;
                    processLine(s, ch);
                }
                br.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        } 
        return methods;
    }

    private static void processLine(String s, ClassHierarchy ch) {
        try { 
            if (s.contains("@")) {
                // s is a method.
                int strudelPos = s.indexOf('@');
                if (strudelPos > 0) {
                    String cName = s.substring(strudelPos+1);
                    String mSubsig = s.substring(0, strudelPos);
                    SootClass parentClass  = SootResolver.v().makeClassRef(cName);
                    //parentClass.prepare();  
                    SootMethod m = parentClass.getMethod(mSubsig);
                    methods.add(m);
                } //badly formatted; skip
            } else { //s is a class name
                SootClass pubI  =  SootResolver.v().makeClassRef(s);
                if (pubI == null) {
                    System.err.println("ERR: no such class " + s );
                    return;
                } // else pubI.prepare();  

                //two cases: pubI is an interface/abstract class or pubI is a concrete class.
                if (pubI.isInterface() || pubI.isAbstract()) {
                    Set<String> impls =  ch.getConcreteSubclasses(pubI.getName());
                    if (impls == null) {
                        System.err.println("ExtraEntryPoints: found no concrete impls or subclasses of " + pubI.getName());
                        return;
                    }

                    for (String impl:impls) {
                        SootClass implClass = SootResolver.v().makeClassRef(impl);
                        //implClass.prepare();
                        Iterator<SootMethod> it = pubI.methodIterator();
                        while (it.hasNext()) {
                        	SootMethod ifaceM = it.next();
                            if (!ifaceM.isStatic()) {
                            	SootClass implementingClass = implClass;
                                while(implementingClass != null) {
                                    SootMethod implM = implementingClass.getMethod(ifaceM.getSubSignature());
                                    if (implM != null) {
                                        methods.add(implM);
                                        break;
                                    } else {
                                        implementingClass = implementingClass.getSuperclass();
                                        //implementingClass.prepare();
                                    }
                                }
                            }   
                        }
                    }
                } else { //class is concrete
                	 Iterator<SootMethod> it = pubI.methodIterator();
                     while (it.hasNext()) {
                     	SootMethod m = it.next();
                         if (!m.isPrivate())
                             methods.add(m);
                     }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
