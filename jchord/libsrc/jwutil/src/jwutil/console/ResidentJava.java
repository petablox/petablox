// ResidentJava.java, created May 26, 2004 6:31:55 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.console;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jwutil.classloader.HijackingClassLoader;
import jwutil.strings.MyStringTokenizer;

/**
 * ResidentJava
 * 
 * @author jwhaley
 * @version $Id: ResidentJava.java,v 1.2 2005/04/29 02:32:25 joewhaley Exp $
 */
public class ResidentJava {
    
    /**
     * A special exception type that is thrown when System.exit() is called.
     * 
     * @author jwhaley
     * @version $Id: ResidentJava.java,v 1.2 2005/04/29 02:32:25 joewhaley Exp $
     */
    public static class SystemExitException extends SecurityException {

        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3257570594202399025L;
        
        int status;
        
        /**
         * @param status
         */
        public SystemExitException(int status) {
            this.status = status;
        }
        
    }
    
    /**
     * Initializes a security manager that will trap calls to System.exit(),
     * causing them to throw a SystemExitException instead.
     * 
     * @see jwutil.console.ResidentJava.SystemExitException
     */
    public static void trapOnSystemExit() {
        SecurityManager sm = new SecurityManager() {
            public void checkAccept(String host, int port) {}
            public void checkAccess(Thread t) {}
            public void checkAccess(ThreadGroup t) {}
            public void checkAwtEventQueueAccess(ThreadGroup t) {}
            public void checkConnect(String host, int port) {}
            public void checkConnect(String host, int port, Object context) {}
            public void checkCreateClassLoader() {}
            public void checkDelete() {}
            public void checkExec(String file) {}
            public void checkExit(int status) {
                throw new SystemExitException(status);
            }
            public void checkLink(String lib) {}
            public void checkListen(int port) {}
            public void checkMemberAccess(Class clazzz, int which) {}
            public void checkMulticast(java.net.InetAddress maddr) {}
            public void checkPackageAccess(String pkg) {}
            public void checkPackageDefinition(String pkg) {}
            public void checkPermission(java.security.Permission perm) {}
            public void checkPermission(java.security.Permission perm, Object context) {}
            public void checkPrintJobAccess() {}
            public void checkPropertiesAccess() {}
            public void checkPropertyAccess(String key) {}
            public void checkRead(java.io.FileDescriptor fd) {}
            public void checkRead(String file) {}
            public void checkRead(String file, Object context) {}
            public void checkSecurityAccess(String target) {}
            public void checkSetFactory() {}
            public void checkSystemClipboardAccess() {}
            public boolean checkTopLevelWindow(Object window) { return true; }
            public void checkWrite(java.io.FileDescriptor fd) {}
            public void checkWrite(String file) {}
        };
        System.setSecurityManager(sm);
    }
    
    public static int CR   = 1;
    public static int CRLF = 2;
    public static int AUTO = 3;
    public static int EOL = AUTO;
    
    public static String readLine(InputStream in) throws IOException {
        char[] buf = new char[256];
        StringBuffer s = null;
        outer:
        for (;;) {
            int j;
            for (j = 0; j < buf.length; ++j) {
                int i = in.read();
                if (i == -1) {
                    // EOF.
                    if (s == null && j > 0) s = new StringBuffer(j);
                    break;
                }
                char c = (char) i;
                if (c == '\r') {
                    // EOL.
                    if (EOL == CRLF || EOL == AUTO) {
                        if (in.markSupported()) {
                            in.mark(1);
                            i = in.read();
                            if ((char) i != '\n') in.reset();
                        } else if (EOL == CRLF) {
                            i = in.read();
                        }
                    }
                    if (s == null) s = new StringBuffer(j);
                    break;
                } else if (c == '\n') {
                    // EOL.
                    if (s == null) s = new StringBuffer(j);
                    break;
                }
                buf[j] = c;
            }
            if (j == 0) break;
            s.append(buf, 0, j);
            if (j < buf.length) break;
        }
        if (s != null) return s.toString();
        else return null;
    }
    
    public static void main(String[] args) throws IOException {
        trapOnSystemExit();
        for (;;) {
            // Cache System.in/out/err in case the application changes them.
            InputStream in = System.in;
            PrintStream out = System.out;
            PrintStream err = System.err;
            
            // Read the command line.
            String commandLine = readLine(in);
            if (commandLine == null) break;
            
            // Execute the program.
            if (executeProgram(commandLine))
                break;
            
            // Reset System.in/out/err.
            if (in != System.in) System.setIn(in);
            if (out != System.out) System.setOut(out);
            if (err != System.err) System.setErr(err);
        }
    }
    
