package chord.analyses.confdep;

import java.util.Map;
import javassist.*;
import javassist.expr.*;
import chord.instr.Instrumentor;
import chord.project.Config;
import chord.util.Utils;

/**
 * Code to insert calls to DynConfDepRuntime and similar.
 *
 */
public class ArgMonInstr extends Instrumentor {

	protected final String methodCallAftEventCallSuper;
	protected final String methodCallBefEventCallSuper;

	boolean HANDLE_EXCEPTIONS = false;

	public ArgMonInstr(Map<String, String> argsMap) {
		super(argsMap);
		System.out.println("using ArgMonInstr");

		/*if(!aloadReferenceEvent.present() || !astoreReferenceEvent.present()) {
      System.err.println("ArgMonInstr expected load and store events");
      System.exit(-1);
    }*/
		String rtClassName = eventHandlerClassName;
		rtClassName = DynConfDepRuntime.class.getCanonicalName();
		methodCallAftEventCallSuper = rtClassName + ".afterMethodCall(";
		methodCallBefEventCallSuper = rtClassName + ".beforeMethodCall(";

		HANDLE_EXCEPTIONS = Utils.buildBoolProperty("confdep.dyn.catchexceptions", false);
	}

	/*
	 * I think this is actually just for nested ctor calls, and therefore almost useless
	 *   --asr
	 */
	@Override
	public void edit(ConstructorCall c)  {
		//    System.out.println("editing a constructor call, class is " + c.getClassName());
		try {
			String aftInstr = "";
			int iId =  set(Imap, c) ;
			String o =  "$0" ;


			aftInstr += methodCallAftEventCallSuper +iId + ",\""+c.getClassName()+"\",\"<init>\",($w)$_,null,$args);";        
			c.replace("{ $_ = $proceed($$); " +  aftInstr + " }");
		} catch (CannotCompileException ex) {
			throw new RuntimeException(ex);
		} 
	}

	/*
	 * This one handles most constructor calls.
	 */
	@Override
	public void edit(NewExpr e) throws CannotCompileException {
		int iId = set(Imap, e); //.indexOfOriginalBytecode() 

	//Not usually a major problem, so ignore	
//		if(iId == -2)
//			System.out.println("ArgMonInstr.edit(NewExpr) couldn't find iId for call to " +
//					e.getClassName() + " <init>");
	
		String befInstr = "";
		befInstr += methodCallBefEventCallSuper + iId+",\""+e.getClassName()+"\",\"<init>\",$0,$args);"; 

		e.replace("{" + befInstr + " $_ = $proceed($$); " + methodCallAftEventCallSuper +
				iId +",\""+ 
				e.getClassName()+"\",\"<init>\",($w)$_,null,$args); }");
	}

	public void edit(MethodCall e) {
		try {
			String aftInstr = "";
			String exRet = "";
			String befInstr = "";
			int iId =  set(Imap, e) ;

			if(Modifier.isStatic(e.getMethod().getModifiers())) { 
				befInstr += methodCallBefEventCallSuper + iId + ",\""+e.getClassName()+"\",\""+ 
				e.getMethodName() + "\",null,$args);";

				aftInstr += methodCallAftEventCallSuper +iId + ",\""+e.getClassName()+"\",\""+ 
				e.getMethodName() + "\",($w)$_,null,$args);";

				exRet += methodCallAftEventCallSuper +iId + ",\""+e.getClassName()+"\",\""+ 
				e.getMethodName() + "\",null,null,$args);";

			} else {
				befInstr += methodCallBefEventCallSuper + iId+",\""+e.getClassName()+"\",\""+ 
				e.getMethodName() + "\",$0,$args);"; 

				aftInstr += methodCallAftEventCallSuper + iId+",\""+e.getClassName()+"\",\""+ 
				e.getMethodName() + "\",($w)$_,$0,$args);"; 

				exRet += methodCallAftEventCallSuper + iId+",\""+e.getClassName()+"\",\""+ 
				e.getMethodName() + "\",null,$0,$args);";
			}

			if(HANDLE_EXCEPTIONS)
				e.replace(" { "+ befInstr+ " try { $_ = $proceed($$); " +aftInstr + " } catch (java.lang.Throwable ex) { " +
						exRet +  " throw ex;} }");
			else
				e.replace("{ " + befInstr+  "  $_ = $proceed($$); " +  aftInstr + " }"); 

			return;
		} catch (CannotCompileException ex) {
			ex.printStackTrace();
			//      throw new RuntimeException(ex);
		} catch (NotFoundException ex) {
			ex.printStackTrace();
			//      throw new RuntimeException(ex);
		}
	}
}
