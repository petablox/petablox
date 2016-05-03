package petablox.android.missingmodels.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import petablox.android.missingmodels.util.FileManager;
import petablox.android.missingmodels.util.FileManager.FileType;
import petablox.android.missingmodels.util.StubModelSet;
import petablox.android.missingmodels.util.StubModelSet.ModelType;
import petablox.android.missingmodels.util.StubModelSet.StubModel;
import petablox.android.missingmodels.util.Util.Counter;
import petablox.android.missingmodels.util.Util.MultivalueMap;
import petablox.android.missingmodels.viz.flow.JCFLSolverFiles.StubModelSetInputFile;


public class Aggregation {
	private static String rootPath = "experiment2_results/";
	private static MultivalueMap<String,StubModel> stubModelsByApp = new MultivalueMap<String,StubModel>();
	private static StubModelSet groundTruth = null;
	private static Random random = new Random();
	
	public static List<String> randomize(List<String> input) {
		List<String> copy = new ArrayList<String>(input);
		List<String> output = new ArrayList<String>();
		while(copy.size() > 0) {
			int index = random.nextInt(copy.size());
			output.add(copy.get(index));
			copy.remove(index);
		}
		return output;
	}
	
	public static double getAverage(double[] result, int end) {
		if(end == 0) return 1.0;
		double sum = 0.0;
		for(int i=0; i<end; i++) {
			sum += result[i];
		}
		return sum/end;
	}
	
	public static double[] run(List<String> appNames) {		
		Set<StubModel> accumulatedModels = new HashSet<StubModel>();
		int prevModels = 0;
		double[] result = new double[appNames.size()];
		for(int i=0; i<appNames.size(); i++) {
			String appName = appNames.get(i);
			if(stubModelsByApp.get(appName).isEmpty()) {
				result[i] = getAverage(result, i);
			} else {
				accumulatedModels.addAll(stubModelsByApp.get(appName));
				int addedModels = accumulatedModels.size() - prevModels;
				//System.out.println("Step " + i + ": " + appName);
				//System.out.println((double)addedModels/stubModelsByApp.get(appName).size());
				result[i] = (double)addedModels/stubModelsByApp.get(appName).size();
				prevModels = accumulatedModels.size();
			}
		}
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		File root = new File("../" + rootPath);
		for(File appDir : root.listFiles()) {
			if(appDir.getCanonicalPath().equals("/home/obastani/Documents/projects/stamp/shord/stamp_output/app-reports.db")) {
				continue;
			}
			String[] tokens = appDir.getName().split("_");
			String appName = tokens[tokens.length-1];
			try {
				File outputDir = new File(appDir, "cfl/");
				FileManager manager = new FileManager(new File("../osbert/permanent/"), outputDir, new File("osbert/scratch"), true);
				if(groundTruth == null) {
					groundTruth = manager.read(new StubModelSetInputFile("StubModelSet.txt", FileType.PERMANENT));
				}
				StubModelSet m = manager.read(new StubModelSetInputFile("StubModelSet.txt", FileType.OUTPUT));
				stubModelsByApp.ensure(appName);
				for(StubModel s : m.keySet()) {
					stubModelsByApp.add(appName, s);
				}
			} catch(IOException e) {
				//e.printStackTrace();
			}
		}
		
		List<String> appNames = new ArrayList<String>();
		for(String appName : stubModelsByApp.keySet()) {
			appNames.add(appName);
		}

		// get common true models and count true models
		Counter<StubModel> stubModelCounts = new Counter<StubModel>();
		for(StubModel model : groundTruth.keySet()) {
			for(Set<StubModel> modelSet : stubModelsByApp.values()) {
				if(modelSet.contains(model)) {
					stubModelCounts.increment(model);
				}
			}
		}

		Set<StubModel> commonModels = new HashSet<StubModel>();
		int totalNumTrueModels = 0;
		int commonCount = 5;
		for(StubModel model : stubModelCounts.keySet()) {
			if(groundTruth.get(model) == ModelType.TRUE && stubModelCounts.getCount(model) >= commonCount) {
				commonModels.add(model);
			}
			if(groundTruth.get(model) == ModelType.TRUE) {
				totalNumTrueModels++;
			}
		}
		System.out.println("Total true models: " + totalNumTrueModels);
		System.out.println("Total models: " + stubModelCounts.keySet().size());

		int numTrials = 100;
		
		// count fraction after 20 apps
		double[] numTrueModelsAddedAverage = new double[appNames.size()];
		for(int i=0; i<numTrials; i++) {
			int[] numTrueModelsAdded = new int[appNames.size()];
			List<String> randomNames = randomize(appNames);
			Set<StubModel> addedModels = new HashSet<StubModel>();
			for(int j=0; j<randomNames.size(); j++) {
				numTrueModelsAdded[j] = j > 0 ? numTrueModelsAdded[j-1] : 0;
				for(StubModel model : stubModelsByApp.get(randomNames.get(j))) {
					if(groundTruth.get(model) == ModelType.TRUE && commonModels.contains(model) && !addedModels.contains(model)) {
						numTrueModelsAdded[j]++;
						addedModels.add(model);
					}
				}
			}
			for(int j=0; j<numTrueModelsAddedAverage.length; j++) {
				numTrueModelsAddedAverage[j] += (double)numTrueModelsAdded[j]/numTrials;
			}
		}
		double totalTrueModels = numTrueModelsAddedAverage[numTrueModelsAddedAverage.length-1];
		for(int i=0; i<numTrueModelsAddedAverage.length; i++) {
			System.out.println((double)numTrueModelsAddedAverage[i]/totalTrueModels);
		}
		//System.out.println((double)numTrueModelsAddedAverage[50]/totalTrueModels);
		int totalNumGroundTruthModels = 0;
		for(StubModel model : groundTruth.keySet()) {
			if(groundTruth.get(model) == ModelType.TRUE) {
				totalNumGroundTruthModels++;
			}
		}
		//System.out.println(counter);

		//run(appNames);
		double[] averageResult = new double[appNames.size()];
		for(int i=0; i<numTrials; i++) {
			double[] result = run(randomize(appNames));
			for(int j=0; j<result.length; j++) {
				averageResult[j] += result[j]/numTrials;	
			}
		}
		for(int i=0; i<averageResult.length; i++) {
			//System.out.println(averageResult[i]);
		}
	}
}
