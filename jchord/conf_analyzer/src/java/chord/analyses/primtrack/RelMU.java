/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.primtrack;

import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * Relation containing each tuple (m,v) such that method m
 * declares local variable v, that is, v is either an
 * argument or temporary variable of m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MU",
	sign = "M0,U0:M0_U0"
)
public class RelMU extends ProgramRel {
	public void fill() {
		DomU domU = (DomU) doms[1];
		for (Register v : domU) {
			jq_Method m = domU.getMethod(v);
			add(m, v);
		}
	}
}
