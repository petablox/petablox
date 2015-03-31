// jq_Method.java, created Mon Feb  5 23:23:20 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Class;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import joeq.ClassLib.ClassLibInterface;
import joeq.Compiler.BytecodeAnalysis.Bytecodes;
import joeq.Main.jq;
import joeq.UTF.Utf8;
import jwutil.collections.Pair;
import jwutil.util.Assert;
import jwutil.util.Convert;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Branch;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.BasicBlockTableOperand;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.SSA.EnterSSA;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.ICFGBuilder;

/*
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: jq_Method.java,v 1.52 2004/09/22 22:17:28 joewhaley Exp $
 */
public abstract class jq_Method extends jq_Member {
	enum SSAKind {
		NONE, PHI, NO_PHI, NO_MOVE, NO_MOVE_PHI
	};
	private static SSAKind ssaKind = SSAKind.NONE;
	private static boolean verbose;
	private static String[] scopeExcludedPrefixes = new String[0];
	private static Map<String, String> nativeCFGBuildersMap = Collections.EMPTY_MAP;

	public static void doSSA(boolean hasPhi, boolean noMove) {
		if (hasPhi) {
			ssaKind = noMove ? SSAKind.NO_MOVE_PHI : SSAKind.PHI;
		} else {
			ssaKind = noMove ? SSAKind.NO_MOVE : SSAKind.NO_PHI;
		}
	}
	public static void setVerbose() { verbose = true; }
	public static void setNativeCFGBuilders(Map<String, String> map) {
		nativeCFGBuildersMap = map;
	}
	public static void exclude(String[] a) {
		scopeExcludedPrefixes = a;
	}

    // Available after loading
    protected char max_stack;
    protected char max_locals;
    protected jq_Class[] thrown_exceptions_table;
    protected byte[] bytecode;
    protected jq_TryCatchBC[] exception_table;
    protected jq_LineNumberBC[] line_num_table;
    protected jq_LocalVarTableEntry[] localvar_table;
    protected Map codeattribMap;
    protected jq_Type[] param_types;
    protected jq_Type return_type;
    protected int param_words;
    protected String sign;

    // Available after compilation
    protected jq_CompiledCode default_compiled_version;
    
    // inherited: clazz, name, desc, access_flags, attributes
    protected jq_Method(jq_Class clazz, jq_NameAndDesc nd) {
        super(clazz, nd);
        parseMethodSignature();
    }
    
    public final void load(jq_Method that) {
        this.access_flags = that.access_flags;
        this.max_stack = that.max_stack;
        this.max_locals = that.max_locals;
        this.bytecode = that.bytecode;
        this.exception_table = that.exception_table;
        this.line_num_table = that.line_num_table;
        this.codeattribMap = that.codeattribMap;
        this.attributes = new HashMap();
        state = STATE_LOADED;
    }
    
    public final void load(char access_flags, char max_stack, char max_locals, byte[] bytecode,
                           jq_TryCatchBC[] exception_table, jq_LineNumberBC[] line_num_table,
                           Map codeattribMap) {
        this.access_flags = access_flags;
        this.max_stack = max_stack;
        this.max_locals = max_locals;
        this.bytecode = bytecode;
        this.exception_table = exception_table;
        this.line_num_table = line_num_table;
        this.codeattribMap = codeattribMap;
        this.attributes = new HashMap();
        state = STATE_LOADED;
        if (jq.RunningNative) {
            if (this instanceof jq_Initializer) {
                ClassLibInterface.DEFAULT.initNewConstructor((java.lang.reflect.Constructor)this.getJavaLangReflectMemberObject(), (jq_Initializer)this);
            } else {
                ClassLibInterface.DEFAULT.initNewMethod((java.lang.reflect.Method)this.getJavaLangReflectMemberObject(), this);
            }
        }
    }

    public final void load(char access_flags, Map attributes) throws ClassFormatError {
        super.load(access_flags, attributes);
        parseAttributes();
    }
    
    public final void load(char access_flags, DataInput in)
    throws IOException, ClassFormatError {
        super.load(access_flags, in);
        parseAttributes();
    }
    
    private final jq_Class getResolvedClassFromCP(char cpidx) {
    if (clazz.getCPtag(cpidx) != CONSTANT_ResolvedClass)
        throw new ClassFormatError();
    jq_Type class_type = clazz.getCPasType(cpidx);
    if (!class_type.isClassType())
        throw new ClassFormatError();
    return (jq_Class)class_type;
    }

