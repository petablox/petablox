class BufferedInputStream 
{
	@STAMP(flows={@Flow(from="in",to="this")})	
    public BufferedInputStream(java.io.InputStream in) { super((java.io.InputStream)null);  }

	@STAMP(flows={@Flow(from="in",to="this")})		
    public BufferedInputStream(java.io.InputStream in, int size) { super((java.io.InputStream)null); }
    
    @STAMP(origin="dr-modelgen-safe",flows={@Flow(from="this",to="@return"),@Flow(from="this",to="buffer"),@Flow(from="byteCount",to="this"),@Flow(from="byteCount",to="@return"),@Flow(from="byteCount",to="buffer")})
    public int read (byte[] buffer, int byteOffset, int byteCount) { return 0; }
}
