package petablox.util.soot;

import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.DirectedGraph;

public interface CFG extends DirectedGraph<Block> {
	
	public List<Block> reversePostOrder();
	public Body getBody();
	public List<Block> getBlocks();
	public List<Block> getHeads();
	public List<Block> getTails();
	public List<Block> getPredsOf(Block b);
	public List<Block> getSuccsOf(Block b);
	public Iterator<Block> iterator();
	public int size();
	public String toString();
	public List<Block> getExceptionalPredsOf(Block n);
	public List<Block> getExceptionalSuccsOf(Block n);
	public List<Block> getUnexceptionalPredsOf(Block n);
	public List<Block> getUnexceptionalSuccsOf(Block n);
}
