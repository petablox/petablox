package petablox.analyses.cg;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import petablox.util.Utils;
import petablox.project.OutDirUtils;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;

/*
 * This analysis unions more than one call-graphs.
 *
 * petablox.union.apps[default ""]  string giving a comma-separated list of apps whose cg has to be unioned.
 * petablox.union.dirprefix [default ""] string that gives the common prefix that is prefixed to every app name.
 * petablox.union.dirsuffix [default ""] string that gives the common suffix that is prefixed to every app name.
 */
@Petablox(name="union-java")
public class Union extends JavaAnalysis {
    HashMap<String, Integer> name2GNdx = new HashMap<String, Integer>();
    ArrayList<String> gNdx2Str = new ArrayList<String>();
    HashMap<Integer, Set<Integer>> unionCicg = new HashMap<Integer, Set<Integer>>();
    String UNION_FILE = "union.txt";
    String GNAME_FILE = "gname.txt";
    String CICG_FILE = "cicg.txt";
    String NAME_FILE = "name.txt";
    
	public void run() {
		String apps = System.getProperty("petablox.union.apps");
		String dirPrefix = System.getProperty("petablox.union.dirprefix");
		String dirSuffix = System.getProperty("petablox.union.dirsuffix");
		
		int currGNdx = 0;
		String parts[] = apps.split(",");
		for (int i = 0; i < parts.length; i++) {
			String app = parts[0];
			String currDir = dirPrefix + app + dirSuffix;
			String cicgFName = currDir + "/" + CICG_FILE;
			String nameFName = currDir + "/" + NAME_FILE;
			
			ArrayList<String> lNdx2Name = new ArrayList<String>();
			
			File nameFile = new File(nameFName);
			if (nameFile.exists()) {
				List<String> l = Utils.readFileToList(nameFile);
	            for (String s : l) lNdx2Name.add(s.trim());
			}
			
			File cicgFile = new File(cicgFName);
			if (cicgFile.exists()) {
				List<String> l = Utils.readFileToList(cicgFile);
	            for (String s : l) {
	            	String[] nodes = s.split(" ");
	            	int key = Integer.parseInt(nodes[0]);
	            	String keyName = lNdx2Name.get(key);
	            	int keyGNdx = 0;
	            	if (name2GNdx.containsKey(keyName))
	            		keyGNdx = name2GNdx.get(keyName);
	            	else {
	            		keyGNdx = currGNdx;
	            		gNdx2Str.add(currGNdx, keyName);
	            		name2GNdx.put(keyName, currGNdx++);
	            	}
	            	Set<Integer> successors = new HashSet<Integer>();
	            	for (int j = 1; j < nodes.length; j++) {
	            		int succ = Integer.parseInt(nodes[j]);
	            		String succName = lNdx2Name.get(succ);
	            		int succGNdx = 0;
		            	if (name2GNdx.containsKey(succName))
		            		succGNdx = name2GNdx.get(succName);
		            	else {
		            		succGNdx = currGNdx;
		            		gNdx2Str.add(currGNdx, succName);
		            		name2GNdx.put(succName, currGNdx++);
		            	}
		            	successors.add(succGNdx);
	            	}
	            	if (unionCicg.containsKey(keyGNdx)) {
	            		Set<Integer> existSuccs = unionCicg.get(keyGNdx);
	            		existSuccs.addAll(successors);
	            	} else
	            		unionCicg.put(keyGNdx, successors);
	            }
			}
		}
		PrintWriter unionOut = OutDirUtils.newPrintWriter(UNION_FILE);
		for (Integer node : unionCicg.keySet()) {
			unionOut.print(node.intValue());
			Set<Integer> successors = unionCicg.get(node);
			for (Integer succ : successors) unionOut.print(" " + succ.intValue());
			unionOut.println("");
		}
		unionOut.close();
		
		PrintWriter gnameOut = OutDirUtils.newPrintWriter(GNAME_FILE);
		for (String s : gNdx2Str) gnameOut.println(s);
		gnameOut.close();
	}
}