    private final void parseAttributes() throws ClassFormatError {
        // parse attributes
        byte[] a = getAttribute("Code");
        if (a != null) {
            max_stack = Convert.twoBytesToChar(a, 0);
            max_locals = Convert.twoBytesToChar(a, 2);
            int bytecode_length = Convert.fourBytesToInt(a, 4);
            if (bytecode_length <= 0)
                throw new ClassFormatError();
            bytecode = new byte[bytecode_length];
            System.arraycopy(a, 8, bytecode, 0, bytecode_length);
            int ex_table_length = Convert.twoBytesToChar(a, 8+bytecode_length);
            exception_table = new jq_TryCatchBC[ex_table_length];
            int idx = 10+bytecode_length;
            for (int i=0; i<ex_table_length; ++i) {
                char start_pc = Convert.twoBytesToChar(a, idx);
                char end_pc = Convert.twoBytesToChar(a, idx+2);
                char handler_pc = Convert.twoBytesToChar(a, idx+4);
                char catch_cpidx = Convert.twoBytesToChar(a, idx+6);
                idx += 8;
                jq_Class catch_class = null;
                if (catch_cpidx != 0) {
            catch_class = getResolvedClassFromCP(catch_cpidx);
                }
                exception_table[i] = new jq_TryCatchBC(start_pc, end_pc, handler_pc, catch_class);
            }
            int attrib_count = Convert.twoBytesToChar(a, idx);
            codeattribMap = new HashMap();
            idx += 2;
            for (int i=0; i<attrib_count; ++i) {
                char name_index = Convert.twoBytesToChar(a, idx);
                if (clazz.getCPtag(name_index) != CONSTANT_Utf8)
                    throw new ClassFormatError();
                Utf8 attribute_desc = (Utf8)clazz.getCPasUtf8(name_index);
                int attribute_length = Convert.fourBytesToInt(a, idx+2);
                // todo: maybe we only want to parse attributes we care about...
                byte[] attribute_data = new byte[attribute_length];
                System.arraycopy(a, idx+6, attribute_data, 0, attribute_length);
                codeattribMap.put(attribute_desc, attribute_data);
                idx += 6 + attribute_length;
            }
            if (idx != a.length) {
                throw new ClassFormatError();
            }
            a = getCodeAttribute(Utf8.get("LineNumberTable"));
            if (a != null) {
                char num_of_line_attribs = Convert.twoBytesToChar(a, 0);
                if (a.length != (num_of_line_attribs*4+2))
                    throw new ClassFormatError();
                this.line_num_table = new jq_LineNumberBC[num_of_line_attribs];
                for (int i=0; i<num_of_line_attribs; ++i) {
                    char start_pc = Convert.twoBytesToChar(a, i*4+2);
                    char line_number = Convert.twoBytesToChar(a, i*4+4);
                    this.line_num_table[i] = new jq_LineNumberBC(start_pc, line_number);
                }
                Arrays.sort(this.line_num_table);
            } else {
                this.line_num_table = new jq_LineNumberBC[0];
            }
            a = getCodeAttribute(Utf8.get("LocalVariableTable"));
            if (a != null) {
                char num_of_local_vars = Convert.twoBytesToChar(a, 0);
                if (a.length != (num_of_local_vars*10+2))
                    throw new ClassFormatError();

                this.localvar_table = new jq_LocalVarTableEntry[num_of_local_vars];
                for (int i=0; i<num_of_local_vars; ++i) {
                    char start_pc = Convert.twoBytesToChar(a, i*10+2);
                    char length = Convert.twoBytesToChar(a, i*10+4);
                    char name_index = Convert.twoBytesToChar(a, i*10+6);
                    if (clazz.getCPtag(name_index) != CONSTANT_Utf8)
                        throw new ClassFormatError();
                    char desc_index = Convert.twoBytesToChar(a, i*10+8);
                    if (clazz.getCPtag(desc_index) != CONSTANT_Utf8)
                        throw new ClassFormatError();
                    char index = Convert.twoBytesToChar(a, i*10+10);
                    this.localvar_table[i] = new jq_LocalVarTableEntry(start_pc, length, new jq_NameAndDesc(clazz.getCPasUtf8(name_index), clazz.getCPasUtf8(desc_index)), index);
                }
                Arrays.sort(this.localvar_table);
            }
        } else {
            if (!isNative() && !isAbstract())
                throw new ClassFormatError();
        }
        // read thrown Exceptions
        a = getAttribute("Exceptions");
        if (a != null) {
            char number_of_thrown_exceptions = Convert.twoBytesToChar(a, 0);
        thrown_exceptions_table = new jq_Class[number_of_thrown_exceptions];
        for (int i = 0; i < number_of_thrown_exceptions; i++) {
        char cpidx = Convert.twoBytesToChar(a, 2 + 2*i);
        thrown_exceptions_table[i] = getResolvedClassFromCP(cpidx);
        }
    }
        state = STATE_LOADED;
        if (jq.RunningNative) {
            if (this instanceof jq_Initializer) {
                ClassLibInterface.DEFAULT.initNewConstructor((java.lang.reflect.Constructor)this.getJavaLangReflectMemberObject(), (jq_Initializer)this);
            } else {
                ClassLibInterface.DEFAULT.initNewMethod((java.lang.reflect.Method)this.getJavaLangReflectMemberObject(), this);
            }
        }
    }

