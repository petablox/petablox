class ExifInterface
{
	@STAMP(flows={@Flow(from="$ExifInterface.Attribute",to="@return")})
	public  java.lang.String getAttribute(java.lang.String tag) { 
		return new String();
	}
	
	@STAMP(flows={@Flow(from="$ExifInterface.Attribute",to="@return")})
	public  double getAttributeDouble(java.lang.String tag, double defaultValue) { 
		return 0.0;
	}

	@STAMP(flows={@Flow(from="$ExifInterface.Attribute",to="@return")})
	public  int getAttributeInt(java.lang.String tag, int defaultValue) { 
		return 0;
	}

	@STAMP(flows={@Flow(from="$ExifInterface.Attribute",to="@return")})
	public  double getAltitude(double defaultValue) { 
		return 0.0;
	}
	
	public  boolean getLatLong(float[] output) { 
		output[0] = taintedFloat();
		return true;
	}
	
	@STAMP(flows={@Flow(from="$ExifInterface.LatLong",to="@return")})
	private static float taintedFloat(){
		return 0.0f;
	}

	@STAMP(flows={@Flow(from="$ExifInterface.Thumbnail",to="@return")})
	public  byte[] getThumbnail() { 
		return new byte[0];
	}
}