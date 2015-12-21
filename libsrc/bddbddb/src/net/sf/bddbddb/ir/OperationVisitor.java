// OperationVisitor.java, created Jul 3, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor;
import net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor;
import net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor;

/**
 * OperationVisitor
 * 
 * @author John Whaley
 * @version $Id: OperationVisitor.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public interface OperationVisitor extends HighLevelOperationVisitor, LowLevelOperationVisitor, DynamicOperationVisitor {
}