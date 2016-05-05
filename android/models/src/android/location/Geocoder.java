class Geocoder
{
	@STAMP(flows={@Flow(from="latitude",to="@return"),@Flow(from="longitude",to="@return")})
	public  java.util.List<android.location.Address> getFromLocation(double latitude, double longitude, int maxResults) throws java.io.IOException 
	{ 
		java.util.List<android.location.Address> ret = new java.util.ArrayList<android.location.Address>();
		ret.add(taintedAddress(latitude, longitude));
		return ret;
	} 
	
	@STAMP(flows={@Flow(from="lowerLeftLatitude",to="@return"),@Flow(from="lowerLeftLongitude",to="@return"),@Flow(from="upperRightLatitude",to="@return"),@Flow(from="upperRightLongitude",to="@return")})
    public java.util.List<android.location.Address> getFromLocationName(java.lang.String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude) throws java.io.IOException {
		java.util.List<android.location.Address> ret = new java.util.ArrayList<android.location.Address>();
		ret.add(taintedAddress(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude));
		return ret;
    }

	@STAMP(flows={@Flow(from="latitude",to="@return"),@Flow(from="longitude",to="@return")})
	private static Address taintedAddress(double latitude, double longitude)
	{
		return new Address((java.util.Locale) null);
	}

	@STAMP(flows={@Flow(from="lowerLeftLatitude",to="@return"),@Flow(from="lowerLeftLongitude",to="@return"),@Flow(from="upperRightLatitude",to="@return"),@Flow(from="upperRightLongitude",to="@return")})
	private static Address taintedAddress(double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude)
	{
		return new Address((java.util.Locale) null);
	}
}