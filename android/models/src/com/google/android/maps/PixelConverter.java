class PixelConverter
{
    @STAMP(flows={@Flow(from="$LOCATION",to="@return")})		
	public com.google.android.maps.GeoPoint fromPixels(int param1, int param2)
	{
		return new GeoPoint(0,0);
	}
}

