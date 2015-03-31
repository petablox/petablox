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

/** A .zip or .jar file in the CLASSPATH. */
public class ZipFileElement extends ClasspathElement {
	private ZipFile zf;
	private Map<String, ZipEntry> entries;

	public ZipFileElement(ZipFile zf) {
		this.zf = zf;
	}

	private void initializeEntryMap() {
		int size = zf.size();
		entries = new HashMap<String, ZipEntry>(size + (size >> 1));
		if (size > 0) {
			for (Enumeration e = zf.entries(); e.hasMoreElements(); ) {
				ZipEntry ze = (ZipEntry) e.nextElement();
				entries.put(ze.getName(), ze);
			}
		}
		if (TRACE) System.out.println(this+" contains: "+entries.keySet());
	}

	public String toString() { return zf.getName(); }

	public Set<String> getEntries() {
		if (entries == null) initializeEntryMap();
		return entries.keySet();
	}

	public InputStream getResourceAsStream(String name) {
		if (TRACE) System.out.println("Getting resource for "+name+" in zip file "+zf.getName());
		if (entries == null) initializeEntryMap();
		if (name.charAt(0) == '/') name = name.substring(1);
		ZipEntry ze = entries.get(name);
		try { // look for name in zipfile, return null if something goes wrong.
			return (ze==null)?null:zf.getInputStream(ze);
		} catch (IOException e) { return null; }
	}

	public boolean containsResource(String name) {
		if (TRACE) System.out.println("Searching for "+name+" in zip file "+zf.getName());
		if (entries == null) initializeEntryMap();
		return entries.containsKey(name);
	}

	public Iterator listPackage(final String pathname, final boolean recursive) {
		if (TRACE) System.out.println("Listing package "+pathname+" of zip file "+zf.getName());
		// look for directory name first
		if (entries == null) initializeEntryMap();
		final String filesep   = "/";
		return new FilterIterator(entries.values().iterator(), new Filter() {
			public boolean isElement(Object o) {
				ZipEntry zze = (ZipEntry) o;
				String name = zze.getName();
				if (TRACE) System.out.println("Checking if zipentry "+name+" is in package "+pathname);
				return (!zze.isDirectory()) && name.startsWith(pathname) &&
					name.endsWith(".class") &&
					(recursive || name.lastIndexOf(filesep)==(pathname.length()-1));
			}
			public Object map(Object o) {
				return ((ZipEntry)o).getName();
			}
		});
	}

	public Iterator listPackages() {
		if (TRACE) System.out.println("Listing packages of zip file "+zf.getName());
		if (entries == null) initializeEntryMap();
		LinkedHashSet result = new LinkedHashSet();
		for (Iterator i=entries.values().iterator(); i.hasNext(); ) {
			ZipEntry zze = (ZipEntry) i.next();
			if (zze.isDirectory()) continue;
			String name = zze.getName();
			if (name.endsWith(".class")) {
				int index = name.lastIndexOf('/');
				result.add(name.substring(0, index+1));
			}
		}
		if (TRACE) System.out.println("Result: "+result);
		return result.iterator();
	}

	/** Close the zipfile when this object is garbage-collected. */
	protected void finalize() throws Throwable {
		// yes, it is possible to finalize an uninitialized object.
		try { if (zf!=null) zf.close(); } finally { super.finalize(); }
	}
}
