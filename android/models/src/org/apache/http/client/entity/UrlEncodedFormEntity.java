class UrlEncodedFormEntity
{
	@STAMP(flows = {@Flow(from="parameters",to="this")})
	public  UrlEncodedFormEntity(java.util.List<? extends org.apache.http.NameValuePair> parameters) throws java.io.UnsupportedEncodingException { 
		super((java.lang.String)null);
	}
	
	@STAMP(flows = {@Flow(from="parameters",to="this")})
	public  UrlEncodedFormEntity(java.util.List<? extends org.apache.http.NameValuePair> parameters	, java.lang.String encoding) throws java.io.UnsupportedEncodingException { 
		super((java.lang.String)null);
	}

}