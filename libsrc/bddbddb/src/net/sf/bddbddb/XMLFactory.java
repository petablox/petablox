// XMLFactory.java, created Oct 27, 2004 3:36:48 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import net.sf.bddbddb.FindBestDomainOrder.ConstraintInfoCollection;
import net.sf.bddbddb.order.OrderConstraint;
import net.sf.bddbddb.order.EpisodeCollection;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * XMLFactory
 * 
 * @author jwhaley
 * @version $Id: XMLFactory.java 450 2005-03-07 20:49:47Z cs343 $
 */
public class XMLFactory {
    
    Solver solver;
    
    XMLFactory(Solver s) {
        solver = s;
    }
    
    public InferenceRule getRule(String s) {
        return solver.getRule(Integer.parseInt(s.substring(4)));
    }
    
    public Relation getRelation(String s) {
        return (Relation) solver.nameToRelation.get(s);
    }
    
    public Object fromXML(Element e) {
        String name = e.getName();
        Object o;
        if (name.equals("trialCollections") || name.equals("episodeCollections")) {
            String fn = e.getAttributeValue("datalog");
            List results = new LinkedList();
            if (fn.equals(solver.inputFilename)) {
                for (Iterator i = e.getContent().iterator(); i.hasNext(); ) {
                    Object q = i.next();
                    if (q instanceof Element) {
                        Element e2 = (Element) q;
                        if (name.equals("trialCollection") || e2.getName().equals("episodeCollection")) {
                            results.add(fromXML(e2));
                        }
                    }
                }
            }
            o = results;
        } else if (name.equals("trialCollection") || name.equals("episodeCollection")) {
            EpisodeCollection tc = EpisodeCollection.fromXMLElement(e, solver);
            o = tc;
        } else if (name.equals("findBestOrder")) {
            ConstraintInfoCollection c = null;
            for (Iterator i = e.getContent().iterator(); i.hasNext(); ) {
                Object q = i.next();
                if (q instanceof Element) {
                    Element e2 = (Element) q;
                    if (e2.getName().equals("constraintInfoCollection")) {
                        c = (ConstraintInfoCollection) fromXML(e2);
                    }
                }
            }
            if (c == null) c = new ConstraintInfoCollection(solver);
            o = new FindBestDomainOrder(c);
        } else if (name.equals("rule")) {
            o = InferenceRule.fromXMLElement(e, solver);
        } else if (name.equals("relation")) {
            o = Relation.fromXMLElement(e, this);
        } else if (name.equals("attribute")) {
            o = Attribute.fromXMLElement(e, this);
        } else if (name.equals("variable")) {
            o = Variable.fromXMLElement(e, this);
        } else if (name.endsWith("Constraint")) {
            o = OrderConstraint.fromXMLElement(e, this);
        } else {
            throw new IllegalArgumentException("Cannot parse XML element "+name);
        }
        return o;
    }
    
    public static void dumpXML(String filename, Element e) {
        Writer w = null;
        try {
            w = new BufferedWriter(new FileWriter(filename)); 
            dumpXML(w, e);
            w.close();
        } catch (IOException x) {
            System.err.println("Error writing "+filename+": "+x);
            x.printStackTrace();
        } finally {
            try { if (w != null) w.close(); } catch (IOException _) { }
        }
    }
    
    /**
     * Dumps the given element as an XML document to the given writer.
     * 
     * @param out  output writer
     * @param e  element to write
     */
    public static void dumpXML(Writer out, Element e) throws IOException {
        Document d = new Document(e);
        XMLOutputter serializer = new XMLOutputter();
        serializer.setFormat(Format.getPrettyFormat());
        serializer.output(d, out);
    }
    
}