    public Bytecodes.CodeException[] getExceptionTable(Bytecodes.InstructionList il) {
        Bytecodes.CodeException[] ex_table = new Bytecodes.CodeException[exception_table.length];
        for (int i=0; i<ex_table.length; ++i) {
            ex_table[i] = new Bytecodes.CodeException(il, bytecode, exception_table[i]);
        }
        return ex_table;
    }
    
	public jq_LocalVarTableEntry[] getLocalTable() {
		return localvar_table;
	}
    public Bytecodes.LineNumber[] getLineNumberTable(Bytecodes.InstructionList il) {
        Bytecodes.LineNumber[] line_num = new Bytecodes.LineNumber[line_num_table.length];
        for (int i=0; i<line_num.length; ++i) {
            line_num[i] = new Bytecodes.LineNumber(il, line_num_table[i]);
        }
        return line_num;
    }
    
    public void setCode(Bytecodes.InstructionList il,
                       Bytecodes.CodeException[] ex_table,
                       Bytecodes.LineNumber[] line_num,
                       jq_ConstantPool.ConstantPoolRebuilder cpr) {
        cpr.resetIndices(il);
        bytecode = il.getByteCode();
        if (exception_table.length != ex_table.length)
            exception_table = new jq_TryCatchBC[ex_table.length];
        // TODO: recalculate max_stack and max_locals.
        for (int i=0; i<ex_table.length; ++i) {
            exception_table[i] = ex_table[i].finish();
        }
        if (line_num_table.length != line_num.length)
            line_num_table = new jq_LineNumberBC[line_num.length];
        for (int i=0; i<line_num.length; ++i) {
            line_num_table[i] = line_num[i].finish();
        }
    }
    
    public void update(jq_ConstantPool.ConstantPoolRebuilder cpr) {
        if (bytecode != null) {
            Bytecodes.InstructionList il = new Bytecodes.InstructionList(getDeclaringClass().getCP(), bytecode);
            Bytecodes.CodeException[] ex_table = getExceptionTable(il);
            Bytecodes.LineNumber[] line_num = getLineNumberTable(il);
            setCode(il, ex_table, line_num, cpr);
        }
    }
    
    public void remakeCodeAttribute(jq_ConstantPool.ConstantPoolRebuilder cpr) {
        if (bytecode != null) {
            // TODO: include line number table.
            int size = 8 + bytecode.length + 2 + 8*exception_table.length + 2;
            byte[] code = new byte[size];
            Convert.charToTwoBytes(max_stack, code, 0);
            Convert.charToTwoBytes(max_locals, code, 2);
            Convert.intToFourBytes(bytecode.length, code, 4);
            System.arraycopy(bytecode, 0, code, 8, bytecode.length);
            Convert.charToTwoBytes((char)exception_table.length, code, 8+bytecode.length);
            int idx = 10+bytecode.length;
            for (int i=0; i<exception_table.length; ++i) {
                Convert.charToTwoBytes(exception_table[i].getStartPC(), code, idx);
                Convert.charToTwoBytes(exception_table[i].getEndPC(), code, idx+2);
                Convert.charToTwoBytes(exception_table[i].getHandlerPC(), code, idx+4);
                char c;
                if (exception_table[i].getExceptionType() == null) c = (char) 0;
                else c = cpr.get(exception_table[i].getExceptionType());
                Convert.charToTwoBytes(c, code, idx+6);
                idx += 8;
            }
            char attrib_count = (char)0; // TODO: code attributes
            // TODO: LocalVariableTable
            Convert.charToTwoBytes(attrib_count, code, idx);
            attributes.put(Utf8.get("Code"), code);
        }
    }
    
    public void dumpAttributes(DataOutput out, jq_ConstantPool.ConstantPoolRebuilder cpr) throws IOException {
        update(cpr);
        remakeCodeAttribute(cpr);
        // TODO: Exceptions
        super.dumpAttributes(out, cpr);
    }

    public abstract void prepare();

    static interface Delegate {
        jq_CompiledCode compile_stub(jq_Method m);
        jq_CompiledCode compile(jq_Method m);
    }

    private static Delegate _delegate;

