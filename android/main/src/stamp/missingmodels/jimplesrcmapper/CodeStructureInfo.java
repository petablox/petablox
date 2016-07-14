package stamp.missingmodels.jimplesrcmapper;

import java.util.HashMap;
import java.util.Set;

import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;

/**
 * Data structures to store code structure information.
 * 
 * @author Osbert Bastani
 */
public class CodeStructureInfo {
	public static class CodeStructure {
		public int start;
		public int end;
		public int declarationLineNum;
		public int declarationStart;
		public int declarationEnd;
		public int bodyStart;
		public int bodyStartLineNum;
		public int bodyEnd;
		public int bodyEndLineNum;
		
		public CodeStructure() {
			this.start = -1;
			this.end = -1;
			this.declarationLineNum = -1;
			this.declarationStart = -1;
			this.declarationEnd = -1;
			this.bodyStart = -1;
			this.bodyStartLineNum = -1;
			this.bodyEnd = -1;
			this.bodyEndLineNum = -1;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("lineNum=" + this.declarationLineNum);
			return sb.toString();			
		}
	}
	
	public static class SimpleCodeStructure {
		public int lineNum;
		public int start;
		public int end;
		
		public SimpleCodeStructure() {
			this.lineNum = -1;
			this.start = -1;
			this.end = -1;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("lineNum=" + this.lineNum);
			return sb.toString();			
		}
	}
	
	protected static class CodeStructureMap<T> extends HashMap<T,CodeStructure> {
		private static final long serialVersionUID = 4757319167635621292L;
		@Override public CodeStructure get(Object obj) {
			CodeStructure cs = super.get(obj);
			if(cs == null) {
				cs = new CodeStructure();
				super.put((T)obj, cs);
			}
			return cs;
		}
	}
	
	protected static class SimpleCodeStructureMap<T> extends HashMap<T,SimpleCodeStructure> {
		private static final long serialVersionUID = -8673062463430885963L;
		@Override public SimpleCodeStructure get(Object obj) {
			SimpleCodeStructure cs = super.get(obj);
			if(cs == null) {
				cs = new SimpleCodeStructure();
				super.put((T)obj, cs);
			}
			return cs;
		}
	}
	
	/** File structure info */
	protected SimpleCodeStructure fileInfo = new SimpleCodeStructure();
	/** Class structure info */
	protected CodeStructureMap<SootClass> classInfo = new CodeStructureMap<SootClass>();
	/** Method structure info */
	protected CodeStructureMap<SootMethod> methodInfo = new CodeStructureMap<SootMethod>();
	/** Field structure info */
	protected SimpleCodeStructureMap<SootField> fieldInfo = new SimpleCodeStructureMap<SootField>();
	/** Local structure info */
	protected SimpleCodeStructureMap<Local> localInfo = new SimpleCodeStructureMap<Local>();
	/** Unit structure info */
	protected SimpleCodeStructureMap<Unit> unitInfo = new SimpleCodeStructureMap<Unit>();
	
	public SimpleCodeStructure getFileInfo() {
		return this.fileInfo;
	}
	
	public Set<SootClass> getClasses() {
		return this.classInfo.keySet();
	}
	
	public CodeStructure getClassInfo(SootClass cl) {
		return this.classInfo.get(cl);
	}
	
	public Set<SootMethod> getMethods() {
		return this.methodInfo.keySet();
	}
	
	public CodeStructure getMethodInfo(SootMethod m) {
		return this.methodInfo.get(m);
	}
	
	public Set<SootField> getFields() {
		return this.fieldInfo.keySet();
	}
	
	public SimpleCodeStructure getFieldInfo(SootField f) {
		return this.fieldInfo.get(f);
	}
	
	public Set<Local> getLocals() {
		return this.localInfo.keySet();
	}
	
	public SimpleCodeStructure getLocalInfo(Local local) {
		return this.localInfo.get(local);
	}
	
	public Set<Unit> getUnits() {
		return this.unitInfo.keySet();
	}
	
	public SimpleCodeStructure getUnitInfo(Unit unit) {
		return this.unitInfo.get(unit);
	}
}
