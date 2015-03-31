// BuildEquivalenceRelation.java, created Jul 30, 2004 12:46:08 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Iterator;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import jwutil.collections.IndexMap;
import jwutil.collections.Pair;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;

/**
 * Utility to build equivalence relations between multiple domains.
 * 
 * @author jwhaley
 * @version $Id: BuildEquivalenceRelation.java 549 2005-05-17 10:17:33Z joewhaley $
 */
public class BuildEquivalenceRelation {

    public static void main(String[] args) throws Exception {
        int n = args.length / 2;
        
        BDDDomain[] domains = new BDDDomain[n];
        long[] sizes = new long[n];
        BDDDomain targetDomain;
        
        BDDSolver s = new BDDSolver();
        DatalogParser parser = new DatalogParser(s);
        parser.readDatalogProgram(s.basedir+"fielddomains.pa");
        System.out.println("Domains: "+s.nameToDomain);
        s.loadBDDDomainInfo();
        s.setVariableOrdering();

        targetDomain = s.getBDDDomain(args[0]);
        if (targetDomain == null) throw new Exception("Invalid domain: "+args[0]);
        for (int i = 0; i < domains.length; ++i) {
            String name = args[i*2 + 1];
            domains[i] = s.getBDDDomain(name);
            if (domains[i] == null)
                throw new Exception("Invalid domain: "+args[0]);
            String size = args[i*2 + 2];
            try {
                sizes[i] = Long.parseLong(size);
            } catch (NumberFormatException x) {
                BufferedReader in = new BufferedReader(new FileReader(s.basedir+size));
                IndexMap m = IndexMap.loadStringMap("map", in);
                in.close();
                sizes[i] = m.size();
            }
        }
        
        long index = 0;
        for (int i = 0; i < domains.length; ++i) {
            int bits = Math.min(domains[i].varNum(), targetDomain.varNum());
            BDD b = domains[i].buildAdd(targetDomain, bits, index);
            b.andWith(domains[i].varRange(0, sizes[i]));
            System.out.println(domains[i]+" [0.."+sizes[i]+"] corresponds to "+targetDomain+"["+index+".."+(index+sizes[i])+"]");
            System.out.println("Result: "+b.nodeCount()+" nodes");
            bdd_save("map_"+domains[i]+"_"+targetDomain+".bdd", b, new Pair(domains[i], targetDomain));
            index += sizes[i] + 1;
        }
    }
    
    public static void bdd_save(String filename, BDD b, List ds) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filename));
            out.write('#');
            for (Iterator i = ds.iterator(); i.hasNext(); ) {
                BDDDomain d = (BDDDomain) i.next();
                out.write(' ');
                out.write(d.getName());
                out.write(':');
                out.write(Integer.toString(d.varNum()));
            }
            out.write('\n');
            for (Iterator i = ds.iterator(); i.hasNext(); ) {
                BDDDomain d = (BDDDomain) i.next();
                out.write('#');
                int[] vars = d.vars();
                for (int j = 0; j < vars.length; ++j) {
                    out.write(' ');
                    out.write(Integer.toString(vars[j]));
                }
                out.write('\n');
            }
            b.getFactory().save(out, b);
        } finally {
            if (out != null) try { out.close(); } catch (IOException _) { }
        }
    }
    
}
