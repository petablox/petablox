/*
 * Copyright Ari Rabkin (asrabkin@gmail.com)
 * See the COPYING file for copyright license information. 
 */
package edu.berkeley.confspell;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.util.Map;
import edu.berkeley.confspell.SpellcheckConf.Slurper;

/**
 * Slurper for Hadoop configuration files. Has a run-time and compile-time
 * dependency on Hadoop common components.
 * 
 */
public class HSlurper implements Slurper {

	// stolen from hadoop source

	public void slurp(File f, OptionSet res) {
		Configuration c = new Configuration(false);
		c.addResource(new Path(f.getAbsolutePath())); // to search filesystem, not
																									// classpath
		c.reloadConfiguration();
		fromHConf(res, c);
	}

	public static void fromHConf(OptionSet res, Configuration c) {
		for (Map.Entry<String, String> e : c) {

			String rawV = e.getValue();
			String cookedV = c.get(e.getKey());

			res.put(e.getKey(), cookedV); // to force substitution

			res.checkForSubst(rawV);
		}
	}

	public static OptionSet fromHConf(Configuration c) {
		OptionSet res = new OptionSet();
		fromHConf(res, c);
		return res;
	}

}
