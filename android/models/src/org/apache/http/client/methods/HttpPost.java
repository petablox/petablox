class HttpPost
{
	@STAMP(flows = {@Flow(from="uri",to="this")})
	public  HttpPost(java.net.URI uri) 
	{ 
	}

	@STAMP(flows = {@Flow(from="uri",to="this")})
	public  HttpPost(java.lang.String uri) 
	{ 
	}
}
