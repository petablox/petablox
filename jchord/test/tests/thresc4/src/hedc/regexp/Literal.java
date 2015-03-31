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

/** Regexp subclass that matches a literal string. */
class Literal extends Regexp {
    static char caseMap[];
    static {
        caseMap = new char[128];
        int i;
        for (i = 0; i < 128; i++)
            caseMap[i] = (char) i;
        for (i = 'a'; i <= 'z'; i++)
            caseMap[i] = (char) (i + ('A' - 'a'));
    }

    char data[] = new char[0];
    int count;
    boolean mapCase = false;

    Literal(Regexp prev, int c, boolean mapCase) {
	super(prev);
	this.mapCase = mapCase;
	appendChar(c);
    }

    void appendChar(int c) {
	if (count >= data.length) {
	    char nd[] = new char[data.length + 16];

	    System.arraycopy(data, 0, nd, 0, data.length);
	    data = nd;
	}
	data[count++] = (char) c;
    }

    Regexp advance(State state) {
	int cnt = count;
	int offset, i;

	offset = state.offset;

	if (state.charsLeft() < cnt)
	    return null;
	i = 0;
	if (mapCase) {
	    while (--cnt >= 0)
		if (caseMap[data[i++]] != caseMap[state.getchar(offset++)])
		    return null;
	} else {
	    while (--cnt >= 0)
		if (data[i++] != state.getchar(offset++))
		    return null;
	}
	/* success! */
	state.offset = offset;
	return next;
    }

    /** Makes a multi out of us.  If we have 1 character, we replace
        ourselves with a Multi of one character.  If we have more than
        one, then the * only applies to the last character, so we
        strip it off, leave ourselves intact, and append the Multi to
        us.  Get it? */
    Regexp makeMulti(int kind) {
	if (count == 1)
	    return new Multi(prev, this, kind);
	else {
	    count -= 1;		/* strip off last char */	    
	    return new Multi(this, new Literal(null, data[count], mapCase),
			     kind);
	}
    }			 

    int firstCharacter() {
	if (mapCase)
	    return -1;
	return data[0];
    }

    public String toStringThis() {
	return new String(data, 0, count);
    }
}
