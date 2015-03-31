package waitnotify;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

public class LockTree {
	protected LockNode root;
	protected LockNode cur;
	protected List<LockNode> newlyAddedNodes;

	public LockTree(){
		root = new LockNode();
		cur = root;
		newlyAddedNodes = new LinkedList<LockNode>();
	}
	
	public void reset(){
		root.removeAllChildren();
		newlyAddedNodes.clear();
		cur = root;
	}

	public void resetNewlyAddedNodes(){
		newlyAddedNodes.clear();
	}
	
	public void addChildToCurNode(int lId, String locId){
		LockNode child = new LockNode(lId, locId);
		LockNode childThatWasAdded = cur.addChild(child);
		cur = childThatWasAdded;
		newlyAddedNodes.add(childThatWasAdded);
	}

	private void removeNewlyAddedNodesFromTree(){
		for(LockNode n : newlyAddedNodes){
			if(n.parent != null){
				n.parent.children.remove(n);
				n.parent = null;
			}	
			//if((n.locId != null) && (n.locId.contains("ConnectionTable.java:286"))){
				//System.out.println("node being removed");
			//}
		}
	}
	
	public void moveUp(){
		cur = cur.parent;
	}
	
	public List<String> dumpToStmts(){
		removeNewlyAddedNodesFromTree();
		List<String> stmts = new LinkedList<String>();
		int nTabs = 0;
		Stack<LockNode> traversedNodes = new Stack<LockNode>();
		traversedNodes.push(root);
		LockNode c = getChildThatHasNotBeenVisited(root);
		if(c == null){
			return stmts;
		}

		traversedNodes.push(c);
		stmts.add("synchronized(l"+c.lId+"){"+c.locId);
		
		while(traversedNodes.size() > 0){
			LockNode topOfStack = traversedNodes.peek();
			LockNode cOfTopOfStack = getChildThatHasNotBeenVisited(topOfStack);
			if(cOfTopOfStack == null){
				if(topOfStack == root){
					traversedNodes.pop();
					assert(traversedNodes.size() == 0);
					continue;
				}
				String tabs = WNLogger.getStringWithTabsGivenNumTabs(nTabs);
				String s = tabs + "}";
				stmts.add(s);
				nTabs--;
				topOfStack.isVisited = true;
				traversedNodes.pop();
			}
			else{
				nTabs++;
				String tabs = WNLogger.getStringWithTabsGivenNumTabs(nTabs);
				int lId = cOfTopOfStack.lId;
				String locId = cOfTopOfStack.locId;
				String s = tabs + "synchronized(l"+lId+") {"+locId;
				stmts.add(s);
				traversedNodes.push(cOfTopOfStack);
			}
		}
		
		//System.out.println("dumping the lock tree to the following stmts : "+stmts);	
		return stmts;
	}

	//not used
	public List<String> dumpCurPath(){
		Stack<LockNode> pathToCur = new Stack<LockNode>();
		LockNode curNode = cur;
		while(curNode != root){
			pathToCur.push(curNode);
			curNode = curNode.parent;
		}
		int nTabs = 0;
		List<String> stmts = new LinkedList<String>();
		while(!pathToCur.empty()){
			LockNode l = pathToCur.pop();
			int lId = l.lId;
			String locId = l.locId;
			String tabs = WNLogger.getStringWithTabsGivenNumTabs(nTabs);
			nTabs++;
			String s = tabs + "synchronized(l"+lId+") {"+locId;
			stmts.add(s);
		}
		System.out.println("dumping the cur path to the following stmts : "+stmts);	
		return stmts;
	}
	
	private LockNode getChildThatHasNotBeenVisited(LockNode p){
		Vector<LockNode> children = p.children;
		if(children == null){
			return null;
		}
		//can do binary search here
		Iterator<LockNode> childrenItr = children.iterator();
		while(childrenItr.hasNext()){
			LockNode c = (LockNode)(childrenItr.next());
			if(c.isVisited == false){
				return c;
			}
		}
		return null;
	}
}
