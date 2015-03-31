// PrimordialClassLoader.java, created Mon Feb  5 23:23:20 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Class;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.zip.ZipFile;
import joeq.ClassLib.ClassLibInterface;
import joeq.Main.jq;
import joeq.UTF.Utf8;
import jwutil.collections.AppendIterator;
import jwutil.collections.UnmodifiableIterator;
import jwutil.util.Assert;
import java.io.Serializable;

public class Classpath {
    public static boolean TRACE = false;
    // These should be static so that we don't need to look them up during class loading.
    public static final String pathsep = System.getProperty("path.separator");
    public static final String filesep = System.getProperty("file.separator");

    private final List<ClasspathElement> classpathList = new ArrayList<ClasspathElement>();
    private Set duplicates = new HashSet(); // don't add duplicates.

	public List<ClasspathElement> getClasspathElements() {
		return classpathList;
	}

	public void addFullClasspath() {
		addToClasspath(System.getProperty("sun.boot.class.path"));
		addExtClasspath();
		addToClasspath(System.getProperty("java.class.path"));
	}

	public void addExtClasspath() {
        String javaHomeDir = System.getProperty("java.home");
        assert (javaHomeDir != null);
        File libExtDir = new File(javaHomeDir, File.separator + "lib" + File.separator + "ext");
        if (libExtDir.exists()) {
            final java.io.FilenameFilter filter = new java.io.FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (name.endsWith(".jar"))
                        return true;
                    return false;
                }
            };
            File[] subFiles = libExtDir.listFiles(filter);
            for (File file : subFiles) {
                String fileName = file.getAbsolutePath();
                addToClasspath(fileName);
            }
        }
	}

    /** Vector of ClasspathElements corresponding to CLASSPATH entries. */
    public void addToClasspath(String s) {
        for (Iterator it = classpaths(s); it.hasNext(); ) {
            String path = (String) it.next();
			// skip duplicates
            if (duplicates.add(path)) {
				if (path.toLowerCase().endsWith(".zip") || path.toLowerCase().endsWith(".jar")) {
					try {
						if (TRACE) System.out.println("Adding zip file "+path+" to classpath");
						classpathList.add(new ZipFileElement(new ZipFile(path)));
					} catch (IOException ex) { /* skip this zip file, then. */ }
				} else {
					if (TRACE) System.out.println("Adding path "+path+" to classpath");
					classpathList.add(new PathElement(path));
				}
			}
		}
        ((ArrayList) classpathList).trimToSize(); // save memory.
    }

    /** Iterate over the components of the system CLASSPATH.
     *  Each element is a <code>String</code> naming one segment of the
     *  CLASSPATH. */
    public static final Iterator classpaths(String classpath) {
        // For convenience, make sure classpath begins with and ends with pathsep.
        if (!classpath.startsWith(pathsep)) classpath = pathsep + classpath;
        if (!classpath.endsWith(pathsep)) classpath = classpath + pathsep;
        final String cp = classpath;

        return new UnmodifiableIterator() {
            int i=0;
            public boolean hasNext() {
                return (cp.length() > (i+pathsep.length()));
            }
            public Object next() {
                i+=pathsep.length(); // cp begins with pathsep.
                String path = cp.substring(i, cp.indexOf(pathsep, i));
                i+=path.length(); // skip over path.
                return path;
            }
        };
    }

    public Iterator listPackage(final String pathname) {
        return listPackage(pathname, false);
    }

    public Iterator listPackage(final String pathname, boolean recursive) {
        Iterator result = null;
        for (ClasspathElement cpe : classpathList) {
            Iterator lp = cpe.listPackage(pathname, recursive);
            if (!lp.hasNext()) continue;
            result = result==null?lp:new AppendIterator(lp, result);
        }
        if (result == null) return Collections.EMPTY_SET.iterator();
        return result;
    }
    
    public Iterator listPackages() {
        Iterator result = null;
        for (Iterator it = classpathList.iterator(); it.hasNext(); ) {
            ClasspathElement cpe = (ClasspathElement)it.next();
            Iterator lp = cpe.listPackages();
            if (!lp.hasNext()) continue;
            result = result==null?lp:new AppendIterator(lp, result);
        }
        if (result == null) return Collections.EMPTY_SET.iterator();
        return result;
    }

    public String classpathToString() {
        StringBuffer result = new StringBuffer(pathsep);
        for (ClasspathElement cpe : classpathList) {
            result.append(cpe.toString());
            result.append(pathsep);
        }
        return result.toString();
    }
    
    public static String descriptorToResource(String desc) {
        Assert._assert(desc.charAt(0)==jq_ClassFileConstants.TC_CLASS);
        Assert._assert(desc.charAt(desc.length()-1)==jq_ClassFileConstants.TC_CLASSEND);
        Assert._assert(desc.indexOf('.')==-1); // should have '/' separators.
        return desc.substring(1, desc.length()-1) + ".class";
    }
    
    /** Translate a class name into a corresponding resource name.
     * @param classname The class name to translate.
     */
    public static String classnameToResource(String classname) {
        Assert._assert(classname.indexOf('/')==-1); // should have '.' separators.
        // Swap all '.' for '/' & append ".class"
        return classname.replace('.', '/') + ".class";
    }

    public String getResourcePath(String name) {
        for (ClasspathElement cpe : classpathList) {
            if (cpe.containsResource(name))
                return cpe.toString();
        }
        // Couldn't find resource.
        return null;
    }

    public String getPackagePath(String name) {
        for (ClasspathElement cpe : classpathList) {
            for (Iterator it2 = cpe.listPackages(); it2.hasNext(); ) {
                if (name.equals(it2.next()))
                    return cpe.toString();
            }
        }
        // Couldn't find resource.
        return null;
    }

    /** Open an <code>InputStream</code> on a resource found somewhere
     *  in the CLASSPATH.
     * @param name The filename of the resource to locate.
     */
    public InputStream getResourceAsStream(String name) {
        for (ClasspathElement cpe : classpathList) {
            InputStream is = cpe.getResourceAsStream(name);
            if (is != null) {
                return is; // return stream if found.
            }
        }
        // Couldn't find resource.
        return null;
    }
    
    public DataInputStream getClassFileStream(Utf8 descriptor) throws IOException {
        String resourceName = descriptorToResource(descriptor.toString());
        InputStream is = getResourceAsStream(resourceName);
        if (is == null) return null;
        return new DataInputStream(is);
    }
}
