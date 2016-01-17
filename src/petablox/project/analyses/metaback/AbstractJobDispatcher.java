package petablox.project.analyses.metaback;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import petablox.project.ClassicProject;
import petablox.project.analyses.metaback.dnf.DNF;
import petablox.project.analyses.parallelizer.JobDispatcher;
import petablox.project.analyses.parallelizer.ParallelAnalysis;
import petablox.project.analyses.parallelizer.Scenario;
import petablox.util.Execution;
import petablox.util.Timer;
import petablox.util.Utils;

/**
 * The base job dispatcher for all metaback analyses
 * 
 * @author xin
 * 
 */
public abstract class AbstractJobDispatcher implements JobDispatcher {
	protected Map<Query, DNF> ancs;
	protected TObjectIntHashMap<Query> iterMap;
	protected Map<Abstraction,Set<Query>> provenQs;
	protected Map<Query, Boolean> processMap;
	int provenNum = 0;
	protected List<Query> impossiQs;
	protected List<Query> timedQs;
	protected List<Query> explodedQs;
	protected List<Query> iterExceedQs;
	protected Map<Abstraction, List<Set<Query>>> jobPool;
	protected AbstractionFactory absFac;
	protected QueryFactory qFac;
	protected Execution EX = Execution.v();
	protected ParallelAnalysis masterAnalysis;
	protected String xmlToHtmlTask;
	public final static String QUERY = "0";
	public final static String RESULT = "1";
	public static boolean DEBUG;
	public static int dnfLimit = -1;
	public static int jobPatchSize = 100;
	public static int iterLimit = 1000;
	protected boolean init;

	public AbstractJobDispatcher(String xmlToHtmlTask, ParallelAnalysis masterAnalysis) {
		jobPool = new HashMap<Abstraction, List<Set<Query>>>();
		provenQs = new TreeMap<Abstraction,Set<Query>>();
		processMap = new HashMap<Query,Boolean>();
		impossiQs = new ArrayList<Query>();
		timedQs = new ArrayList<Query>();
		explodedQs = new ArrayList<Query>();
		iterExceedQs = new ArrayList<Query>();
		iterMap = new TObjectIntHashMap<Query>();
		this.xmlToHtmlTask = xmlToHtmlTask;
		this.masterAnalysis = masterAnalysis;
		init = false;
	}

	public void init(){
		if(init)
			return;
		absFac = getAbsFactory();
		qFac = getQueryFactory();
		ancs = this.getInitialANCS();
		init = true;
	}
	
	/**
	 * Get the initial anc-query map
	 * 
	 * @return
	 */
	protected abstract Map<Query, DNF> getInitialANCS();

	public abstract String getMinorSep();

	public abstract String getMajorSep();

	protected abstract AbstractionFactory getAbsFactory();

	protected abstract QueryFactory getQueryFactory();

	@Override
	public Scenario createJob() {
		init();
		
		fillJobPool();
		if(jobPool.isEmpty())
			return null;
		Map.Entry<Abstraction, List<Set<Query>>> jobEntry = jobPool.entrySet()
				.iterator().next();
//		jobPool.remove(jobEntry.getKey());
		Abstraction entryKey = jobEntry.getKey();
		List<Set<Query>> entryValue = jobEntry.getValue();
		Set<Query> queries = entryValue.remove(0);
		if(entryValue.isEmpty())
			jobPool.remove(entryKey);
		String in = entryKey.encode();
		StringBuffer sb = new StringBuffer();
		for (Query q : queries) {
			if (sb.length() != 0)
				sb.append(getMinorSep());
			sb.append(q.encode());
			processMap.put(q, true);
		}
		Scenario job = new Scenario(QUERY, in, sb.toString(), getMajorSep());
		return job;
	}

