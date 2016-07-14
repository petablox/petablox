class HttpGet
{
	@STAMP(flows = {@Flow(from="uri",to="this")})
	public  HttpGet(java.net.URI uri) 
	{ 
	}

	@STAMP(flows = {@Flow(from="uri",to="this")})
	public  HttpGet(java.lang.String uri) 
	{ 
	}
}
