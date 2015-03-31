package chord.analyses.confdep.rels;


import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Maps elements of h to i, where a given call is treated as an allocation site
 * @author asrabkin
 *
 */
@Chord(
    name = "HI",
    sign = "H0,I0:H0_I0"
  )
public class RelHI extends ProgramRel {
  public void fill() {
    DomH domH = (DomH) doms[0];
    DomI domI = (DomI) doms[1];
    int numA = domH.getLastA() + 1;
//    int numH = domH.size();
    for (int hIdx = 1; hIdx < numA; hIdx++) {
      Object h = domH.get(hIdx);
      if(h instanceof Quad) {
        int iIdx = domI.indexOf(h);
        if(iIdx > -1)
          add(hIdx,iIdx);
      }
    }
  }

}