	private void fillJobPool() {
		init();
		
		if (ancs.isEmpty() || !jobPool.isEmpty())
			return;
		int numofWorkers = masterAnalysis.getNumWorkers();
		if(numofWorkers > 0){
		int newJobPatchSize = ancs.size()/masterAnalysis.getNumWorkers()+1;
		if(newJobPatchSize < jobPatchSize)
			jobPatchSize = newJobPatchSize;
		}
		
		for (Map.Entry<Query, DNF> entry : ancs.entrySet()) {
			DNF anc = entry.getValue();
			Query q = entry.getKey();
			Boolean inProcess = processMap.get(q);
			if(inProcess!=null&&inProcess.booleanValue())
				continue;
			if (anc.isFalse())
				throw new RuntimeException("Something wrong with NC update!");
			Abstraction abs = absFac.genAbsFromNC(anc);
			List<Set<Query>> qq = jobPool.get(abs);
			if(qq == null){
				qq = new ArrayList<Set<Query>>();
				jobPool.put(abs, qq);
			}
			Set<Query> qs;
			if(qq.isEmpty()){
				qs = new HashSet<Query>();
				qq.add(qs);
			}else{
				qs = qq.get(qq.size()-1);
				if(qs.size() >= jobPatchSize){
					qs = new HashSet<Query>();
					qq.add(qs);
				}
			}
			qs.add(q);
		}
	}

	@Override
	public void onJobResult(Scenario scenario) {
		init();
		if (!scenario.getType().equals(RESULT))
			throw new RuntimeException("The reulst scenario expected here!");
		Timer timer = new Timer("Refine-JobDispatcher");
		timer.init();
		String results[] = Utils.split(scenario.getOut(), getMinorSep(), true,
				true, -1);
		Abstraction abs = absFac.genAbsFromStr(scenario.getIn());
		for (String r : results) {
			QueryResult qr = qFac.genResultFromStr(r);
			Query q = qr.getQuery();
			int iters = iterMap.get(q);
			iterMap.put(q, iters+1);
			processMap.put(q, false);
			if (qr.getResult() == QueryResult.PROVEN) {
				ancs.remove(q);
				Set<Query> qSet = provenQs.get(abs);
				provenNum++;
				if(qSet == null){
					qSet = new HashSet<Query>();
					provenQs.put(abs, qSet);
				}
				qSet.add(q);
			}
			if (qr.getResult() == QueryResult.IMPOSSIBILITY) {
				ancs.remove(q);
				impossiQs.add(q);
			}
			if (qr.getResult() == QueryResult.TIMEOUT) {
				ancs.remove(q);
				timedQs.add(q);
			}
			if (qr.getResult() == QueryResult.REFINE) {
				DNF nc = qr.getNC();
				DNF anc = ancs.get(q);
				DNF nanc = anc.intersect(nc);
				if (nanc.isFalse()) {// Impossible result
					ancs.remove(q);
					impossiQs.add(q);
				} else{
					if(dnfLimit > 0 && nanc.size() > dnfLimit){
						ancs.remove(q);
						explodedQs.add(q);
					}else
						if(iterLimit > 0 && iters+1 > iterLimit){
							ancs.remove(q);
							iterExceedQs.add(q);
						}
						else
							ancs.put(q, nanc);
				}
			}
		}
		System.out.println("Proven queries: "+provenNum);
		System.out.println("Impossible queries: "+impossiQs.size());
		System.out.println("Timed out queries: "+timedQs.size());
		System.out.println("DNF exploded queries: "+explodedQs.size());
		System.out.println("Iteration exceeded queries: "+iterExceedQs.size());
		System.out.println("Remained queries: "+ancs.size());
		timer.done();
		if(DEBUG)
			dumpState();
		System.out.println(timer.getInclusiveTimeStr());
	}

