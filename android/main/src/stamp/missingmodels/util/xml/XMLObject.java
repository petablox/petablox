package stamp.missingmodels.util.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * A standard way of printing out XML files.
 * 
 * @author Osbert Bastani
 */
public abstract class XMLObject implements Serializable {
	private static final long serialVersionUID = 1L;
		
	/** The XML information for each node */
	private final String name;
	//private List<String> attributeKeys = new ArrayList<String>();
	private Map<String,String> attributes = new HashMap<String,String>();
	private boolean hasBody;

	public XMLObject(String name, boolean hasBody) {
		this.name = name;
		this.hasBody = hasBody;
	}

	public void putAttribute(String key, String value) {
		/*
		if(attributes.keySet().contains(key)) {
			this.attributeKeys.remove(key);
		}
		this.attributeKeys.add(key);
		*/
		this.attributes.put(key, value);
	}
	
	public Set<String> getAttributeKeys() {
		return this.attributes.keySet();
	}
	
	public String getAttribute(String key) {
		return this.attributes.get(key);
	}
	
	public abstract String getInnerXML(int tabs);
	public abstract List<XMLObject> getAllChildrenByName(String name);
	
	protected String getName() {
		return this.name;
	}
	
	@Override
	public String toString() {
		return this.toString(0);
	}

	public String toString(int tabs) {
		StringBuilder sb = new StringBuilder();

		// initial tabs
		for(int i=0; i<tabs; i++) {
			sb.append("\t");
		}
		
		// build the header
		sb.append("<" + this.name + " ");
		for(Map.Entry<String,String> entry : this.attributes.entrySet()) {
			sb.append(entry.getKey() + "=\"" + entry.getValue() + "\" ");
		}

		// close and build the body and footer
		if(this.hasBody) {
			// build the body
			sb.append(">\n" + this.getInnerXML(tabs+1) + "\n");
			
			// ending tabs
			for(int i=0; i<tabs; i++) {
				sb.append("\t");
			}
			
			// build the ending
			sb.append("</" + this.name + ">\n");
		} else {
			sb.append("/>\n");
		}
		return sb.toString();
	}
	
	public static class XMLContainerObject extends XMLObject {
		private static final long serialVersionUID = 1L;
		
		private List<XMLObject> children = new ArrayList<XMLObject>();
		
		public XMLContainerObject(String name) {
			super(name, true);
		}

		public void addChild(XMLObject child) {
			this.children.add(child);
		}
		
		@Override
		public List<XMLObject> getAllChildrenByName(String name) {
			List<XMLObject> result = new ArrayList<XMLObject>();
			for(XMLObject child : this.children) {
				if(child.getName().equals(name)) {
					result.add(child);
				}
				result.addAll(child.getAllChildrenByName(name));
			}
			return result;
		}

		@Override
		public String getInnerXML(int tabs) {
			StringBuilder sb = new StringBuilder();
			for(XMLObject child : this.children) {
				sb.append(child.toString(tabs));
			}
			return sb.toString();
		}
	}
	
	public static class XMLTextObject extends XMLObject {
		private static final long serialVersionUID = 1L;
		
		private String innerXML = null;
		
		public XMLTextObject(String name) {
			super(name, true);
		}

		public void setInnerXML(String innerXML) {
			this.innerXML = innerXML;
		}

		@Override
		public List<XMLObject> getAllChildrenByName(String name) {
			return new ArrayList<XMLObject>();
		}

		@Override
		public String getInnerXML(int tabs) {
			StringBuilder sb = new StringBuilder();
			
			// tabs
			for(int i=0; i<tabs; i++) {
				sb.append("\t");
			}
			
			// text
			sb.append(this.innerXML);
			
			return sb.toString();
		}
	}
	
	public static class XMLEmptyObject extends XMLObject {
		private static final long serialVersionUID = 1L;

		public XMLEmptyObject(String name) {
			super(name, false);
		}

		@Override
		public List<XMLObject> getAllChildrenByName(String name) {
			return new ArrayList<XMLObject>();
		}

		@Override
		public String getInnerXML(int tabs) {
			return "";
		}
	}
}
