class HttpEntityEnclosingRequestBase
{
	@STAMP(flows = {@Flow(from="entity",to="this")})
	public  void setEntity(org.apache.http.HttpEntity entity) { 
	}
}