/*
 * Copyright Ari Rabkin (asrabkin@gmail.com)
 * See the COPYING file for copyright license information. 
 */
package edu.berkeley.confspell;

import java.io.*;
import java.util.*;
import edu.berkeley.confspell.SpellcheckConf.Slurper;

/**
 * Reads a Java Properties file into an OptionSet
 */
class PSlurper implements Slurper {
	public void slurp(File f, OptionSet res) throws IOException {
		Properties p = new Properties();
		FileInputStream fis = new FileInputStream(f);
		p.load(fis);
		fis.close();

		for (Map.Entry e : p.entrySet()) {
			String v = e.getValue().toString();
			// no need to remove comments, Java does it for us.
			// if(v.contains("#"))
			// v = v.substring(0, v.indexOf("#"))
			res.put(e.getKey().toString(), v);
			res.checkForSubst(v);
		}
		// "PROP-" +
	}

}