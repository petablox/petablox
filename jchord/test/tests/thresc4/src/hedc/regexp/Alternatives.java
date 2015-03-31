/*
 * @(#)Regexp.java	1.10 96/02/02
 *
 * Copyright (c) 1995 Starwave Corporation.  All Rights Reserved.
 *
 * A perl-like regular expression matcher for java.
 *
 * @version 1.10, 02 Feb 1996
 * @author Jonathan Payne
 */

/* Regular expression compiler for java. */

package hedc.regexp;

import java.util.Vector;

/** Implements alternations. */
class Alternatives extends Regexp {
    Vector alts = new Vector(2);

    Alternatives(Regexp prev) {
	super(prev);
    }

    void addAlt(Regexp alt) {
	alts.addElement(alt);
    }

    int firstCharacter() {
	int limit = alts.size();
	int firstc = -1;
	for (int i = 0; i < limit; i++) {
	    Regexp reg = (Regexp) alts.elementAt(i);
	    int c = reg.firstCharacter();
	    if (firstc == -1)
		firstc = c;
	    else if (c != firstc)
		return -1;
	}
	return firstc;
    }

    Regexp advance(State state) {
	int offset = state.offset;
	int limit = alts.size();

	for (int i = 0; i < limit; i++) {
	    Regexp reg = (Regexp) alts.elementAt(i);
	    if (reg.match(state))
		return success;
	    state.offset = offset;
	}
	return null;
    }

    public boolean canStar() {
	return false;
    }

    public String toStringThis() {
	StringBuffer buf = new StringBuffer();
	int limit = alts.size();
	for (int i = 0; i < limit; i++) {
	    buf.append(alts.elementAt(i).toString());
	    if (i < limit - 1)
		buf.append("|");
	}
	return buf.toString();
    }
}
