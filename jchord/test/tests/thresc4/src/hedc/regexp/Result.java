/*
 * @(#)Result.java	1.3 96/01/12
 *
 * Copyright (c) 1995 Starwave Corporation.  All Rights Reserved.
 *
 * A regular expression parser/matcher for java.
 *
 * @version 1.3, 12 Jan 1996
 * @author Jonathan Payne
 */

package hedc.regexp;

/** A Regexp result class.  This is what's returned from a successful
    Regexp match or search operation.  This contains methods to return
    the beginning and ending positions of the match, as well as the
    actual text of the match.  It is also possible get the beginning,
    end and text of any of the submatches, as specified with the \(
    and \) notations. */
public class Result {
    State state;

    Result(State state) {
	this.state = state;
    }

    /** Returns the starting position of the nth parenthesized
      * substring match in the regular expression.  The 0th substring is
      * the entire match.
      *
      * @return the starting position of the nth substring
      * @exception NoSuchMatchException if n is out of range
      */
    public int getMatchStart(int n) {
	return state.getGroupStart(n);
    }

    /** Returns the ending position of the nth parenthesized
      * substring match in the regular expression.  The 0th substring is
      * the entire match.
      * 
      * @return the end position of the nth substring
      * @exception NoSuchMatchException if n is out of range
      */
    public int getMatchEnd(int n) {
	return state.getGroupEnd(n);
    }

    /** Returns the text of the nth parenthesized substring match in
      * the regular expression.  The 0th substring is the entire match.
      *
      * @return the text of the nth substring
      * @exception NoSuchMatchException if n is out of range
      */
    public String getMatch(int n) {
	return state.getGroupString(n);
    }

    /** Returns the starting position of the matched string.
      *
      * @return the starting position of the matched string
      */
    public int getMatchStart() {
	return getMatchStart(0);
    }

    /** Returns the ending position of the matched string.
      *
      * @return the ending position of the matched string
      */
    public int getMatchEnd() {
	return getMatchEnd(0);
    }

    /** Returns the text of the matched string.
      *
      * @return the text of the matched string
      */
    public String getMatch() {
	return getMatch(0);
    }

    public String toString() {
	return getClass().getName() + "[" + getMatchStart(0)
	    + ", " + getMatchEnd(0) + "]";
    }
}
