package com.logicblox.unit.translator;

import java.util.List;

import com.logicblox.unit.Term;
import com.logicblox.unit.Variable;

/**
 * A simple transaction abstraction to allow for convenient code generation. 
 * 
 * @author Thiago T. Bartolomei
 */
public class Transaction {

	/**
	 * The string builder used to accumulate source code.
	 */
	protected final StringBuilder b;
	
	/**
	 * By default, the transaction is "exec".
	 *
	 * @param builder
	 */
	public Transaction(StringBuilder builder) {
		this(builder, null, "exec");
	}
		
	/**
	 * By default, the transaction is "exec".
	 *
	 * @param b
	 * @param comment
	 */
	public Transaction(StringBuilder b, String comment) {
		this(b, comment, "exec");
	}	
	
	/**
	 * Internal builder.
	 *
	 * @param comment
	 * @param kind
	 */
	public Transaction(String comment, String kind) {
		this(new StringBuilder(), comment, kind);
	}
	
	/**
	 * Creates a transaction of this kind, with this builder, starting with a comment.
	 *
	 * @param b
	 * @param comment
	 * @param kind
	 */
	public Transaction(StringBuilder b, String comment, String kind) {
		this.b = b;
		if (null != comment) comment(comment);
		b.append("transaction\n");
		if (null != kind) e(kind).e(" <doc>\n");
		code();
		if (null != kind) e("</doc>\n");
		e("commit\n\n");
	}
	
	/**
	 * Callback to let sub-types emit code for the transaction.
	 */
	public void code() {}
	
	/**
	 * Emits code.
	 *
	 * @param s
	 * @return
	 */
	public Transaction e(String s) {
		b.append(s);
		return this;
	}
	
	/**
	 * Emits an integer constant in code.
	 *
	 * @param i
	 * @return
	 */
	public Transaction e(int i) {
		b.append(i);
		return this;
	}
	
	/**
	 * Emits a line with a comment.
	 *
	 * @param s
	 * @return
	 */
	public Transaction comment(String s) {
		b.append("// ").append(s).append("\n");
		return this;
	}
	
	/**
	 * Generates a comma-separated string list.
	 *
	 * @param list
	 * @return
	 */
	public Transaction csslist(List<Object> list) {
		boolean c = false;
		for(Object o : list) {
			if (c) e(", "); else c = true;
			e("\"").e(o.toString()).e("\"");
		}
		return this;
	}
	
	/**
	 * Generates a comma-separated term list.
	 *
	 * @param list
	 * @return
	 */
	public Transaction cstlist(List<Term> list) {
		boolean c = false;
		for(Term o : list) {
			if (c) e(", "); else c = true;
			o.appendLogicBloxString(b);
		}
		return this;
	}
	
	/**
	 * Generates a comma-separated variable list.
	 *
	 * @param list
	 * @return
	 */
	public Transaction csvlist(List<Variable> list) {
		boolean c = false;
		for(Variable v : list) {
			if (c) e(", "); else c = true;
			e("?").e(v.getName());
		}
		return this;
	}
	
	/**
	 * Generates a comma-separated list of anonymous.
	 *
	 * @param arity
	 * @return
	 */
	public Transaction csAnonlist(int arity) {
		for (int i = 0; i < arity; i++) {
			if (i == 0)
				e("_");
			else
				e(", _");
		}
		return this;
	}
	/**
	 * Gets the contents of the transaction.
	 * 
	 * @return the string in the string builder. 
	 */
	public String toString() {
		return b.toString();
	}
}