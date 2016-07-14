package stamp.util.tree;

import java.lang.UnsupportedOperationException;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Tree Nodes each contain a chronologically ordered
 * list of their children, a reference to their parent
 * and a data field of the type of the node. The
 * parameterized types of the Tree and Node ought to 
 * agree. This implementaiton is mildly intrusive, 
 * so you could probably make do without keeping references
 * to the tree around, although it might be useful on 
 * occasion.
 *
 * @author brycecr
 */
public class Node<T> {

    /* List of Children */
    protected ArrayList<Node<T>> children = null;

    /* Reference to parent. Can be circular or null */
    protected Node<T> parent = null;

    /* The data stored by this node */
    protected T data = null;

    /**
     * Initialize everything to null 
     */
    public Node() {
    }

    /**
     * Initialize node with data id
     */
    public Node(T id) {
        data = id;
    }

    /**
     * Initialize node with data id
     * and a pre-made ArrayList of children.
     * Doesn't do any type checking off the bat, 
     * so be careful.
     */
    public Node(T id, ArrayList<Node<T>> initchildren) {
        children = initchildren;
        data = id;
    }

    /**
     * Initialize node with data id and
     * initial parent object initparent.
     */
    public Node(T id, Node<T> initparent) {
        parent = initparent;
        data = id;
    }

    /**
     * Initialize data to id, parent to init parent,
     * and children to initchildren. Check your types well.
     */
    public Node(T id, Node<T> initparent, ArrayList<Node<T>> initchildren) {
        parent = initparent;
        children = initchildren;
        data = id;
    }

    /**
     * @return parent node of this node. May be null.
     */
    public Node<T> getParent() {
        return parent;
    }

    /**
     * @param newParent to set as parent of this node
     */
    protected void setParent(Node<T> newParent) {
        parent = newParent;
    }

    /**
     * Creates a new node with data replacement
     * removes toReplace from the children of this
     * node, inserts the new node in its place,
     * and adds toReplace to the children of the new node.
     */
    public void replaceChild(Node<T> toReplace, T replacement) {
        Node<T> newNode = new Node<T>(replacement, this);

        if (children == null) {
            return;
        }

        int ind = children.indexOf(toReplace);
        if (ind == -1) {
            // did not find toReplace in the children of this
            return; // TODO throw exception?
        }

        // insert replacement in exact place of toReplace
        children.set(ind, newNode);

        // add toReplace to the children of newNode
        newNode.addChild(toReplace);
        toReplace.setParent(newNode);
    }

    /**
     * @return ArrayList of children of this node
     */
    public ArrayList<Node<T>> getChildren() {
        return children;
    }

    /**
     * Adds newchild to the end of the children
     * List. Creates now ArrayList if necessary.
     *
     * @param newchild the node to insert
     * @return the node just inserted
     */
    public Node<T> addChild(Node<T> newchild) {
        if (children == null) {
            children = new ArrayList<Node<T>>();
        }
        children.add(newchild);
        return newchild;
    }

    /**
     * Creates new node with data newData and appends
     * this to the end of the list of children for this
     * node. Creates the list of children if necessary.
     *
     * @param newData to initialize new node with
     * @return the new node created and inserted
     */
    public Node<T> addChild(T newData) {
        Node<T> newChild = new Node<T>(newData, this);
        return addChild(newChild);
    }

    /**
     * Set the null parent of this to newparent.
     * @throws UnsupportedOperationException if current parent is not null
     */
    public void addParent(Node<T> newparent) throws UnsupportedOperationException {
        if (parent == null) {
            parent = newparent;
        } else {
            throw new UnsupportedOperationException("Tree: Adding new "
                    + "parent to Node with non-null parent.");
        }
    }

    /**
     * Returns the data object of this node
     */
    public T getData() {
        return data;
    }

    /**
     * @return truth of whether this node has any children
     */
    public boolean hasChildren() {
        return !(children == null || children.size() == 0);
    }

    /**
     * @return new NodeIterator over the children of this node
     */
    public NodeIterator iterator() {
        return new NodeIterator();
    }

    /**
     * Just a shell on an arraylist iterator through the
     * ArrayList of children
     */
    class NodeIterator implements Iterator<Node<T>> {
        Iterator<Node<T>> itr = null;
    
        /**
         * Initialize iterator. Your children better
         * not be null or this will throw a NullPointerException
         */
        public NodeIterator() {
            itr = children.iterator();
        }

        /**
         * @return if more children to iterate through
         */
        public boolean hasNext() {
            return itr.hasNext();
        }
    
        /**
         * @return next child of this node
         */
        public Node<T> next() {
            return itr.next();
        }

        /**
         * Remove the current child.
         */
        public void remove() {
            itr.remove();
        }
    }
}
