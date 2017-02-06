package petablox.android.srcmap;

import petablox.android.srcmap.sourceinfo.SourceInfo;
import petablox.android.srcmap.sourceinfo.javainfo.JavaSourceInfo;
import petablox.android.srcmap.sourceinfo.jimpleinfo.JimpleSourceInfo;

public class SourceInfoSingleton {
	public static enum SourceInfoType {
		JIMPLE, JAVA;
	}

	private static JavaSourceInfo javaSourceInfo = null;
	private static JimpleSourceInfo jimpleSourceInfo = null;
	private static SourceInfoType sourceInfoType = SourceInfoType.JAVA;
	
	public static void setSourceInfoType(SourceInfoType type) {
		sourceInfoType = type;
	}
	
	public static JavaSourceInfo getJavaSourceInfo() {
		if(javaSourceInfo == null) {
			javaSourceInfo = new JavaSourceInfo();
		}
		return javaSourceInfo;
	}

	public static JimpleSourceInfo getJimpleSourceInfo() {
		if(jimpleSourceInfo == null) {
			jimpleSourceInfo = new JimpleSourceInfo();
		}
		return jimpleSourceInfo;
	}
	
	public static SourceInfo v() {
		switch(sourceInfoType) {
		case JAVA:
			return getJavaSourceInfo();
		case JIMPLE:
			return getJimpleSourceInfo();
		default:
			throw new RuntimeException("Source info " + sourceInfoType.toString() + " not implemented!");
		}
	}
}
