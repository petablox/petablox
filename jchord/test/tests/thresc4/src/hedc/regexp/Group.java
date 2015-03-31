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

/** Regexp subclass that marks a region of a match as a group.  This
    handles the \( \) constructs. */
class Group extends Regexp {
    int kind;
    int n;

    Group(Regexp prev, int kind, int n) {
	super(prev);
	this.kind = kind;
	this.n = n;
    }

    public String toStringThis() {
	return "" + (char) kind;
    }

    Regexp advance(State s) {
	if (kind == '(')
	    s.startGroup(n);
	else
	    s.endGroup(n);
	return next;
    }

    void backup(State s) {
	try {
	    s.clearGroup(n);
	} catch (NoSuchMatchException e){}
    }

    boolean canStar() {
	return false;
    }

    int firstCharacter() {
	return next != null ? next.firstCharacter() : -1;
    }
}

