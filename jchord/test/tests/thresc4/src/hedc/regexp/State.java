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

/** State of in-progress matches. */
abstract class State {
    int offset;
    int limit;
    int pstart[];
    int pend[];

    State(int offset, int limit) {
	this.offset = offset;
	this.limit = limit;
    }

    int charsLeft() {
	return limit - offset;
    }

    abstract int getchar();

    abstract int getchar(int offset);

    abstract int indexOf(int c, int offset);

    abstract int lastIndexOf(int c, int offset);

    final int getLimit() {
	return limit;
    }

    final void ensureGroup(int n) {
	if (pstart == null || n >= pstart.length) {
	    int np[] = new int[n + 4];
	    if (pstart != null)
		System.arraycopy(pstart, 0, np, 0, pstart.length);
	    pstart = np;

	    np = new int[n + 4];
	    if (pend != null)
		System.arraycopy(pend, 0, np, 0, pend.length);
	    pend = np;
	}
    }

    final void startGroup(int n) {
	ensureGroup(n);
	pstart[n] = offset + 1;
    }

    final void endGroup(int n) {
	ensureGroup(n);
	pend[n] = offset + 1;
    }

    final void validateGroup (int n) {
	if (pstart == null || n >= pstart.length
	    || pstart[n] == 0 || pend[n] == 0)
	    throw new NoSuchMatchException(": " + n);
    }

    final void clearGroup(int n) {
	validateGroup(n);
	pstart[n] = pend[n] = 0;
    }

    abstract String getGroupString(int n);

    final int getGroupLength(int n) {
	validateGroup(n);
	return pend[n] - pstart[n];
    }

    final int getGroupStart(int n) {
	validateGroup(n);
	return pstart[n] - 1;
    }

    final int getGroupEnd(int n) {
	validateGroup(n);
	return pend[n] - 1;
    }

    public String toString() {
	return "offset = " + offset + ", limit = " + limit;
    }
}
