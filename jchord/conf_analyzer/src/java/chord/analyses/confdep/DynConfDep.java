package chord.analyses.confdep;

import chord.analyses.confdep.optnames.DomOpts;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.*;
import chord.util.Utils;
import chord.util.tuple.object.Pair;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operator.Invoke;

@Chord(
  name = "dynamic-cdep-java",
  consumes = { "H","V", "Z", "I"},
  produces = {"dynCUse","nullI", "Opt", "nullConf","confMethod" },
    signs = {"I0,Z0,Opt0",  "I0", "V0,Opt0:V0_Opt0", "M0"},
    namesOfSigns = {"dynCUse" , "nullI", "nullConf", "confMethod"}
  //nullconf = "H0,UV0,Opt0",
  
  
//    namesOfTypes = { "H" ,"UV","StrConst", "Z","I"},
//    types = { DomH.class, DomUV.class, DomStrConst.class, DomZ.class, DomI.class }
)
public class DynConfDep extends BasicDynamicAnalysis {
  //Config.workDirName, 
  static File results = new File("dyn_cdep.temp");
  static final String SCHEME_FILE= "dynconfdep.instr";
  
  static final Pattern readPat = Pattern.compile("([0-9]*) calling .* returns option (.*)-([0-9]+) value=(.*)");
  static final Pattern taintlessCall = Pattern.compile("([0-9]*) calling .*");
  static final Pattern usePat = Pattern.compile("([0-9]*) invoking .* ([0-9]+)=(.+)");
  static final Pattern nullVPat = Pattern.compile("([0-9]*) returns null");
  
  @Override
  public Map<String, String> getInstrumentorArgs() {
    Map<String,String> args = super.getInstrumentorArgs();
    if(args == null)
    	args = new HashMap<String, String>();

    InstrScheme instrScheme = new InstrScheme();
    args.put(InstrScheme.INSTR_SCHEME_FILE_KEY, SCHEME_FILE);
    args.put("chord.scopeExclude", Config.scopeExcludeStr);
    
    instrScheme.setAloadReferenceEvent(false, false, true, false, true);
    instrScheme.setAstoreReferenceEvent(false, false, true, false, true);
    instrScheme.setBefMethodCallEvent(true, true, true);
    instrScheme.save(SCHEME_FILE);
    
    return args;
  }
  
  @Override
  public Class getInstrumentorClass() {
  	return ArgMonInstr.class;
  }
  
  @Override
	public Class getEventHandlerClass() {
		return DynConfDepRuntime.class;
	}
  
  @Override
	public void processTrace(String fileName) {
	}
 
  boolean retrace = false;
  @Override
  public void initAllPasses() {
  	retrace = Utils.buildBoolProperty("retrace_conf", false);
  	if(!retrace) {
  		results.delete();
  		System.out.println("DynConfDep starting execution; clearing buffer file.");
  	}
  }

  @Override
	public void run() {
  	if(retrace && results.exists()) {			
  		doneAllPasses();
  		return;
  	} else
  		super.run();
  }

  @Override
  public void doneAllPasses() {
    ClassicProject project = ClassicProject.g();
    System.out.println("done all passes; slurping results");

    DomI domI = (DomI) project.getTrgt("I");
    project.runTask(domI);
    
    DomV domV = (DomV) project.getTrgt("V");
    project.runTask(domV);


    try {
      
      DomOpts domOpts = buildDomOpts(domI);
      
      ProgramRel relConf = (ProgramRel) project.getTrgt("OptNames"); //opt, i
      ProgramRel relUse = (ProgramRel) project.getTrgt("dynCUse");
      ProgramRel relNullC = (ProgramRel) project.getTrgt("nullConf");
      ProgramRel relNullI = (ProgramRel) project.getTrgt("nullI");
      ProgramRel relConfM = (ProgramRel) project.getTrgt("confMethod");

      relConf.zero();
      relUse.zero();
      relNullC.zero();
      relNullI.zero();
      relConfM.zero();
    
      BufferedReader br = new BufferedReader(new FileReader(results));
      
      String s = null;
      while( (s = br.readLine()) != null) {
      	if(s.startsWith("//"))
      		continue;
      	
        Matcher m = readPat.matcher(s);
        if(m.matches()) {
          int iId = Integer.parseInt(m.group(1));
          Quad q = (Quad) domI.get(iId);
          
          String cst = m.group(2);
          int cstID = domOpts.getOrAdd(cst);
          if(cstID == -1) {
            cstID = 0;
            System.err.println("UNKNOWN OPTION " + cst);
          }
//           else System.out.println("Found option " + cst + " at idx " + cstID);
          
          RegisterFactory.Register targ = Invoke.getDest(q).getRegister();
          int vID = domV.indexOf(targ);
          
          String value = m.group(4);

          if(iId > -1 ) {
            relConf.add(cstID, iId);
            if(vID >-1 && "null".equals(value)) //vID will be null for prim-typed options
              relNullC.add(vID,cstID); 
          }
        } else {
          m = usePat.matcher(s);
          if(m.matches()) {
            int iId = Integer.parseInt(m.group(1));
            int zId = Integer.parseInt(m.group(2));
            String cstList = m.group(3);
            String[] confDeps = cstList.split("\\|");
            for(String dep: confDeps) {
              int i = dep.lastIndexOf('-');
              String optName = dep.substring(0, i); //ConfDefines.pruneName(dep.substring(0, i));
              
              int cstID = domOpts.indexOf(optName);
              if(cstID == -1) {
                System.out.println("WARN: found use of option " + optName + " without ever having seen a read");
                cstID = 0;
              }

              relUse.add(iId, zId, cstID);
            }
          } else {
            m = nullVPat.matcher(s);
            if(m.matches()) {
              int i = Integer.parseInt(m.group(1));
              Quad u = (Quad) domI.get(i);
              if(u != null && (u.getOperator() instanceof Invoke) && Invoke.getDest(u) != null) {
                relNullI.add(i);
              }
            } else
              if(!s.contains("without taint"))
                System.err.println("NO MATCH FOR LINE: " + s);
          }
        }
      }
      
      br.close();
//      domOpts.save();
      relConf.save();
      relUse.save();
      relNullC.save();
      relNullI.save();
      relConfM.save(); //note size == 0; we don't mark any outer methods as returning conf.
    } catch(IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    
  }

  private DomOpts buildDomOpts(DomI domI) throws IOException {
    ClassicProject project = ClassicProject.g();
    
    DomOpts domOpts =  (DomOpts)  project.getTrgt("Opt");
    domOpts.addPt(domI.get(0), DomOpts.NONE);
//    System.out.println("in buildDomOpts, filename is " + results);
    BufferedReader br = new BufferedReader(new FileReader(results));
    String s;
    while( (s = br.readLine()) != null) {
      Matcher m = readPat.matcher(s);
      if(m.matches()) {
        int iId = Integer.parseInt(m.group(1));
        
        String cst = m.group(2);
//        String prefix = ConfDefines.optionPrefix(domI.get(iId))
        if(domOpts.contains(cst))
        	continue;
        
        int cstID = domOpts.addPt(domI.get(iId), cst);
        if(cstID == -1) {
          cstID = 0;
          System.err.println("UNKNOWN OPTION " + cst);
        } else
          System.out.println("Found and added option " + cst + " at idx " + cstID);
       }
    }
    br.close();
    domOpts.save();
    return domOpts;
  }


}
