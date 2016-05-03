class InputStreamEntity
{
	@STAMP(flows = {@Flow(from="instream",to="this")})
	public  InputStreamEntity(java.io.InputStream instream, long length) { 
	}
}