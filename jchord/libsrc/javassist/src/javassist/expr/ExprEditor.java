/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.expr;

import javassist.compiler.*;
import javassist.bytecode.*;
import javassist.CtClass;
import javassist.CannotCompileException;

/**
 * A translator of method bodies.
 *
 * <p>The users can define a subclass of this class to customize how to
 * modify a method body.  The overall architecture is similar to the
 * strategy pattern.
 *
 * <p>If <code>instrument()</code> is called in
 * <code>CtMethod</code>, the method body is scanned from the beginning
 * to the end.
 * Whenever an expression, such as a method call and a <tt>new</tt>
 * expression (object creation),
 * is found, <code>edit()</code> is called in <code>ExprEdit</code>.
 * <code>edit()</code> can inspect and modify the given expression.
 * The modification is reflected on the original method body.  If
 * <code>edit()</code> does nothing, the original method body is not
 * changed.
 *
 * <p>The following code is an example:
 *
 * <ul><pre>
 * CtMethod cm = ...;
 * cm.instrument(new ExprEditor() {
 *     public void edit(MethodCall m) throws CannotCompileException {
 *         if (m.getClassName().equals("Point")) {
 *             System.out.println(m.getMethodName() + " line: "
 *                                + m.getLineNumber());
 *     }
 * });
 * </pre></ul>
 *
 * <p>This code inspects all method calls appearing in the method represented
 * by <code>cm</code> and it prints the names and the line numbers of the
 * methods declared in class <code>Point</code>.  This code does not modify
 * the body of the method represented by <code>cm</code>.  If the method
 * body must be modified, call <code>replace()</code>
 * in <code>MethodCall</code>.
 *
 * @see javassist.CtClass#instrument(ExprEditor)
 * @see javassist.CtMethod#instrument(ExprEditor)
 * @see javassist.CtConstructor#instrument(ExprEditor)
 * @see MethodCall
 * @see NewExpr
 * @see FieldAccess
 *
 * @see javassist.CodeConverter
 */
public class ExprEditor {
	private int numAdded;
    /**
     * Default constructor.  It does nothing.
     */
    public ExprEditor() {}

    /**
     * Undocumented method.  Do not use; internal-use only.
     */
    public boolean doit(CtClass clazz, MethodInfo minfo)
        throws CannotCompileException
    {
        CodeAttribute codeAttr = minfo.getCodeAttribute();
        if (codeAttr == null)
            return false;

        CodeIterator iterator = codeAttr.iterator();
        boolean edited = false;
        LoopContext context = new LoopContext(codeAttr.getMaxLocals());


		numAdded = 0;
		int originalLen = iterator.getCodeLength();
        while (iterator.hasNext()) {
            if (loopBody(iterator, clazz, minfo, context)) {
                int currentLen = iterator.getCodeLength();
                numAdded = currentLen - originalLen;
                edited = true;
            } else {
                int currentLen = iterator.getCodeLength();
                assert (currentLen == numAdded + originalLen);

            }
        }

        ExceptionTable et = codeAttr.getExceptionTable();
        int n = et.size();
        for (int i = 0; i < n; ++i) {
            Handler h = new Handler(et, i, iterator, clazz, minfo);
            edit(h);
            if (h.edited()) {
                edited = true;
                context.updateMax(h.locals(), h.stack());
            }
        }

        // codeAttr might be modified by other partiess
        // so I check the current value of max-locals.
        if (codeAttr.getMaxLocals() < context.maxLocals)
            codeAttr.setMaxLocals(context.maxLocals);

        codeAttr.setMaxStack(codeAttr.getMaxStack() + context.maxStack);
        try {
            if (edited)
                minfo.rebuildStackMapIf6(clazz.getClassPool(),
                                         clazz.getClassFile2());
        }
        catch (BadBytecode b) {
            throw new CannotCompileException(b.getMessage(), b);
        }

        return edited;
    }

