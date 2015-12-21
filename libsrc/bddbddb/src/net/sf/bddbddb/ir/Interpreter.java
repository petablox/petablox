//IR.java, created Jul 13, 2004 12:24:59 PM 2004 by mcarbin
//Copyright (C) 2004 Michael Carbin
//Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import java.util.Collection;
import java.util.Map;
import jwutil.io.SystemProperties;

/**
 * Interpreter
 * 
 * @author mcarbin
 * @version $Id: Interpreter.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public abstract class Interpreter {
    boolean TRACE = SystemProperties.getProperty("traceinterpreter") != null;
    IR ir;
    OperationInterpreter opInterpreter;
    Map/*<Relation,RelationStats>*/ relationStats;
    Map/*<IterationList,LoopStats>*/ loopStats;

    public abstract void interpret();
    public static class RelationStats {
        public int size;

        public RelationStats() {
            size = 0;
        }

        /**
         * @param that
         * @return the result of joining
         */
        public RelationStats join(RelationStats that) {
            RelationStats result = new RelationStats();
            result.size = (this.size > that.size) ? this.size : that.size;
            return result;
        }

        public RelationStats copy() {
            RelationStats result = new RelationStats();
            result.size = this.size;
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof RelationStats) {
                return this.size == ((RelationStats) o).size;
            }
            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.size;
        }
        
        public String toString() {
            return "size: " + Double.toString(size);
        }
    }
    public static class LoopStats {
        Collection/* Relation */inputRelations;
    }
}
