/*
 * @(#)MalformedRegexpException.java	1.3 96/01/11
 *
 * Copyright (c) 1995 Starwave Corporation.  All Rights Reserved.
 *
 * A regular expression parser/matcher for java.
 *
 * @version 1.3, 11 Jan 1996
 * @author Jonathan Payne
 */

package hedc.regexp;

public class MalformedRegexpException extends RuntimeException {
    MalformedRegexpException(String msg) {
	super(msg);
    }
}
