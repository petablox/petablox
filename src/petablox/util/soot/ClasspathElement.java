package petablox.util.soot;

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
import jwutil.collections.AppendIterator;
import jwutil.collections.Filter;
import jwutil.collections.FilterIterator;
import jwutil.collections.UnmodifiableIterator;
import jwutil.util.Assert;
import java.io.Serializable;

public abstract class ClasspathElement {
    public static boolean TRACE = false;
	/** Open a stream to read the given resource, or return
	 *  <code>null</code> if resource cannot be found. */
	public abstract InputStream getResourceAsStream(String resourcename);
	public abstract boolean containsResource(String name);
	/** Iterate over all classes in the given package. */
	public Iterator listPackage(String packagename) {
		return listPackage(packagename, false);
	}
	public abstract Iterator listPackage(String packagename, boolean recursive);
	public abstract Iterator listPackages();
	public abstract Set<String> getEntries();
}
