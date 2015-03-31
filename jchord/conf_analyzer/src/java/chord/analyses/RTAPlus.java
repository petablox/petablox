package chord.analyses;

import java.util.Collection;
import java.util.LinkedHashSet;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import chord.analyses.invk.StubRewrite;
import chord.analyses.method.RelExtraEntryPoints;
import chord.program.RTA;
import chord.project.Config;
import chord.util.Utils;

public class RTAPlus extends RTA {
	
	
	Collection<jq_Method> publicMethods;
	private String[] appCodePrefixes;


	
	public RTAPlus() {
		super(Config.reflectKind);
		System.out.println("Using extended RTA");
		
		String fullscan = System.getProperty("chord.scope.fullscan");
		if (fullscan == null)
		  appCodePrefixes = new String[0];
		else
		  appCodePrefixes = Utils.toArray(fullscan);
	}
	
	@Override
	protected void prepAdditionalEntrypoints() {
		publicMethods = RelExtraEntryPoints.slurpMList();		 
		publicMethods = new LinkedHashSet<jq_Method>(publicMethods);
		StubRewrite.addNewDests(publicMethods); //to avoid mutating upstream copy
	}
	
	@Override
	protected void visitAdditionalEntrypoints() {
		// visit classes just once each
		LinkedHashSet<jq_Class> extraClasses = new LinkedHashSet<jq_Class>();
		for (jq_Method m: publicMethods) {
			extraClasses.add(m.getDeclaringClass());
		}

		for (jq_Class cl: extraClasses) {
			visitClass(cl);

			jq_Method ctor = cl.getInitializer(new jq_NameAndDesc("<init>", "()V"));
			if (ctor != null)
				visitMethod(ctor);
		}

		for (jq_Method m: publicMethods) {
			visitMethod(m);
		}
	}
	
	protected void visitClass(jq_Reference r) {
		super.visitClass(r);
		if(r instanceof jq_Class) {
			jq_Class c = (jq_Class) r;
			
			if (shouldExpandAggressively(c)) {
				for (jq_Method m: c.getDeclaredInstanceMethods()) 
					visitMethod(m);
				for (jq_Method m: c.getDeclaredStaticMethods())
					visitMethod(m); 
			}
		}
	}
	

	private boolean shouldExpandAggressively(jq_Class c) {
		return Utils.prefixMatch(c.getName(), appCodePrefixes);
	}

}
