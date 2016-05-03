class LocationManager
{
	@STAMP(flows={@Flow(from="$LOCATION",to="@return")})
	private Location getLocation()
	{
		return new Location((String) null);
	}

	private void registerListener(final android.location.LocationListener listener)
	{
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						listener.onLocationChanged(getLocation());
					}
				});
	}

	@STAMP(flows={@Flow(from="$FINE_LOCATION",to="!INTENT")})
        public  void requestLocationUpdates(java.lang.String provider, long minTime, float minDistance, android.app.PendingIntent intent) {

	}
	
	public  void requestLocationUpdates(java.lang.String provider, long minTime, float minDistance, android.location.LocationListener listener) 
	{ 
		registerListener(listener);
	}

	public  void requestLocationUpdates(java.lang.String provider, long minTime, float minDistance, android.location.LocationListener listener, android.os.Looper looper) 
	{ 
		registerListener(listener);
	}

	public  void requestLocationUpdates(long minTime, float minDistance, android.location.Criteria criteria, android.location.LocationListener listener, android.os.Looper looper) 
	{ 
		registerListener(listener);
	}

        @STAMP(flows={@Flow(from="$FINE_LOCATION",to="!INTENT")})
        public  void requestLocationUpdates(long minTime, float minDistance, android.location.Criteria criteria, android.app.PendingIntent intent) {
	}

	public  void requestSingleUpdate(java.lang.String provider, android.location.LocationListener listener, android.os.Looper looper) 
	{ 
		registerListener(listener);
	}

	public  void requestSingleUpdate(android.location.Criteria criteria, android.location.LocationListener listener, android.os.Looper looper) 
	{
		registerListener(listener);
	}

	@STAMP(flows={@Flow(from="$FINE_LOCATION",to="!INTENT")})
	public  void requestSingleUpdate(java.lang.String provider, android.app.PendingIntent intent) {
	}

	@STAMP(flows={@Flow(from="$FINE_LOCATION",to="!INTENT")})
	public  void requestSingleUpdate(android.location.Criteria criteria, android.app.PendingIntent intent) {
	}

	public  android.location.Location getLastKnownLocation(java.lang.String provider) 
	{ 
		return getLocation();
	}

	private static LocationManager locationManager = new LocationManager();
	public static LocationManager getInstance()
	{
		return locationManager;
	}

        @STAMP(flows={@Flow(from="$FINE_LOCATION",to="!INTENT")})
        public  void addProximityAlert(double latitude, double longitude, float radius, long expiration, android.app.PendingIntent intent) {
	}

	@STAMP(flows={@Flow(from="$FINE_LOCATION.nmea",to="@return")})
	private String getNmea()
	{
		return new String();
	}

        public  boolean addNmeaListener(final android.location.GpsStatus.NmeaListener listener) {
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
					    listener.onNmeaReceived(0,getNmea());
					}
				});
		return true;
	}

}
