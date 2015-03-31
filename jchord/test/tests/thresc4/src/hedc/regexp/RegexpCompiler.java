/*
 * @(#)RegexpCompiler.java	1.3 96/02/02
 *
 * Copyright (c) 1995 Starwave Corporation.  All Rights Reserved.
 *
 * A perl-like regular expression compiler for java.
 *
 * @version 1.3, 02 Feb 1996
 * @author Jonathan Payne
 */

/* Regular expression compiler for java. */

package hedc.regexp;

/** Regular expression compiler. */

class CompilerState {
    String input;
    int offset;
    int groupCount = 0;
    int limit;
    boolean mapCase;
    boolean eof = false;

    CompilerState(String input, boolean mapCase) {
	this.input = input;
	this.mapCase = mapCase;
	limit = input.length();
    }

    final int nextChar() {
	if (offset < limit)
	    return input.charAt(offset++);
	eof = true;
	return -1;
    }

    final int currentChar() {
	if (eof)
	    return -1;
	return input.charAt(offset - 1);
    }

    final void ungetc() {
	eof = false;
	offset -= 1;
    }

    final int nextGroup() {
	return groupCount++;
    }

    final String substring(int from) {
	return input.substring(from, offset);
    }

    final boolean atEop() {
	return offset == limit;
    }

    public String toString() {
	return eof ? "EOF" : input.substring(offset);
    }
}

class RegexpCompiler {
    static Regexp compile(String expr, boolean mapCase) {
			Regexp first = new Regexp(null);
			compileAlternatives(first, null, -1);
			return first.next;
	}

    static void compileAlternatives(Regexp prev, CompilerState state, int term) {
		Regexp prev2 = new Group(prev, '(', 0);
		Regexp last = new Alternatives(prev2);
		compileAlternative(state, last);
	}

    static void compileAlternative(CompilerState state, Regexp end) {
		Regexp last = new Regexp(null);
		if (state != null)
			compileAlternatives(last, state, ')');
	}
}

