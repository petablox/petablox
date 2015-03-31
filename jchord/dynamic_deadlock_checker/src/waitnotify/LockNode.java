package waitnotify;

import java.util.Vector;

public class LockNode {
	protected int lId;
	protected String locId;
	protected LockNode parent;
	protected Vector<LockNode> children;
	protected boolean isVisited;

	public LockNode(){
		lId = -1;
		locId = null;
		parent = null;
		children = null;
		isVisited = false;	
	}
	
	public LockNode(int l, String loc){
		this();
		lId = l;
		locId = loc;
	}
	
	
	public LockNode addChild(LockNode c){
		if(children == null){
			children = new Vector<LockNode>();
		}
		//if((c.locId != null) && (c.locId.contains("ConnectionTable.java:286"))){
			//System.out.println("parent has "+children.size()+" nodes");
		//}
		if(!children.contains(c)){
			children.add(c);
			c.parent = this;
			return c;
		}
		else{
			int cPos = children.indexOf(c);
			LockNode cAtcPos = children.get(cPos);
			return cAtcPos;
		}
	}
	
	public void removeAllChildren(){
		if(children != null){
			for(LockNode n : children){
				n.parent = null;
			}
			children = null;
		}
	}
	
	public boolean equals(Object other){
		if(!(other instanceof LockNode)){
			return false;
		}
		LockNode othrLockNode = (LockNode)other;
		boolean areLIdsEqual = (lId == othrLockNode.lId);
		boolean areLocIdsEqual = (locId == null)? (othrLockNode.locId == null) : 
			locId.equals(othrLockNode.locId) ;
		
		return areLIdsEqual && areLocIdsEqual;
	}
}
