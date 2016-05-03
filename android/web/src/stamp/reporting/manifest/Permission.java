package stamp.reporting.manifest;

import java.util.*;
import org.json.*;

public class Permission{
	private String name;
	private List<String> sourceLabels;
	private List<String> sinkLabels;
	
	public Permission(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	/*public List<String> getSourceLabels(){
		return sourceLabels;
	}
	
	public List<String> getSinkLabels(){
		return sinkLabels;
	}*/
	
	public JSONObject toJSON() throws Exception{
		JSONObject json = new JSONObject();
		json.put("name", name.toLowerCase());
		return json;
	}
}