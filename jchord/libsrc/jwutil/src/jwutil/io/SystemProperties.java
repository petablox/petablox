//SystemProperties.java, created Sun Dec  7 14:20:28 PST 2003
//Copyright (C) 2004 Godmar Back <gback@cs.utah.edu, @stanford.edu>
//Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;

/**
 * Read system properties from a file.
 * 
 * @version $Id: SystemProperties.java,v 1.4 2005/10/10 07:02:00 joewhaley Exp $
 * @author gback
 * @author John Whaley
 */
public class SystemProperties {
    
    /**
     * Return the value of a system property if we have access, null otherwise.
     * 
     * @param key  system property to get
     * @return  value or null
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }
    
    /**
     * Return the value of a system property if we have access and it is defined,
     * def otherwise.
     * 
     * @param key  system property to get
     * @return  value of system property or def
     */
    public static String getProperty(String key, String def) {
        try {
            return System.getProperty(key, def);
        } catch (AccessControlException _) {
            return def;
        }
    }
    
    /**
     * Like getProperty, but also first checks in the current directory for a
     * file with a name the same as the key.  If it exists, it returns the contents
     * of that file, rather than the value of the system property.  This is useful
     * when you would like to be able to easily dynamically update system properties
     * as a program runs.
     * 
     * @param key  system property to get
     * @return  value or null
     */
    public static String getPropertyFromFile(String key) {
        return getPropertyFromFile(key, null);
    }
    
    /**
     * Like getProperty, but also first checks in the current directory for a
     * file with a name the same as the key.  If it exists, it returns the contents
     * of that file, rather than the value of the system property.  This is useful
     * when you would like to be able to easily dynamically update system properties
     * as a program runs.
     * 
     * @param key  system property to get
     * @return  value of system property or def
     */
    public static String getPropertyFromFile(String key, String def) {
        try {
            File f = new File(key);
            if (f.exists()) {
                // readLine() returns null on empty file.
                String s = new BufferedReader(new FileReader(f), 64).readLine();
                return s == null ? "" : s;
            }
        } catch (IOException _) {
            ; // silent
        }
        try {
            return System.getProperty(key);
        } catch (AccessControlException _) {
            return def;
        }
    }
    
    /**
     * Read the system properties from the given file.
     */
    public static void read(String filename) {
        FileInputStream propFile = null;
        try {
            propFile = new FileInputStream(filename);
            Properties p = new Properties(System.getProperties());
            p.load(propFile);
            System.setProperties(p);
        } catch (IOException ie) {
            ; // silent
        } catch (AccessControlException _) {
            ; // silent
        } finally {
            if (propFile != null) try {
                propFile.close();
            } catch (IOException x) {
            }
        }
    }
    
    protected Map flags = new HashMap();

    public void registerFlag(String flagName, Field f) {
        flags.put(flagName, f);
    }

    public void registerFlag(String flagName, Class c, String fieldName) {
        try {
            Field f = c.getDeclaredField(fieldName);
            flags.put(flagName, f);
        } catch (SecurityException e) {
            System.err.println("Error, " + c.getName() + "." + fieldName
                + " is private.");
        } catch (NoSuchFieldException e) {
            System.err.println("Error, " + c.getName() + "." + fieldName
                + " not found.");
        }
    }

    public void registerFlag(String flagName, String className, String fieldName) {
        try {
            Class c = Class.forName(className);
            registerFlag(flagName, c, fieldName);
        } catch (ClassNotFoundException e) {
            System.err.println("Error, " + className + " not found.");
        }
    }

    public void setDefaultFlags() {
        setDefaultFlags(null);
    }

    public void setDefaultFlags(Object base) {
        for (Iterator i = flags.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String flagName = (String) e.getKey();
            Field field = (Field) e.getValue();
            String value = System.getProperty(flagName);
            if (value != null) setField(base, field, value);
        }
    }

    public static void setField(Object base, Field f, String v) {
        if (base != null && Modifier.isStatic(f.getModifiers()) || base == null
            && !Modifier.isStatic(f.getModifiers())) {
            return;
        }
        f.setAccessible(true);
        Class c = f.getType();
        try {
            if (c == int.class) {
                int val = Integer.parseInt(v);
                f.setInt(base, val);
            } else if (c == boolean.class) {
                boolean val = v.equals("") || v.equals("yes")
                    || v.equals("true");
                f.setBoolean(base, val);
            } else if (c == String.class) {
                f.set(base, v);
            } else if (c == float.class) {
                float val = Float.parseFloat(v);
                f.setFloat(base, val);
            } else if (c == long.class) {
                long val = Long.parseLong(v);
                f.setLong(base, val);
            } else if (c == double.class) {
                double val = Double.parseDouble(v);
                f.setDouble(base, val);
            } else if (c == byte.class) {
                byte val = Byte.parseByte(v);
                f.setByte(base, val);
            } else if (c == char.class) {
                char val = v.charAt(0);
                f.setChar(base, val);
            } else if (c == short.class) {
                short val = Short.parseShort(v);
                f.setShort(base, val);
            } else if (Collection.class.isAssignableFrom(c)) {
                Collection col = (Collection) f.get(base);
                col.add(v);
            } else if (Map.class.isAssignableFrom(c)) {
                Map map = (Map) f.get(base);
                int split = v.indexOf(',');
                if (split == -1) split = v.indexOf(':');
                if (split == -1) split = v.indexOf('=');
                if (split == -1) {
                    System.err.println("Cannot parse " + f + " map entry \""
                        + v + "\"");
                } else {
                    String key = v.substring(0, split);
                    String val = v.substring(split + 1);
                    map.put(key, val);
                }
            } else {
                System.err.println("Unknown type for " + f + ": " + c);
            }
        } catch (IllegalAccessException _) {
            System.err.println("Cannot access " + f + ".");
        }
    }
}