    public static boolean executeProgram(String line) {
        ExecuteOptions ops;
        try {
            ops = ExecuteOptions.parse(line);
            try {
                if (ops != null) ops.go();
            } catch (InvocationTargetException e) {
                Throwable x = e.getTargetException();
                if (x instanceof SystemExitException) {
                    int status = ((SystemExitException)x).status;
                    if (status != 0) {
                        System.err.println("Java process exited with error code "+status);
                    }
                } else {
                    System.err.println("Java process ended with exception: "+x.toString());
                    x.printStackTrace(System.err);
                }
            } catch (Error x) {
                System.err.println("Java process ended with error: "+x.toString());
                x.printStackTrace(System.err);
            }
        } catch (SystemExitException e) {
            System.err.println("Exiting.");
            return true;
        } catch (SecurityException e) {
            System.err.println("Security exception while accessing class/method.");
        } catch (IllegalAccessException e) {
            System.err.println("Class/method is not public.");
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: "+e.getLocalizedMessage());
        } catch (NoSuchMethodException e) {
            System.err.println("Class does not contain an appropriate main method.");
        } catch (Error x) {
            System.err.println("Could not start Java process due to error: "+x.toString());
            x.printStackTrace(System.err);
        }
        return false;
    }
    
    public static boolean USE_HIJACKING_CLASSLOADER = true;
    
    public static String format(long num) {
        if (num >= 1048576) return (num / 1048576) + "M";
        if (num >= 1024) return (num / 1024) + "K";
        return Long.toString(num);
    }
    
    public static void garbageCollect() {
        long totalMem, usedMem;
        totalMem = Runtime.getRuntime().totalMemory();
        usedMem = totalMem - Runtime.getRuntime().freeMemory();
        System.err.print("Memory: "+format(usedMem));
        System.gc();
        totalMem = Runtime.getRuntime().totalMemory();
        usedMem = totalMem - Runtime.getRuntime().freeMemory();
        System.err.println(" -> "+format(usedMem)+" ("+format(totalMem)+" total)");
    }
    
    public static void printHelp() {
        System.err.println("ResidentJava keeps the Java virtual machine resident, allowing you to execute");
        System.err.println("multiple commands without the overhead of restarting the virtual machine.");
        System.err.println("Simply type your java command as normal:");
        System.err.println();
        System.err.println("e.g.   java -cp test.jar:baz my.package.HelloWorld");
        System.err.println();
        System.err.println("The java keyword is optional.");
        System.err.println();
        System.err.println("Other commands:");
        System.err.println("    gc    run garbage collector");
        System.err.println("    help  print this message");
        System.err.println("    quit  exit the loader");
    }
    
    public static class ExecuteOptions {
        
        Properties properties;
        Method mainMethod;
        String[] args;
        
        public static ExecuteOptions parse(String commandLine) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
            MyStringTokenizer st = new MyStringTokenizer(commandLine);
            if (!st.hasMoreTokens()) return null;
            
            ExecuteOptions dis = new ExecuteOptions();
            dis.properties = new Properties(System.getProperties());
            ClassLoader myClassLoader;
            if (USE_HIJACKING_CLASSLOADER) myClassLoader = HijackingClassLoader.makeClassLoader();
            else myClassLoader = ClassLoader.getSystemClassLoader();
            String s = st.nextToken();
            if (s.equals("java") || s.equals("javaw")) {
                s = st.nextToken();
            } else if (s.equals("gc")) {
                garbageCollect();
                return null;
            } else if (s.equals("quit")) {
                System.exit(0);
                return null;
            } else if (s.equals("help")) {
                printHelp();
                return null;
            }
            for (;;) {
                if (s.startsWith("-D")) {
                    String propertyName;
                    String propertyValue;
                    int index = s.indexOf('=');
                    if (index > 0) {
                        propertyName = s.substring(2, index);
                        propertyValue = s.substring(index+1);
                    } else {
                        propertyName = s.substring(2);
                        propertyValue = "";
                    }
                    if (propertyName.equals("java.library.path")) {
                        System.err.println("Warning: Setting java.library.path property has no effect.");
                    }
                    dis.properties.put(propertyName, propertyValue);
                } else if (s.equals("-cp") || s.equals("-classpath")) {
                    String cp = st.nextToken();
                    dis.properties.setProperty("java.class.path", cp);
                    if (myClassLoader instanceof HijackingClassLoader) {
                        HijackingClassLoader cl = (HijackingClassLoader) myClassLoader;
                        cl.addURLs(HijackingClassLoader.getURLs(cp));
                    } else {
                        System.err.println("Warning: Cannot change class path when using system class loader.");
                    }
                } else if (s.startsWith("-")) {
                    System.err.println("Warning: Unsupported option: "+s);
                } else {
                    s = s.replace('/', '.');
                    Class mainClass = Class.forName(s, true, myClassLoader);
                    dis.mainMethod = mainClass.getDeclaredMethod("main", new Class[] { String[].class });
                    List a = new LinkedList();
                    while (st.hasMoreTokens()) {
                        a.add(st.nextToken());
                    }
                    dis.args = (String[]) a.toArray(new String[a.size()]);
                    return dis;
                }
                if (!st.hasMoreTokens()) break;
                s = st.nextToken();
            }
            return null;
        }
        
        private ExecuteOptions() { }
        
        public void go() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            Properties old = System.getProperties();
            System.setProperties(properties);
            try {
                mainMethod.invoke(null, new Object[] { args });
            } finally {
                System.setProperties(old);
            }
        }

    }
}
