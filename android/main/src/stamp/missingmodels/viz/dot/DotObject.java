package stamp.missingmodels.viz.dot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stamp.missingmodels.util.Util.Pair;

public class DotObject {
	private int curId = 0;
	private class Node {
		private final int id;
		private final String name;

		private Map<String,String> properties = new HashMap<String,String>();

		private Node(String name) {
			this.id = curId++;
			this.name = name;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			Node other = (Node) obj;
			return this.name.equals(other.name);
		}

		private String toDotString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.id + "[label=\"" + this.name + "\"");
			for(Map.Entry<String,String> property : this.properties.entrySet()) {
				sb.append("," + property.getKey() + "=" + property.getValue());
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private final String name;
	private Map<String,Node> nodes = new HashMap<String,Node>();
	private Set<Pair<Node,Node>> edges = new HashSet<Pair<Node,Node>>();

	public DotObject(String name) {
		this.name = name;
	}

	private Node getNode(String name) {
		Node node = this.nodes.get(name);
		if(node == null) {
			node = new Node(name);
			this.nodes.put(name, node);
		}
		return node;
	}

	public void addEdge(String source, String sink) {
		Node sourceNode = this.getNode(source);
		Node sinkNode = this.getNode(sink);
		this.edges.add(new Pair<Node,Node>(sourceNode, sinkNode));
	}

	public void setNodeProperty(String name, String propertyKey, String propertyValue) {
		Node node = this.getNode(name);
		node.properties.put(propertyKey, propertyValue);
	}

	public String toDotString() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph " + this.name + " {\n");
		for(Node node : this.nodes.values()) {
			sb.append(node.toDotString() + "\n");
		}
		for(Pair<Node,Node> edge : this.edges) {
			sb.append(edge.getX().id + "->" + edge.getY().id + "\n");
		}
		sb.append("}\n");
		return sb.toString();
	}
}
