// CodeFragment.java, created Jan 25, 2005 5:41:21 PM 2005 by jwhaley
// Copyright (C) 2005 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jwutil.io.InputStreamGobbler;
import jwutil.io.SystemProperties;
import net.sf.javabdd.BDD;

/**
 * CodeFragment
 * 
 * @author jwhaley
 * @version $Id: CodeFragment.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public class CodeFragment {

    static boolean TRACE = false;
    
    String fragment;
    Method method;
    
    /**
     */
    public CodeFragment(String string, InferenceRule ir) {
        this.fragment = string;
        this.method = genMethod(ir);
        if (this.method == null) throw new IllegalArgumentException();
    }

    /**
     */
    public CodeFragment(String string, Relation r) {
        this.fragment = string;
        this.method = genMethod(r);
        if (this.method == null) throw new IllegalArgumentException();
    }
    
    /**
     * Find a place for a temp file in classpath.
     * 
     * @return path in classpath that is writable, or null.
     */
    public static File findWritablePath() {
        // Find a place for a temp file in classpath.
        String cp = SystemProperties.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(cp, SystemProperties.getProperty("path.separator"));
        while (st.hasMoreTokens()) {
            String p = st.nextToken();
            File f = new File(p);
            if (!f.isDirectory()) continue;
            if (!f.canWrite()) continue;
            if (TRACE) System.out.println("Path for code fragment: "+f);
            return f;
        }
        return null;
    }
    
    /**
     * Search for javac executable.
     * 
     * @return path to javac executable, or null
     */
    static String searchForJavac(String path, String[] dirs) {
        String sep = SystemProperties.getProperty("file.separator");
        for (int i = 0; i < dirs.length; ++i) {
            File f2 = new File(path+sep+dirs[i]+sep+"bin"+sep+"javac");
            if (f2.exists()) return f2.getAbsolutePath();
            f2 = new File(path+sep+dirs[i]+sep+"bin"+sep+"javac.exe");
            if (f2.exists()) return f2.getAbsolutePath();
        }
        return null;
    }
    
    /**
     * Try to find where javac executable is installed.
     * 
     * @return path to javac, or null.
     */
    public static String findJavac() {
        // First, check to see if it is in the normal path.
        try {
            Process p = Runtime.getRuntime().exec("javac");
            new InputStreamGobbler(p.getInputStream(), (OutputStream)null).start();
            new InputStreamGobbler(p.getErrorStream(), (OutputStream)null).start();
            int c = p.waitFor();
            return "javac";
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        try {
            Process p = Runtime.getRuntime().exec("jikes");
            new InputStreamGobbler(p.getInputStream(), (OutputStream)null).start();
            new InputStreamGobbler(p.getErrorStream(), (OutputStream)null).start();
            int c = p.waitFor();
            return "jikes";
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        
        // Try harder.
        String path = SystemProperties.getProperty("java.home");
        String sep = SystemProperties.getProperty("file.separator");
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.startsWith("jdk")) return true;
                if (name.startsWith("j2sdk")) return true;
                return false;
            }
        };
        // Look relative to "java.home".
        File f = new File(path+sep+"..");
        String[] s = f.list(filter);
        String s2 = searchForJavac(path+sep+"..", s);
        if (s2 != null) return s2;
        
        // Look in the root directory.
        f = new File(sep);
        s = f.list(filter);
        s2 = searchForJavac(sep, s);
        if (s2 != null) return s2;
        
        // Look in some other common places.
        f = new File("/usr/java/bin/javac");
        if (f.exists()) return f.getAbsolutePath();
        f = new File("/usr/java");
        s = f.list(filter);
        if (s != null) s2 = searchForJavac("/usr/java", s);
        if (s2 != null) return s2;
        
        // Give up!
        return null;
    }
    
    Method genMethod(Object o) {
        File path = findWritablePath();
        if (path == null) {
            System.err.println("Cannot find writable directory in class path, skipping code fragment generation.");
            return null;
        }
        String javacName = findJavac();
        if (javacName == null) {
            System.err.println("Cannot find java compiler, skipping code fragment generation.");
            return null;
        }
        if (TRACE) System.out.println("Using Java compiler "+javacName);
        
        String className;
        try {
            // Create temp file.
            File temp = File.createTempFile("frag", ".java", path);
            
            // Delete temp file when program exits.
            temp.deleteOnExit();
            
            // Get class name from file name.
            className = temp.getName();
            className = className.substring(0, className.length()-5);
            if (TRACE) System.out.println("Writing temporary file for code fragment to: "+temp);
            
            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write("import net.sf.bddbddb.*;\nimport net.sf.javabdd.*;\n");
            out.write("import net.sf.bddbddb.dataflow.*;\nimport net.sf.bddbddb.ir.*;\nimport net.sf.bddbddb.order.*;\n\n");
            out.write("public class ");
            out.write(className);
            out.write(" {\n    public static void go(");
            if (o instanceof BDDInferenceRule) {
                BDDInferenceRule ir = (BDDInferenceRule) o;
                out.write("BDDInferenceRule rule, BDD val) throws Exception {\n");
                out.write("    java.util.List subgoals = rule.getSubgoals();\n");
                int k = 0;
                for (Iterator i = ir.top.iterator(); i.hasNext(); ) {
                    RuleTerm rt = (RuleTerm) i.next();
                    out.write("    RuleTerm subgoal"+k+" = (RuleTerm) subgoals.get("+k+");\n");
                    out.write("    Relation "+rt.relation.name+" = subgoal"+k+".getRelation();\n");
                    ++k;
                }
                out.write("    RuleTerm head = rule.getHead();\n");
                out.write("    Relation "+ir.bottom.relation.name+" = head.getRelation();\n");
            } else {
                BDDRelation r = (BDDRelation) o;
                out.write("BDDRelation "+r.name+", BDD val) throws Exception {\n");
            }
            out.write(fragment);
            out.write("\n    }\n}\n");
            out.close();
            
            String cp = SystemProperties.getProperty("java.class.path");
            Process javac = Runtime.getRuntime().exec(
                new String[] { javacName, "-source", "1.3", "-target", "1.3",
                    "-classpath", cp, className+".java" }, null, path);
            new InputStreamGobbler(javac.getInputStream(), System.out).start();
            new InputStreamGobbler(javac.getErrorStream(), System.err).start();
            javac.waitFor();
            int rc = javac.exitValue();
            if (rc != 0) {
                System.err.println("Error occurred while compiling code fragment: "+rc);
                return null;
            }
            File classFile = new File(path, className+".class");
            classFile.deleteOnExit();
            
            Class c = Class.forName(className);
            try {
                Class type = (o instanceof BDDInferenceRule) ? BDDInferenceRule.class : BDDRelation.class;
                Method method = c.getDeclaredMethod("go", new Class[] { type, BDD.class });
                return method;
            } catch (SecurityException e) {
                System.err.println("Security exception occurred while accessing method for code fragment.");
            } catch (NoSuchMethodException e) {
                System.err.println("Cannot find method for code fragment.");
            }
        } catch (IOException e) {
            System.err.println("Error occurred while writing class file for code fragment: "+e);
        } catch (InterruptedException e) {
            System.err.println("Error: compilation of code fragment was interrupted.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: cannot find compiled code fragment.");
        }
        return null;
    }
    
    /**
     * @param rule
     * @param oldValue
     */
    public void invoke(InferenceRule rule, BDD oldValue) {
        if (method == null) return;
        try {
            method.invoke(null, new Object[] { rule, oldValue });
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * @param r
     * @param oldValue
     */
    public void invoke(Relation r, BDD oldValue) {
        if (method == null) return;
        try {
            method.invoke(null, new Object[] { r, oldValue });
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