    /**
     * Visits each bytecode in the given range. 
     */
    boolean doit(CtClass clazz, MethodInfo minfo, LoopContext context,
                 CodeIterator iterator, int endPos)
        throws CannotCompileException
    {
        boolean edited = false;
        while (iterator.hasNext() && iterator.lookAhead() < endPos) {
            int size = iterator.getCodeLength();
            if (loopBody(iterator, clazz, minfo, context)) {
                edited = true;
                int size2 = iterator.getCodeLength();
                if (size != size2)  // the body was modified.
                    endPos += size2 - size;
            }
        }

        return edited;
    }

    final static class NewOp {
        NewOp next;
        int pos;
		int adjustedPos;
        String type;

        NewOp(NewOp n, int p, int p2, String t) {
            next = n;
            pos = p;
			adjustedPos = p2;
            type = t;
        }
    }

    final static class LoopContext {
        NewOp newList;
        int maxLocals;
        int maxStack;

        LoopContext(int locals) {
            maxLocals = locals;
            maxStack = 0;
            newList = null;
        }

        void updateMax(int locals, int stack) {
            if (maxLocals < locals)
                maxLocals = locals;

            if (maxStack < stack)
                maxStack = stack;
        }
    }

    final boolean loopBody(CodeIterator iterator, CtClass clazz,
                           MethodInfo minfo, LoopContext context)
        throws CannotCompileException
    {
        try {
			Expr expr = null;
			int pos = iterator.next();
			int c = iterator.byteAt(pos);
			String s = insertBefore(pos - numAdded);
			int numInsertedBefore;
			if (s != null) {
				CodeAttribute ca = iterator.get();
				int paramVar = ca.getMaxLocals();
				Javac jc = new Javac(clazz);
				try {
					jc.compileStmnt(s);
				} catch (CompileError ex) { throw new RuntimeException(ex); }
				Bytecode bc = jc.getBytecode();
				byte[] code = bc.get();
				numInsertedBefore = iterator.insertGap(pos, code.length);
				iterator.write(code, pos);
				iterator.insert(bc.getExceptionTable(), pos);  // TODO: check
				int maxLocals = bc.getMaxLocals();
				int maxStack = bc.getMaxStack();
				context.updateMax(maxLocals, maxStack);
			} else
				numInsertedBefore = 0;
			int newPos = pos + numInsertedBefore;
			assert (iterator.byteAt(newPos) == c);
			int adjustedPos = pos - numAdded;
			pos = newPos;
			if (c < Opcode.GETSTATIC) {  // c < 178
				if ((c >= Opcode.IALOAD  && c <= Opcode.SALOAD) ||
					(c >= Opcode.IASTORE && c <= Opcode.SASTORE)) {
					expr = new ArrayAccess(pos, adjustedPos, iterator, clazz, minfo, c);
					edit((ArrayAccess)expr);
				} else {
					/* skip */;
				}
			} else if (c < Opcode.NEWARRAY) { // c < 188
				if (c == Opcode.INVOKESTATIC
                    || c == Opcode.INVOKEINTERFACE
                    || c == Opcode.INVOKEVIRTUAL) {
                    expr = new MethodCall(pos, adjustedPos, iterator, clazz, minfo);
                    edit((MethodCall)expr);
                }
                else if (c == Opcode.GETFIELD || c == Opcode.GETSTATIC
                         || c == Opcode.PUTFIELD
                         || c == Opcode.PUTSTATIC) {
                    expr = new FieldAccess(pos, adjustedPos, iterator, clazz, minfo, c);
                    edit((FieldAccess)expr);
                }
                else if (c == Opcode.NEW) {
                    int index = iterator.u16bitAt(pos + 1);
                    context.newList = new NewOp(context.newList, pos, adjustedPos,
                                        minfo.getConstPool().getClassInfo(index));
                }
                else if (c == Opcode.INVOKESPECIAL) {
                    NewOp newList = context.newList;
                    if (newList != null
                        && minfo.getConstPool().isConstructor(newList.type,
                                            iterator.u16bitAt(pos + 1)) > 0) {
                        expr = new NewExpr(pos, newList.adjustedPos, iterator, clazz, minfo,
                                           newList.type, newList.pos);
                        edit((NewExpr)expr);
                        context.newList = newList.next;
                    }
                    else {
                        MethodCall mcall = new MethodCall(pos, adjustedPos, iterator, clazz, minfo);
                        if (mcall.getMethodName().equals(MethodInfo.nameInit)) {
                            ConstructorCall ccall = new ConstructorCall(pos, adjustedPos, iterator, clazz, minfo);
                            expr = ccall;
                            edit(ccall);
                        }
                        else {
                            expr = mcall;
                            edit(mcall);
                        }
                    }
                }
            }
			else {  // c >= 188
                if (c == Opcode.NEWARRAY || c == Opcode.ANEWARRAY
                    || c == Opcode.MULTIANEWARRAY) {
                    expr = new NewArray(pos, adjustedPos, iterator, clazz, minfo, c);
                    edit((NewArray)expr);
                }
				else if (c == Opcode.INSTANCEOF) {
                    expr = new Instanceof(pos, adjustedPos, iterator, clazz, minfo);
                    edit((Instanceof)expr);
                }
				else if (c == Opcode.CHECKCAST) {
                    expr = new Cast(pos, adjustedPos, iterator, clazz, minfo);
                    edit((Cast)expr);
				} else if (c == Opcode.MONITOREXIT) {
					expr = new MonitorExit(pos, adjustedPos, iterator, clazz, minfo);
					edit((MonitorExit)expr);
				} else if (c == Opcode.MONITORENTER) {
					expr = new MonitorEnter(pos, adjustedPos, iterator, clazz, minfo);
					edit((MonitorEnter)expr);
				}
            }

            if (expr != null && expr.edited()) {
                context.updateMax(expr.locals(), expr.stack());
                return true;
            }
            else
                return numInsertedBefore > 0;
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Edits a <tt>new</tt> expression (overridable).
     * The default implementation performs nothing.
     *
     * @param e         the <tt>new</tt> expression creating an object.
     */
    public void edit(NewExpr e) throws CannotCompileException {}

    /**
     * Edits an expression for array creation (overridable).
     * The default implementation performs nothing.
     *
     * @param a         the <tt>new</tt> expression for creating an array.
     * @throws CannotCompileException
     */
    public void edit(NewArray a) throws CannotCompileException {}

    /**
     * Edits a method call (overridable).
     *
     * The default implementation performs nothing.
     */
    public void edit(MethodCall m) throws CannotCompileException {}

    /**
     * Edits a constructor call (overridable).
     * The constructor call is either
     * <code>super()</code> or <code>this()</code>
     * included in a constructor body.
     *
     * The default implementation performs nothing.
     *
     * @see #edit(NewExpr)
     */
    public void edit(ConstructorCall c) throws CannotCompileException {}

    /**
     * Edits a field-access expression (overridable).
     * Field access means both read and write.
     * The default implementation performs nothing.
     */
    public void edit(FieldAccess f) throws CannotCompileException {}

    /**
     * Edits an instanceof expression (overridable).
     * The default implementation performs nothing.
     */
    public void edit(Instanceof i) throws CannotCompileException {}

    /**
     * Edits an expression for explicit type casting (overridable).
     * The default implementation performs nothing.
     */
    public void edit(Cast c) throws CannotCompileException {}

    /**
     * Edits a catch clause (overridable).
     * The default implementation performs nothing.
     */
    public void edit(Handler h) throws CannotCompileException {}

    public void edit(ArrayAccess a) throws CannotCompileException {}

    public void edit(MonitorEnter e) throws CannotCompileException {}

    public void edit(MonitorExit e) throws CannotCompileException {}

	public String insertBefore(int pos) {
		return null;
	}
}
