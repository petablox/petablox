package petablox.project.analyses.provenance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import petablox.bddbddb.Dom;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "provenance-vis")
public class ConstraintVisualizer extends JavaAnalysis {
	private final static String DLOG_CONFIG = "chord.provenance.instrConfig";
	private final static String QUERY = "chord.provenance.query";
	// by specify this property, print a certain relation to the given file
	private final static String OUTPUT_R = "chord.provenance.out_r";
	private List<LookUpRule> rules = new ArrayList<LookUpRule>();
	private Set<Tuple> solvedTuples = new HashSet<Tuple>();
	Set<ConstraintItem> constraints;
	private Tuple queryT;

	@Override
	public void run() {
		String configFile = System.getProperty(DLOG_CONFIG);
		String queryString = System.getProperty(QUERY, null);
		String outputR = System.getProperty(OUTPUT_R, null);
		if (outputR != null) {
			String outFile = Config.outDirName + File.separator + outputR
					+ ".out";
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new File(outFile));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			ProgramRel outRel = (ProgramRel) ClassicProject.g()
					.getTrgt(outputR);
			outRel.load();
			Dom doms[] = outRel.getDoms();
			for (int[] t : outRel.getAryNIntTuples()) {
				for (int i = 0; i < t.length; i++) {
					pw.print(t[i] + " ");
					pw.print(doms[i].get(t[i]) + " ");
				}
				pw.println();
				pw.flush();
			}
			pw.flush();
			pw.close();
			return;// output a certain relation and terminate
		}
		if (configFile == null)
			return;
		String configs[] = configFile.split(",");
		for (String config : configs)
			createRules(config);
		if (queryString != null) {
			String outFile = Config.outDirName + File.separator + "constraints"
					+ ".out";
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new File(outFile));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			queryT = new Tuple(queryString);
			constraints = lookup(queryT);
			for (ConstraintItem ci : constraints) {
				pw.println(ci);
			}
			boolean changed = false;
			do {
				changed = false;
				Set<ConstraintItem> consToAdd = new HashSet<ConstraintItem>();
				for (ConstraintItem ci : constraints) {
					for (Tuple t : ci.subTuples) {
						Set<ConstraintItem> subCons = lookup(t);
						for (ConstraintItem ci1 : subCons) {
							changed = true;
							pw.println(ci1);
							consToAdd.add(ci1);
						}
					}
				}
				constraints.addAll(consToAdd);
			} while (changed);
			pw.flush();
			pw.close();
		}else{
			String outFile = Config.outDirName + File.separator + "constraints"
					+ ".out";
			try {
				PrintWriter pw = new PrintWriter(new File(outFile));
				for(LookUpRule r: rules){
					Iterator<ConstraintItem> iter = r.getAllConstrIterator();
					while(iter.hasNext())
					pw.println(iter.next());
				}
				pw.flush();
				pw.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	
	private Set<ConstraintItem> lookup(Tuple t) {
		Set<ConstraintItem> ret = new HashSet<ConstraintItem>();
		if (this.solvedTuples.contains(t))
			return ret;
		else
			solvedTuples.add(t);
		for (LookUpRule rule : rules) {
			if (rule.match(t)) {
				List<ConstraintItem> items = rule.lookUp(t);
				if (items != null)
					ret.addAll(items);
			}
		}
		return ret;
	}

	private void createRules(String ruleFile) {
		try {
			Scanner sc = new Scanner(new File(ruleFile));
			while (sc.hasNext()) {
				String line = sc.nextLine().trim();
				if (!line.equals("")) {
					LookUpRule rule = new LookUpRule(line);
					rules.add(rule);
				}
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
