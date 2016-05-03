class JSONArray
{
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(boolean value) { return this; }

	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(double value) throws org.json.JSONException { return this; }

	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(int value) { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(long value) { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(java.lang.Object value) { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(int index, boolean value) throws org.json.JSONException { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(int index, double value) throws org.json.JSONException { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(int index, int value) throws org.json.JSONException { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(int index, long value) throws org.json.JSONException { return this; }
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public  org.json.JSONArray put(int index, java.lang.Object value) throws org.json.JSONException { return this; }

}