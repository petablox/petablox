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

    public SootMethodWrapper(){
        m = null;
        preOrder = -1;
        postOrder = -1;
    }
    public SootMethodWrapper(SootMethod m){
        this();
        this.m = m;
    }
    public SootMethod getSootMethod(){
        return m;
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
