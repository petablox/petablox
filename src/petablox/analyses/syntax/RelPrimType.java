package petablox.analyses.syntax;

import soot.Type;
import soot.PrimType;

import petablox.analyses.type.DomT;
import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;

@Petablox(name="PrimType", sign="T0:T0", consumes = { "T" })
public class RelPrimType extends ProgramRel {
    public void fill() {
        DomT domT = (DomT) ClassicProject.g().getTrgt("T");
        for (int i = 0; i < domT.size(); i++) {
            Type t = (Type) domT.get(i);
            if (t instanceof PrimType)
                add(t);
        }
    }
}
