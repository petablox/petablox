class AbstractHttpClient
{
	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public final  org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest request) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}

	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public final  org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest request, org.apache.http.protocol.HttpContext context) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}

	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public final  org.apache.http.HttpResponse execute(org.apache.http.HttpHost target, org.apache.http.HttpRequest request) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}

	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public final  org.apache.http.HttpResponse execute(org.apache.http.HttpHost target, org.apache.http.HttpRequest request, org.apache.http.protocol.HttpContext context) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}

	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public <T> T execute(org.apache.http.client.methods.HttpUriRequest request, org.apache.http.client.ResponseHandler<? extends T> responseHandler) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}

	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public <T> T execute(org.apache.http.client.methods.HttpUriRequest request, org.apache.http.client.ResponseHandler<? extends T> responseHandler, org.apache.http.protocol.HttpContext context) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}
	
	@STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	 )
	public <T> T execute(org.apache.http.HttpHost target, org.apache.http.HttpRequest request, org.apache.http.client.ResponseHandler<? extends T> responseHandler) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}

    @STAMP(
		   flows = {@Flow(from="request",to="!INTERNET")}
	)
	public <T> T execute(org.apache.http.HttpHost target, org.apache.http.HttpRequest request, org.apache.http.client.ResponseHandler<? extends T> responseHandler, org.apache.http.protocol.HttpContext context) throws java.io.IOException, org.apache.http.client.ClientProtocolException 
	{ 
		return null;
	}
}