    public final jq_CompiledCode compile_stub() {
        chkState(STATE_PREPARED);
        if (state >= STATE_SFINITIALIZED) return default_compiled_version;
        if (jq.DontCompile) {
            state = STATE_SFINITIALIZED;
            return default_compiled_version = new jq_CompiledCode(this, null, 0, null, null, null, null, 0, null, null);
        }
        if (_compile.getState() < STATE_CLSINITIALIZED) _compile.compile();
        default_compiled_version = _delegate.compile_stub(this);
        state = STATE_SFINITIALIZED;
        return default_compiled_version;
    }
    public final jq_CompiledCode compile() {
        if (state == STATE_CLSINITIALIZED) return default_compiled_version;
        synchronized (this) {
            Assert._assert(!jq.DontCompile);
            chkState(STATE_PREPARED);
            default_compiled_version = _delegate.compile(this);
            state = STATE_CLSINITIALIZED;
        }
        return default_compiled_version;
    }
    
    public final void setDefaultCompiledVersion(jq_CompiledCode cc) {
        default_compiled_version = cc;
    }
    
    public final int getReturnWords() {
        if (return_type == jq_Primitive.VOID) return 0;
        if (return_type == jq_Primitive.LONG ||
            return_type == jq_Primitive.DOUBLE) return 2;
        return 1;
    }
    
    protected abstract void parseMethodSignature();

    public final void unsynchronize() {
        chkState(STATE_LOADING2);
        access_flags &= ~ACC_SYNCHRONIZED;
    }
    public final boolean isSynchronized() { return checkAccessFlag(ACC_SYNCHRONIZED); }
    public final boolean isNative() { return checkAccessFlag(ACC_NATIVE); }
    public final boolean isAbstract() { return checkAccessFlag(ACC_ABSTRACT); }
    public final boolean isStrict() { return checkAccessFlag(ACC_STRICT); }
    public final jq_CompiledCode getDefaultCompiledVersion() { chkState(STATE_SFINITIALIZED); return default_compiled_version; }
    public char getMaxStack() {
        chkState(STATE_LOADED);
        Assert._assert(getBytecode() != null);
        return max_stack;
    }
    public void setMaxStack(char m) {
        this.max_stack = m;
    }
    public char getMaxLocals() {
        chkState(STATE_LOADED);
        Assert._assert(getBytecode() != null);
        return max_locals;
    }
    public void setMaxLocals(char m) {
        this.max_locals = m;
    }
    public byte[] getBytecode() {
        chkState(STATE_LOADED);
        return bytecode;
    }
    public jq_TryCatchBC[] getExceptionTable() {
        chkState(STATE_LOADED);
        Assert._assert(getBytecode() != null);
        return exception_table;
    }
    public jq_Class[] getThrownExceptionsTable() { 
        chkState(STATE_LOADED);
    return thrown_exceptions_table; 
    }
    public jq_LocalVarTableEntry getLocalVarTableEntry(int bci, int index) {
        if (localvar_table == null)
            return null;
        int inspoint = Arrays.binarySearch(localvar_table, new jq_LocalVarTableEntry((char)bci, (char)index));
        if (inspoint >= 0)
            return localvar_table[inspoint];
        inspoint = -inspoint-2;
        if(inspoint >= 0 && localvar_table[inspoint].isInRange(bci, index))
            return localvar_table[inspoint];
        return null;
    }
    public int getLineNumber(int bci) {
        // todo: binary search
        if (line_num_table == null)
            return -1;
        for (int i=line_num_table.length-1; i>=0; --i) {
            if (bci >= line_num_table[i].getStartPC()) return line_num_table[i].getLineNum();
        }
        return -1;
    }
    public jq_LineNumberBC[] getLineNumberTable() { return line_num_table; }
    public jq_Type[] getParamTypes() { return param_types; }
    public int getParamWords() { return param_words; }
    public final jq_Type getReturnType() { return return_type; }
    public byte[] getCodeAttribute(Utf8 a) { chkState(STATE_LOADING2); return (byte[])codeattribMap.get(a); }
    public final byte[] getCodeAttribute(String name) { return getCodeAttribute(Utf8.get(name)); }

    public jq_LineNumberBC getLineNumber(char linenum) {
        // todo: binary search
        jq_LineNumberBC[] ln = getLineNumberTable();
        if (ln == null)
            return null;
        for (int i = 0; i < ln.length; ++i) {
            if (ln[i].getLineNum() == linenum)
                return ln[i];
        }
        return null;
    }

    public void accept(jq_MethodVisitor mv) {
        mv.visitMethod(this);
    }
    
    public String toString() {
        return getName() + ":" + getDesc() + "@" + getDeclaringClass();
	}

