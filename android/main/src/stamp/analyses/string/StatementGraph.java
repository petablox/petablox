package stamp.analyses.string;

import java.util.*;

public class StatementGraph 
{
	private Map<Statement,Set<Statement>> nodeToPreds = new HashMap();
	private Set<Statement> nodes = new LinkedHashSet();

	public void addNode(Statement stmt)
	{
		nodes.add(stmt);
	}

	public void addEdge(Statement from, Statement to)
	{
		Set<Statement> preds = nodeToPreds.get(to);
		if(preds == null){
			preds = new HashSet();
			nodeToPreds.put(to, preds);
		}
		preds.add(from);
	}
	
	public Iterable<Statement> topDownIterator()
	{
		List<Statement> list = new LinkedList();
		for(Statement stmt : nodes){
			list.add(0, stmt);
		}
		return list;
	}

	public Iterable<Statement> stmts()
	{
		return nodes;
	}

	public Iterable<Statement> predsOf(Statement node)
	{
		Set<Statement> preds = nodeToPreds.get(node);
		if(preds == null)
			preds = Collections.EMPTY_SET;
		return preds;
	}
	
	
}