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

import java.io.FileInputStream;	/* for debugging */
import java.util.Vector;

class StringState extends State {
    String s;

    StringState(String s, int offset) {
	super(offset, s.length());
	this.s = s;
    }

    int getchar() {
	return s.charAt(offset);
    }

    int getchar(int offset) {
	return s.charAt(offset);
    }

    int indexOf(int c, int offset) {
	return s.indexOf(c, offset);
    }

    int lastIndexOf(int c, int offset) {
	return s.lastIndexOf(c, offset);
    }

    public String getGroupString(int n) {
	validateGroup(n);
	return s.substring(pstart[n] - 1, pend[n] - 1);
    }
}

class CharArrayState extends State {
    char data[];

    CharArrayState(char data[], int offset, int length) {
	super(offset, length);
	this.data = data;
    }

    int getchar() {
	if (offset >= limit)
	    throw new ArrayIndexOutOfBoundsException(offset + " >= " + limit);
	return data[offset];
    }

    int getchar(int offset) {
	if (offset >= limit)
	    throw new ArrayIndexOutOfBoundsException(offset + " >= " + limit);
	return data[offset];
    }

    int indexOf(int c, int offset) {
	char data[] = this.data;
	int limit = this.limit;

	while (offset < limit) {
	    if (data[offset] == c)
		return offset;
	    offset += 1;
	}
	return -1;
    }

    int lastIndexOf(int c, int offset) {
	char data[] = this.data;

	while (offset >= 0) {
	    if (data[offset] == c)
		return offset;
	    offset -= 1;
	}
	return -1;
    }

    public String getGroupString(int n) {
	validateGroup(n);
	return String.copyValueOf(data, pstart[n] - 1,
				  pend[n] - pstart[n]);
    }
}

/** A Regexp is a piece of a regular expression.  The system is
 * designed to be at once flexible and efficient.  Therefore, very
 * little allocation is done while matching a regexp.  It's mostly all
 * done during the compilation stage.
 * 
 * Here's an example of how to use this class:
 *	import regexp.*;
 *
 *	Regexp reg = Regexp.compile("^([a-z]*) ([0-9]*)");
 *	String buffer = readFileIntoString("somefile.text");
 *	Result result;
 *	int pos = 0;
 *
 *	while ((result = reg.searchForward(buffer, pos)) != null) {
 *	    System.out.println("Matched " + result.getMatch(1)
 *	        + " and " + result.getMatch(2));
 *	    pos = result.matchEnd() + 1;
 *	}
 */

public class Regexp {
    /* The way this class works is fairly simple.  Regexp's are
     * subclassed for particular kinds of regular expressions.  They
     * are linked together in a doubly-linked list, but really only
     * the next pointer is needed at runtime (I was just being lazy at
     * compile time).  To match a regular expression, you start with
     * the first Regexp, and call the advance method.  Advance()
     * returns null on failure or "next" on success
     *
     * Some regexp subclasses are more complicated than others,
     * obviously.  Multi, which is how "*" "+" and "?" are
     * implemented, is complicated because it has to advance its child
     * regexp (the one which is being '*'d) repeatedly, and then match
     * (not advance over just one) the entire rest of the regexp.  The
     * reason it has to do that is that it has to know if the entire
     * rest of the regexp works, because if it doesn't it has to
     * backup and try again.  The alternation guy is similar: it tries
     * to "match" the first alternative (which actually continues to
     * the very end of the regexp), and if that fails, backs up and
     * tries again with the next alternative.
     *
     * The static variable "success" is used to terminate a regexp.
     * The matching code knows it has successed by whether or not it
     * sees a "success" node. */

    static final boolean debug = false;
    static Regexp success = new SuccessRegexp(null);

    public static Regexp compile(String expr) {
	return compile(expr, false);
    }

    /** Return a compiled regular expression.  A compiled expression
     * consists of a bunch of Regexp subclasses linked together in a
     * double linked list.  The only reason for prev pointers is to
     * easily handled Multi processing, where you have to splice into
     * the list sometimes.  */
    public static Regexp compile(String expr, boolean mapCase) {
	return RegexpCompiler.compile(expr, mapCase);
    }

    /** Next in re. */
    Regexp next;

    /** Previous in re (for compilation purposes only). */
    Regexp prev;

    /** Package private constructor for Regexp.  Users of this package
     * should use Regexp.compile(String expr) to create a compiled
     * regular expression, and then use match and searchForward to use
     * it.
     */
    Regexp(Regexp prev) {
	this.prev = prev;
	if (prev != null)
	    prev.next = this;
    }

    /** Returns <em>next</em> if we successfully advanced to a new
     * state, null otherwise.  This must not modify state unless it
     * advances.  To differentitate between failure and reaching the
     * end, the global Regexp <em>success</em> is used to end
     * all expressions.
     */
    Regexp advance(State state) {
	return next;
    }

    /** Backs up the state by one advance.  By default this backs up
     * by one character.  For paren back references, this might back up
     * big chunks at once.  It's up to an outer layer whether or not it
     * should even try to backup.  This exists soley for *, + and ?
     * processing.
     */
    void backup(State state) {
	state.offset -= 1;
    }

    /** Returns the single literal character that can match the
     * beginning of this Regexp.  If there is no unique first
     * character, returns -1.  This is an optimization.
     */
    int firstCharacter() {
	return -1;
    }

