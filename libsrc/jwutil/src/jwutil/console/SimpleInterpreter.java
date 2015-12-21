// SimpleInterpreter.java, created Sun Feb  8 16:38:30 PST 2004 by gback
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu> and Godmar Back <gback@stanford.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.console;

/**
 * A simple Java interpreter.
 * Its purpose to allow simple invocations.
 * An interpreter maintains its own store, which can be provided for it
 * on initialization.
 * Every newly constructed object gets its own classloader.
 * This is tailored towards a sequence of tokens in a String[] array.
 *
 * This code is pure Java (completely independent of jq_Class.*)
 *
 * @author Godmar Back
 */
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

public class SimpleInterpreter {
    public static boolean verbose = false;
    URL         [] classpath;
    Map         store;
   
    public SimpleInterpreter(URL []path) {
        this(path, new HashMap());
    }

    public SimpleInterpreter(URL []path, Map store) {
        this.classpath = path;
        this.store = store;
    }

    public Map getStore() {
        return store;
    }

    public void setClassPath(URL []path) {
        this.classpath = path;
    }

    /**
     * Create a new object.
     * args must match args of first constructor returned by getConstructors()
     * object is stored in store with 'name'
     */
    public int newObject(String name, String classname, String []s_args, int pos) {
        try {
            ClassLoader loader = new URLClassLoader(classpath);
            if (verbose)
                System.out.println("loading class " + classname + " from " + Arrays.asList(classpath));
            Class clazz = loader.loadClass(classname);
            Constructor c = clazz.getConstructors()[0];
            Class []argtypes = c.getParameterTypes();
            int nargs = argtypes.length;
            Object []args = new Object[nargs];
            pos = parseMethodArgs(args, argtypes, 0, s_args, pos, store);
            Object o = c.newInstance(args);
            store.put(name, o);
        } catch (InstantiationException e) {
            e.printStackTrace(System.err);
        } catch (IllegalAccessException e) {
            e.printStackTrace(System.err);
        } catch (IllegalArgumentException e) {
            e.printStackTrace(System.err);
        } catch (InvocationTargetException e) {
            e.printStackTrace(System.err);
        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.err);
        }
        return pos;
    }

    /**
     * Invoke method, store return value as "$last" in store.
     */
    public int invokeMethod(String objectname, String methodname, String []s_args, int pos) {
        Object o = store.get(objectname);
        if (o == null) {
            System.err.println("no object with name " + objectname);
            return pos;
        }
        return invokeMethod(o, methodname, s_args, pos);
    }

    public int invokeMethod(final Object object, String methodname, String []s_args, int pos) {
        Class clazz = object.getClass();
        Method m = null;
    outer:
        while (clazz != null) {
            Method [] meths = clazz.getDeclaredMethods();
            for (int i = 0; i < meths.length; i++) {
                if (meths[i].getName().equals(methodname)) {
                    m = meths[i]; 
                    break outer;
                }
            }
            clazz = clazz.getSuperclass();
        }
        if (m == null) {
            System.err.println("Method " + methodname + " not found in class " + object.getClass());
            return pos;
        }
        m.setAccessible(true);

        try {
            Class []argtypes = m.getParameterTypes();
            int nargs = argtypes.length;
            final Object []args = new Object[nargs];
            pos = parseMethodArgs(args, argtypes, 0, s_args, pos, store);
            final Method mth = m;
            Object o = mth.invoke(object, args);
            store.put("$last", o);
        } catch (IllegalAccessException e) {
            e.printStackTrace(System.err);
        } catch (IllegalArgumentException e) {
            e.printStackTrace(System.err);
        } catch (InvocationTargetException e) {
            e.printStackTrace(System.err);
        }
        return pos;
    }

    public static int parseArg(Object[] args, int m, Class type, String[] s_args, int j, Map store) {
        if (type == String.class)
            args[m] = s_args[++j];
        else if (type == Boolean.TYPE)
            args[m] = Boolean.valueOf(s_args[++j]);
        else if (type == Byte.TYPE)
            args[m] = Byte.valueOf(s_args[++j]);
        else if (type == Short.TYPE)
            args[m] = Short.valueOf(s_args[++j]);
        else if (type == Character.TYPE)
            args[m] = new Character(s_args[++j].charAt(0));
        else if (type == Integer.TYPE)
            args[m] = Integer.valueOf(s_args[++j]);
        else if (type == Long.TYPE) {
            args[m] = Long.valueOf(s_args[++j]);
        } else if (type == Float.TYPE)
            args[m] = Float.valueOf(s_args[++j]);
        else if (type == Double.TYPE) {
            args[m] = Double.valueOf(s_args[++j]);
        } else if (type.isArray()) {
            if (!s_args[++j].equals("{"))
                throw new Error("array parameter doesn't start with {");
            int count = 0;
            while (!s_args[++j].equals("}")) ++count;
            Class elementType = type.getComponentType();
            if (elementType == String.class) {
                String[] array = new String[count];
                for (int k = 0; k < count; ++k)
                    array[k] = s_args[j - count + k];
                args[m] = array;
            } else if (elementType == Boolean.TYPE) {
                boolean[] array = new boolean[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Boolean.valueOf(s_args[j - count + k]).booleanValue();
                args[m] = array;
            } else if (elementType == Byte.TYPE) {
                byte[] array = new byte[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Byte.parseByte(s_args[j - count + k]);
                args[m] = array;
            } else if (elementType == Short.TYPE) {
                short[] array = new short[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Short.parseShort(s_args[j - count + k]);
                args[m] = array;
            } else if (elementType == Character.TYPE) {
                char[] array = new char[count];
                for (int k = 0; k < count; ++k)
                    array[k] = s_args[j - count + k].charAt(0);
                args[m] = array;
            } else if (elementType == Integer.TYPE) {
                int[] array = new int[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Integer.parseInt(s_args[j - count + k]);
                args[m] = array;
            } else if (elementType == Long.TYPE) {
                long[] array = new long[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Long.parseLong(s_args[j - count + k]);
                args[m] = array;
            } else if (elementType == Float.TYPE) {
                float[] array = new float[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Float.parseFloat(s_args[j - count + k]);
                args[m] = array;
            } else if (elementType == Double.TYPE) {
                double[] array = new double[count];
                for (int k = 0; k < count; ++k)
                    array[k] = Double.parseDouble(s_args[j - count + k]);
                args[m] = array;
            } else
                throw new Error("Parsing of type " + type + " is not implemented");
        } else if (store != null) {
            args[m] = store.get(s_args[++j]);
        } else
            throw new Error("Parsing of type " + type + " is not implemented");
        return j;
    }

    public static int parseMethodArgs(Object[] args, Class[] paramTypes, int paramOffset, String[] s_args, int j, Map store) {
        try {
            for (int i = paramOffset, m = 0; i < paramTypes.length; ++i, ++m) {
                j = parseArg(args, m, paramTypes[i], s_args, j, store);
            }
        } catch (ArrayIndexOutOfBoundsException x) {
            System.err.println("not enough method arguments");
            x.printStackTrace(System.err);
            throw x;
        }
        return j;
    }

    public static void main(String []av) throws Exception {
        SimpleInterpreter si = new SimpleInterpreter(new URL[] { new File(av[0]).toURL() });
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        for (;;) {
            System.out.print("> ");
            System.out.flush();
            String s = r.readLine();
            if (s == null)
                break;
            StringTokenizer st = new StringTokenizer(s);
            ArrayList a = new ArrayList();
            while (st.hasMoreTokens())
                a.add(st.nextToken());
            String []args = new String[a.size()];
            a.toArray(args);

            int pos = 0;
            while (pos < a.size()) {
                String cmd = args[pos];
                if (cmd.equals("store")) {
                    System.out.println(si.getStore());
                } else
                if (cmd.equals("new")) {
                    String name = args[++pos];
                    String type = args[++pos];
                    pos = si.newObject(name, type, args, pos);
                } else
                if (cmd.indexOf(".") != -1) {
                    int b = cmd.lastIndexOf(".");
                    String object = cmd.substring(0, b);
                    String mname = cmd.substring(b+1);
                    pos = si.invokeMethod(object, mname, args, pos);
                    System.out.println(si.getStore().get("$last"));
                } else {
                    System.out.println("bad command: " + cmd);
                    break;
                }
                pos++;
            }
        }
    }

    /**
     * Clone an object fields.  Not used currently.
     */
    public static Object clone(Object srcobj, Class dstclass) {
        Class srcclass = srcobj.getClass();
        try {
            Object dstobj = dstclass.newInstance();
            Field [] srcfields = srcclass.getDeclaredFields();
            for (int i = 0; i < srcfields.length; i++) {
                Field srcfield = srcfields[i];
                srcfield.setAccessible(true);
                Field dstfield = dstclass.getDeclaredField(srcfield.getName());
                dstfield.setAccessible(true);
                dstfield.set(dstobj, srcfield.get(srcobj));
            }
            return dstobj;
        } catch (NoSuchFieldException e) {
            e.printStackTrace(System.err);
        } catch (IllegalAccessException e) {
            e.printStackTrace(System.err);
        } catch (InstantiationException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
}
