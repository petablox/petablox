class BufferedOutputStream 
{
	@STAMP(flows={@Flow(from="out",to="this")})	
    public BufferedOutputStream(java.io.OutputStream out) { super((java.io.OutputStream)null);}
	
	@STAMP(flows={@Flow(from="out",to="this")})	
	public BufferedOutputStream(java.io.OutputStream out, int size) { super((java.io.OutputStream)null);}
}
