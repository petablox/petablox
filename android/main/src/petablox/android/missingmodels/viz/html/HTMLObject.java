package petablox.android.missingmodels.viz.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * A standard way of printing out HTML files.
 * 
 * @author Osbert Bastani
 */
public abstract class HTMLObject {
	private static int curId = 0;

	private final int id;
	private final String name;
	private Map<String,String> attributes = new HashMap<String,String>();
	private Map<String,String> styles = new HashMap<String,String>();

	protected void putAttribute(String key, String value) {
		this.attributes.put(key, value);
	}

	public void putStyle(String key, String value) {
		this.styles.put(key, value);
	}

	public HTMLObject(String name) {
		this.name = name;
		this.id = curId++;
	}

	public abstract String getInnerHTML();

	public String getId() {
		return "obobj" + id;
	}

	private String buildStyle() {
		if(this.styles.keySet().size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<String,String> entry : this.styles.entrySet()) {
			sb.append(entry.getKey() + ":" + entry.getValue() + ";");
		}
		return sb.toString();
	}

	@Override public String toString() {
		// build the header
		StringBuilder sb = new StringBuilder();
		sb.append("<" + this.name + " id=\"" + this.getId() + "\" style=\"" + this.buildStyle() + "\" ");
		for(Map.Entry<String,String> entry : this.attributes.entrySet()) {
			sb.append(entry.getKey() + "=\"" + entry.getValue() + "\" ");
		}

		// close and build the body and footer
		sb.append(">\n" + this.getInnerHTML() + "\n</" + this.name + ">\n");
		return sb.toString();
	}

	public static BreakObject breakObject = new BreakObject();

	private static class BreakObject extends HTMLObject {
		private BreakObject() {
			super("br");
		}

		@Override public String getInnerHTML() {
			return "";
		}

		@Override public String toString() {
			return "<br/>\n";
		}
	}

	public static class SpanObject extends HTMLObject {
		private String innerHTML;

		public SpanObject(String innerHTML) {
			super("span");
			this.innerHTML = innerHTML;
		}

		@Override public String getInnerHTML() {
			return this.innerHTML;
		}
	}

	public static class ButtonObject extends HTMLObject {
		private String innerHTML;

		public ButtonObject(String innerHTML) {
			super("button");
			this.innerHTML = innerHTML;
		}

		@Override public String getInnerHTML() {
			return this.innerHTML;
		}

		public void putOnClick(String onclick) {
			super.putAttribute("onclick", onclick);
		}
	}

	public static class DivObject extends HTMLObject {
		private List<HTMLObject> objects = new ArrayList<HTMLObject>();

		public DivObject() {
			super("div");
		}

		public void addObject(HTMLObject object) {
			this.objects.add(object);
		}

		public void addBreak() {
			this.objects.add(HTMLObject.breakObject);
		}

		public void removeLastObject() {
			if(objects.size() > 0) {
				objects.remove(objects.size()-1);
			}
		}

		@Override public String getInnerHTML() {
			StringBuilder sb = new StringBuilder();
			for(HTMLObject object : this.objects) {
				sb.append(object.toString());
			}
			return sb.toString();
		}
	}
}