    public boolean isBodyLoaded() {
        return getBytecode()!=null;
    }
    
    public static final jq_Class _class;
    public static final jq_InstanceMethod _compile;
    public static final jq_InstanceField _default_compiled_version;
    static {
        _class = (jq_Class)PrimordialClassLoader.loader.getOrCreateBSType("Ljoeq/Class/jq_Method;");
        _compile = _class.getOrCreateInstanceMethod("compile", "()Ljoeq/Class/jq_CompiledCode;");
        _default_compiled_version = _class.getOrCreateInstanceField("default_compiled_version", "Ljoeq/Class/jq_CompiledCode;");
        /* Set up delegates. */
        _delegate = null;
        boolean nullVM = jq.nullVM;
        if (!nullVM) {
            _delegate = attemptDelegate("joeq.Class.Delegates$Method");
        }
        if (_delegate == null) {
            _delegate = new NullDelegates.Method();
        }
    }

    private static Delegate attemptDelegate(String s) {
        String type = "method delegate";
        try {
            Class c = Class.forName(s);
            return (Delegate)c.newInstance();
        } catch (java.lang.ClassNotFoundException x) {
            //System.err.println("Cannot find "+type+" "+s+": "+x);
        } catch (java.lang.InstantiationException x) {
            //System.err.println("Cannot instantiate "+type+" "+s+": "+x);
        } catch (java.lang.IllegalAccessException x) {
            //System.err.println("Cannot access "+type+" "+s+": "+x);
        }
        return null;
    }

	private void buildEmptyCFG() {
		jq_Type[] argTypes = getParamTypes();
		int n = argTypes.length;
		RegisterFactory rf = new RegisterFactory(0, n);
		for (int i = 0; i < n; i++) {
			jq_Type t = argTypes[i];
    		rf.getOrCreateLocal(i, t);
		}
		cfg = new ControlFlowGraph(this, 1, 0, rf);
		BasicBlock bb = cfg.createBasicBlock(1, 1, 1, null);
		Quad q;
		if (getReturnType().isReferenceType()) {
			q = Return.create(1, bb, Return.RETURN_A.INSTANCE);
			Return.setSrc(q, new AConstOperand(null));
		} else {
    		q = Return.create(1, bb, Return.RETURN_V.INSTANCE);
		}
		bb.appendQuad(q);
		BasicBlock entry = cfg.entry();
		BasicBlock exit = cfg.exit();
		bb.addPredecessor(entry);
		bb.addSuccessor(exit);
		entry.addSuccessor(bb);
		exit.addPredecessor(bb);
	}

	private ControlFlowGraph cfg;
	private Map<Quad, Integer> bcMap;
	private boolean hasBCmap;

	public boolean isExcluded() {
		String cName = getDeclaringClass().getName();
		for (String c : scopeExcludedPrefixes) {
			if (cName.startsWith(c))
				return true;
		}
		return false;
	}

	public ControlFlowGraph getCFG() {
        assert (!isAbstract());
        if (cfg != null)
			return cfg;
		String sign = getName().toString() + ":" + getDesc().toString() +
			"@" + getDeclaringClass().getName();
		String clsName = nativeCFGBuildersMap.get(sign);
		if (clsName != null) {
			Exception ex = null;
			try {
				Class cls = Class.forName(clsName);
				ICFGBuilder builder = (ICFGBuilder) cls.newInstance();
				cfg = builder.run(this);
			} catch (ClassNotFoundException e) {
				ex = e;
			} catch (IllegalAccessException e) {
				ex = e;
			} catch (InstantiationException e) {
				ex = e;
			}
			if (ex != null) {
				if (verbose) {
					System.err.println("WARN: Failed to get CFG of method " +
						this + "; setting it to no-op.  Error follows.");
					ex.printStackTrace();
				}
				buildEmptyCFG();
			}
		} else if (isNative()) {
			if (verbose) {
				System.out.println("WARN: Regarding CFG of method " + this +
					" as no-op as it is native.");
			}
			buildEmptyCFG();
		} else if (isExcluded()) {
			if (verbose)
				System.out.println("WARN: Regarding CFG of method " + this + " as no-op.");
			buildEmptyCFG();
		} else {
			try {
				cfg = CodeCache.getCode(this);
				switch (ssaKind) {
				case NONE:
					break;
				case PHI:
					(new EnterSSA()).visitCFG(cfg);
					break;
				case NO_PHI:
					(new EnterSSA()).visitCFG(cfg);
					removePhis();
					break;
				case NO_MOVE:
					(new EnterSSA()).visitCFG(cfg);
					removeMoves();
					break;
				case NO_MOVE_PHI:
					(new EnterSSA()).visitCFG(cfg);
					removeMoves();
					removePhis();
					break;
				default:
					break;
				}
				assert (cfg != null);
			} catch (Throwable ex) {
				System.err.println("WARN: Failed to get CFG of method " +
					this + "; setting it to no-op.  Error follows.");
				ex.printStackTrace();
				buildEmptyCFG();
			}
		}
        return cfg;
	}

