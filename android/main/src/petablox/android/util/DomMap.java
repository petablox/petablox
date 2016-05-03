package petablox.android.util;

import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramDom;

public class DomMap extends LazyMap<String,ProgramDom> {
	@Override
		public ProgramDom lazyFill(String domName) {
		return (ProgramDom) ClassicProject.g().getTrgt(domName);
	}

	public int getSize(String domName) {
		return get(domName).size();
	}
}
