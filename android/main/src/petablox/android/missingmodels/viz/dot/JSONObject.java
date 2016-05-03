package petablox.android.missingmodels.viz.dot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONObject implements JSONValue {

	public static class JSONString implements JSONValue {
		private final String value;

		public JSONString(String value) {
			this.value = value;
		}

		@Override
		public String toJSONString() {
			return "\"" + this.value + "\"";
		}
	}

	public static class JSONList implements JSONValue {
		private List<JSONValue> list = new ArrayList<JSONValue>();

		public void add(JSONValue value) {
			this.list.add(value);
		}

		@Override
		public String toJSONString() {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for(JSONValue value : this.list) {
				sb.append(value.toJSONString() + ",");
			}
			if(!this.list.isEmpty()) {
				sb.deleteCharAt(sb.length()-1);
			}
			sb.append("]");
			return sb.toString();
		}
	}

	public static class JSONBoolean implements JSONValue {
		private final boolean value;

		public JSONBoolean(boolean value) {
			this.value = value;
		}

		@Override
		public String toJSONString() {
			return value ? "true" : "false";
		}
	}

	private Map<String,JSONValue> map = new HashMap<String,JSONValue>();

	public void put(String key, JSONValue value) {
		this.map.put(key, value);
	}

	@Override
	public String toJSONString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for(Map.Entry<String,JSONValue> entry : this.map.entrySet()) {
			sb.append("\"" + entry.getKey() + "\":" + entry.getValue().toJSONString() + ",");
		}
		if(!this.map.isEmpty()) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("}");
		return sb.toString();
	}
}
