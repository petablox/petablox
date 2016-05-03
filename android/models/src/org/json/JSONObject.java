class JSONObject 
{
	@STAMP(flows={@Flow(from="name",to="this"),@Flow(from="value",to="this")})
	public  org.json.JSONObject put(java.lang.String name, boolean value) throws org.json.JSONException { 
		return this;
	}

	@STAMP(flows={@Flow(from="name",to="this"),@Flow(from="value",to="this")})
	public  org.json.JSONObject put(java.lang.String name, double value) throws org.json.JSONException { 
		return this;
	}

	@STAMP(flows={@Flow(from="name",to="this"),@Flow(from="value",to="this")})
	public  org.json.JSONObject put(java.lang.String name, int value) throws org.json.JSONException { 
		return this;
	}

	@STAMP(flows={@Flow(from="name",to="this"),@Flow(from="value",to="this")})
	public  org.json.JSONObject put(java.lang.String name, long value) throws org.json.JSONException { 
		return this;
	}

	@STAMP(flows={@Flow(from="name",to="this"),@Flow(from="value",to="this")})
	public  org.json.JSONObject put(java.lang.String name, java.lang.Object value) throws org.json.JSONException { 
		return this;
	}

	@STAMP(flows={@Flow(from="name",to="this"),@Flow(from="value",to="this")})
	public  org.json.JSONObject putOpt(java.lang.String name, java.lang.Object value) throws org.json.JSONException { 
		return this;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String getString(java.lang.String name) throws org.json.JSONException {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.String toString() {
		return new String();
    }
}