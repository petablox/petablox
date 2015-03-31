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

/** Regexp subclass that handles *, + and ? processing.  The 0 or
  more, 1 or more, 0 or 1 processing is applied to another regexp,
  which is the child of this one. */
class Multi extends Regexp {
    Regexp child;
    int kind;

    Multi(Regexp prev, Regexp child, int kind) {
	super(prev);
	this.child = child;
	child.next = success;
	this.kind = kind;
    }

    Regexp advance(State state) {
	int count = 0;
	int offset = state.offset;

	switch (kind) {
	  case '*':
	    /* 1 or more matches */
	  case '+':
	    /* 0 or more matches */
	    while (child.match(state))
		count += 1;
	    if (kind == '+' && count == 0)
		return null;
	    break;

	  case '?':
	    /* zero or one matches */
	    child.match(state);
	    break;
	}
	if (debug)
	    System.out.println("Multi " + this.toStringThis()
			       + " advances from " + offset
			       + " to " + state.offset);
	while (state.offset > offset) {
	    if (next.match(state)) {
		if (debug)
		    System.out.println("Multi succeeds at " + state.offset);
		return success;
	    }
	    child.backup(state);
	    if (debug)
		System.out.println("Multi backs up to " + state.offset);
	}
	/* If we're here, we matched 0 times.  That's OK if we're
	   STAR or QUESTION, but not PLUS. */
	return (kind != '+') ? next : null;
    }

    boolean canStar() {
	return false;
    }

    public String toStringThis() {
	return child.toString() + new Character((char) kind);
    }
}
