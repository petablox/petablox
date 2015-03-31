// ControlFlowGraph.java, created Fri Jan 11 16:42:38 2002 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Compiler.Quad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand.BasicBlockTableOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.TargetOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import jwutil.collections.Filter;
import jwutil.collections.FilterIterator;
import jwutil.graphs.Graph;
import jwutil.graphs.Navigator;
import jwutil.strings.Strings;
import jwutil.util.Assert;
import java.io.Serializable;

/**
 * Control flow graph for the Quad format.
 * The control flow graph is a fundamental part of the quad intermediate representation.
 * The control flow graph organizes the basic blocks for a method.
 * 
 * Control flow graphs always include an entry basic block and an exit basic block.
 * These basic blocks are always empty and have id numbers 0 and 1, respectively.
 * 
 * A control flow graph includes references to the entry and exit nodes, and the
 * set of exception handlers for the method.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: ControlFlowGraph.java,v 1.34 2006/03/09 03:42:51 livshits Exp $
 */
public class ControlFlowGraph implements Graph, Serializable {

    /* Method that this control flow graph represents. May be null for synthetic methods. */
    private final jq_Method method;
    /* Reference to the start node of this control flow graph. */
    private final BasicBlock start_node;
    /* Reference to the end node of this control flow graph. */
    private final BasicBlock end_node;
    /* List of exception handlers for this control flow graph. */
    private final List<ExceptionHandler> exception_handlers;
    
    /* Register factory that we use on this control flow graph. */
    private final RegisterFactory rf;
    
    /* Current number of basic blocks, used to generate unique id's. */
    private int bb_counter;
    /* Current number of quads, used to generate unique id's. */
    private int quad_counter;
    
    /**
     * Creates a new ControlFlowGraph.
     * 
     * @param numOfExits  the expected number of branches to the exit node.
     * @param numOfExceptionHandlers  the expected number of exception handlers.
     */
    public ControlFlowGraph(jq_Method method, int numOfExits, int numOfExceptionHandlers, RegisterFactory rf) {
        this.method = method;
        start_node = BasicBlock.createStartNode(method);
        end_node = BasicBlock.createEndNode(method, numOfExits);
        exception_handlers = new ArrayList<ExceptionHandler>(numOfExceptionHandlers);
        this.rf = rf;
        bb_counter = 1; quad_counter = 0; // MAYUR: changed from 0 to 1
    }

    /**
     * Returns the entry node.
     * 
     * @return  the entry node.
     */
    public BasicBlock entry() { return start_node; }
    
    /**
     * Returns the exit node.
     * 
     * @return  the exit node.
     */
    public BasicBlock exit() { return end_node; }

    /**
     * Returns the method this control flow graph represents.
     * May be null for synthetic methods.
     * 
     * @return method this control flow graph represents, or null for synthetic.
     */
    public jq_Method getMethod() { return method; }

    /**
     * Returns the register factory used by this control flow graph.
     * 
     * @return  the register factory used by this control flow graph.
     */
    public RegisterFactory getRegisterFactory() { return rf; }

    /**
     * Create a new basic block in this control flow graph.  The new basic block
     * is given a new, unique id number.
     * 
     * @param numPreds  number of predecessor basic blocks that this
                                 basic block is expected to have.
     * @param numSuccs  number of successor basic blocks that this
                               basic block is expected to have.
     * @param numInstrs  number of instructions that this basic block
                                 is expected to have.
     * @param ehs  set of exception handlers for this basic block.
     * @return  the newly created basic block.
     */
    public BasicBlock createBasicBlock(int numPreds, int numSuccs, int numInstrs, ExceptionHandlerList ehs) {
        return BasicBlock.createBasicBlock(++bb_counter, this.method, numPreds, numSuccs, numInstrs, ehs);
    }
    
    /** Use with care after renumbering basic blocks. */
    void updateBBcounter(int value) { bb_counter = value-1; }
    
    /**
     * Returns a maximum on the number of basic blocks in this control flow graph.
     * 
     * @return  a maximum on the number of basic blocks in this control flow graph.
     */
    public int getNumberOfBasicBlocks() { return bb_counter+1; }
    
    public int getNumberOfQuads() {
        int total = 0;
        for (BasicBlock bb : reversePostOrder())
            total += bb.size();
        return total;
    }

    /** Returns a new id number for a quad. */
    public int getNewQuadID() { return ++quad_counter; }
    
    /** Returns the maximum id number for a quad. */
    public int getMaxQuadID() { return quad_counter; }
    
    Map jsr_map;
    
    public void addJSRInfo(JSRInfo info) {
        if (jsr_map == null) jsr_map = new HashMap();
        jsr_map.put(info.entry_block, info);
        jsr_map.put(info.exit_block, info);
    }
    
    public JSRInfo getJSRInfo(BasicBlock bb) {
        return (JSRInfo) jsr_map.get(bb);
    }

