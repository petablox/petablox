// HijackingClassLoader.java, created Jun 24, 2004 5:39:24 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.classloader;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedExceptionAction;
import sun.misc.Resource;
import sun.misc.URLClassPath;

/**
 * A special classloader that allows you to hijack all of the classes and load
 * them from your list of URLs. Plus, you can dynamically change the classpath
 * at run time!
 * 
 * @author jwhaley
 * @version $Id: HijackingClassLoader.java,v 1.2 2004/10/03 10:56:05 joewhaley Exp $
 */
public class HijackingClassLoader extends URLClassLoader {
    
    public static HijackingClassLoader makeClassLoader() {
        String cp = System.getProperty("java.class.path");
        URL[] u = getURLs(cp);
        return new HijackingClassLoader(u);
    }
    
    public static URL[] getURLs(String cp) {
        StringTokenizer st = new StringTokenizer(cp, System.getProperty("path.separator"));
        List urls = new LinkedList();
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            try {
                URL url = getFileURL(s);
                if (url != null) urls.add(url);
            } catch (IOException x) {
                // URL cannot be loaded, skip it.
            }
        }
        URL[] u = (URL[]) urls.toArray(new URL[urls.size()]);
        return u;
    }
    
    public static URL getFileURL(String name) throws IOException {
        File f = new File(name);
        if (f.exists()) return f.toURL();
        else return null;
    }
    
    public static final boolean TRACE = false;
    
    /* The search path for classes and resources */
    private URLClassPath ucp;
    /* The context to be used when loading classes and resources */
    private AccessControlContext acc;
    /* Whether we should skip hijacking java.lang stuff. */
    boolean skipJavaLang = true;
    
    /**
     * @param urls
     * @param parent
     */
    public HijackingClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        ucp = new URLClassPath(urls);
        acc = AccessController.getContext();
    }

    /**
     * @param urls
     */
    public HijackingClassLoader(URL[] urls) {
        super(urls);
        ucp = new URLClassPath(urls);
        acc = AccessController.getContext();
    }

    /**
     * @param urls
     * @param parent
     * @param factory
     */
    public HijackingClassLoader(URL[] urls, ClassLoader parent,
        URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        ucp = new URLClassPath(urls, factory);
        acc = AccessController.getContext();
    }

    /* (non-Javadoc)
     * @see java.net.URLClassLoader#addURL(java.net.URL)
     */
    public void addURL(URL url) {
        ucp.addURL(url);
        super.addURL(url);
    }

    /**
     * Add an array of URLs to the class path.
     * 
     * @param url  array of URLs
     */
    public void addURLs(URL[] url) {
        for (int i = 0; i < url.length; ++i) {
            ucp.addURL(url[i]);
            super.addURL(url[i]);
        }
    }
    
    /*
     * Defines a Class using the class bytes obtained from the specified
     * Resource. The resulting Class must be resolved before it can be used.
     */
    private Class defineClass(String name, Resource res) throws IOException {
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Package pkg = getPackage(pkgname);
            Manifest man = res.getManifest();
            if (pkg != null) {
                // Package found, so check package sealing.
                if (pkg.isSealed()) {
                    // Verify that code source URL is the same.
                    if (!pkg.isSealed(url)) {
                        throw new SecurityException(
                            "sealing violation: package " + pkgname
                                + " is sealed");
                    }
                } else {
                    // Make sure we are not attempting to seal the package
                    // at this code source URL.
                    if ((man != null) && isSealed(pkgname, man)) {
                        throw new SecurityException(
                            "sealing violation: can't seal package " + pkgname
                                + ": already loaded");
                    }
                }
            } else {
                if (man != null) {
                    definePackage(pkgname, man, url);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null,
                        null);
                }
            }
        }
        // Now read the class bytes and define the class
        byte[] b = res.getBytes();
        java.security.cert.Certificate[] certs = res.getCertificates();
        CodeSource cs = new CodeSource(url, certs);
        return defineClass(name, b, 0, b.length, cs);
    }

    /*
     * Returns true if the specified package name is sealed according to the
     * given manifest.
     */
    private boolean isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /* (non-Javadoc)
     * Overridden so that we can add trace info.
     * 
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    protected Class findClass(final String name) throws ClassNotFoundException {
        try {
            return (Class) AccessController.doPrivileged(
                new PrivilegedExceptionAction() {
                    public Object run() throws ClassNotFoundException {
                        String path = name.replace('.', '/').concat(".class");
                        Resource res = ucp.getResource(path, false);
                        if (res != null) {
                            if (TRACE) System.out.println("Hijacked! " + res);
                            try {
                                return defineClass(name, res);
                            } catch (IOException e) {
                                throw new ClassNotFoundException(name, e);
                            }
                        } else {
                            throw new ClassNotFoundException(name);
                        }
                    }
                }, acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    public final synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        if (!skipJavaLang || !name.startsWith("java.lang.")) {
            // Check if we have it before we check the parent class loader.
            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                // We don't have it, load from the superclass instead.
            }
        }
        Class c = super.loadClass(name, resolve);
        return c;
    }
}
