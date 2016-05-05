class Address {

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public java.lang.String getAddressLine(int index) {
        return new String();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public java.lang.String getSubLocality() {
        return new String();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
	public java.lang.String getLocality() {
        return new String();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public java.lang.String getCountryCode() {
        return new String();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
	public java.lang.String getCountryName() {
        return new String();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public double getLongitude() {
        return 0.0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
	public double getLatitude() {
        return 0.0;
    }
	
    @STAMP(flows = { @Flow(from = "this", to = "@return") })
	public java.lang.String getAdminArea() {
		return new String();
    }
}

