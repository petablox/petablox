// OrderConstraint.java, created Oct 22, 2004 5:22:13 PM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Comparator;

import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Variable;
import net.sf.bddbddb.XMLFactory;

import org.jdom.Element;

/**
 * OrderConstraint
 * 
 * @author jwhaley
 * @version $Id: OrderConstraint.java 435 2005-02-13 03:24:59Z cs343 $
 */
public abstract class OrderConstraint {
    
    public static final Comparator elementComparator = new Comparator() {

        public int compare(Object o1, Object o2) {
            if (o1.equals(o2)) return 0;
            return OrderConstraint.compare(o1, o2)?-1:1;
        }
        
    };
    
    static final boolean compare(Object a, Object b) {
        if (a.equals(b)) return true;
        String s1 = a.toString();
        String s2 = b.toString();
        int c = s1.compareTo(s2);
        if (c == 0) {
            if (a instanceof Attribute)
                c = ((Attribute) a).getRelation().toString().compareTo(((Attribute) b).getRelation().toString());
        }
        //Assert._assert(c != 0);
        return c <= 0;
    }
    
    public static OrderConstraint makeConstraint(int type, Object a, Object b) {
        if (compare(a, b)) {
            switch (type) {
                case 0: return new BeforeConstraint(a, b);
                case 1: return new InterleaveConstraint(a, b);
                case 2: return new AfterConstraint(a, b);
            }
        } else {
            switch (type) {
                case 0: return new BeforeConstraint(b, a);
                case 1: return new InterleaveConstraint(b, a);
                case 2: return new AfterConstraint(b, a);
            }
        }
        return null;
    }
    
    public static OrderConstraint makePrecedenceConstraint(Object a, Object b) {
        if (compare(a, b)) return new BeforeConstraint(a, b);
        else return new AfterConstraint(b, a);
    }
    
    public static OrderConstraint makeInterleaveConstraint(Object a, Object b) {
        if (compare(a, b)) return new InterleaveConstraint(a, b);
        else return new InterleaveConstraint(b, a);
    }
    
    Object a, b;
    
    protected OrderConstraint(Object a, Object b) {
        this.a = a;
        this.b = b;
    }
    
    public Object getFirst() {
        return a;
    }
    
    public Object getSecond() {
        return b;
    }
    
    public int hashCode() {
        return a.hashCode() ^ (b.hashCode() << 1);
    }
    
    public boolean equals(OrderConstraint that) {
        return this.getClass().equals(that.getClass()) &&
               this.a.equals(that.a) && this.b.equals(that.b);
    }
    
    public boolean equals(Object o) {
        if (o instanceof OrderConstraint)
            return equals((OrderConstraint) o);
        else
            return false;
    }
    
    public int compareTo(Object o) {
        return compareTo((OrderConstraint) o);
    }
    
    public int compareTo(OrderConstraint o) {
        return this.toString().compareTo(o.toString());
    }
    
    public boolean isAttributeConstraint() {
        return a instanceof Attribute;
    }
    public boolean isVariableConstraint() {
        return a instanceof Variable;
    }
    
    public boolean obeyedBy(Order o) {
        return o.getConstraints().contains(this);
    }
    
    public abstract int getType();
    public abstract boolean isOpposite(OrderConstraint that);
    public abstract OrderConstraint getOpposite1();
    public abstract OrderConstraint getOpposite2();
    public abstract Element toXMLElement(InferenceRule ir);
    
    private static Element toXMLElement(Object o, InferenceRule ir) {
        if (o instanceof Variable) {
            return ((Variable) o).toXMLElement(ir);
        } else if (o instanceof Attribute) {
            return ((Attribute) o).toXMLElement();
        } else {
            return null;
        }
    }
    
    public static OrderConstraint fromXMLElement(Element e, XMLFactory f) {
        Object a = getElement((Element) e.getContent(0), f);
        Object b = getElement((Element) e.getContent(1), f);
        if (e.getName().equals("beforeConstraint")) {
            return new BeforeConstraint(a, b);
        } else if (e.getName().equals("afterConstraint")) {
            return new AfterConstraint(a, b);
        } else if (e.getName().equals("interleaveConstraint")) {
            return new InterleaveConstraint(a, b);
        } else {
            return null;
        }
    }
    
    protected void addXMLContent(Element e, InferenceRule ir) {
        e.addContent(toXMLElement(a, ir));
        e.addContent(toXMLElement(b, ir));
    }
    
    protected static Object getElement(Element e, XMLFactory f) {
        if (e.getName().equals("variable"))
            return Variable.fromXMLElement(e, f);
        else if (e.getName().equals("attribute"))
            return Attribute.fromXMLElement(e, f);
        else
            return null;
    }
    
    public static class BeforeConstraint extends OrderConstraint {
        
        private BeforeConstraint(Object a, Object b) {
            super(a, b);
        }
        
        public String toString() {
            return a+"<"+b;
        }
        
        public int getType() { return 0; }
        
        public boolean isOpposite(OrderConstraint that) {
            if (that instanceof AfterConstraint ||
                that instanceof InterleaveConstraint) {
                return this.a.equals(that.a) && this.b.equals(that.b);
            } else {
                return false;
            }
        }
        
        public OrderConstraint getOpposite1() {
            return new AfterConstraint(a, b);
        }
        
        public OrderConstraint getOpposite2() {
            return new InterleaveConstraint(a, b);
        }
        
        public Element toXMLElement(InferenceRule ir) {
            Element e = new Element("beforeConstraint");
            addXMLContent(e, ir);
            return e;
        }
    }
    
    public static class AfterConstraint extends OrderConstraint {
        
        private AfterConstraint(Object a, Object b) {
            super(a, b);
        }
        
        public String toString() {
            return a+">"+b;
        }
        
        public int getType() { return 2; }
        
        public boolean isOpposite(OrderConstraint that) {
            if (that instanceof BeforeConstraint ||
                that instanceof InterleaveConstraint) {
                return this.a.equals(that.a) && this.b.equals(that.b);
            } else {
                return false;
            }
        }
        
        public OrderConstraint getOpposite1() {
            return new BeforeConstraint(a, b);
        }
        
        public OrderConstraint getOpposite2() {
            return new InterleaveConstraint(a, b);
        }
        
        public Element toXMLElement(InferenceRule ir) {
            Element e = new Element("afterConstraint");
            addXMLContent(e, ir);
            return e;
        }
    }
    
    public static class InterleaveConstraint extends OrderConstraint {
        
        private InterleaveConstraint(Object a, Object b) {
            super(a, b);
        }
        
        public String toString() {
            return a+"~"+b;
        }
        
        public int getType() { return 1; }
        
        public boolean isOpposite(OrderConstraint that) {
            if (that instanceof BeforeConstraint ||
                that instanceof AfterConstraint) {
                return this.a.equals(that.a) && this.b.equals(that.b);
            } else {
                return false;
            }
        }
        
        public OrderConstraint getOpposite1() {
            return new BeforeConstraint(a, b);
        }
        
        public OrderConstraint getOpposite2() {
            return new AfterConstraint(a, b);
        }
        
        public Element toXMLElement(InferenceRule ir) {
            Element e = new Element("afterConstraint");
            addXMLContent(e, ir);
            return e;
        }
    }
}
