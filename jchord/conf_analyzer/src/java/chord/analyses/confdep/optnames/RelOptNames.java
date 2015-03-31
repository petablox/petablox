package chord.analyses.confdep.optnames;

import java.util.Map;
import joeq.Compiler.Quad.Quad;
import chord.analyses.confdep.ConfDefines;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

@Chord(name="OptNames",
  sign = "Opt0,I0"
 )
public class RelOptNames extends ProgramRel {
  
  @Override
  public void fill() {
    
    for( Pair<Quad, String> e: DomOpts.optSites) {
      String prefix = ConfDefines.optionPrefix(e.val0);

      super.add(prefix+ e.val1,e.val0);
    }
  }
  

}
