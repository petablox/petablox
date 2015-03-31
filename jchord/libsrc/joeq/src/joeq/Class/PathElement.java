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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import joeq.ClassLib.ClassLibInterface;
import joeq.Main.jq;
import joeq.UTF.Utf8;
import jwutil.collections.AppendIterator;
import jwutil.collections.Filter;
import jwutil.collections.FilterIterator;
import jwutil.collections.UnmodifiableIterator;
import jwutil.util.Assert;
import java.io.Serializable;

/** A regular path string in the CLASSPATH. */
class PathElement extends ClasspathElement {
	private String path;
	private Set<String> entries;
	public PathElement(String path) {
		this.path = path;
	}

	private void initializeEntryMap() {
		entries = new HashSet<String>();
		buildEntries(null);
		if (TRACE) System.out.println(this+" contains: "+entries);
	}

	public String toString() { return path; }

	public Set<String> getEntries() {
		if (entries == null) initializeEntryMap();
		return entries;
	}

	public InputStream getResourceAsStream(String name) {
		if (TRACE) System.out.println("Getting resource for "+name+" in path "+path);
		if (entries == null) initializeEntryMap();
		if (name.charAt(0) == '/') name = name.substring(1);
		if (!entries.contains(name))
			return null;
		if (Classpath.filesep.charAt(0) != '/')
			name = name.replace('/', Classpath.filesep.charAt(0));
		try { // try to open the file, starting from path.
			File f = new File(path, name);
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			return null; // if anything goes wrong, return null.
		}
	}
		
	public boolean containsResource(String name) {
		if (TRACE) System.out.println("Searching for "+name+" in path "+path);
		if (entries == null) initializeEntryMap();
		return entries.contains(name);
	}
		
	public Iterator listPackage(final String pathn, final boolean recursive) {
		if (TRACE) System.out.println("Listing package "+pathn+" in path "+path);
		if (entries == null) initializeEntryMap();
		final String filesep   = "/";
		return new FilterIterator(entries.iterator(), new Filter() {
			public boolean isElement(Object o) {
				String name = (String) o;
				if (TRACE) System.out.println("Checking if file "+name+" is in package "+pathn);
				return name.startsWith(pathn) &&
				   name.endsWith(".class") &&
				   (recursive || name.lastIndexOf(filesep)==(pathn.length()-1));
			}
		});
	}

	public Iterator listPackages() {
		if (TRACE) System.out.println("Listing packages of path "+path);
		HashSet hs = new HashSet();
		listPackages(null, hs);
		return hs.iterator();
	}
		
	private void listPackages(final String dir, final HashSet pkgs) {
		final File f = dir == null ? new File(path) : new File(path, dir);
		if (!f.exists() || !f.isDirectory()) return;
		//pkgs.add(path);	// add the current directory first
		String [] subdirs = f.list(new java.io.FilenameFilter() {
			public boolean accept(File _dir, String name) {
				if (dir != null && name.endsWith(".class"))
					pkgs.add(dir);
				return new File(_dir, name).isDirectory();
			}
		});
		for (int i = 0; i < subdirs.length; i++) {
			String dn = (String)subdirs[i];
			if (dir != null)
				dn = dir + Classpath.filesep + dn;
			listPackages(dn, pkgs);
		}
	}
		
	private void buildEntries(final String pathn) {
		File f;
		if (pathn == null) {
			f = new File(path);
		} else if (Classpath.filesep.charAt(0) == '/') {
			f = new File(path, pathn);
		} else {
			f = new File(path, pathn.replace('/', Classpath.filesep.charAt(0)));
		}
		if (!f.exists() || !f.isDirectory()) return;
		String[] cls = f.list(new java.io.FilenameFilter() {
			public boolean accept(File _dir, String name) {
				return !new File(_dir, name).isDirectory();
			}
		});
		
		if (cls != null) {
			for (int i = 0; i < cls.length; ++i) {
				String s = (pathn==null)?(cls[i]):(pathn+cls[i]);
				entries.add(s);
			}
		}

		String [] subdirs = f.list(new java.io.FilenameFilter() {
			public boolean accept(File _dir, String name) {
				return new File(_dir, name).isDirectory();
			}
		});
		if (subdirs != null) {
			for (int i = 0; i < subdirs.length; i++) {
				String dn = (String)subdirs[i];
				if (pathn != null) dn = pathn + dn;
				buildEntries(dn + '/');
			}
		}
	}
}

