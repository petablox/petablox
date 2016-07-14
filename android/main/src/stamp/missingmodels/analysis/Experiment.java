package stamp.missingmodels.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import stamp.missingmodels.analysis.JCFLSolverRunner.RelationAdder;
import stamp.missingmodels.util.FileManager.FileType;
import stamp.missingmodels.util.FileManager.StampOutputFile;
import stamp.missingmodels.util.StubModelSet;
import stamp.missingmodels.util.StubModelSet.ModelType;
import stamp.missingmodels.util.StubModelSet.StubModel;
import stamp.missingmodels.util.Util.Pair;
import stamp.missingmodels.util.jcflsolver.Edge;
import stamp.missingmodels.util.jcflsolver.Graph;

public class Experiment implements StampOutputFile {
	private JCFLSolverRunner j;
	private Class<? extends Graph> c;
	private String appDirectory;
	
	private List<ProposedStubModelSet> proposed = new ArrayList<ProposedStubModelSet>();

	/*
	 * Name of the experimental results file.
	 * @see stamp.missingmodels.util.FileManager.StampFile#getName()
	 */
	@Override
	public String getName() {
		return "results.txt";
	}

	/*
	 * File type of the experimental results file.
	 * @see stamp.missingmodels.util.FileManager.StampFile#getType()
	 */
	@Override
	public FileType getType() {
		return FileType.OUTPUT;
	}

	/*
	 * Content of the experimental results file.
	 * @see stamp.missingmodels.util.FileManager.StampOutputFile#getContent()
	 */
	@Override
	public String getContent() {
		StringBuilder sb = new StringBuilder();
		sb.append("Results file name: ").append(this.appDirectory).append("\n");
		sb.append("Number of proposed models: ").append(this.getNumProposed()).append("\n");
		sb.append("Number of rounds: ").append(this.getNumRounds()).append("\n");
		sb.append("Accuracy: ").append(this.getAccuracy()).append("\n");
		sb.append("Number of flows [before,after]: ").append(this.getNumFlows()).append("\n");
		return sb.toString();
	}
	
	/*
	 * Returns the runner.
	 */
	public JCFLSolverRunner j() {
		return this.j;
	}
	
	/*
	 * Returns a list of all proposed models.
	 */
	public ProposedStubModelSet getAllProposedModels() {
		ProposedStubModelSet allProposed = new ProposedStubModelSet();
		for(ProposedStubModelSet m : this.proposed) {
			allProposed.putAll(m);
		}
		return allProposed;
	}
	
	/*
	 * Statistic 1: The number of proposed models.
	 */
	public int getNumProposed() {
		return this.getAllProposedModels().size();
	}
	
	/*
	 * Statistic 2: The number of rounds.
	 */
	public int getNumRounds() {
		return this.proposed.size();
	}
	
	/*
	 * Statistic 3: The accuracy.
	 */
	public float getAccuracy() {
		int totalCount = 0;
		int correctCount = 0;
		ProposedStubModelSet allProposed = this.getAllProposedModels();
		for(Map.Entry<StubModel,ModelType> entry : allProposed.entrySet()) {
			totalCount++;
			if(entry.getValue() == allProposed.getData(entry.getKey()).getX()) {
				correctCount++;
			}
		}
		return (float)correctCount/totalCount;
	}
	
	/*
	 * Statistic 3: The number of flows before and after.
	 */
	public Pair<Integer,Integer> getNumFlows() {
		// STEP 0: Get the graph edges.
		List<Edge> edges = new ArrayList<Edge>(this.j.g().getEdges("Src2Sink"));
		
		// STEP 1: Count the edges.
		int numZeroWeightEdges = 0;
		for(Edge edge : edges) {
			if(edge.weight == 0) {
				numZeroWeightEdges++;
			}
		}
		
		// STEP 2: Return the result.
		return new Pair<Integer,Integer>(numZeroWeightEdges, edges.size()); 
	}