	private void removePhis() {
		BasicBlock entryBB = cfg.entry(), entrySuccBB = null;
		for (BasicBlock bb : cfg.reversePostOrder()) {
			while (true) {
				int n = bb.size();
				if (n == 0) break;
				Quad q = bb.getQuad(0);
				if (!(q.getOperator() instanceof Phi))
					break;
				ParamListOperand ros = Phi.getSrcs(q);
				int k = ros.length();
				BasicBlockTableOperand bos = Phi.getPreds(q);
				assert (bos.size() == k);
				RegisterOperand lo = Phi.getDest(q);
				for (int i = 0; i < k; i++) {
					RegisterOperand ro = ros.get(i);
					BasicBlock bb2 = bos.get(i);
					if (bb2 == entryBB) {
						if (entrySuccBB == null) {
							int numSuccs = entryBB.getNumberOfSuccessors();
							entrySuccBB = cfg.createBasicBlock(1, numSuccs, 1, null);
						}
						bb2 = entrySuccBB;
					}
					assert (bb2 != cfg.exit());
					jq_Type t = lo.getType();
					Move mop = (t == null) ? Move.MOVE_A.INSTANCE : Move.getMoveOp(t);
					Quad q2 = Move.create(-1, bb2, mop, (RegisterOperand) lo.copy(), (ro == null) ? null : ro.copy());
					int n2 = bb2.size();
					int j = n2 - 1;
					while (j >= 0 && bb2.getQuad(j).getOperator() instanceof Branch)
						j--;
					bb2.addQuad(j + 1, q2);
				}
				bb.removeQuad(0);
			}
		}
		if (entrySuccBB != null) {
			for (BasicBlock bb : entryBB.getSuccessors()) {
				entrySuccBB.addSuccessor(bb);
				if (bb.removePredecessor(entryBB))
					bb.addPredecessor(entrySuccBB);
			}
			entryBB.removeAllSuccessors();
			entryBB.addSuccessor(entrySuccBB);
			entrySuccBB.addPredecessor(entryBB);
		}
	}

	// presumes program is in SSA form
	private void removeMoves() {
		RegisterFactory rf = cfg.getRegisterFactory();
		int numArgs = getParamTypes().length;
		List<Quad> moves = new ArrayList<Quad>();
		for (BasicBlock bb : cfg.reversePostOrder()) {
			Iterator it = bb.iterator();
			loop: while (it.hasNext()) {
				Quad q = (Quad) it.next();
				if (q.getOperator() instanceof Move && Move.getSrc(q) instanceof RegisterOperand) {
					Register l = Move.getDest(q).getRegister();
					for (int i = 0; i < numArgs; i++) {
						if (rf.get(i) == l)
							continue loop;
					}
					moves.add(q);
				}
			}
		}
		for (Quad q : moves) {
			RegisterOperand ro = (RegisterOperand) Move.getSrc(q);
			RegisterOperand lo = Move.getDest(q);
			Register r = ro.getRegister();
			Register l = lo.getRegister();
			BasicBlock bb = q.getBasicBlock();
			assert (bb.removeQuad(q));
			for (BasicBlock bb2 : cfg.reversePostOrder()) {
				Iterator it = bb2.iterator();
				while (it.hasNext()) {
					Quad q2 = (Quad) it.next();
					process(q2.getOp1(), l, r, q2);
					process(q2.getOp2(), l, r, q2);
					process(q2.getOp3(), l, r, q2);
					process(q2.getOp4(), l, r, q2);
				}
			}
		}
	}

