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

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.*;
import javassist.compiler.ast.ASTList;

/**
 * Expression for monitorenter.
 */
public class MonitorEnter extends Expr {
    protected MonitorEnter(int pos, int adjustedPos, CodeIterator i, CtClass declaring,
                          MethodInfo m) {
        super(pos, adjustedPos, i, declaring, m);
    }

    /**
     * Returns the method or constructor containing the field-access
     * expression represented by this object.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the line number of the source line containing the
     * field access.
     *
     * @return -1       if this information is not available.
     */
    public int getLineNumber() {
        return super.getLineNumber();
    }

    /**
     * Returns the source file containing the field access.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
    }

    /**
     * Returns the list of exceptions that the expression may throw.
     * This list includes both the exceptions that the try-catch statements
     * including the expression can catch and the exceptions that
     * the throws declaration allows the method to throw.
     */
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }

    /**
     * Replaces the method call with the bytecode derived from
     * the given source text.
     *
     * <p>$0 is available even if the called method is static.
     * If the field access is writing, $_ is available but the value
     * of $_ is ignored.
     *
     * @param statement         a Java statement.
     */
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();   // to call checkModify().
        ConstPool constPool = getConstPool();
        int pos = currentPos;

        Javac jc = new Javac(thisClass);
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params = new CtClass[0];
            int paramVar = ca.getMaxLocals();
            jc.recordParams(javaLangObject, params, true, paramVar, withinStatic());

            jc.recordProceed(new Proceed(paramVar));

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, false, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            jc.compileStmnt(statement);

            replace0(pos, bytecode, 1); // TODO
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    /* <field type> $proceed()
     */
    static class Proceed implements ProceedHandler {
        int targetVar;
        Proceed(int var) {
            targetVar = var;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (args != null && !gen.isParamListName(args))
                throw new CompileError(Javac.proceedName
                        + "() cannot take a parameter for monitor enter");

            bytecode.addAload(targetVar);
			bytecode.addOpcode(Opcode.MONITORENTER);
            gen.setType(CtClass.voidType);
            // gen.addNullIfVoid();
        }

        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.setType(CtClass.voidType);
            // c.addNullIfVoid();
        }
    }
}
