class MapView
{
	public com.google.android.maps.Projection getProjection()
	{
		return new PixelConverter();
	}

	@STAMP(flows={@Flow(from="$LOCATION",to="@return")})
	public com.google.android.maps.GeoPoint getMapCenter()
	{
		return new GeoPoint(0,0);	
	}
}