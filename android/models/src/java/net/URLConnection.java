class URLConnection
{
	@STAMP(flows={@Flow(from="field",to="this"),@Flow(from="newValue",to="this")})
	public  void addRequestProperty(java.lang.String field, java.lang.String newValue) {
	}

	@STAMP(flows={@Flow(from="field",to="this"),@Flow(from="newValue",to="this")})
    public void setRequestProperty(java.lang.String field, java.lang.String newValue) {
    }
}