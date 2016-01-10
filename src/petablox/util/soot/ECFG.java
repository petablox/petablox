package petablox.util.soot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.HashMap;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.ExceptionalBlockGraph;

public class ECFG extends ExceptionalBlockGraph implements ICFG {
	private boolean isEmpty = false;
	
	public ECFG (SootMethod m) {
		super (m.retrieveActiveBody());
		addEntryAndExit (m);
	}
	
	private void addEntryAndExit(SootMethod m) {	
		List<Block> heads = getHeads();
		List<Block> tails = getTails();
        if(heads.size() == 0 || tails.size() == 0) {
            System.out.println ("ERROR: while adding entry and exit nodes to cfg of method: " + m);
            return;
        }
        
        if(!((heads.size() == 1) && (heads.get(0) instanceof EDummyBlock) &&
             (tails.size() == 1) && (tails.get(0) instanceof EDummyBlock))) {
	        List<Block> blocks = getBlocks();
	        Unit hnop = new JEntryNopStmt();
	        m.getActiveBody().getUnits().add(hnop);
	        EDummyBlock head = new EDummyBlock(getBody(), 0, hnop, this);
	        head.makeHeadBlock(heads);
	        mHeads = new ArrayList<Block>();
	        mHeads.add(head);
	        
	        Iterator<Block> blocksIt = blocks.iterator();
	        while(blocksIt.hasNext()){
	            Block block = (Block) blocksIt.next();
	            block.setIndexInMethod(block.getIndexInMethod() + 2);
	        }
	        
		    List<Block> newBlocks = new ArrayList<Block>();
		    newBlocks.add(head);
		    newBlocks.addAll(blocks);
		    mBlocks = newBlocks;
 
	        List<Block> blocks1 = getBlocks();
	        Unit tnop = new JExitNopStmt();
	        m.getActiveBody().getUnits().add(tnop);
	        EDummyBlock tail = new EDummyBlock(getBody(), 1, tnop, this);
	        tail.makeTailBlock(tails);
	        mTails = new ArrayList<Block>();
	        mTails.add(tail);
	        blocks1.add(tail);
        }
	}
	
	public List<Block> reversePostOrder(){
		if (!isEmpty) {
			PseudoTopologicalOrderer<Block> pto = new PseudoTopologicalOrderer<Block>();
			List<Block> rpo = pto.newList(this, false);	
			return rpo;
		} else {
			List<Block> rpo = new ArrayList<Block>();
			rpo.addAll(mHeads);
			rpo.addAll(mTails);
			return rpo;
		}	
	}
}

class EDummyBlock extends Block
{
    EDummyBlock(Body body, int indexInMethod, Unit u, BlockGraph bg)
    {
        super(u, u, body, indexInMethod, 1, bg);
    }

    void makeHeadBlock(List<Block> oldHeads)
    {
        setPreds(new ArrayList<Block>());
        setSuccs(new ArrayList<Block>(oldHeads));

        Iterator<Block> headsIt = oldHeads.iterator();
        while(headsIt.hasNext()){
            Block oldHead = (Block) headsIt.next();

            List<Block> newPreds = new ArrayList<Block>();
            newPreds.add(this);

            List<Block> oldPreds = oldHead.getPreds();
            if(oldPreds != null)
                newPreds.addAll(oldPreds);
            
            oldHead.setPreds(newPreds);
        }
    }

    void makeTailBlock(List<Block> oldTails)
    {
        setSuccs(new ArrayList<Block>());
        setPreds(new ArrayList<Block>(oldTails));

        Iterator<Block> tailsIt = oldTails.iterator();
        while(tailsIt.hasNext()){
            Block oldTail = (Block) tailsIt.next();

            List<Block> newSuccs = new ArrayList<Block>();
            newSuccs.add(this);

            List<Block> oldSuccs = oldTail.getSuccs();
            if(oldSuccs != null)
                newSuccs.addAll(oldSuccs);

            oldTail.setSuccs(newSuccs);
        }
    }    
}
