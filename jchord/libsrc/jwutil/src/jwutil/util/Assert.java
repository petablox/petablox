// Assert.java, created Wed Mar  5  0:26:32 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.util;

/**
 * Includes methods for an assertion mechanism.  When an assertion fails, it
 * drops into the debugger (in native mode) or just exits (in hosted mode).
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Assert.java,v 1.2 2004/12/10 09:41:32 joewhaley Exp $
 */
public abstract class Assert {
    
    /**
     * Assert that the given predicate is true.  If it is false, we drop into
     * the debugger (in native mode) or just exit (in hosted mode).
     * 
     * @param b predicate to check
     */
    public static void _assert(boolean b) {
        _assert(b, "");
    }

    /**
     * Assert that the given predicate is true.  If it is false, we print
     * the given reason and drop into the debugger (in native mode) or just exit
     * (in hosted mode).
     * 
     * @param b predicate to check
     * @param reason string to print if the assertion fails
     */
    public static void _assert(boolean b, String reason) {
        if (!b) {
            _debug.writeln("Assertion Failure!");
            _debug.writeln(reason);
            _debug.die(-1);
        }
    }

    /**
     * Print a TODO message and drop into the debugger (in native mode) or just
     * exit (in hosted mode).
     * 
     * @param s message to print
     */
    public static void TODO(String s) {
        _debug.write("TODO: ");
        _debug.writeln(s);
        _debug.die(-1);
    }

    /**
     * Print a TODO message and drop into the debugger (in native mode) or just
     * exit (in hosted mode).
     */
    public static void TODO() {
        _debug.writeln("TODO");
        _debug.die(-1);
    }

    /**
     * Print an UNREACHABLE message and drop into the debugger (in native mode)
     * or just exit (in hosted mode).
     * 
     * @param s message to print
     */
    public static void UNREACHABLE(String s) {
        _debug.write("UNREACHABLE: ");
        _debug.writeln(s);
        _debug.die(-1);
    }

    /**
     * Print an UNREACHABLE message and drop into the debugger (in native mode)
     * or just exit (in hosted mode).
     */
    public static void UNREACHABLE() {
        _debug.writeln("BUG! unreachable code reached!");
        _debug.die(-1);
    }
    
    public static DebugDelegate _debug;
    
    public static interface DebugDelegate {
        void write(byte[] msg, int size);
        void write(String msg);
        void writeln(byte[] msg, int size);
        void writeln(String msg);
        void die(int code);
    }
    
    static {
        /* Set up delegates. */
        _debug = attemptDelegate("joeq.Runtime.DebugImpl");
        if (_debug == null) {
            _debug = new DefaultDebugDelegate();
        }
    }

    private static DebugDelegate attemptDelegate(String s) {
        String type = "debug delegate";
        try {
            Class c = Class.forName(s);
            return (DebugDelegate) c.newInstance();
        } catch (ClassNotFoundException x) {
            //System.err.println("Cannot find "+type+" "+s+": "+x);
        } catch (InstantiationException x) {
            System.err.println("Cannot instantiate "+type+" "+s+": "+x);
        } catch (IllegalAccessException x) {
            System.err.println("Cannot access "+type+" "+s+": "+x);
        }
        return null;
    }
    
    public static class DefaultDebugDelegate implements DebugDelegate {

        public void write(byte[] msg, int size) {
            for (int i=0; i<size; ++i)
                System.err.print((char) msg[i]);
        }

        public void write(String msg) {
            System.err.print(msg);
        }

        public void writeln(byte[] msg, int size) {
            write(msg, size);
            System.err.println();
        }

        public void writeln(String msg) {
            System.err.println(msg);
        }

        public void die(int code) {
            new InternalError().printStackTrace();
            System.exit(code);
        }

    }
}
