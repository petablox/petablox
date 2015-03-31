package chord.analyses.primtrack;


import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

@Chord(
		name = "UvvuMap",
		consumes = { "U", "V", "UV"},
		produces = { "UVU", "UVV" },
		signs = {"UV0,U0:UV0_U0","UV0,V0:UV0_V0"},
		namesOfSigns = { "UVU", "UVV" },
		namesOfTypes = { "U" ,"UV", "V"},
		types = { DomU.class, DomUV.class, DomV.class}
)
public class MakeUVVUMaps extends JavaAnalysis{
	private DomU domU;
	private DomUV domUV;
	private DomV domV;
	private ProgramRel UVV;
	private ProgramRel UVU;

	@Override
	public void run() {
		ClassicProject project = ClassicProject.g();
		domU = (DomU) project.getTrgt("U");
		domUV = (DomUV) project.getTrgt("UV");
		domV = (DomV) project.getTrgt("V");

		UVV = (ProgramRel) project.getTrgt("UVV");
		UVU = (ProgramRel) project.getTrgt("UVU");

		UVV.zero();
		UVU.zero();

		for(Register r: domUV) {

			if(r.getType().isPrimitiveType())
				UVU.add(r,r);
			else
				UVV.add(r,r);
		}

		UVV.save();
		UVU.save();
	}
}
