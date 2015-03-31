/*
 * Copyright Ari Rabkin (asrabkin@gmail.com)
 * See the COPYING file for copyright license information. 
 */
package edu.berkeley.confspell;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

/**
 * The dictionary for the configuration spellchecker. Each dictionary entry is a
 * regular expression matching some set of options, followed by an optional type
 * for the associated options and an optional annotation to further constrain
 * the valid values.
 * 
 * This class has methods for both reading and writing dictionary files.
 * 
 * 
 */
public class OptDictionary {

	static final String RAW_BANNER = "# This is a RAW dictionary file, not checked by hand for accuracy.\n"
			+ "# It was produced automatically by the configuration analyzer.\n"
			+ "# Contact Ari Rabkin <asrabkin@gmail.com> with comments or questions.\n"
			+ "# This file may be freely redistributed; no copyright is asserted.";

	public OptDictionary() {
	}

	public OptDictionary(File f) throws IOException {
		read(f);
	}
	
	public OptDictionary(InputStream is) throws IOException {
		read(is);
	}

	// use a tree map to get alphabetization for free
	TreeMap<String, String> dict = new TreeMap<String, String>();
	ArrayList<Pattern> regexOpts = new ArrayList<Pattern>();
	HashMap<String, String> annotations = new HashMap<String, String>();

	public void update(String opt, String ty) {
		String oldT = dict.get(opt);
		if (oldT == null)
			dict.put(opt, ty);
		else {
			if (ty == null || oldT.contains(ty))
				return;
			else
				dict.put(opt, oldT + " or " + ty);
		}
	}

	/**
	 * Add an annotation for the given option
	 * 
	 * @param optName
	 * @param annotation
	 */
	public void annotate(String optName, String annotation) {
		if (!annotation.contains("\n")) // line breaks here can break file format
			annotations.put(optName, annotation);

	}

	/**
	 * Write this dictionary to the specified PrintWriter
	 * 
	 * @param writer
	 */
	public void dump(PrintWriter writer, boolean RawHeader) {

		if (RawHeader) {
			writer.println(RAW_BANNER);
		}

		for (Map.Entry<String, String> e : dict.entrySet()) {
			String k = e.getKey();
			String v = e.getValue();
			if (v == null)
				v = "";
			String annot = annotations.get(k);
			if (annot != null)
				writer.println(k + "\t" + v + "\t" + annot);
			else
				writer.println(k + "\t" + v);
		}
	}

	/**
	 * Read content from the specified file. Can be called on an existing
	 * dictionary. New values will override older values.
	 * 
	 * @param dictionary
	 * @throws IOException
	 */
	public void read(File dictionary) throws IOException {

		FileInputStream fis = new FileInputStream(dictionary);
		read(fis);
		fis.close();
	}
	
	public void read(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String s = null;
		while ((s = br.readLine()) != null) {
			if (s.contains("#"))
				s = s.substring(0, s.indexOf("#")); // prune at #
			if (s.length() < 4) // skip blank lines
				continue;

			String[] parts = s.split("\t");
			String opt = pruneName(parts[0]);

			if (opt.contains(".*")) {
				regexOpts.add(Pattern.compile(opt));
			}

			if (parts.length == 1 || parts[1].length() < 1)
				dict.put(opt, null);
			else
				dict.put(opt, parts[1]);

			if (parts.length > 2) {
				annotations.put(opt, parts[2]);
			}
		}
	}

	private String pruneName(String s) {
		if (s.startsWith("CONF-") || s.startsWith("PROP-"))
			return s.substring(5);
		if (s.startsWith("$"))
			return s.substring(1);
		else if (s.startsWith("CXCONF-"))
			return s.substring(7);
		else
			return s;
	}

	/**
	 * Dumps the dictionary to standard out in a readable format. Intended
	 * primarily for debugging
	 */
	public void show() {
		for (Map.Entry<String, String> ent : dict.entrySet()) {
			String type = ent.getValue();
			String opt = ent.getKey();
			if (type != null)
				System.out.println("option " + opt + " has type " + type);
			else
				System.out.println("option " + opt + " has unknown type");
		}
	}

	/**
	 * Returns true if either s is in the dictionary exactly, or if a regular
	 * expression in the dictionary matches s.
	 * 
	 * @param s
	 *          the option name to look up
	 * @return true if a dictionary entry matches s, either exactly or as a
	 *         regular expression.
	 */
	public boolean contains(String s) {
		if (dict.containsKey(s))
			return true;
		else {
			for (Pattern regex : regexOpts) {
				if (regex.matcher(s).matches())
					return true;
			}
			return false;
		}
	}

	private String lookupPat(String k) {
		if (dict.containsKey(k))
			return k;

		for (Pattern regex : regexOpts) {
			if (regex.matcher(k).matches()) {
				return regex.pattern();
			}
		}
		return null;
	}

	/**
	 * Return the type associated with a given option name. Will first try an
	 * exact match, and then will try to find a regex that maches.
	 * 
	 * @param k
	 *          the name of the option to look up
	 * @return the type associated with this option name, or null, if no match.
	 */
	public String get(String k) {
		String s = dict.get(k);
		if (s == null) {
			String pat = lookupPat(k);
			if (pat == null) {
				System.err.println("DICT: lookup for absent entry " + k);
				return null;
			}
			return dict.get(pat);
		}
		return s;
	}

	/**
	 * Concatenates an option's type with annotation, if any.
	 */
	public String getFullname(String k) {
		String pat = lookupPat(k);
		if (pat == null)
			return null;
		else {
			String ty = dict.get(pat);
			if (ty == null)
				ty = "unknown";
			String annot = annotations.get(pat);
			if (annot == null)
				return ty;
			else
				return ty + " " + annot;
		}
	}

	public Set<String> names() {
		return dict.keySet();
	}

}
