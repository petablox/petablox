class BasicNameValuePair
{
	private String name;
	private String value;

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  BasicNameValuePair(java.lang.String name, java.lang.String value) 
	{ 
		this.name = name;
		this.value = value;
	}

	public  java.lang.String getName() 
	{ 
		return name;
	}

	public  java.lang.String getValue() 
	{ 
		return value;
	}

	public  java.lang.String toString() 
	{  
		return name+value;
	}
}