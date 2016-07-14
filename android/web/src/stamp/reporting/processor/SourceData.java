package stamp.reporting.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SourceData extends HashMap<String,SourceData.MethodInfo> implements Serializable 
{
    public static class ClassInfo implements Serializable {
		public String name;
		public String qualifiedName;
    }
	
    public static class InvocationInfo implements Serializable {
		public String chordSig;
		
		public int start;
		public int end;
		
		public String calleeChordSig;
    }
	
    public static class MethodInfo implements Serializable {
		public String chordSig;
		
		//add the button here
		public int methNamePos;
        public int methNameEndPos;
		public int methNameLineNum;
		public int methNameColNum;
		
		//add annotations here
		public int javaDocEndPos;
		public int javaDocEndLineNum;
		public int javaDocEndColNum;
		
		//start and end (comments included)
		public int beginPos;
		public int beginLineNum;
		public int beginColNum;
		
		public int endPos;
		public int endLineNum;
		public int endColNum;

		public boolean isStub;
		public boolean isVoidReturnType;
		
		// info for building method declaration
		public String returnType;
		public String methName;
		public List<String> paramNames = new ArrayList();
		public List<String> paramTypes = new ArrayList();
		public boolean isStatic;
		public boolean isVarargs;
		public int bodyStart;
		public int bodyEnd;
        public boolean isConstructor;
		public boolean hasBody;

		// list of flows
		public List<String> sources = new ArrayList();
		public List<String> sinks = new ArrayList();

		// info storing location of annotations
		public List<Integer> annotBeginPos = new ArrayList<Integer>();
		public List<Integer> annotEndPos = new ArrayList<Integer>();
		public int declEnd;
		public List<String> annot = new ArrayList<String>();
		
		public ClassInfo classInfo = new ClassInfo();;
    }
    
    public SourceData() {
		super();
    }

    public SourceData(SourceData data) {
		super(data);
    }
    
    public static SourceData read(File f) throws Exception 
	{
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
		SourceData s = (SourceData)in.readObject();
		in.close();
		return s;
    }

    public void write(File f) throws IOException {
		f.getParentFile().mkdirs();
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
		out.writeObject(this);
		out.close();
    }
}