    /**
     * Visits all of the basic blocks in this graph with the given visitor.
     * 
     * @param bbv  visitor to visit each basic block with.
     */
    public void visitBasicBlocks(BasicBlockVisitor bbv) {
        for (BasicBlock bb : reversePostOrder())
            bbv.visitBasicBlock(bb);
    }
    
    public List<BasicBlock> reversePostOrder() {
        return reversePostOrder(start_node);
    }

    public List<BasicBlock> reversePostOrderOnReverseGraph() {
    	return reversePostOrderOnReverseGraph(end_node);
    }
    /**
     * Returns a list of basic blocks in reverse post order, starting at the given basic block.
     * 
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks in reverse post order, starting at the given basic block.
     */
    private List<BasicBlock> reversePostOrder(BasicBlock start_bb) {
        List<BasicBlock> result = new ArrayList<BasicBlock>();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, true);
        return result;
    }

    /**
     * Returns a list of basic blocks of the reversed graph in reverse post order, starting at the given basic block.
     * 
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks of the reversed graph in reverse post order, starting at the given basic block.
     */
    private List<BasicBlock> reversePostOrderOnReverseGraph(BasicBlock start_bb) {
        List<BasicBlock> result = new ArrayList<BasicBlock>();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, false);
        return result;
    }
    
    /**
     * Returns a list of basic blocks of the reversed graph in post order, starting at the given basic block.
     * 
     * @param start_bb  basic block to start from.
     * @return  a list of basic blocks of the reversed graph in post order, starting at the given basic block.
     */
    public List<BasicBlock> postOrderOnReverseGraph(BasicBlock start_bb) {
        List<BasicBlock> result = new ArrayList<BasicBlock>();
        boolean[] visited = new boolean[bb_counter+1];
        reversePostOrder_helper(start_bb, visited, result, false);
        java.util.Collections.reverse(result);
        return result;
    }
    
    /** Helper function to compute reverse post order. */
    private void reversePostOrder_helper(BasicBlock b, boolean[] visited, List<BasicBlock> result, boolean direction) {
        if (visited[b.getID()]) return;
        visited[b.getID()] = true;
        List<BasicBlock> bbs = direction ? b.getSuccessors() : b.getPredecessors();
        for (BasicBlock b2 : bbs)
            reversePostOrder_helper(b2, visited, result, direction);
        if (direction) {
            List<ExceptionHandler> ehl = b.getExceptionHandlers();
            for (ExceptionHandler eh : ehl) {
                BasicBlock b2 = eh.getEntry();
                reversePostOrder_helper(b2, visited, result, direction);
            }
        } else {
            if (b.isExceptionHandlerEntry()) {
                Iterator<ExceptionHandler> ex_handlers = getExceptionHandlersMatchingEntry(b);
                while (ex_handlers.hasNext()) {
                    ExceptionHandler eh = ex_handlers.next();
                    List<BasicBlock> handled = eh.getHandledBasicBlocks();
                    for (BasicBlock bb : handled)
                        reversePostOrder_helper(bb, visited, result, direction);
                }
            }
        }
        result.add(0, b);
    }

    void addExceptionHandler(ExceptionHandler eh) {
        exception_handlers.add(eh);
    }
    
    /**
     * Return the list of exception handlers in this control flow graph.
     */
    public List<ExceptionHandler> getExceptionHandlers() {
        return exception_handlers;
    }

    /**
     * Return an iterator of the exception handlers with the given entry point.
     * 
     * @param b  basic block to check exception handlers against.
     * @return  an iterator of the exception handlers with the given entry point.
     */
    public Iterator<ExceptionHandler> getExceptionHandlersMatchingEntry(BasicBlock b) {
        final BasicBlock bb = b;
        return new FilterIterator(exception_handlers.iterator(),
            new Filter() {
                public boolean isElement(Object o) {
                    ExceptionHandler eh = (ExceptionHandler)o;
                    return eh.getEntry() == bb;
                }
        });
    }
    
    /**
     * Returns a verbose string of every basic block in this control flow graph.
     * 
     * @return  a verbose string of every basic block in this control flow graph.
     */
    public String fullDump() {
        StringBuffer sb = new StringBuffer();
        sb.append("Control flow graph for "+method+":"+Strings.lineSep);
        for (BasicBlock bb : reversePostOrder())
            sb.append(bb.fullDump());
        sb.append("Exception handlers: "+exception_handlers);
        sb.append(Strings.lineSep+"Register factory: "+rf);
        return sb.toString();
    }

    private ExceptionHandler copier(Map map, ExceptionHandler this_eh) {
        ExceptionHandler that_eh = (ExceptionHandler)map.get(this_eh);
        if (that_eh != null) return that_eh;
        map.put(this_eh, that_eh = new ExceptionHandler(this_eh.getExceptionType()));
        that_eh.setEntry(copier(map, this_eh.getEntry()));
        for (BasicBlock bb : this_eh.getHandledBasicBlocks())
        	that_eh.addHandledBasicBlock(copier(map, bb));
        return that_eh;
    }

    private ExceptionHandlerList copier(Map map, ExceptionHandlerList this_ehl) {
        if (this_ehl == null || this_ehl.size() == 0) return null;
        ExceptionHandlerList that_ehl = (ExceptionHandlerList)map.get(this_ehl);
        if (that_ehl != null) return that_ehl;
        map.put(this_ehl, that_ehl = new ExceptionHandlerList());
        that_ehl.setHandler(copier(map, this_ehl.getHandler()));
        that_ehl.setParent(copier(map, this_ehl.getParent()));
        return that_ehl;
    }

    private void updateOperand(Map map, Operand op) {
        if (op == null) return;
        if (op instanceof TargetOperand) {
            ((TargetOperand)op).setTarget(copier(map, ((TargetOperand)op).getTarget()));
        } else if (op instanceof BasicBlockTableOperand) {
            BasicBlockTableOperand bt = (BasicBlockTableOperand)op;
            for (int i=0; i<bt.size(); ++i) {
                bt.set(i, copier(map, bt.get(i)));
            }
        } else if (op instanceof RegisterOperand) {
            RegisterOperand rop = (RegisterOperand)op;
            Register r = (Register)map.get(rop.getRegister());
            if (r == null) {
                if (rop.getRegister().getNumber() == -1) {
                    r = RegisterFactory.makeGuardReg().getRegister();
                    map.put(rop.getRegister(), r);
                } else {
                    Assert.UNREACHABLE(rop.toString());
                }
            } else {
                rop.setRegister(r);
            }
        } else if (op instanceof ParamListOperand) {
            ParamListOperand plo = (ParamListOperand)op;
            for (int i=0; i<plo.length(); ++i) {
                updateOperand(map, plo.get(i));
            }
        }
    }

    private Quad copier(Map map, Quad this_q) {
        Quad that_q = (Quad)map.get(this_q);
        if (that_q != null) return that_q;
        map.put(this_q, that_q = this_q.copy(++quad_counter));
        updateOperand(map, that_q.getOp1());
        updateOperand(map, that_q.getOp2());
        updateOperand(map, that_q.getOp3());
        updateOperand(map, that_q.getOp4());
        
        return that_q;
    }
    
    private BasicBlock copier(Map map, BasicBlock this_bb) {
        BasicBlock that_bb = (BasicBlock)map.get(this_bb);
        if (that_bb != null) return that_bb;
        that_bb = BasicBlock.createBasicBlock(++this.bb_counter, this.method,
        	this_bb.getNumberOfPredecessors(), this_bb.getNumberOfSuccessors(),
        	this_bb.size());
        map.put(this_bb, that_bb);
        ExceptionHandlerList that_ehl = copier(map, this_bb.getExceptionHandlers());
        that_bb.setExceptionHandlerList(that_ehl);
        for (BasicBlock bbs : this_bb.getSuccessors()) {
            that_bb.addSuccessor(copier(map, bbs));
        }
        for (BasicBlock bbs : this_bb.getPredecessors()) {
            that_bb.addPredecessor(copier(map, bbs));
        }
        for (Quad q : this_bb.getQuads()) {
            that_bb.appendQuad(copier(map, q));
        }
        return that_bb;
    }

    public void appendExceptionHandlers(ExceptionHandlerList ehl) {
        if (ehl == null || ehl.size() == 0) return;
        for (BasicBlock bb : reversePostOrder()) {
            if (bb.isEntry() || bb.isExit()) continue;
            bb.appendExceptionHandlerList(ehl);
        }
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getRoots()
     */
    public Collection getRoots() {
        return Collections.singleton(start_node);
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getNavigator()
     */
    public Navigator getNavigator() {
        return new ControlFlowGraphNavigator(this);
    }
    
    public boolean removeUnreachableBasicBlocks() {
        Collection allBasicBlocks = new HashSet(reversePostOrder(entry()));
        boolean change = false;
        for (BasicBlock bb : reversePostOrder()) {
            if (bb.getPredecessors().retainAll(allBasicBlocks))
                change = true;
        }
        for (Iterator i = exception_handlers.iterator(); i.hasNext(); ) {
            ExceptionHandler eh = (ExceptionHandler) i.next();
            if (eh.getHandledBasicBlocks().retainAll(allBasicBlocks))
                change = true;
            if (eh.getHandledBasicBlocks().isEmpty()) {
                i.remove();
            }
        }
        for (;;) {
            Collection allBasicBlocks2 = reversePostOrderOnReverseGraph(exit());
            if (allBasicBlocks2.containsAll(allBasicBlocks)) {
                return change;
            }
            change = true;
            allBasicBlocks.removeAll(allBasicBlocks2);
            BasicBlock bb = (BasicBlock) allBasicBlocks.iterator().next();
            System.out.println("Infinite loop discovered in "+this.getMethod()+", linking "+bb+" to exit.");
            bb.addSuccessor(exit());
            exit().addPredecessor(bb);
            allBasicBlocks = new HashSet(reversePostOrder(entry()));
            
            //Fix added: If infinite loop exists, remove dangling predecessors
            exit().getPredecessors().retainAll(allBasicBlocks);
        }
    }
    
}
