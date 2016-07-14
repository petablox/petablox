package stamp.analyses;

import soot.Scene;
import soot.Type;
import soot.SootClass;
import soot.SootMethod;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

import shord.project.analyses.ProgramRel;
import shord.project.analyses.ProgramDom;
import shord.project.analyses.JavaAnalysis;
import shord.project.ClassicProject;
import shord.program.Program;
import shord.analyses.DomM;

import chord.project.Chord;
import chord.util.tuple.object.Trio;

/**
 * @author Saswat Anand
**/
@Chord(name = "annot-java",
	   consumes = { "M", "Z" },
	   produces = { "L",
					"ArgArgTransfer", "ArgRetTransfer", 
					"ArgArgFlow",
					"SrcLabel", "SinkLabel",
					"InLabelArg", "InLabelRet",
					"OutLabelArg", "OutLabelRet" },
	   namesOfTypes = { "L" },
	   types = { DomL.class },
	   namesOfSigns = { "ArgArgTransfer", "ArgRetTransfer", 
						"ArgArgFlow",
						"SrcLabel", "SinkLabel",
						"InLabelArg", "InLabelRet",
						"OutLabelArg", "OutLabelRet" },
	   signs = { "M0,Z0,Z1:M0_Z0_Z1", "M0,Z0:M0_Z0", 
				 "M0,Z0,Z1:M0_Z0_Z1",
				 "L0:L0", "L0:L0",
				 "L0,M0,Z0:L0_M0_Z0", "L0,M0:L0_M0",
				 "L0,M0,Z0:L0_M0_Z0", "L0,M0:L0_M0" }
	   )
public class AnnotationReader extends JavaAnalysis
{
	private ProgramRel relArgArgTransfer;
	private ProgramRel relArgRetTransfer;
	private ProgramRel relArgArgFlow;

	private ProgramRel relInLabelArg; 
	private ProgramRel relInLabelRet;
	private ProgramRel relOutLabelArg; 
	private ProgramRel relOutLabelRet;

	private List<Trio<SootMethod,String,String>> worklist = new LinkedList();
	private List<String> srcLabels = new ArrayList();
	private List<String> sinkLabels = new ArrayList();

	private DomM domM;

	public void run()
	{	
		read();

		//fill DomL
		DomL domL = (DomL) ClassicProject.g().getTrgt("L");
		for(String l : srcLabels)
			domL.add(l);
		for(String l : sinkLabels)
			domL.add(l);
		domL.save();

		//fille SrcLabel
		ProgramRel relSrcLabel = (ProgramRel) ClassicProject.g().getTrgt("SrcLabel");
		relSrcLabel.zero();
		for(String l : srcLabels)
			relSrcLabel.add(l);
		relSrcLabel.save();

		//fill SinkLabel
		ProgramRel relSinkLabel = (ProgramRel) ClassicProject.g().getTrgt("SinkLabel");
		relSinkLabel.zero();
		for(String l : sinkLabels)
			relSinkLabel.add(l);
		relSinkLabel.save();

		//fill LabelArg and LabelRet
		relInLabelArg = (ProgramRel) ClassicProject.g().getTrgt("InLabelArg");
		relInLabelRet = (ProgramRel) ClassicProject.g().getTrgt("InLabelRet");
		relOutLabelArg = (ProgramRel) ClassicProject.g().getTrgt("OutLabelArg");
		relOutLabelRet = (ProgramRel) ClassicProject.g().getTrgt("OutLabelRet");
		relArgArgTransfer = (ProgramRel) ClassicProject.g().getTrgt("ArgArgTransfer");
		relArgRetTransfer = (ProgramRel) ClassicProject.g().getTrgt("ArgRetTransfer");
		relArgArgFlow = (ProgramRel) ClassicProject.g().getTrgt("ArgArgFlow");

		relInLabelArg.zero();
		relInLabelRet.zero();		
		relOutLabelArg.zero();
		relOutLabelRet.zero();		
		relArgArgTransfer.zero();
		relArgRetTransfer.zero();
		relArgArgFlow.zero();

		domM = (DomM) ClassicProject.g().getTrgt("M");
		while(!worklist.isEmpty()){
			Trio<SootMethod,String,String> trio = worklist.remove(0);
			SootMethod meth = trio.val0;
			if(domM.contains(meth)){
				String from = trio.val1;
				String to = trio.val2;
				addFlow(meth, from, to);
			}
		}
		
		relInLabelArg.save();
		relInLabelRet.save();
		relOutLabelArg.save();
		relOutLabelRet.save();
		relArgArgTransfer.save();	
		relArgRetTransfer.save();
		relArgArgFlow.save();	
	}