	private void process(Operand op, Register l, Register r, Quad q) {
        if (op instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) op;
			if (ro.getRegister() == l) {
				ro.setRegister(r);
			}
        } else if (op instanceof ParamListOperand) {
            ParamListOperand ros = (ParamListOperand) op;
            int n = ros.length();
            for (int i = 0; i < n; i++) {
                RegisterOperand ro = ros.get(i);
                if (ro == null) continue;
				if (ro.getRegister() == l) {
					ro.setRegister(r);
				}
			}
		}
	}

	public Map<Quad, Integer> getBCMap() {
		if (hasBCmap)
			return bcMap;
		Map<Quad, Integer> map;
		if (getBytecode() == null)
			map = null;
		else
			map = CodeCache.getBCMap(this);
		bcMap = map;
		hasBCmap = true;
		return map;
	}

	public Quad getQuad(int bci) {
		return getQuad(bci, new Class[] { Operator.class });
	}

	public Quad getQuad(int bci, Class quadOpClass) {
		return getQuad(bci, new Class[] { quadOpClass });
	}

	public Quad getQuad(int bci, Class[] quadOpClasses) {
        Map<Quad, Integer> map = getBCMap();
        if (map != null) {
			for (Map.Entry<Quad, Integer> e : map.entrySet()) {
				Integer bci2 = e.getValue();
				if (bci2.intValue() == bci) {
					Quad q = e.getKey();
					Class c = q.getOperator().getClass();
					for (Class qc : quadOpClasses) {
						try {
							c.asSubclass(qc);
							return q;
						} catch (final ClassCastException ex) {
							// do nothing
						}
					}
				}
			}
		}
		return null;
	}

	private List<Register> liveRefVars;

	public List<Register> getLiveRefVars() {
		if (liveRefVars == null) {
			liveRefVars = new ArrayList<Register>();
			ControlFlowGraph cfg = getCFG();
			RegisterFactory rf = cfg.getRegisterFactory();
			jq_Type[] paramTypes = getParamTypes();
			int numArgs = paramTypes.length;
			for (int i = 0; i < numArgs; i++) {
				jq_Type t = paramTypes[i];
				if (t.isReferenceType()) {
					Register v = rf.get(i);
					assert (!liveRefVars.contains(v));
					liveRefVars.add(v);
				}
			}
			for (BasicBlock bb : cfg.reversePostOrder()) {
				for (Quad q : bb.getQuads()) {
					process(q.getOp1(), q);
					process(q.getOp2(), q);
					process(q.getOp3(), q);
					process(q.getOp4(), q);
				}
			}
        }
		return liveRefVars;
	}

    private void process(Operand op, Quad q) {
        if (op instanceof RegisterOperand) {
            RegisterOperand ro = (RegisterOperand) op;
            Register v = ro.getRegister();
            jq_Type t = ro.getType();
            if (t == null || t.isReferenceType()) {
				if (!liveRefVars.contains(v))
                	liveRefVars.add(v);
            }
        } else if (op instanceof ParamListOperand) {
            ParamListOperand ros = (ParamListOperand) op;
            int n = ros.length();
            for (int i = 0; i < n; i++) {
                RegisterOperand ro = ros.get(i);
                if (ro == null) continue;
                jq_Type t = ro.getType();
                if (t == null || t.isReferenceType()) {
                    Register v = ro.getRegister();
					if (!liveRefVars.contains(v)) 
                    	liveRefVars.add(v);
                }
            }
        }
    }
    
    /* Code to return line number given a register in the method*/
    private Map<Register, List<Integer> > varToLineNumsMap = null;
    
    public List<Integer> getLineNumber(Register v) {
    	if (varToLineNumsMap == null) {
    		varToLineNumsMap = new HashMap<RegisterFactory.Register, List<Integer>>();
    		this.getLineNums();
    	}
    	return varToLineNumsMap.get(v);
    }
    
    private void getLineNums() {
		ControlFlowGraph cfg = this.getCFG();
		RegisterFactory rf = cfg.getRegisterFactory();
		jq_Type[] paramTypes = this.getParamTypes();
		int numArgs = paramTypes.length;
		for (int i = 0; i < numArgs; i++) {
				Register v = rf.get(i);
				getLocalLineNum(v,null);
		}

		for (BasicBlock bb : cfg.reversePostOrder()) {
			for (Quad q : bb.getQuads()) {
				processForLineNum(q);
			}
		}
	}

	private void processForLineNum(Quad q) {
		for(Operand op : q.getDefinedRegisters()){
			if (op instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) op;
					Register v = ro.getRegister();
					if(v.isTemp())
						getStackLineNum(v,q);
					else
						getLocalLineNum(v,q);
			} else if (op instanceof ParamListOperand) {
				ParamListOperand ros = (ParamListOperand) op;
				int n = ros.length();
				for (int i = 0; i < n; i++) {
					RegisterOperand ro = ros.get(i);
					if (ro == null)
						continue;
						Register v = ro.getRegister();
						if(v.isTemp())
							getStackLineNum(v,q);
						else
							getLocalLineNum(v,q);
				}
			}
		}
	}

	private void getLocalLineNum(Register v, Quad q){
		//System.out.println(m.getCFG().fullDump());
		int lineNum = -1;
		if(q!=null){
			int index = -1;
			Map localNum = this.getCFG().getRegisterFactory().getLocalNumberingMap();
			for(Object o: localNum.keySet()){
				if(localNum.get(o).equals(v)){
					//System.out.println(localNum.get(o) + ":" + v);
					index = (Integer) ((Pair)o).right;
					break;
				}
			}

			jq_LocalVarTableEntry localVarTableEntry = this.getLocalVarTableEntry(q.getBCI() + 1, index);
			if(localVarTableEntry==null){
				getStackLineNum(v, q);
				return;
			}
			//int qbci = q.getBCI();
			//jq_LocalVarTableEntry[] arr = m.getLocalTable();
			//for(jq_LocalVarTableEntry var: arr)
			//	System.out.println(var.toString());
			lineNum = this.getLineNumber(((int)(localVarTableEntry.getStartPC())) - 1);
		} else {
			lineNum = this.getLineNumber(0);
		}
		List<Integer> lineNums = varToLineNumsMap.get(v);
		if (lineNums==null) {
			lineNums = new ArrayList<Integer>();
			lineNums.add(lineNum);
			varToLineNumsMap.put(v, lineNums);
		} else
			lineNums.add(lineNum);
	}

	private void getStackLineNum(Register v, Quad q){
		List<Integer> lineNums = varToLineNumsMap.get(v);
		if (lineNums==null) {
			lineNums = new ArrayList<Integer>();
			lineNums.add(q.getLineNumber());
			varToLineNumsMap.put(v, lineNums);
		} else
			lineNums.add(q.getLineNumber());
	}
	
    /* Code to return the source name of a register in the method*/
    private Map<Register, List<String> > varToRegNameMap = null;
    
    public List<String> getRegName(Register v) {
    	if (varToRegNameMap == null) {
    		varToRegNameMap = new HashMap<RegisterFactory.Register, List<String>>();
    		this.getRegNames();
    	}
    	return varToRegNameMap.get(v);
    }
    
    private void getRegNames() {
		ControlFlowGraph cfg = this.getCFG();
		RegisterFactory rf = cfg.getRegisterFactory();
		jq_Type[] paramTypes = this.getParamTypes();
		int numArgs = paramTypes.length;
		for (int i = 0; i < numArgs; i++) {
				Register v = rf.get(i);
				getLocalRegName(v,null);
		}

		for (BasicBlock bb : cfg.reversePostOrder()) {
			for (Quad q : bb.getQuads()) {
				processForRegName(q);
			}
		}
	}

	private void processForRegName(Quad q) {
		for(Operand op : q.getDefinedRegisters()){
			if (op instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) op;
					Register v = ro.getRegister();
					if(v.isTemp())
						getStackRegName(v,q);
					else
						getLocalRegName(v,q);
			} else if (op instanceof ParamListOperand) {
				ParamListOperand ros = (ParamListOperand) op;
				int n = ros.length();
				for (int i = 0; i < n; i++) {
					RegisterOperand ro = ros.get(i);
					if (ro == null)
						continue;
						Register v = ro.getRegister();
						if(v.isTemp())
							getStackRegName(v,q);
						else
							getLocalRegName(v,q);
				}
			}
		}
	}

	private void getLocalRegName(Register v, Quad q){
		//System.out.println(m.getCFG().fullDump());
		String regName = "";
			int index = -1;
			Map localNum = this.getCFG().getRegisterFactory().getLocalNumberingMap();
			for(Object o: localNum.keySet()){
				if(localNum.get(o).equals(v)){
					//System.out.println(localNum.get(o) + ":" + v);
					index = (Integer) ((Pair)o).right;
					break;
				}
			}
			
			jq_LocalVarTableEntry localVarTableEntry = null;
			if(q!=null){
				localVarTableEntry = this.getLocalVarTableEntry(q.getBCI()+1, index);
			}else{
				localVarTableEntry = this.getLocalVarTableEntry(0, index);
			}
			
			if(localVarTableEntry==null){
				getStackRegName(v, q);
				return;
			}
			
			
			jq_NameAndDesc regNd = localVarTableEntry.getNameAndDesc();
			regName += regNd.getName() + ":" + regNd.getDesc();
			
			List<String> regNames = varToRegNameMap.get(v);
		if(regNames==null){
			regNames = new ArrayList<String>();
			regNames.add(regName);
			varToRegNameMap.put(v, regNames);
		}else
			regNames.add(regName);
	}

	private void getStackRegName(Register v, Quad q){
		List<String> regNames = varToRegNameMap.get(v);
		if(regNames==null){
			regNames = new ArrayList<String>();
			regNames.add(v.toString());
			varToRegNameMap.put(v, regNames);
		}else{
			//regNames.add(v.toString());
		}
	}
}
