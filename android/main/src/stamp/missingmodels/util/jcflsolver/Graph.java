package stamp.missingmodels.util.jcflsolver;

import java.util.*;

import stamp.missingmodels.util.Util.MultivalueMap;
import stamp.missingmodels.util.Util.Pair;

public abstract class Graph {		
	public abstract void process(Edge edge);

	public abstract boolean isTerminal(int kind);

	public abstract int numKinds();

	public abstract int symbolToKind(String symbol);

	public abstract String kindToSymbol(int kind);

	public abstract String[] outputRels();

	public abstract short kindToWeight(int kind);

	public abstract boolean useReps();

	public Algorithm algo;

	//public Collection<Node> nodes = new ArrayList();
	public Map<String,Node> nodes = new HashMap<String,Node>();

	public Graph() {
		if(this.useReps()) {
			this.algo = new RepsAlgo(this);
		} else {
			this.algo = new KnuthsAlgo(this);
		}
	}

	public final void addNode(Node node) {
		nodes.put(node.name, node);
	}

	public final void addEdge(Node from, Node to, int kind, Edge edgeA, boolean addLabel) {
		int label = addLabel ? Math.max(edgeA.label(), -1) : -1;
		this.addEdgeInternal(from, to, kind, addLabel, label, edgeA.weight, edgeA, null);
	}

	public final void addEdge(Node from, Node to, int kind, Edge edgeA, Edge edgeB, boolean addLabel) {
		if(!edgeA.matchesLabel(edgeB))
			return;
		int label = addLabel ? Math.max(edgeA.label(), edgeB.label()) : -1;
		this.addEdgeInternal(from, to, kind, addLabel, label, (short)(edgeA.weight + edgeB.weight), edgeA, edgeB);
	}

	public final void setAlgo(Algorithm algo) {
		this.algo = algo;
	}

	public final Collection<Node> allNodes() {
		return nodes.values();
	}

	public Node getNode(String name) {
		Node node = this.nodes.get(name);
		if(node == null) {
			node = new Node(name, numKinds());
			this.nodes.put(name, node);
		}
		return node;
	}

	public void addInputEdge(String from, String to, int kind, int label) {
		this.addWeightedInputEdge(from, to, kind, label, (short)0);
	}

	public void addWeightedInputEdge(String from, String to, int kind, int label, short weight) {
		assert label >= 0;

		Node fromNode = getNode(from);
		Node toNode = getNode(to);

		Edge newEdge = new LabeledEdge(kind, fromNode, toNode, label);
		newEdge.weight = weight;
		fromNode.addInputOutEdge(newEdge);
		toNode.addInEdge(newEdge);
		algo.addEdge(newEdge, null);
	}

	public void addInputEdge(String from, String to, int kind) {
		this.addWeightedInputEdge(from, to, kind, (short)0);
	}

	public void addWeightedInputEdge(String from, String to, int kind, short weight) {

		Node fromNode = getNode(from);
		Node toNode = getNode(to);

		if(kind == symbolToKind("Src2Sink")) {
			System.out.println("ERROR: adding input Src2Sink edge!");
		}
		Edge newEdge = new NonLabeledEdge(kind, fromNode, toNode);
		newEdge.weight = weight;
		fromNode.addInputOutEdge(newEdge);
		toNode.addInEdge(newEdge);
		algo.addEdge(newEdge, null);
	}

	private void addEdgeInternal(Node from, Node to, int kind, boolean addLabel, int label, short weight, Edge edgeA, Edge edgeB) {
		Edge newEdge = (addLabel && label >= 0)  ? new LabeledEdge(kind, from, to, label) : new NonLabeledEdge(kind, from, to);
		newEdge.weight = weight;
		newEdge.firstInput = edgeA;
		newEdge.secondInput = edgeB;

		Edge oldEdge = from.addOutEdge(newEdge);
		if(oldEdge == null) {
			to.addInEdge(newEdge);
		}

		algo.addEdge(newEdge, oldEdge);
	}

	public List<Edge> getPositiveWeightInputs(Edge edge) {
		List<Edge> inputs = new LinkedList<Edge>();
		if(this.isTerminal(edge.kind)) {
			if(edge.weight > 0) {
				inputs.add(edge);
			}
			return inputs;
		} else {
			if(edge.firstInput != null) {
				inputs.addAll(this.getPositiveWeightInputs(edge.firstInput));
			}
			if(edge.secondInput != null) {
				inputs.addAll(this.getPositiveWeightInputs(edge.secondInput));
			}
		}
		return inputs;	
	}

	public Collection<Edge> getEdges(int kind) {
		List<Edge> allEdges = new LinkedList<Edge>();
		for(Node node : this.allNodes()) {
			for(Edge edge : node.getOutEdges(kind)) {
				allEdges.add(edge);
			}
		}
		return allEdges;
	}

	public Collection<Edge> getEdges(String symbol) {
		try {
			return this.getEdges(symbolToKind(symbol));
		} catch(Exception e) {
			return new ArrayList<Edge>();
		}
	}

