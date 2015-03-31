// RegisterNumberVisitor.java, created Jun 15, 2003 2:00:45 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Compiler.Quad;

import joeq.Compiler.Quad.Operand.RegisterOperand;
import jwutil.collections.IndexMap;

/**
 * RegisterNumberVisitor
 * 
 * @author John Whaley
 * @version $Id: RegisterNumberVisitor.java,v 1.4 2004/09/22 22:17:26 joewhaley Exp $
 */
public class RegisterNumberVisitor extends QuadVisitor.EmptyVisitor {

    IndexMap m = new IndexMap("Register numbers");

    public void visitQuad(Quad q) {
        for (RegisterOperand ro : q.getDefinedRegisters()) {
            m.get(ro.getRegister());
        }
        for (RegisterOperand ro : q.getUsedRegisters()) {
            m.get(ro.getRegister());
        }
    }

}
