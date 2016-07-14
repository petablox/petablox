class URI
{
	@STAMP(flows={@Flow(from="spec",to="this")})
	public  URI(java.lang.String spec) throws java.net.URISyntaxException 
	{ 
	}

	@STAMP(flows={@Flow(from="fragment",to="this")})
    public URI(java.lang.String scheme, java.lang.String schemeSpecificPart, java.lang.String fragment) throws java.net.URISyntaxException {
    }

	@STAMP(flows={@Flow(from="fragment",to="this"), @Flow(from="path",to="this")})
    public URI(java.lang.String scheme, java.lang.String host, java.lang.String path, java.lang.String fragment) throws java.net.URISyntaxException {
    }

	@STAMP(flows={@Flow(from="fragment",to="this"), @Flow(from="path",to="this"), @Flow(from="authority",to="this"), @Flow(from="query",to="this")})
    public URI(java.lang.String scheme, java.lang.String authority, java.lang.String path, java.lang.String query, java.lang.String fragment) throws java.net.URISyntaxException {
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String toASCIIString() {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String toString() {
		return new String();
    }

	@STAMP(flows={@Flow(from="uri",to="@return")})
    public static java.net.URI create(java.lang.String uri) {
		try{
			return new URI(uri);
		}catch(URISyntaxException e){
			return null;
		}
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.net.URL toURL() throws java.net.MalformedURLException {
		return new URL((String) null);
    }

}