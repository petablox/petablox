class WebView
{
	@STAMP(flows={@Flow(from="url",to="!WebView")})
	public  void loadUrl(java.lang.String url, java.util.Map<java.lang.String, java.lang.String> additionalHttpHeaders) 
	{ 
	}

	@STAMP(flows={@Flow(from="url",to="!WebView")})
	public  void loadUrl(java.lang.String url) 
	{ 
	}
 
	@STAMP(flows={@Flow(from="url",to="!WebView"),@Flow(from="postData",to="!WebView")})
	public  void postUrl(java.lang.String url, byte[] postData) 
	{ 
	}

	@STAMP(flows={@Flow(from="data",to="!WebView")})
	public  void loadData(java.lang.String data, java.lang.String mimeType, java.lang.String encoding) 
	{ 
	}

	@STAMP(flows={@Flow(from="baseUrl",to="!WebView"),@Flow(from="data",to="!WebView")})
	public  void loadDataWithBaseURL(java.lang.String baseUrl, java.lang.String data, java.lang.String mimeType, java.lang.String encoding, java.lang.String historyUrl) 
	{ 
	}

}