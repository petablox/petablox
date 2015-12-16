package chord.analyses.invk;

import chord.analyses.method.DomM;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import soot.RefLikeType;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver;

/**
 * Used to rewrite method calls, to facilitate stub implementations or
 * analyzable models.
 * If property chord.methodRemapFile is set, will read a map from that file.
 * There two sorts of entries supported: overriding a particular concrete method,
 * and overriding an entire class.
 * 
 * For overriding a method, the format is source-method dest-method, separated
 * by a space. Both source and dest should be fully qualified method names, of 
 * the form method_subsignature@declaringClass
 * 
 * Note that for virtual function calls, the rewrite happens AFTER
 * the call target is resolved. So if you have a stub implementation for
 * Derived.foo, then a call to Base.foo on an instance of Derived should
 * call the stub.
 * 
 * Alternatively, you can override an entire class. The format is simply
 * previousClass newClass, separated by a space. Unlike the method case,
 * here the substitution happens BEFORE virtual function resolution.
 * The idea is to let you override the behavior of a whole class hierarchy,
 * e.g. by replacing all Collections with an easier-to-model one.
 * 
 * Be careful about the prototype for the function being mapped; the remap
 * will fail with a warning message if any details do not match.
 * 
 * Blank lines and lines starting with a # are ignored as comments.
 * 
 * Note also that there is no checking performed that the old and new functions
 * have the compatible prototypes. Arguments and return values may wind up
 * not propagating correctly if, e.g., a 2-argument function is remapped
 * to a 3-argument function.
 * 
 *
 */
public class StubRewrite {
    
    private static HashMap<SootMethod,SootMethod> methLookupTable; //initialized only once
    private static HashMap<RefLikeType,RefType> classLookupTable; //initialized only once
    //private static HashSet<SootClass> stubDests = new HashSet<SootClass>();
    
    private static SootMethod getMeth(String clName, String methSubsig) {
        SootClass cl = SootResolver.v().makeClassRef(clName);
        //cl.prepare();
        return (SootMethod) cl.getMethod(methSubsig);
    }
    

    private static void mapClassNames(String srcClassName, String destClassName) {
        SootClass src = SootResolver.v().makeClassRef(srcClassName);
        //src.prepare();
        SootClass dest = SootResolver.v().makeClassRef(destClassName);
        //dest.prepare();
        System.out.println("StubRewrite mapping "+ srcClassName + " to " + destClassName);
        classLookupTable.put(src.getType(), dest.getType());        
    }
    
    //called for virtual function lookup
    public static SootMethod maybeReplaceVirtCallDest(SootMethod base,
            SootMethod derived, DomM domM) {
        SootClass baseClass= base.getDeclaringClass();
        RefType remapClass = classLookupTable.get(baseClass.getType());
        if(remapClass != null) {
//            System.out.println("found class-level remap of " + baseClass);
            String subsig = base.getSubSignature();
            SootMethod remapped = remapClass.getSootClass().getMethod(subsig);
            if(remapped == null) {
                System.err.println("WARN StubRewrite remap failed due to missing target method for " + remapClass.getSootClass() + " " + subsig);
                assert false;
                return derived; //assume no remap
            } else  if(!domM.contains(remapped)) {
                System.err.println("WARN: StubRewrite tried to map " + derived + " to "+ 
                        remapped + ", which doesn't exist");
                return derived;
            } else
                return remapped;
        } else { //didn't remap entire class. What about methods?
            SootMethod replacement = methLookupTable.get(derived);
            if(replacement == null || replacement.equals(base)) {//can't map derived to base {
//                System.out.println("NOTE: NOT virtual call to " + derived + "; no replacement found");
                return derived;
            } else {
                System.out.println("NOTE: mapping virtual call to " + derived + " being redirected to " + replacement);
                return replacement;
            }
        }
    }

    //called for all other cases
    public static SootMethod maybeReplaceCallDest(SootMethod caller, SootMethod m) {
        //Don't rewrite stub calls that would become recursive.
        //this allows us to insert decorators that call the underlying method.
        SootMethod replacement = methLookupTable.get(m);
        if(replacement == null || replacement.equals(caller))
            return m;
        else return replacement;
    }
    
//    public static jq_Method maybeReplaceCallDest(jq_Method m) {
//        jq_Method replacement = maybeReplaceCallDest
//    }
    
    //static Pattern linePat = Pattern.compile("([^:]*):([^@]*)@([^ ]*) ([^:]*):([^@]*)@([^ ]*)");
    static Pattern linePat = Pattern.compile("([^@]*)@([^ ]*) ([^@]*)@([^ ]*)");
    static {
        init();
    }
    
    public static void init() {
        if(methLookupTable != null)
            return;
        else { 
            //we iterate over these maps in addNewDests.
            methLookupTable = new LinkedHashMap<SootMethod,SootMethod>();
            classLookupTable = new LinkedHashMap<RefLikeType, RefType>();
        }
        try {
            String fileNames = System.getProperty("chord.methodRemapFile");
            if(fileNames == null)
                return;
            String[] names = fileNames.split(",");
            for(String fileName:names) {
                readStubFile(fileName);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    private static void readStubFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String ln = null;
        while((ln = br.readLine()) != null) {
            if(ln.length() < 1 || ln.startsWith("#"))
                continue;
            
            Matcher methRewriteMatch = linePat.matcher(ln);
            if(methRewriteMatch.find()) {
                String srcMethSubsig = methRewriteMatch.group(1);
                String srcClassName = methRewriteMatch.group(2);

                String destMethSubsig = methRewriteMatch.group(3);
                String destClassName = methRewriteMatch.group(4);

                SootMethod src = getMeth(srcClassName, srcMethSubsig);
                SootMethod dest = getMeth(destClassName, destMethSubsig);
                if(src != null && dest != null) {
                    //can do more checks here, for e.g., arity matching
                    methLookupTable.put(src, dest);
                    System.out.println("StubRewrite mapping "+ srcClassName + "." + srcMethSubsig + " to " + destClassName+"."+destMethSubsig);
                } else {
                    if(src == null)
                        System.err.println("WARN: StubRewrite failed to map "+ srcClassName + "." + srcMethSubsig +", couldn't resolve source");
                    else
                        System.err.println("WARN: StubRewrite failed to map "+ destClassName + "." + destMethSubsig + " " + " -- couldn't resolve dest");
                }
            } else {
                String[] parts = ln.split(" ");
                if(parts.length == 2) {
                    String srcClassName = parts[0];
                    String destClassName = parts[1];
                    mapClassNames(srcClassName, destClassName);
                } else
                    System.err.println("WARN: StubRewrite couldn't parse line "+ ln);
            }
        } //end while
        br.close();
    }

    /**
     * The stub methods that we intend to call need to be part of domM etc.
     * This method is called by RTA and should add all stub targets to
     * the publicMethods collection that's passed in.
     */
    public static void addNewDests(Collection<SootMethod> publicMethods) {
//        System.out.println("in StubRewrite.addNewDests");
        publicMethods.addAll(methLookupTable.values());
        for(RefType rt: classLookupTable.values()) {
        	SootClass cl = rt.getSootClass();
        	Iterator<SootMethod> it = cl.methodIterator();
        	 while (it.hasNext()) {
             	SootMethod m = it.next();
                 if (!m.isStatic())
//                   System.out.println("StubRewrite adding method " + m.getNameAndDesc() + " added to publicMethods");
                     publicMethods.add(m);
             }
        }
    }

    public static RefType fakeSubtype(RefLikeType t1) {
        return classLookupTable.get(t1);
    }

}
