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

/** Regexp subclass that matches various contexts, such as beginning
    of line, end of line, word boundary, etc. */
class ContextMatch extends Regexp {
    static CharClass word;

    static {
	word = CharClass.cloneCharClass(null, 'w');
    }
    int kind;

    public ContextMatch(Regexp prev, int kind) {
	super(prev);
	this.kind = kind;
    }

    Regexp advance(State state) {
	boolean wordLeft, wordRight;
	int offset = state.offset;

	switch (kind) {
	  case '$':
	    if (state.charsLeft() > 0 && state.getchar() != '\n')
		return null;
	    break;

	  case '^':
	    if (offset > 0 && state.getchar(offset - 1) != '\n')
		return null;
	    break;

	  case 'b':
	  case 'B':
	    wordLeft = (offset > 0 && state.charsLeft() > 0
			&& word.charInClass(state.getchar(offset - 1)));
	    wordRight = (state.charsLeft() > 0
			 && word.charInClass(state.getchar()));
	    if ((kind == 'B') != (wordLeft == wordRight))
		return null;
	    break;
		    
	  default:
//	    throw new Exception("\\"
//				+ new Character((char) kind)
//				+ ": not supported");
	    return null;
	}
	return next;
    }

    int firstCharacter() {
	if (kind == '^')
	    return next.firstCharacter();
	return -1;
    }

    void backup(State state) {}

    boolean canStar() {
	return false;
    }

    public String toStringThis() {
	String value = "" + new Character((char) kind);

	return ((kind == 'b' || kind == 'B') ? "\\" + value : value);
    }
}
