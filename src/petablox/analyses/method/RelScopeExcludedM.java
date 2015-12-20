package petablox.analyses.method;

import soot.SootMethod;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.analyses.ProgramRel;
import petablox.util.IndexSet;
import petablox.util.Utils;

/**
 * @author Ariel Rabkin (asrabkin@gmail.com)
 */
@Petablox(
    name = "scopeExcludedM",
    sign = "M0:M0"
  )
public class RelScopeExcludedM extends ProgramRel {

  public static boolean isOutOfScope(String cName) {
    return Utils.prefixMatch(cName, Config.scopeExcludeAry);
  }
  
  public static boolean isOutOfScope(SootMethod m) {
    String cName = m.getDeclaringClass().getName();
    return isOutOfScope(cName);
  }
  
  public void fill() {
    Program program = Program.g();
    IndexSet<SootMethod> methods = program.getMethods();
    for(SootMethod m: methods) {
      if(isOutOfScope(m))
        add(m);
    }
  }
}

