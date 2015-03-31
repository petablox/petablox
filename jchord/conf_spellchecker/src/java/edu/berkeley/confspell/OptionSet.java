/*
 * Copyright Ari Rabkin (asrabkin@gmail.com)
 * See the COPYING file for copyright license information. 
 */
package edu.berkeley.confspell;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model for a set of options with associated auxiliary data. Stores a set of
 * key value pairs as well as a list of options that are used indirectly by
 * substitution.
 * 
 */
public class OptionSet { // extends TreeMap<String,String>

	private static Pattern varPat = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");

	private static final long serialVersionUID = 1L;
	HashSet<String> usedBySubst = new HashSet<String>();
	Map<String, String> conf = new TreeMap<String, String>();
	private boolean substitute = false;

	public OptionSet() {
	}

	/**
	 * Constructs using values from a given Properties object
	 */
	public OptionSet(Properties p) {
		addAll(p, "");
	}

	/**
	 * Constructs using values from a given Properties object, provided the option
	 * names start with specified prefix
	 */
	public OptionSet(Properties p, String prefix) {
		addAll(p, prefix);
	}

	/**
	 * Add all values from a given Properties object.
	 */
	public void addAll(Properties p) {
		addAll(p, "");
	}

	/**
	 * Add entries from properties bundle starting with prefix
	 * 
	 * @param p
	 */
	public void addAll(Properties p, String prefix) {
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String key = e.getKey().toString();
			if (key.startsWith(prefix))
				conf.put(key, e.getValue().toString());
		}
	}

	/**
	 * True if k is a value used indirectly, by substitution, in this properties
	 * bundle
	 */
	public boolean usedBySubst(String k) {
		return usedBySubst.contains(k);
	}

	public Set<Map.Entry<String, String>> entrySet() {
		if (substitute) {
			Map<String, String> res = new LinkedHashMap<String, String>(); // make
																																			// copy to
																																			// substitute
			for (Map.Entry<String, String> ent : conf.entrySet()) {
				String val = ent.getValue();

				Matcher m = varPat.matcher(val);

				if (m.find()) {
					String repl = m.group();
					String innerOpt = repl.substring(2, repl.length() - 1);
					if (conf.containsKey(innerOpt)) {
						val = val.replace(repl, conf.get(innerOpt));
						m = varPat.matcher(val);
					}
				}
				res.put(ent.getKey(), val);
			}
			return res.entrySet();
		} else
			return conf.entrySet();
	}

	public void put(String key, String string) {
		conf.put(key, string);
	}

	public void addSubstUse(String s) {
		usedBySubst.add(s);
	}

	public boolean contains(String s) {
		return conf.containsKey(s);
	}

	/**
	 * Construct an OptionSet from a given properties file.
	 */
	public static OptionSet fromPropsFile(String propsfilename)
			throws java.io.IOException {
		return fromPropsFile(new File(propsfilename));
	}

	/**
	 * Construct an OptionSet from a given properties file.
	 */
	public static OptionSet fromPropsFile(File propsfilename)
			throws java.io.IOException {

		OptionSet result = new OptionSet();
		result.enableSubstitution();
		PSlurper ps = new PSlurper();
		ps.slurp(propsfilename, result);
		return result;
	}

	public void enableSubstitution() {
		substitute = true;
		if(conf.size() > 0 && usedBySubst.size() == 0) {
			for (Map.Entry<String,String> e : conf.entrySet()) {
				String v = e.getValue();
				checkForSubst(v);
			}
		}
	}

	public void checkForSubst(String rawV) {
		Matcher m = varPat.matcher(rawV);
		if (m.find()) {
			String var = m.group();
			var = var.substring(2, var.length() - 1); // remove ${ .. }
			addSubstUse(var);
		}
	}

	public int size() {
		return conf.size();
	}

}
