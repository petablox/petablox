class BufferedInputStream 
{
	@STAMP(flows={@Flow(from="in",to="this")})	
    public BufferedInputStream(java.io.InputStream in) { super((java.io.InputStream)null);  }

	@STAMP(flows={@Flow(from="in",to="this")})		
    public BufferedInputStream(java.io.InputStream in, int size) { super((java.io.InputStream)null); }
}
