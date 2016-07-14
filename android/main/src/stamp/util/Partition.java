package stamp.util;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
 
public class Partition<E>
{ 
	private LinkedHashMap<E,Element> objectToPartitionElementMap;
 
	public Partition() 
	{
		objectToPartitionElementMap = new LinkedHashMap();
	}
 
	public Element find(E object) 
	{
		Element partitionElement = 
			objectToPartitionElementMap.get(object);
		return findPartitionElement(partitionElement);
	}
 
	private Element findPartitionElement(Element partitionElement) 
	{
		Element parent = partitionElement.getParent();
		if (parent==null) {
			return partitionElement;
		}
		Element root = findPartitionElement(parent);
		partitionElement.setParent(root);
		return root;
	}
 
	public void makeSet(E object) 
	{
		Element partitionElement = new Element(object);
		objectToPartitionElementMap.put(object, partitionElement);
	}
 
	public Element union(Element subSet1Root, Element subSet2Root) 
	{
		Element result;
		if(subSet1Root == subSet2Root)
			return subSet1Root;
		if (subSet1Root.getRank() > subSet2Root.getRank()) {
			subSet2Root.setParent(subSet1Root);
			result = subSet1Root;
		}
		else if (subSet1Root.getRank() < subSet2Root.getRank()) {
			subSet1Root.setParent(subSet2Root);
			result = subSet2Root;
		}
		else {
			subSet1Root.incRank();
			subSet2Root.setParent(subSet1Root);
			result = subSet1Root;
		}
		return result;
	}

	public Collection<List<E>> allPartitions()
	{
		Map<Element,List<E>> m = new HashMap();
		for(E e : objectToPartitionElementMap.keySet()){
			Element root = find(e);
			List<E> l = m.get(root);
			if(l == null){
				l = new ArrayList();
				m.put(root, l);
			}
			l.add(e);
		}
		return m.values();
	}
	
	public static class Element
	{
		private Object value;
		private int rank;
		private Element parent;
 
		public Element(Object value) {
			this.value = value;
		}
 
		public Object getValue() {
			return value;
		}
 
		public int getRank() {
			return rank;
		}
		public void incRank() {
			this.rank += 1;
		}
 
		public Element getParent() {
			return parent;
		}
		public void setParent(Element parent) {
			this.parent = parent;
		}
	}
}