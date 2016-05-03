package stamp.reporting;

import java.util.*;

public class ParameterValueSet{
	
	private HashSet<String> values;
	private String type;

	public ParameterValueSet(){
		values = new HashSet<String>();
		type = "UnsetType";
	}

	public String getType(){
		return type;
	}

	public void setType(String type){
		this.type = type;
	}

	public HashSet<String> getValues(){
		return values;
	}

	public void addValue(String s){
		if(values.contains(s))
			return;
		values.add(s);
	}
}
