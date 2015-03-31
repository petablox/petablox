package chord.analyses.mantis;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import chord.instr.BasicInstrumentor;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.analyses.method.DomM;
import chord.project.Messages;
import chord.project.Config;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.expr.MethodCall;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.CtConstructor;
import javassist.CtBehavior;
import javassist.Modifier;

import gnu.trove.TIntObjectHashMap;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;

/**
 * 1. Instruments all classes not excluded by property chord.scope.exclude.
 * 2. Generates user classes Mantis0, ..., MantisN.
 * 3. Generates user class MantisPrinter and instruments the program's main method.
 * 4. Generates files [ctrl|bool|long|real]_feature_name.txt in the directory
 *    specified by property chord.mantis.data.dir.  Each of these files contains the 
 *    names of all features resulting from the corresponding instrumentation scheme
 *    defined below.
 *
 * Relevant system properties:
 * - chord.scope.exclude
 * - chord.mantis.data.dir
 * - chord.mantis.max.flds.class
 * - chord.mantis.max.flds.method
 *
 * Instrumentation is done as follows, one class at a time:
 *
 * Let <CNAME> denote the name of a class not in chord.scope.exclude.
 * Let <Mid> be the index in domain M of a method in such a class.
 * Let <Bid> be the bytecode index of an instruction in the body of such a method.
 * Let <K> be the number of the current Mantis<K> class being generated.
 *
 * 1. If the instruction is an IntIfCmp quad then the following fields are created
 *    in class Mantis<K>:
 *    public static int <C>_b<Bid>m<Mid>_bef;
 *    public static int <C>_b<Bid>m<Mid>_aft;
 *    and the following code is inserted immediately before/after the
 *    instruction respectively:
 *    Mantis<K>.<C>_b<Bid>m<Mid>_bef++;
 *    Mantis<K>.<C>_b<Bid>m<Mid>_aft++;
 * 
 * 2. If the instruction is a method call that returns a value V of type boolean
 *    then the following fields are created in class Mantis<K>:
 *    public static int <C>_b<Bid>m<Mid>_true;
 *    public static int <C>_b<Bid>m<Mid>_false;
 *    and the following code is inserted immediately after the call:
 *    if (V)
 *        Mantis<K>.<C>_b<Bid>m<Mid>_true++;
 *    else
 *        Mantis<K>.<C>_b<Bid>m<Mid>_false++;
 * 3. If the instruction is a method call that returns a value V of type byte,
 *    short, int, or long then the following fields are created in class Mantis<K>:
 *    public static long <C>_b<Bid>m<Mid>_long_sum;
 *    public static int  <C>_b<Bid>m<Mid>_long_freq;
 *    and the following code is inserted immediately after the call:
 *    Mantis<K>.<C>_b<Bid>m<Mid>_long_sum += V;
 *    Mantis<K>.<C>_b<Bid>m<Mid>_long_freq++;
 *
 * 4. If the instruction is a method call that returns a value V of type float or
 *    double then the following fields are created in class Mantis<K>:
 *    public static double <C>_b<Bid>m<Mid>_double_sum;
 *    public static int    <C>_b<Bid>m<Mid>_double_freq;
 *    and the following code is inserted immediately after the call:
 *    Mantis<K>.<C>_b<Bid>m<Mid>_double_sum += V;
 *    Mantis<K>.<C>_b<Bid>m<Mid>_double_freq++;
 *
 * ======
 *
 * Generated classes Mantis0, Mantis1, ..., MantisN are of the form:
 *
 * class Mantis<K> {
 *    // each static field below is generated during instrumentation of classes
 *    // as described above; at most chord.mantis.max.flds.class fields are
 *    // defined in each such Mantis<K> class. 
 *    public static int <C>_b<Bid>m<Mid>_bef;
 *    public static int <C>_b<Bid>m<Mid>_aft;
 *    public static int <C>_b<Bid>m<Mid>_true;
 *    public static int <C>_b<Bid>m<Mid>_false;
 *    public static long <C>_b<Bid>m<Mid>_long_sum;
 *    public static int <C>_b<Bid>m<Mid>_long_freq;
 *    public static double <C>_b<Bid>m<Mid>_double_sum;
 *    public static int <C>_b<Bid>m<Mid>_double_freq;
 * }
 *
 * ======
 *
 * Generated class MantisPrinter is of the form:
 *
 * class MantisPrinter {
 *     public static java.io.PrintWriter ctrlOut;
 *     public static java.io.PrintWriter boolOut;
 *     public static java.io.PrintWriter longOut;
 *     public static java.io.PrintWriter realOut;
 *     // each print<K> method below prints the values of at most
 *     // chord.mantis.max.flds.method static fields defined in classes
 *     // Mantis0, Mantis1, ..., MantisN to one of the four relevant
 *     // output files above.
 *     public static void print0() { ... }
 *     public static void print1() { ... }
 *     ...
 *     public static void printM() { ... }
 *     public static void done() {
 *        try { 
 *           ctrlOut = new java.io.PrintWriter(new java.io.FileWriter(...));
 *           boolOut = new java.io.PrintWriter(new java.io.FileWriter(...));
 *           longOut = new java.io.PrintWriter(new java.io.FileWriter(...));
 *           realOut = new java.io.PrintWriter(new java.io.FileWriter(...));
 *           print0();
 *           print1();
 *           ...
 *           printM();
 *           ctrlOut.close();
 *           boolOut.close();
 *           longOut.close();
 *           realOut.close();
 *        } catch (java.io.IOException ex) {
 *           ex.printStackTrace();
 *           System.exit(1);
 *        }
 *     }
 * }
 *
 * This class generates at runtime files [ctrl|bool|long|real]_feature_data.txt
 * in the current directory.  Each of these files contains feature values
 * resulting from the corresponding instrumentation scheme, in the same order as
 * that in the *_feature_name.txt files.
 *
 * ======
 *
 * Finally, a call to MantisPrinter.done() is inserted at the end of the
 * main method of the program.
 */
