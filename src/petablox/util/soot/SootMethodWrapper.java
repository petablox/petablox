package petablox.util.soot;

import soot.SootMethod;

/**
 * This class is a wrapper to store DFS pre and post order
 * numbering of a method during scope construction in RTA
 *
 * Author Aditya Kamath(adityakamath@gatech.edu)
 */
public class SootMethodWrapper {
    private SootMethod m;
    private int preOrder;
    private int postOrder;
    private int index;
    public boolean processed;

    public SootMethodWrapper(){
        m = null;
        preOrder = -1;
        postOrder = -1;
        index = -1;
        processed = false;
    }
    public SootMethodWrapper(SootMethod m){
        this();
        this.m = m;
    }

    public SootMethodWrapper(SootMethod m, int pre, int index){
        this.m = m;
        this.preOrder = pre;
        this.postOrder = -1;
        this.index = index;
        processed = false;
    }

    public SootMethod getSootMethod(){
        return m;
    }

    public int getIndex(){
        return index;
    }

    public int getPreOrder(){
        return preOrder;
    }

    public void setPreOrder(int pre){
        this.preOrder = pre;
    }

    public int getPostOrder(){
        return postOrder;
    }

    public void setPostOrder(int post){
        this.postOrder = post;
    }
}