	private String[] split(String line)
	{
		String[] tokens = new String[3];
		int index = line.indexOf(" ");
		tokens[0] = line.substring(0, index);
		
		String delim = "$stamp$stamp$";
		
		index++;
		char c = line.charAt(index);
		if((c == '$' || c == '!') && line.startsWith(delim, index+1)){
			int j = line.indexOf(delim, index+2);
			tokens[1] = c+line.substring(index+1+delim.length(), j);
			index = j+delim.length();
			if(line.charAt(index) != ' ')
				throw new RuntimeException("Cannot parse annotation "+line);
		} else {
			int j = line.indexOf(' ', index);
			tokens[1] = line.substring(index, j);
			index = j;
		}
		
		index++;		
		c = line.charAt(index);
		if(c == '!' && line.startsWith(delim, index+1)){
			int j = line.indexOf(delim, index+2);
			tokens[2] = c+line.substring(index+1+delim.length(), j);
			index = j+delim.length();
			if(index != line.length())
				throw new RuntimeException("Cannot parse annotation "+line);
		} else {
			assert c != '$';
			tokens[2] = line.substring(index, line.length());
		}

		return tokens;
	}

	public void read()
	{
		Scene scene = Program.g().scene();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(System.getProperty("stamp.out.dir"), "stamp_annotations.txt")));
			String line = reader.readLine();
			while(line != null){
				final String[] tokens = split(line);
				String chordMethodSig = tokens[0];
				int atSymbolIndex = chordMethodSig.indexOf('@');
				String className = chordMethodSig.substring(atSymbolIndex+1);
				if(scene.containsClass(className)){
					SootClass klass = scene.getSootClass(className);
					String subsig = SootUtils.getSootSubsigFor(chordMethodSig.substring(0,atSymbolIndex));
					SootMethod meth = klass.getMethod(subsig);
					String from = tokens[1];
					String to = tokens[2];
					
					boolean b1 = addLabel(from);
					boolean b2 = addLabel(to);
					
					char c = from.charAt(0);
					boolean src = (c == '$' || c == '!');
					boolean sink = to.charAt(0) == '!';
					if(b1 && b2){
						System.out.println("Unsupported annotation type "+line);
					} else {
						worklist.add(new Trio(meth, from, to));
					}
				}
				line = reader.readLine();
			}
			reader.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	private boolean addLabel(String label)
	{
		char c = label.charAt(0);
		if(c == '$'){
			srcLabels.add(label);
			return true;
		} 
		if(c == '!'){
			sinkLabels.add(label);
			return true;
		}
		return false;
	}

	private void addFlow(SootMethod meth, String from, String to) //throws NumberFormatException
	{
		//overriding methods inherit annotations
		//also handle compiler-generated methods that result from co-variant return types

		Set<SootMethod> meths = new HashSet();
		for(SootMethod m : SootUtils.overridingMethodsFor(meth)){
			if(domM.contains(m))
				meths.add(m);
			String mName = m.getName();
			List<Type> mTypes = m.getParameterTypes();
			for(SootMethod covm : m.getDeclaringClass().getMethods())
				if(mName.equals(covm.getName()) && mTypes.equals(covm.getParameterTypes()))
					if(domM.contains(covm))
						meths.add(covm);
		}

		char from0 = from.charAt(0);
		if(from0 == '$' || from0 == '!') {
			if(to.equals("-1")){
				for(SootMethod m : meths)
					relInLabelRet.add(from, m);
			}
			else{
				for(SootMethod m : meths)
					relInLabelArg.add(from, m, Integer.valueOf(to));
			}
		} else {
			Integer fromArgIndex = Integer.valueOf(from);
			char to0 = to.charAt(0);
			if(to0 == '!'){
				if(from.equals("-1")){
					for(SootMethod m : meths)
						relOutLabelRet.add(to, m);
				} else{
					for(SootMethod m : meths)
						relOutLabelArg.add(to, m, fromArgIndex);
				}
			} else if(to0 == '?'){
				Integer toArgIndex = Integer.valueOf(to.substring(1));
				for(SootMethod m : meths)
					relArgArgFlow.add(m, fromArgIndex, toArgIndex);
			} else if(to.equals("-1")){
				for(SootMethod m : meths)
					relArgRetTransfer.add(m, fromArgIndex);
			} else {
				Integer toArgIndex = Integer.valueOf(to);
				for(SootMethod m : meths)
					relArgArgTransfer.add(m, fromArgIndex, toArgIndex);
			}
		}
	}
}
