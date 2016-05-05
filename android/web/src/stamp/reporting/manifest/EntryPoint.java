package stamp.reporting.manifest;

import java.util.*;
import org.json.*;

public class EntryPoint{
	public static final String ACTIVITY = "Activity";
	public static final String SERVICE = "Service";
	public static final String RECEIVER = "Receiver";
	
	private static final String[] ACTIVITY_METHODS = new String[]{
		"onCreate:(Landroid/os/Bundle;)V", "onStart:()V", "onResume:()V"
	};
	
	// TODO - verify signature for onStartCommand
	private static final String[] SERVICE_METHODS = new String[]{
		"onCreate:()V", "onStartCommand:(Landroid/content/Intent;II)"
	};
	
	private static final String[] RECEIVER_METHODS = new String[]{
		"onReceive:()V"
	};
	
	private JSONObject json;
	
	public EntryPoint(String klass, List<String> actions, String kind) throws Exception{
		json = new JSONObject();
		json.put("kind", kind);
		json.put("class", klass);
		json.put("actions", new JSONArray(actions));
		
		String[] methods;
		if(kind.equals(ACTIVITY)){ methods = ACTIVITY_METHODS; } 
		else if(kind.equals(SERVICE)){ methods = SERVICE_METHODS; }
		else if(kind.equals(RECEIVER)){ methods = RECEIVER_METHODS; } 
		else {
			throw new Error("Unexpected EntryPoint kind: " + kind);
		}
		
		JSONArray jsonMethods = new JSONArray();
		for(int i = 0; i < methods.length; i++){
			JSONObject obj = new JSONObject();
			obj.put("method", methods[i]);
			jsonMethods.put(obj);
		}
		json.put("methods", jsonMethods);
		
	}
	
	public JSONObject toJSON(){
		return json;
	}
}