	/*
	 * Retrieves the positive weight edges from the graph.
	 */
	public MultivalueMap<Edge,Pair<Edge,Boolean>> getPositiveWeightEdges(String symbol) {
		MultivalueMap<Edge,Pair<Edge,Boolean>> positiveWeightEdges = new MultivalueMap<Edge,Pair<Edge,Boolean>>();
		for(Edge edge : this.getEdges(symbol)) {
			positiveWeightEdges.ensure(edge);
			List<Pair<Edge,Boolean>> path = this.getPath(edge);
			for(Pair<Edge,Boolean> pair : path) {
				if(pair.getX().weight > 0) {
					positiveWeightEdges.add(edge, pair);
				}
			}
		}
		return positiveWeightEdges;
	}

	public List<Pair<Edge,Boolean>> getPath(Edge edge) {
		return this.getPath(edge, new HashSet<String>(), true);
	}

	public List<Pair<Edge,Boolean>> getPath(Edge edge, boolean forward) {
		return this.getPath(edge, new HashSet<String>(), forward);
	}

	public List<Pair<Edge,Boolean>> getPath(Edge edge, Set<String> terminals) {
		return this.getPath(edge, terminals, true);
	}

	public List<Pair<Edge,Boolean>> getPath(Edge edge, Set<String> terminals, boolean forward) {
		List<Pair<Edge,Boolean>> inputs = new ArrayList<Pair<Edge,Boolean>>();
		if(terminals.contains(kindToSymbol(edge.kind)) || (edge.firstInput == null && edge.secondInput == null)) {
			inputs.add(new Pair<Edge,Boolean>(edge, forward));
			return inputs;
		} else {
			if(edge.firstInput != null && edge.secondInput != null) {

				List<Pair<Edge,Boolean>> firstInputs = new LinkedList<Pair<Edge,Boolean>>();
				List<Pair<Edge,Boolean>> secondInputs = new LinkedList<Pair<Edge,Boolean>>();

				// -> = -> ->
				// edge(u,w) <- edge.firstInput(u,v) edge.secondInput(v,w)
				if(edge.from.id == edge.firstInput.from.id && edge.to.id == edge.secondInput.to.id) {
					firstInputs = this.getPath(edge.firstInput, terminals, forward);
					secondInputs = this.getPath(edge.secondInput, terminals, forward);

				}
				// -> = -> <-
				// edge(u,w) <- edge.firstInput(u,v) edge.secondInput(w,v)
				else if(edge.from.id == edge.firstInput.from.id && edge.to.id == edge.secondInput.from.id) {
					firstInputs = this.getPath(edge.firstInput, terminals, forward);
					secondInputs = this.getPath(edge.secondInput, terminals, !forward);
				}
				// -> = <- ->
				// edge(u,w) <- edge.firstInput(v,u) edge.secondInput(v,w)
				else if(edge.from.id == edge.firstInput.to.id && edge.to.id == edge.secondInput.to.id) {
					firstInputs = this.getPath(edge.firstInput, terminals, !forward);
					secondInputs = this.getPath(edge.secondInput, terminals, forward);
				}
				// -> = <- <-
				// edge(u,w) <- edge.firstInput(w,v) edge.secondInput(v,u)
				else if(edge.from.id == edge.firstInput.to.id && edge.to.id == edge.secondInput.from.id) {
					firstInputs = this.getPath(edge.firstInput, terminals, !forward);
					secondInputs = this.getPath(edge.secondInput, terminals, !forward);
				} else {
					// probably mixed up the paths
					Edge temp = edge.firstInput;
					edge.firstInput = edge.secondInput;
					edge.secondInput = temp;
					return this.getPath(edge, terminals, forward);
					/*
		    System.out.println("ERROR1!");
		    System.out.println("EDGE: (" + edge.from.id + "," + edge.to.id + "); (" + edge.firstInput.from.id + "," + edge.firstInput.to.id + "); (" + edge.secondInput.from.id + "," + edge.secondInput.to.id + ");");
		    System.out.println("PROD: " + kindToSymbol(edge.kind) + " <- " + kindToSymbol(edge.firstInput.kind) + ", " + kindToSymbol(edge.secondInput.kind));
					 */
				}
				// TODO: any other cases?

				if(forward) {
					inputs.addAll(firstInputs);
					inputs.addAll(secondInputs);
					return inputs;
				} else {
					inputs.addAll(secondInputs);
					inputs.addAll(firstInputs);
					return inputs;
				}
			} else if(edge.firstInput != null) {
				if(edge.from.id == edge.firstInput.from.id && edge.to.id == edge.firstInput.to.id) {
					inputs.addAll(this.getPath(edge.firstInput, terminals, forward));
					return inputs;
				} else {
					inputs.addAll(this.getPath(edge.firstInput, terminals, !forward));
					return inputs;
				}
				// TODO: any other cases?
			} else {
				System.out.println("ERROR!");
				return inputs;
			}
		}
	}
}