	/*
	 * @param groundTruthModels The ground truth stub models.
	 * @param initialModels The set of models to use initially.
	 * @param defaultModelValue If a model is not present, how
	 * to treat it. Default is UNKNOWN.
	 */ 
	public void run(StubModelSet groundTruthModels, StubModelSet initialModels, RelationAdder relationLookup, ModelType defaultModelType) {
		// STEP 0: Initilize the total set of models
		StubModelSet total = new StubModelSet();
		total.putAll(initialModels);
		
		// STEP 1: Iteratively run the solver and add models.
		ProposedStubModelSet curProposed;
		int round = 0;
		do {
			// STEP 1a: Run the analysis.
			this.j.run(this.c, total, relationLookup);
			
			// STEP 1b: Get the proposed models.
			curProposed = this.j.getProposedModels(round++);

			// STEP 1c: Set the proposed models to their ground truths, if available.
			// Otherwise, set them to unknown.
			for(StubModel model : curProposed.keySet()) {
				// For now, not containing a model is equivalent to its having value 0.
				// TODO: Is there a situation where we would want to keep this separate?
				if(groundTruthModels.get(model) != ModelType.UNKNOWN) {
					curProposed.put(model, groundTruthModels.get(model), curProposed.getData(model));
				} else {
					curProposed.put(model, ModelType.UNKNOWN, curProposed.getData(model));
				}
			}

			// STEP 1d: Remove models already proposed from the currently proposed models.
			// Note that if we don't do this, and an unknown model is encountered, the
			// experiment will loop infinitely, since the unknown model will remain in
			// curProposed.
			for(StubModel model : total.keySet()) {
				curProposed.remove(model);
			}
			
			// STEP 1e: Add the proposed models to the total set of models. Use the
			// default value if necessary.
			for(Map.Entry<StubModel,ModelType> entry : curProposed.entrySet()) {
				if(curProposed.get(entry.getKey()) == ModelType.UNKNOWN){
					total.put(entry.getKey(), defaultModelType);
				} else {
					total.put(entry.getKey(), entry.getValue());
				}
			}

			// STEP 1f: Add the proposed models to the list.
			this.proposed.add(curProposed);
		} while(!curProposed.isEmpty());
	}
	
	public void run(StubModelSet groundTruthModels, StubModelSet initialModels, RelationAdder relationLookup) {
		run(groundTruthModels, initialModels, relationLookup, ModelType.UNKNOWN); 
	}
	
	/*
	 * Initializes the experiment
	 * @param cj The class of the runner to use to propose models.
	 * @param cg The class of graph to use.
	 */
	public Experiment(Class<? extends JCFLSolverRunner> cj, Class<? extends Graph> c, String appDirectory) {
		try {
			this.j = cj.newInstance();
			this.c = c;
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing jcfl runner: " + c.toString());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing jcfl runner: " + c.toString());
		}
		this.appDirectory = appDirectory;
	}
	
	/*
	 * A stub model class augmented with some data.
	 */
	public abstract static class StubModelSetWithData<T> extends StubModelSet {
		private static final long serialVersionUID = 906602907093081931L;
		
		private Map<StubModel,T> data = new HashMap<StubModel,T>();
		
		public T getData(StubModel model) {
			T datum = this.data.get(model);
			if(datum == null) {
				datum = this.defaultValue();
				this.data.put(model, datum);
			}
			return datum;
		}
		
		public String getDataString(StubModel model) {
			return this.toString(this.getData(model));
		}
		
		public Set<Map.Entry<StubModel,T>> dataEntrySet() {
			return this.data.entrySet();
		}
		
		public void dataPutAll(Map<StubModel,T> data) {
			this.data.putAll(data);
		}
		
		@Override
		public ModelType put(StubModel key, ModelType value) {
			return this.put(key, value, this.defaultValue());
		}
		
		public ModelType put(StubModel key, ModelType value, T data) {
			this.data.put(key, data);
			return super.put(key, value);
		}
		
		@Override
		public void putAll(Map<? extends StubModel,? extends ModelType> map) {
			super.putAll(map);
			for(StubModel model : map.keySet()) {
				this.data.put(model, this.defaultValue());
			}
		}

		public void putAll(StubModelSetWithData<T> m) {
			super.putAll(m);
			this.dataPutAll(m.data);
		}
		
		public abstract T defaultValue();
		
		public abstract String toString(T data);
		public abstract T parseData(String representation);
	}
	
	/*
	 * A stub model class where the data is (proposed,round).
	 * Default proposed value is 0, default round value is -1.
	 */
	public static class ProposedStubModelSet extends StubModelSetWithData<Pair<ModelType,Integer>> {
		public static final int DEFAULT_ROUND = -1;
		private static final long serialVersionUID = 4715908928086091234L;
		
		private ModelType defaultProposedValue = ModelType.UNKNOWN;
		
		public void setDefaultValue(ModelType defaultProposedValue) {
			this.defaultProposedValue = defaultProposedValue;
		}
		
		public ModelType put(StubModel key, ModelType value, ModelType proposed, int round) {
			return super.put(key, value, new Pair<ModelType,Integer>(proposed, round));
		}

		@Override
		public Pair<ModelType,Integer> defaultValue() {
			return new Pair<ModelType,Integer>(this.defaultProposedValue, DEFAULT_ROUND);
		}

		@Override
		public String toString(Pair<ModelType,Integer> data) {
			return data.toString();
		}

		@Override
		public Pair<ModelType,Integer> parseData(String representation) {
			String[] tokens = representation.substring(1, representation.length()-1).split(",");
			return new Pair<ModelType,Integer>(ModelType.getModelType(Integer.parseInt(tokens[0])), Integer.parseInt(tokens[1]));
		}
	}
}