	protected void dumpState(){
		System.out.println("==========================Proven Queries===========================");
		for(Map.Entry<Abstraction, Set<Query>> entry: provenQs.entrySet()){
			System.out.println(entry.getKey());
			for(Query q:entry.getValue())
				System.out.println(q);
			System.out.println();
		}
		System.out.println("==========================Impossible Queries===========================");
		for(Query q:impossiQs)
			System.out.println(q);
		System.out.println("==========================Timed-out Queries===========================");
		for(Query q:timedQs)
			System.out.println(q);
		System.out.println("==========================DNF-exploded Queries===========================");
		for(Query q:explodedQs)
			System.out.println(q);
		System.out.println("==========================Iteration-exceeded Queries===========================");
		for(Query q:iterExceedQs)
			System.out.println(q);
		System.out.println("==========================Remained Queries===========================");
		for(Map.Entry<Query, DNF> entry: ancs.entrySet()){
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
	}
	
	@Override
	public void onError(int scenarioID) {
		EX.logs("Error, scenarion id: %s", scenarioID);
	}

	@Override
	public boolean isDone() {
		init();
		
		return ancs.isEmpty();
	}

	@Override
	public void saveState() {
		// Save to disk
		{
			init();
			
			PrintWriter out = Utils.openOut(EX.path("anaresult.xml"));
			out.println("<resultlist>");
			out.println("<provenQs" + " num=\"" + provenNum + "\">");
			for(Map.Entry<Abstraction, Set<Query>> entry: provenQs.entrySet()){
				out.println("<group num=\""+entry.getValue().size()+"\">");
				out.println(entry.getKey().encodeForXML());
				for(Query q:entry.getValue())
				out.println(q.encodeForXML());
				out.println("</group>");
				}
			out.println("</provenQs>");

			out.println("<impoQs" + " num=\"" + impossiQs.size() + "\">");
			for (Query q : impossiQs)
				out.println(q.encodeForXML());
			out.println("</impoQs>");
			out.println("<timedQs" + " num=\"" + timedQs.size() + "\">");
			for (Query q : timedQs)
				out.println(q.encodeForXML());
			out.println("</timedQs>");
			
			out.println("<explodedQs" + " num=\"" + explodedQs.size() + "\">");
			for (Query q : explodedQs)
				out.println(q.encodeForXML());
			out.println("</explodedQs>");
			
			out.println("<iterExceedQs" + " num=\"" + iterExceedQs.size() + "\">");
			for (Query q : iterExceedQs)
				out.println(q.encodeForXML());
			out.println("</iterExceedQs>");
			
			out.println("</resultlist>");

			out.flush();
			
			out.close();

			//Output the iteration map
			out = Utils.openOut(EX.path("iterations.txt"));
			TObjectIntIterator<Query> iterator = iterMap.iterator();
			int total = 0;
			while(iterator.hasNext()){
				iterator.advance();
				int v = iterator.value();
				total+=v;
				out.println(iterator.key()+"\t\t\t"+v);
			}
			out.println(total);
			
			out.flush();

			out.close();
						
			out = Utils.openOut(EX.path("proven_queries.txt"));
			for(Map.Entry<Abstraction, Set<Query>> entry: provenQs.entrySet()){
				for(Query q:entry.getValue())
					out.println(q.encode());
			}
			
			out.flush();
			out.close();
			
			out = Utils.openOut(EX.path("unproven_queries.txt"));
			for (Query q : impossiQs)
				out.println(q.encode());
			for (Query q : timedQs)
				out.println(q.encode());
			for (Query q : explodedQs)
				out.println(q.encode());
			for (Query q : iterExceedQs)
				out.println(q.encode());
			out.flush();
			out.close();
					
			if (this.xmlToHtmlTask != null) {
				ClassicProject.g().resetTaskDone(this.xmlToHtmlTask);
				ClassicProject.g().runTask(this.xmlToHtmlTask);
			}
		}
	}

	@Override
	public int maxWorkersNeeded() {
		init();
		return ancs.size();
	}

	public static boolean isDEBUG() {
		return DEBUG;
	}

	public static void setDEBUG(boolean dEBUG) {
		DEBUG = dEBUG;
	}

	public static void setExplode(int explode){
		dnfLimit = explode;
	}
	
	public static void setJobPatchSize(int size){
		jobPatchSize = size;
	}
	
	public static void setIterLimit(int limit){
		iterLimit = limit;
	}
}
