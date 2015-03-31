package joeq.Compiler.Quad;

import joeq.Class.jq_Method;

public interface Inst {
	public jq_Method getMethod();
	public BasicBlock getBasicBlock();
	public int getLineNumber();
	public String toByteLocStr();
	public String toJavaLocStr();
	public String toLocStr();
	public String toVerboseStr();
}
