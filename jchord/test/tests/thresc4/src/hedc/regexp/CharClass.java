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

/** Regexp subclass that handles re's of the form [a-z], otherwise
    known as character classes. */
class CharClass extends Regexp {
    static final int	NCHARS = 256;
    static final int	SHIFT = 5;
    static final int	MASK = 31;

    static CharClass sClass = new CharClass(null, "[ \t\n\r]");
    static CharClass wClass = new CharClass(null, "[a-zA-Z0-9_]");
    static CharClass dClass = new CharClass(null, "[0-9]");

    /** Return a copy of a premade character class. */
    static CharClass cloneCharClass(Regexp prev, int kind) {
	CharClass cc;

	switch (kind) {
	  case 's':
	  case 'S':
	    cc = sClass;
	    break;

	  case 'w':
	  case 'W':
	    cc = wClass;
	    break;

	  case 'd':
	  case 'D':
	    cc = dClass;
	    break;

	  default:
	    throw new MalformedRegexpException("Internal exception");
	}
	cc = new CharClass(cc);
	if (Character.isUpperCase((char) kind))
	    cc.in = false;
	cc.prev = prev;
	if (prev != null)
	    prev.next = cc;
	return cc;
    }

    /** characters to match */
    int	bits[];
    
    /** boolean indicating whether characters are in the set or out of
        the set */
    boolean in = true;

    CharClass(CharClass orig) {
	super(null);
	bits = orig.bits;
	in = orig.in;
    }

    CharClass(Regexp prev, String spec) {
	super(prev);
	bits = new int[NCHARS / 32];
	process(spec);
    }

    final boolean charInClass(int c) {
	return (bits[c >> SHIFT] & (1 << (c & MASK))) != 0;
    }

    /* add a single character to this class */
    final void addChar(int c) {
	bits[c >> SHIFT] |= (1 << (c & MASK));
    }

    /* add characters ranging from c0 to c1 inclusive */
    final void addChars(int c0, int c1) {
	if (c0 > c1) {
	    int tmp = c0;
	    c0 = c1;
	    c1 = tmp;
	}
	while (c0 <= c1)
	    addChar(c0++);
    }

    /** Includes all characters from other in this class. */
    final void merge(CharClass other, boolean invert) {
	for (int i = 0; i < bits.length; i++) {
	    int otherbits = other.bits[i];

	    if (invert)
		otherbits = ~otherbits;
	    bits[i] |= otherbits;
	}
    }	

    void process(String spec) {
	int i = 1;
	int limit = spec.length() - 1;
	int c;

	if (spec.charAt(i) == '^') {
	    i += 1;
	    in = false;
	}
	while (i < limit) {
	    switch (c = spec.charAt(i++)) {
	      case '\\':
		switch(c = spec.charAt(i++)) {
		  case 'w':
		  case 'W':
		    merge(wClass, c == 'W');
		    continue;

		  case 's':
		  case 'S':
		    merge(sClass, c == 'S');
		    continue;

		  case 'd':
		  case 'D':
		    merge(dClass, c == 'D');
		    continue;

		  case 'n':
		    c = '\n';
		    break;

		  case 'r':
		    c = '\r';
		    break;

		  case 'f':
		    c = '\f';
		    break;

		  case 't':
		    c = '\t';
		    break;

		  case 'b':
		    c = '\b';
		    break;

		  default:
		    break;
		}
		/* falls through */

	      default:
		addChar(c);
		break;

	      case '-':
		if (i < limit)
		    addChars(spec.charAt(i - 2), spec.charAt(i++));
		else
		    addChar('-');
		break;
	    }
	}
    }

    Regexp advance(State state) {
	int c = state.getchar();

	if (charInClass(c) == in) {
	    state.offset += 1;
	    return next;
	}
	return null;
    }

    final String ppChar(int c) {
	String str;

	switch (c) {
	  case '\n':
	    str = "\\n";
	    break;

	  case '\r':
	    str = "\\r";
	    break;

	  case '\t':
	    str = "\\t";
	    break;

	  default:
	    if (c < ' ')
		str = "^" + new Character((char) (c + '@'));
	    else
		str = String.valueOf(new Character((char) c));
	    break;
	}
	return str;
    }

    public String toStringThis() {
	StringBuffer value = new StringBuffer("[");

	if (!in)
	    value.append("^");
	for (int i = 0; i < 255; i++) {
	    if (charInClass(i)) {
		int j;
		for (j = i + 1; j < 255; j++)
		    if (!charInClass(j))
			break;
		int count = j - i;
		value.append(ppChar(i));
		switch (count) {
		  case 1:
		    break;

		  default:
		  case 3:
		    value.append('-');

		  case 2:
		    value.append(ppChar(j - 1));
		    break;
		}
		i = j - 1;
	    }
	}
	value.append(']');
	return value.toString();
    }
}

