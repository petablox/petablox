package chord.analyses.primtrack;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import joeq.Compiler.Quad.BasicBlock;

/**
 * A worklist of basic blocks, each of which can only be in the queue once
 *
 */
public class UniqueBQ {
  Set<Integer> listContents= new HashSet<Integer>();
  Queue<BasicBlock> list = new java.util.LinkedList<BasicBlock>();

  
  public void add(BasicBlock b) {
    int id = b.getID();
    if(!listContents.contains(id)) {
      listContents.add(id);
      list.add(b);
    }
  }
  
  public boolean isEmpty() {
    return list.isEmpty();
  }
  
  public BasicBlock remove() {
    BasicBlock b = list.remove();
    listContents.remove(b.getID());
    return b;
  }
}