class Environment
{
	@STAMP(flows={@Flow(from="$ExternalStorage",to="@return")})
	public static  java.io.File getExternalStorageDirectory() 
	{ 
		return new java.io.File((String)null);
	}

	@STAMP(flows={@Flow(from="$ExternalStorage",to="@return")})
	public static  java.io.File getExternalStoragePublicDirectory(java.lang.String type) 
	{ 
		return new java.io.File((String)null);
	}
}