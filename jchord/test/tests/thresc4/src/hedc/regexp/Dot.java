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

/** Regexp subclass that matches any character except \n.  So, .* will
  not move beyond a newline character. */
class Dot extends Regexp {
    public Dot(Regexp prev) {
	super(prev);
    }

    Regexp advance(State state) {
	if (state.getchar() != '\n') {
	    state.offset += 1;
	    return next;
	}
	return null;
    }

    public String toStringThis() {
	return ".";
    }
}

