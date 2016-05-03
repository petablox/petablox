package petablox.android.missingmodels.analysis;

import soot.Scene;
import soot.SootMethod;
import petablox.android.missingmodels.util.StubLookup.StubLookupValue;
import petablox.android.missingmodels.util.StubModelSet.StubModel;

public class ModelClassifier {
	public static class ModelInfo {
		public final String sig;
		
		public final int numArgs;
		public final boolean toRet;
		public final boolean toThis;
		public final boolean fromThis;

		public ModelInfo(SootMethod method, String relationName, Integer firstArg, Integer secondArg) {
			this.sig = method.getSignature();
			this.numArgs = method.getParameterCount();
			this.toRet = secondArg == -1;
			this.toThis = !method.isStatic() && secondArg == 0;
			this.fromThis = !method.isStatic() && firstArg == 0;
		}
		
		public ModelInfo(StubLookupValue value) {
			this(value.method, value.relationName, value.firstArg, value.secondArg);
		}
		
		public ModelInfo(StubModel model) {
			this(Scene.v().getMethod(model.methodName), model.relationName, model.firstArg, model.secondArg);
		}
		
		public double[] featurize() {
			double[] features = new double[5];
			features[0] = 1.0;
			features[0] = (double)this.numArgs;
			features[1] = this.toRet ? 1.0 : 0.0;
			features[2] = this.toThis ? 1.0 : 0.0;
			features[3] = this.toThis ? 1.0 : 0.0;
			return features;
		}
	}
}
