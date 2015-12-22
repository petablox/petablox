package petablox.project.analyses.metaback;

import petablox.project.analyses.parallelizer.ParallelAnalysis;
import petablox.project.analyses.parallelizer.Scenario;
import petablox.util.Utils;
/**
 * The base class of iterative refinement
 * @author xin
 *
 */
public abstract class IterRefineAnalysis extends ParallelAnalysis {

protected AbstractionFactory absFac = getAbsFac();
protected QueryFactory qFac = getQueryFac();

	protected abstract AbstractionFactory getAbsFac();
	
	protected abstract QueryFactory getQueryFac();
	
	protected abstract String getSep();

	protected abstract QueryResult[] runAnalysis(Abstraction abst,Query queries[]);
	
	@Override
	public String apply(String line) {
		Scenario inScenario = new Scenario(getSep(),line);
		if(!inScenario.equals(AbstractJobDispatcher.QUERY))
			throw new RuntimeException("Query type scenario expected here!");
		Abstraction abst = absFac.genAbsFromStr(inScenario.getIn());
		String queryStrs[] = Utils.split(inScenario.getOut(), getSep(), true, true, -1);
		Query queries[] = new Query[queryStrs.length];
		for(int i = 0;i < queryStrs.length;i++){
			queries[i] = qFac.getQueryFromStr(queryStrs[i]);
		}
		
		QueryResult[] qResults = runAnalysis(abst,queries);
		
		inScenario.setIn("");//Save some space for transport
		inScenario.setType(AbstractJobDispatcher.RESULT);
		StringBuffer sb = new StringBuffer();
		for(QueryResult qr: qResults)
			sb.append(qr.encode()+getSep());
		inScenario.setOut(sb.toString());
		return inScenario.toString();
	}


}
