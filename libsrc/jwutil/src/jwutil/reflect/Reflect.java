// Reflect.java, created Oct 7, 2004 11:34:29 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;

/**
 * Convenience methods for Java reflection.
 * 
 * @author jwhaley
 * @version $Id: Reflect.java,v 1.3 2005/05/28 10:23:16 joewhaley Exp $
 */
public abstract class Reflect {
    
    /**
     * Get a method by class name and method name.
     * 
     * @param className  class name
     * @param methodName  method name
     * @return  method object, or null if it does not exist
     */
    public static Method getDeclaredMethod(String className, String methodName) {
        try {
            return getDeclaredMethod(Class.forName(className), methodName, null);
        } catch (ClassNotFoundException e0) {
            System.err.println("Cannot load "+className);
            e0.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get a method by name.
     * 
     * @param c  class object
     * @param methodName  method name
     * @return  method object, or null if it does not exist
     */
    public static Method getDeclaredMethod(Class c, String methodName) {
        return getDeclaredMethod(c, methodName, null);
    }
    
    /**
     * Get a method by name.
     * 
     * @param c  class object
     * @param methodName  method name
     * @param argTypes  argument types
     * @return  method object, or null if it does not exist
     */
    public static Method getDeclaredMethod(Class c, String methodName, Class[] argTypes) {
        Method m;
        try {
            if (argTypes != null) {
                m = c.getMethod(methodName, argTypes);
            } else {
                m = null;
                Method[] ms = c.getDeclaredMethods();
                for (int i = 0; i < ms.length; ++i) {
                    if (ms[i].getName().equals(methodName)) {
                        m = ms[i];
                        break;
                    }
                }
                if (m == null) {
                    System.err.println("Can't find "+c.getName()+"."+methodName);
                    return null;
                }
            }
            try {
                m.setAccessible(true);
            } catch (AccessControlException _) { }
        } catch (SecurityException e1) {
            System.err.println("Cannot access "+c.getName()+"."+methodName);
            e1.printStackTrace();
            return null;
        } catch (NoSuchMethodException e1) {
            System.err.println("Can't find "+c.getName()+"."+methodName);
            e1.printStackTrace();
            return null;
        }
        return m;
    }

    /**
     * Invoke a method by class name and method name.
     * 
     * @param className  class name
     * @param methodName  method name
     * @param argTypes  argument types of method
     * @param args  arguments
     * @return  return value
     */
    public static Object invoke(String className, String methodName, Class[] argTypes, Object[] args) {
        return invoke(Reflect.class.getClassLoader(), className, methodName, argTypes, args);
    }
    
    /**
     * Invoke a method by class name and method name.
     * 
     * @param className  class name
     * @param methodName  method name
     * @param args  arguments
     * @return  return value
     */
    public static Object invoke(String className, String methodName, Object[] args) {
        return invoke(Reflect.class.getClassLoader(), className, methodName, args);
    }
    
    /**
     * Invoke a method by class name and method name under the given class loader.
     * 
     * @param cl  class loader
     * @param className  class name
     * @param methodName  method name
     * @param args  arguments
     * @return  return value
     */
    public static Object invoke(ClassLoader cl, String className, String methodName, Object[] args) {
        return invoke(cl, className, methodName, null, args);
    }
    
    /**
     * Helper function for reflective invocation.
     * 
     * @param cl  class loader
     * @param className  class name
     * @param methodName  method name
     * @param argTypes  arg types (optional)
     * @param args  arguments
     * @return  return value from invoked method
     */
    public static Object invoke(ClassLoader cl, String className,
        String methodName, Class[] argTypes, Object[] args) {
        Class c;
        try {
            c = Class.forName(className, true, cl);
        } catch (ClassNotFoundException e0) {
            System.err.println("Cannot load "+className);
            e0.printStackTrace();
            return null;
        }
        Method m = getDeclaredMethod(c, methodName, argTypes);
        Object result;
        try {
            result = m.invoke(null, args);
        } catch (IllegalArgumentException e2) {
            System.err.println("Illegal argument exception");
            e2.printStackTrace();
            return null;
        } catch (IllegalAccessException e2) {
            System.err.println("Illegal access exception");
            e2.printStackTrace();
            return null;
        } catch (InvocationTargetException e2) {
            if (e2.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e2.getTargetException();
            if (e2.getTargetException() instanceof Error)
                throw (Error) e2.getTargetException();
            System.err.println("Unexpected exception thrown!");
            e2.getTargetException().printStackTrace();
            return null;
        }
        return result;
    }
    
    /**
     * Set a boolean field by class name and field name.
     * 
     * @param classname  class name
     * @param fieldname  field name
     * @param value  value to set
     */
    public static void setBooleanField(String classname, String fieldname, boolean value) {
        try {
            Class c = Class.forName(classname);
            Field f = c.getField(fieldname);
            f.setBoolean(null, value);
        } catch (Exception e) {
            System.err.println("Cannot set the flag "+classname+"."+fieldname);
        }
    }
    
}
