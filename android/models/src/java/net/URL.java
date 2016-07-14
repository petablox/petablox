class URL
{
	@STAMP(flows={@Flow(from="!INTERNET",to="this"),@Flow(from="spec",to="this")})
	public  URL(java.lang.String spec) throws java.net.MalformedURLException 
	{
	}

	@STAMP(flows={@Flow(from="!INTERNET",to="this"),@Flow(from="spec",to="spec")})
	public  URL(java.net.URL context, java.lang.String spec) throws java.net.MalformedURLException 
	{ 
	}

	@STAMP(flows={@Flow(from="!INTERNET",to="this"),@Flow(from="spec",to="this")})
	public  URL(java.net.URL context, java.lang.String spec, java.net.URLStreamHandler handler) throws java.net.MalformedURLException 
	{ 
	}

	@STAMP(flows={@Flow(from="!INTERNET",to="this"),@Flow(from="file",to="this")})
	public  URL(java.lang.String protocol, java.lang.String host, java.lang.String file) throws java.net.MalformedURLException 
	{
	}

	@STAMP(flows={@Flow(from="!INTERNET",to="this"),@Flow(from="file",to="this")})
	public  URL(java.lang.String protocol, java.lang.String host, int port, java.lang.String file) throws java.net.MalformedURLException 
	{ 
	}

	@STAMP(flows={@Flow(from="!INTERNET",to="this"),@Flow(from="file",to="this")})
	public  URL(java.lang.String protocol, java.lang.String host, int port, java.lang.String file, java.net.URLStreamHandler handler) throws java.net.MalformedURLException 
	{ 
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.net.URLConnection openConnection() throws java.io.IOException 
	{ 
		return new StampURLConnection(this);
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.net.URLConnection openConnection(java.net.Proxy proxy) throws java.io.IOException 
	{ 
		return new StampURLConnection(this);
	}
	
	@STAMP(flows={@Flow(from="this",to="!this")})
	public final  java.lang.Object getContent() throws java.io.IOException 
	{ 
		return null;
	}
	
	@STAMP(flows={@Flow(from="this",to="!this")})
	public final  java.lang.Object getContent(java.lang.Class[] types) throws java.io.IOException 
	{ 
		return null;
	}

	@STAMP(flows={@Flow(from="this",to="!this"),@Flow(from="this",to="@return")})
    public final java.io.InputStream openStream() throws java.io.IOException {
		return new StampURLInputStream();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String getFile() {
        return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String getPath() {
        return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String getQuery() {
        return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String toString() {
        return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String toExternalForm() {
		return new String();
    }
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.net.URI toURI() throws java.net.URISyntaxException {
		return new URI((String)null);
    }
}
