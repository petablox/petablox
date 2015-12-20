package petablox.project.analyses.provenance;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jwutil.collections.IndexMap;
import jwutil.io.SystemProperties;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.Solver;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.TaskParser;
import petablox.project.analyses.DlogAnalysis;
import petablox.project.analyses.JavaAnalysis;

@Petablox(
		name="provenance-statistics"
		)
public class DlogStatistic extends JavaAnalysis {
	public final static String DLOG = "chord.provenance.dlog";
	private String dlogName;
	private Collection inputRelations;
	private IndexMap allRelations;
	private List rules;
	private Set<Relation> outputRelations = new HashSet<Relation>();
	private Set<Relation> instrumentedRelations = new HashSet<Relation>();
	private final static String MAGIC = "_XZ89_";
	private String header="";
	
	private Solver solver;
	
	private PrintWriter dlogOut;
	private PrintWriter configOut;
	
	@Override
	public void run() {
		dlogName = System.getProperty(DLOG);
		if(dlogName == null)
			throw new RuntimeException("Need to set property: "+DLOG);

		TaskParser taskParser = new TaskParser();
		if(!taskParser.run())
			throw new RuntimeException("Task parsing not successful.");
		ClassicProject.g().runTask(dlogName);
		DlogAnalysis dlogAnalysis = taskParser.getNameToDlogTaskMap().get(dlogName);
		String fileName = dlogAnalysis.getFileName();
		System.setProperty("verbose", ""+Config.verbose);
        System.setProperty("bdd","j");
        System.setProperty("basedir",Config.bddbddbWorkDirName);
        String solverName = SystemProperties.getProperty("solver", "net.sf.bddbddb.BDDSolver");
        
		try {
        solver = (Solver) Class.forName(solverName).newInstance();
		solver.load(fileName);
		inputRelations = solver.getRelationsToLoad();
		allRelations = solver.getRelations();
		int totalSize = 0;
		for(Object o : inputRelations){
			Relation r = (Relation)o;
			System.out.println(r.toString()+": "+r.size());
			totalSize += r.size();
		}
		System.out.println("Total: "+totalSize);
		rules = solver.getRules();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