public class MantisInstrumentor extends BasicInstrumentor {
	private static final String dataDirName =
		System.getProperty("chord.mantis.data.dir");
    private static final int maxFldsPerMantisClass =
		Integer.getInteger("chord.mantis.max.flds.class", 8000);
    private static final int maxFldsPerMantisPrintMethod = 
		Integer.getInteger("chord.mantis.max.flds.method", 1000);
	private final DomM domM;
	// index in domain M of currently instrumented method
	private int mIdx;
	private final List<FldInfo> fldInfosList = new ArrayList<FldInfo>();
	private final TIntObjectHashMap<String> bciToInstrMap = new TIntObjectHashMap<String>();
	private String currClsNameStr;
	private CtClass[] mantisClasses;
    private int currMantisClassId;
    private int numFldsInCurrMantisClass;

    private CtClass createClass(String name) {
        return getPool().getPool().makeClass(name);
    }

	public MantisInstrumentor() {
		super(Collections.EMPTY_MAP);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
	}

    private void ensure(int nf) {
		if (mantisClasses == null)
			mantisClasses = new CtClass[2];
		else if (numFldsInCurrMantisClass + nf > maxFldsPerMantisClass) {
            int nc = mantisClasses.length;
            if (currMantisClassId == nc - 1) {
                CtClass[] newMantisClasses = new CtClass[nc * 2];
                System.arraycopy(mantisClasses, 0, newMantisClasses, 0, nc);
                mantisClasses = newMantisClasses;
            }
            ++currMantisClassId;
            numFldsInCurrMantisClass = 0;
        } else
            return;
        CtClass mantisClass = createClass("Mantis" + currMantisClassId);
        mantisClasses[currMantisClassId] = mantisClass;
    }

	@Override
	public boolean isExplicitlyExcluded(String cName) {
		if (cName.startsWith("Mantis"))
			Messages.fatal("Instrumenting Mantis class itself.");
		return super.isExplicitlyExcluded(cName);
	}

	@Override
	public CtClass edit(CtClass clazz) throws CannotCompileException {
		currClsNameStr = clazz.getName().replace('.', '_').replace('$', '_');
		return super.edit(clazz);
	}

