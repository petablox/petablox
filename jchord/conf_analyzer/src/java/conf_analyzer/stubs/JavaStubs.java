package conf_analyzer.stubs;

/*
 * Holds Java classes to be used as analyzable models
 */
public class JavaStubs {

		//runs its SECOND argument
	public static void stubThreadInit(Thread t, Runnable r) {
		r.run();
	}
	
	public static Object anIdentityFunc(Object o) {
		return o;
	}
	
	public static Object aNullReturn(Object o) {
		return null;
	}
	
	public static Runtime getRuntime() {
		try {
			Class runtime = Class.forName("java.lang.Runtime");
			return (Runtime) runtime.newInstance();
		} catch(Exception e) {}
		return null;
	}

	
}
