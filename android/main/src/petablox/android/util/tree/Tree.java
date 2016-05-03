package petablox.android.util.tree;

import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A simple minimal tree implementation. This is designed
 * to support the callgraph creation in SrcSinkFlowViz class
 * and so is supports functionality that is important there
 * and little more. 
 *
 * A label field allows you to name your trees.
 *
 * @author brycecr
 */
public class Tree<T> {

    protected static final String defaultRootLabel = "AnonTree";

    /* Root Node. Can contain useful data or be null-like */
    protected Node<T> root;

    /* Simple string label */
    protected final String label;

    /**
     * Construct a new tree
     * with a "null" root
     * and label l.
     */
    public Tree(String l) {
        root = new Node<T>();
        root.setParent(root);
        label = l;
    }

    /**
     * Construct a new tree
     * with root node data rootData
     * and default label
     */
    public Tree(T rootData) {
        root = new Node<T>(rootData);
        root.setParent(root);
        label = defaultRootLabel;
    }

    /**
     * Construct new tree with root data
     * rootData and label l
     */
    public Tree(T rootData, String l) {
        root = new Node<T>(rootData);
        root.setParent(root);
        label = l;
    }

    /**
     * @return current label of this tree, a string
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return root node object of this tree
     */
    public Node<T> getRoot() {
        return root;
    }

    /**
     * uses the equals method of generic type T
     * null parameter alwas returns false
     *
     * @return whether node is the root node object. 
     */
    public boolean isRoot(Node<T> node) {
        return node != null && root.equals(node);
    }

    /**
     * Returns whether the parameter
     * is the same data as the data for 
     * the root node for ths tree
     *
     * @return whether T is the same as root data according to T .equals
     */
    public boolean isRoot(T dat) {
        return getRoot().getData() != null && getRoot().getData().equals(dat);
    }

    /**
     * @return parent node of child, or root if no other parent
     */
    public Node<T> getParent(Node<T> child) {
        if (child == null) {
            return null;
        }
        //if (isInTree(child)) { //TODO: implement this part and throw
        //			 //exception on fail
        Node<T> parent = child.getParent();
        if (parent == null) {
            return getRoot();
        }
        return parent;
    }

    public Node<T> getSuccessor(Node<T> node) {
        return getSuccessor(node, new int[1]);
    }

    /**
     * Returns the "next" node in this tree after
     * node. Next is depth-first first, and chronological second.
     * @param node to find successor of
     * @param depthDelta by reference int, 
     *            depth change of successor from node. - is higher in tree
     * @return successor of node, null if unexpected error, root if no successor
     */
    public Node<T> getSuccessor(Node<T> node, int[] depthDelta) {

        //if (isInTree(child)) { //TODO: implement this part and throw
        //			 //exception on fail
        int depthChange = 0;

        while (!isRoot(node)) {

            Node<T> parent = this.getParent(node);
            ArrayList<Node<T>> siblings = parent.getChildren();
            assert siblings != null;

            int ind = siblings.indexOf(node);

            if (ind == -1) {
                // Did not node in children of its parents. Something weird...
                System.err.println("Node not found in getSuccessor");
                return null;

            } else if (ind == siblings.size() - 1) {
                // node is the last child of its parents.
                // so loop up to one level up
                depthChange -= 1;
                node = parent;
                continue;

            } else {
                // Return next sibbling of node
                depthDelta[0] = depthChange;
                return siblings.get(ind + 1);
            }
        }

        depthDelta[0] = depthChange;
        return getRoot();
    }

    /**
     * Create and return new iterator over this tree.
     * The first element returned is an is the first
     * child of the root, the last element is the Root.
     * Note that TreeIterators return objects of type T
     * (the data of nodes) rather than node objects.
     * 
     * @return new iterator over this tree
     */
    public TreeIterator iterator() {
        return new TreeIterator();
    }

    /**
     * Returns a string representation of this tree.
     * Each element is on its own line, printed
     * using the toString method on objects of generic
     * parameter type T, preceeded with dashes to indicate
     * depth into the tree (more dashes is deeper).
     * Iteration order is the same as for the TreeIterator objects.
     * 
     * @return multi-line string representation of this tree
     */
    public String toString() {
        TreeIterator itr = iterator();
        StringBuilder builder = new StringBuilder();

        while (itr.hasNext()) {
            T entry = itr.next();
            int depth = itr.getDepth();

            for (int i = 0; i < depth; ++i) {
                builder.append("----");
            }

            String str;
            if (isRoot(entry)) {
                str = "Root";
            } else {
                str = entry.toString();
            }

            builder.append(str);
            builder.append('\n');
        }

        return builder.toString();
    }

    /**
     * Iterator for trees. Notice that next() returns a T, not
     * Node<T>. Tree is traversed depth-first (first) and in order of
     * insertion (FIFO) (second). The final node is the Root.
     *
     * Iterating while maintianing knowledge of depth requires calling getDepth()
     * after every call to next.
     *
     */
    public class TreeIterator implements Iterator<T> {
    
        /* Last node returned */
        protected Node<T> currentNode = null;

        /* Iterator from which last node was drawn */
        protected Node<T>.NodeIterator currentItr = null;

        /* track depth so clients know when level is changed */
        protected int depth = 0; 

        /**
         * Start new iterator. Try not to change tree structure
         * while iterator is underway, although some concurrent
         * manipulaitons will work.
         */
        public TreeIterator() {
            currentNode = getRoot();
            currentItr = currentNode.iterator();
        }
    
        /**
         * Implements iterator interface function. 
         * Returns whether this iterator has more
         * items to provide.
         */
        public boolean hasNext() {
            return !(isRoot(getSuccessor(currentNode)) && isRoot(currentNode) 
                && (currentItr == null || !currentItr.hasNext()));
        }
        
        /**
         * Returns the data object of type T of the next node to be traversed.
         * Traversal order is depth-first and FIFO second.
         */
        public T next() {
            if (!hasNext()) {
                return null; //TODO should throw exception?
            }

            Node<T> nextNode;

            if (currentNode.hasChildren()) {
                // Next is first child of current
                currentItr = currentNode.iterator();
                assert currentItr.hasNext();

                nextNode = currentItr.next();
                currentNode = nextNode;

                depth += 1;

            } else if (currentItr != null && currentItr.hasNext()) {
                // currentNode is leaf; return sibbling
                nextNode = currentItr.next();

            } else {
                // next node is higher in tree

                // we need to know by how many levels we popped
                int[] depthDelta = new int[1];
                depthDelta[0] = 0; 

                nextNode = getSuccessor(currentNode, depthDelta);
                currentNode = nextNode;

                // will be made non-null on next iteration if necessary
                currentItr = null; 

                depth += depthDelta[0]; 
            }

            return nextNode.getData();
        }

        /**
         * Not currently supported. 
         * Could be implemented, just is not at this time.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Return the depth of the iterator's position
         * currently. Use to track the depth of the iterator
         * in the tree. Should be the same as the number of parents
         * of the last returned node, including the root and the current
         * node. That description is kind of a guess, however...if you need
         * to be precise you should test yourself and update this accordingly.
         */  
        public int getDepth() {
            return depth;
        }
    }
}