    /** Turns this regexp into a Multi, e.g., a *, + or ? modified
     * regexp.  For most regexp, this is simply wrapping this inside
     * a Multi, but a Literal string has a more complicated task.
     */
    Regexp makeMulti(int kind) {
	return new Multi(prev, this, kind);
    }

    /** Walks as far as it can down a regular expression, returning
     * true if it made it all the way to the end, and false otherwise.
     * When false, restores state to original state.
     */
    protected boolean match(State state) {
	Regexp reg = this;
	Regexp next;
	int offset = state.offset;

	try {
	    while ((next = reg.advance(state)) != null) {
		if (next == success)
		    return true;
		reg = next;
	    }
	} catch (StringIndexOutOfBoundsException e) {
	} catch (ArrayIndexOutOfBoundsException e) {
	}
	state.offset = offset;

	return false;
    }

    /** Indicates whether or not this type of regular expression can
     * have a *, + or ? modifier.  Most can, but ones which don't
     * advance the match offset cannot (since the * would match it
     * forever).
     */
    boolean canStar() {
	return true;
    }

    /** Returns true if the specified String is matched by this
     * regular expression.  This is not to be confused with search,
     * which looks all through the string for a match.  This just looks
     * to see if the beginning of the string matches.
     * 
     * @param data string to match
     * @return a regexp.Result on success, null on failure
     * @see Result
     */
    public Result match(String data, int offset) {
	State s = new StringState(data, offset);

	if (match(s))
	    return new Result(s);
	return null;
    }

    /** Returns true if the specified String is matched by this
     * regular expression.  This is not to be confused with search,
     * which looks all through the string for a match.  This just looks
     * to see if the beginning of the string matches.
     * 
     * @param data string to match
     * @return a regexp.Result on success, null on failure
     * @see Result
     */
    public Result match(char data[], int offset, int length) {
	State s = new CharArrayState(data, offset, length);

	if (match(s))
	    return new Result(s);
	return null;
    }

    /** Returns true if the specified String is matched anywhere by
     * this regular expression.  This is not like match, which only
     * matches at the beginning of the string.  This searches through
     * the whole string starting at the specified offset looking for a
     * match.
     * 
     * @param data string to match
     * @parem offset position to start search
     * @return a regexp.Result on success, null on failure
     * @see Result
     */
    public final Result searchForward(String data, int offset) {
	State state = new StringState(data, offset);

	return searchForward(state);
    }

    /** Returns true if the specified char array is matched anywhere by
     * this regular expression.  This is not like match, which only
     * matches at the beginning of the string.  This searches through
     * the whole string starting at the specified offset looking for a
     * match.
     * 
     * @param data string to match
     * @parem offset position to start search
     * @return a regexp.Result on success, null on failure
     * @see Result
     */
    public final Result searchForward(char data[], int offset, int length) {
	State state = new CharArrayState(data, offset, length);

	return searchForward(state);
    }

    private final Result searchForward(State state) {
	int firstc = firstCharacter();

	if (firstc != -1) {
	    /* we can do first character optimization */
	    int i;

	    while ((i = state.indexOf(firstc, state.offset)) != -1) {
		state.offset = i;
		if (match(state))
		    return new Result(state);
		state.offset = i + 1;
	    }
	} else {
	    int limit = state.getLimit();

	    while (state.offset < limit) {
		if (match(state))
		    return new Result(state);
		state.offset += 1;
	    }
	}
	return null;
    }

    /** Returns true if the specified String is matched from the
     * specified offset backward by this regular expression.  This is
     * not like match, which only matches at the beginning of the
     * string.  This searches through the whole string starting at the
     * specified offset looking for a match.
     * 
     * @param data string to match
     * @parem offset position to start search
     * @return a regexp.Result on success, null on failure
     * @see Result
     */
    public Result searchBackward(String data, int offset) {
	State state = new StringState(data, offset);

	return searchBackward(state);
    }

    /** Returns true if the specified char array is matched from the
     * specified offset backward by this regular expression.  This is
     * not like match, which only matches at the beginning of the
     * string.  This searches through the whole string starting at the
     * specified offset looking for a match.
     * 
     * @param data string to match
     * @parem offset position to start search
     * @return a regexp.Result on success, null on failure
     * @see Result
     */
    public Result searchBackward(char data[], int offset, int length) {
	State state = new CharArrayState(data, offset, length);

	return searchBackward(state);
    }

    private final Result searchBackward(State state) {
	int firstc = firstCharacter();

	if (firstc != -1) {
	    /* we can do first character optimization */
	    int i;

	    while (--state.offset >= 0 &&
		   (i = state.lastIndexOf(firstc, state.offset)) != -1) {
		state.offset = i;
		if (match(state))
		    return new Result(state);
	    }
	} else {
	    int limit = state.getLimit();

	    while (--state.offset >= 0) {
		if (match(state))
		    return new Result(state);
	    }
	}
	return null;
    }

    public final String toString() {
	if (next != null && next != success)
	    return toStringThis() + next.toString();
	return toStringThis();
    }

    public String toStringThis() {
	return this == success ? "<!>" : "";
    }
}

/** This subclass always matches.  This avoids a test in Regexp.match
    for "success". */
class SuccessRegexp extends Regexp {
    SuccessRegexp(Regexp prev) {
	super(prev);
    }

    protected boolean match(State state) {
	return true;
    }
}