	@Override
	public void edit(CtBehavior method) throws CannotCompileException {
        int mods = method.getModifiers();
        if (Modifier.isNative(mods) || Modifier.isAbstract(mods))
            return;
        String mName;
        if (method instanceof CtConstructor)
            mName = ((CtConstructor) method).isClassInitializer() ? "<clinit>" : "<init>";
        else
            mName = method.getName();
        String mDesc = method.getSignature();
        String cName = currentClass.getName();
        String mSign = mName + ":" + mDesc + "@" + cName;
        jq_Method meth = Program.g().getMethod(mSign);
        if (meth == null) 
            return;
		mIdx = domM.indexOf(meth);
		assert (mIdx >= 0);
		ControlFlowGraph cfg = meth.getCFG();
		bciToInstrMap.clear();
		for (BasicBlock bb : cfg.reversePostOrder()) {
			int n = bb.size();
			for (int i = 0; i < n; i++) {
				Quad q = bb.getQuad(i);
				Operator op = q.getOperator();
				if (op instanceof IntIfCmp)
					generateIntIfCmpInstr(q);
           }
		}
		super.edit(method);
	}
		
	private void generateIntIfCmpInstr(Quad q) throws CannotCompileException {
		int bci = q.getBCI();
		assert (bci >= 0);
		String fldBaseName = getBaseName(bci);
		String befFldName = fldBaseName + "_bef";
		String aftFldName = fldBaseName + "_aft";
		String javaPos = "(" + q.toJavaLocStr() + ")";
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.CTRL, mantisClassName + fldBaseName, javaPos));
		CtField befFld = CtField.make("public static int " + befFldName + ";", mantisClass);
		CtField aftFld = CtField.make("public static int " + aftFldName + ";", mantisClass);
		mantisClass.addField(befFld);
		mantisClass.addField(aftFld);
        numFldsInCurrMantisClass += 2;
		String befInstr = mantisClassName + befFldName + "++;";
		String aftInstr = mantisClassName + aftFldName + "++;";
		putInstrBefBCI(bci, befInstr);
		putInstrBefBCI(bci + 3, aftInstr);
	}

	private void putInstrBefBCI(int bci, String instr) {
        String s = bciToInstrMap.get(bci);
        bciToInstrMap.put(bci, (s == null) ? instr : instr + s);
	}

	private String getBaseName(MethodCall e) {
		int bci = e.indexOfOriginalBytecode();
		return getBaseName(bci);
	}

	private String getBaseName(int bci) {
		return currClsNameStr + "_b" + bci + "m" + mIdx;
	}

	private static String getJavaPos(MethodCall e) {
		return "(" + e.getFileName() + ":" + e.getLineNumber() + ")";
	}

	private String getBoolInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e);
		String truFldName = fldBaseName + "_true";
		String flsFldName = fldBaseName + "_false";
		String javaPos = getJavaPos(e);
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.DATA_BOOL, mantisClassName + fldBaseName, javaPos));
		CtField truFld = CtField.make("public static int " + truFldName + ";", mantisClass);
		CtField flsFld = CtField.make("public static int " + flsFldName + ";", mantisClass);
		mantisClass.addField(truFld);
		mantisClass.addField(flsFld);
        numFldsInCurrMantisClass += 2;
		String instr = "if ($_) " + mantisClassName + truFldName + "++; else " +
            mantisClassName + flsFldName + "++;";
		return instr;
	}

	private String getLongInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e) + "_long";
		String sumFldName = fldBaseName + "_sum";
		String frqFldName = fldBaseName + "_freq";
		String javaPos = getJavaPos(e);
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.DATA_LONG, mantisClassName + fldBaseName, javaPos));
		CtField sumFld = CtField.make("public static long " + sumFldName + ";", mantisClass);
		CtField frqFld = CtField.make("public static int  " + frqFldName + ";", mantisClass);
		mantisClass.addField(sumFld);
		mantisClass.addField(frqFld);
        numFldsInCurrMantisClass += 2;
		String instr = mantisClassName + sumFldName + " += $_; " +
            mantisClassName + frqFldName + "++;";
		return instr;
	}

	private String getDoubleInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e) + "_double";
		String sumFldName = fldBaseName + "_sum";
		String frqFldName = fldBaseName + "_freq";
		String javaPos = getJavaPos(e);
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.DATA_DOUBLE, mantisClassName + fldBaseName, javaPos));
		CtField sumFld = CtField.make("public static double " + sumFldName + ";", mantisClass);
		CtField frqFld = CtField.make("public static int " + frqFldName + ";", mantisClass);
		mantisClass.addField(sumFld);
		mantisClass.addField(frqFld);
        numFldsInCurrMantisClass += 2;
		String instr = mantisClassName + sumFldName + " += $_; " +
            mantisClassName + frqFldName + "++;";
		return instr;
	}

	@Override
	public String insertBefore(int pos) {
		return bciToInstrMap.get(pos);
	}

	@Override
	public void edit(MethodCall e) throws CannotCompileException {
		CtClass retType = null;
		try {
			retType = e.getMethod().getReturnType();
		} catch (NotFoundException ex) {
			throw new CannotCompileException(ex);
		}
		if (!retType.isPrimitive())
			return;
		String instr = null;
		if (retType == CtClass.booleanType)
			instr = getBoolInvkInstr(e);
		else if (retType == CtClass.floatType || retType == CtClass.doubleType)
			instr = getDoubleInvkInstr(e);
		else if (retType != CtClass.voidType)
			instr = getLongInvkInstr(e);
		if (instr != null)
			e.replace("{ $_ = $proceed($$); " + instr + " }"); 
	}

	public void done() {
        CtClass mantisPrinterClass = createClass("MantisPrinter");
        try {
            CtField ctrlOutFld = CtField.make("public static java.io.PrintWriter ctrlOut;", mantisPrinterClass);
            CtField boolOutFld = CtField.make("public static java.io.PrintWriter boolOut;", mantisPrinterClass);
            CtField longOutFld = CtField.make("public static java.io.PrintWriter longOut;", mantisPrinterClass);
            CtField realOutFld = CtField.make("public static java.io.PrintWriter realOut;", mantisPrinterClass);
            mantisPrinterClass.addField(ctrlOutFld);
            mantisPrinterClass.addField(boolOutFld);
            mantisPrinterClass.addField(longOutFld);
            mantisPrinterClass.addField(realOutFld);
        } catch (CannotCompileException ex) {
            Messages.fatal(ex);
        }

        File ctrlFeatureNameFileName = new File(dataDirName, "ctrl_feature_name.txt");
        File boolFeatureNameFileName = new File(dataDirName, "bool_feature_name.txt");
        File longFeatureNameFileName = new File(dataDirName, "long_feature_name.txt");
        File realFeatureNameFileName = new File(dataDirName, "real_feature_name.txt");
        PrintWriter ctrlWriter = null;
        PrintWriter boolWriter = null;
        PrintWriter longWriter = null;
        PrintWriter realWriter = null;
        try {
            ctrlWriter = new PrintWriter(new FileWriter(ctrlFeatureNameFileName));
            boolWriter = new PrintWriter(new FileWriter(boolFeatureNameFileName));
            longWriter = new PrintWriter(new FileWriter(longFeatureNameFileName));
            realWriter = new PrintWriter(new FileWriter(realFeatureNameFileName));
        } catch (IOException ex) {
            Messages.fatal(ex);
        }

		String globalInstr = "", localInstr = "";
        int numFldInfos = fldInfosList.size();
		for (int i = 0, currMantisPrintMethodId = 0; i < numFldInfos;) {
            FldInfo fldInfo = fldInfosList.get(i);
            String fldBaseName = fldInfo.fldBaseName;
            String javaPos = fldInfo.javaPos;
            switch (fldInfo.kind) {
                case CTRL:
                {
                    String fldName1 = fldBaseName + "_bef";
                    String fldName2 = fldBaseName + "_aft";
                    localInstr += "ctrlOut.println(" + fldName1 + ");";
                    localInstr += "ctrlOut.println(" + fldName2 + ");";
                    ctrlWriter.println(fldName1 + " " + javaPos);
                    ctrlWriter.println(fldName2 + " " + javaPos);
                    break;
                }
                case DATA_BOOL:
                {
                    String fldName1 = fldBaseName + "_true";
                    String fldName2 = fldBaseName + "_false";
                    localInstr += "boolOut.println(" + fldName1 + ");";
                    localInstr += "boolOut.println(" + fldName2 + ");";
                    boolWriter.println(fldName1 + " " + javaPos);
                    boolWriter.println(fldName2 + " " + javaPos);
                    break;
                }
                case DATA_LONG:
                {
                    String fldName1 = fldBaseName + "_sum";
                    String fldName2 = fldBaseName + "_freq";
                    localInstr += "longOut.println(" + fldName1 + ");";
                    localInstr += "longOut.println(" + fldName2 + ");";
                    longWriter.println(fldName1 + " " + javaPos);
                    longWriter.println(fldName2 + " " + javaPos);
                    break;
                }
                case DATA_DOUBLE:
                {
                    String fldName1 = fldBaseName + "_sum";
                    String fldName2 = fldBaseName + "_freq";
                    localInstr += "realOut.println(" + fldName1 + ");";
                    localInstr += "realOut.println(" + fldName2 + ");";
                    realWriter.println(fldName1 + " " + javaPos);
                    realWriter.println(fldName2 + " " + javaPos);
                    break;
                }
            }
			++i;
            if (i % maxFldsPerMantisPrintMethod == 0 || i == numFldInfos) {
                String mName = "print" + currMantisPrintMethodId;
                try {
                    CtMethod m = CtNewMethod.make("private static void " + mName +
                           "() { " + localInstr + " }", mantisPrinterClass);
                    mantisPrinterClass.addMethod(m);
                } catch (CannotCompileException ex) {
                    Messages.fatal(ex);
                }
                localInstr = "";
                globalInstr += mName + "();";
                currMantisPrintMethodId++;
			}
        }
        ctrlWriter.close();
        boolWriter.close();
        longWriter.close();
        realWriter.close();

        String ctrlFeatureDataFileName = "ctrl_feature_data.txt";
        String boolFeatureDataFileName = "bool_feature_data.txt";
        String longFeatureDataFileName = "long_feature_data.txt";
        String realFeatureDataFileName = "real_feature_data.txt";
        String outDir = Config.userClassesDirName;
        try {
            for (int i = 0; i < mantisClasses.length; i++) {
                CtClass c = mantisClasses[i];
                if (c == null)
                    break;
                c.writeFile(outDir);
            }
			CtMethod doneMethod = CtNewMethod.make("public static void done() { " +
                "try { " +
                    "ctrlOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + ctrlFeatureDataFileName + "\")); " +
                    "boolOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + boolFeatureDataFileName + "\")); " +
                    "longOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + longFeatureDataFileName + "\")); " +
                    "realOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + realFeatureDataFileName + "\")); " +
                     globalInstr +
                    " ctrlOut.close(); " +
                    " boolOut.close(); " +
                    " longOut.close(); " +
                    " realOut.close(); " +
                "} catch (java.io.IOException ex) { " + 
                    "ex.printStackTrace(); " +
                    "System.exit(1); " +
                "}" +
			"}", mantisPrinterClass);
			mantisPrinterClass.addMethod(doneMethod);
			CtClass mainClass = getPool().get(Config.mainClassName);
			if (mainClass.isFrozen())
				mainClass.defrost();
            CtMethod mainMethod = mainClass.getMethod("main", "([Ljava/lang/String;)V");
            mainMethod.insertAfter("MantisPrinter.done();");
            mainClass.writeFile(outDir); 
			mantisPrinterClass.writeFile(outDir);
		} catch (NotFoundException ex) {
            Messages.fatal(ex);
        } catch (CannotCompileException ex) {
            Messages.fatal(ex);
        } catch (IOException ex) {
            Messages.fatal(ex);
		}
	}
}

