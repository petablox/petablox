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
 * Expression for accessing an array element
 */
public class ArrayAccess extends Expr {
    int opcode;
	CtClass arrayCompType;

    protected ArrayAccess(int pos, int adjustedPos, CodeIterator i, CtClass declaring,
                          MethodInfo m, int op) {
        super(pos, adjustedPos, i, declaring, m);
        opcode = op;
		switch (opcode) {
		case Opcode.AALOAD:
		case Opcode.AASTORE:
			try {
				arrayCompType =
					declaring.getClassPool().get(javaLangObject);
			} catch (NotFoundException e) {
					throw new RuntimeException();
			}
			break;
		case Opcode.BALOAD:
		case Opcode.BASTORE:
			arrayCompType = CtClass.byteType;
			break;
		case Opcode.SALOAD:
		case Opcode.SASTORE:
			arrayCompType = CtClass.shortType;
			break;
		case Opcode.IALOAD:
		case Opcode.IASTORE:
			arrayCompType = CtClass.intType;
			break;
		case Opcode.FALOAD:
		case Opcode.FASTORE:
			arrayCompType = CtClass.floatType;
			break;
		case Opcode.LALOAD:
		case Opcode.LASTORE:
			arrayCompType = CtClass.longType;
			break;
		case Opcode.DALOAD:
		case Opcode.DASTORE:
			arrayCompType = CtClass.doubleType;
			break;
		case Opcode.CALOAD:
		case Opcode.CASTORE:
			arrayCompType = CtClass.charType;
			break;
		default:
			throw new RuntimeException();
		}
   }

    /**
     * Returns the method or constructor containing the array-access
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
     * Returns the source file containing the array access.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
    }

    /**
     * Returns true if the array element is read.
     */
    public boolean isReader() {
        return opcode >= Opcode.IALOAD && opcode <= Opcode.SALOAD;
    }

    /**
     * Returns true if the array element is written in.
     */
    public boolean isWriter() {
        return opcode >= Opcode.IASTORE && opcode <= Opcode.SASTORE;
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
        try {
			Javac jc = new Javac(thisClass);
            Bytecode bytecode = jc.getBytecode();
			CodeAttribute ca = iterator.get();
            CtClass[] params;
            CtClass retType;
            boolean read = isReader();
            if (read) {
                params = new CtClass[1];
				params[0] = CtClass.intType;
                retType = arrayCompType;
            }
            else {
                params = new CtClass[2];
                params[0] = CtClass.intType;
				params[1] = arrayCompType;
                retType = CtClass.voidType;
            }

            int paramVar = ca.getMaxLocals();
            jc.recordParams(javaLangObject, params,
				true, paramVar, withinStatic());

            /* Is $_ included in the source code?
             */
            boolean included = checkResultValue(retType, statement);
            if (read)
                included = true;

            int retVar = jc.recordReturnType(retType, included);
            if (read) {
				jc.recordProceed(new ProceedForRead(paramVar));
			} else {
                // because $type is not the return type...
                jc.recordType(arrayCompType);
				jc.recordProceed(new ProceedForWrite(paramVar));
			}

            storeStack(params, false, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            if (included) {
                if (retType == CtClass.voidType) {
                    bytecode.addOpcode(ACONST_NULL);
                    bytecode.addAstore(retVar);
                } else {
                    bytecode.addConstZero(retType);
                    bytecode.addStore(retVar, retType);     // initialize $_
                }
			}
            jc.compileStmnt(statement);
            if (read) {
                bytecode.addLoad(retVar, retType);
			}
			final int bytecodeSize = 1;
            replace0(pos, bytecode, bytecodeSize);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

	public CtClass getElemType() {
		return arrayCompType;
	}

    /* <field type> $proceed()
     */
    class ProceedForRead implements ProceedHandler {
        int targetVar;

        ProceedForRead(int var) {
            targetVar = var;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (gen.getMethodArgsLength(args) != 1)
                throw new CompileError(Javac.proceedName
                        + "() cannot take more than one parameter "
                        + "for array element reading");

            bytecode.addAload(targetVar);
            gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
            bytecode.addOpcode(opcode);
            gen.setType(arrayCompType);
        }

        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.atMethodArgs(args, new int[1], new int[1], new String[1]);
            c.setType(arrayCompType);
        }
    }

    /* void $proceed(<field type>)
     *          the return type is not the field type but void.
     */
    class ProceedForWrite implements ProceedHandler {
        int targetVar;

        ProceedForWrite(int var) {
            targetVar = var;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (gen.getMethodArgsLength(args) != 2)
                throw new CompileError(Javac.proceedName
					+ "() cannot take more than two parameters "
					+ "for array element writing");

            bytecode.addAload(targetVar);
            gen.atMethodArgs(args, new int[2], new int[2], new String[2]);
            bytecode.addOpcode(opcode);
            gen.setType(CtClass.voidType);
            gen.addNullIfVoid();
        }

        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.atMethodArgs(args, new int[2], new int[2], new String[2]);
            c.setType(CtClass.voidType);
            c.addNullIfVoid();
        }
    }
}
