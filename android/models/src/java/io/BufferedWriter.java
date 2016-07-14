class BufferedWriter 
{
	@STAMP(flows={@Flow(from="out",to="this")})	
    public BufferedWriter(java.io.Writer out) { }
	
	@STAMP(flows={@Flow(from="out",to="this")})	
    public BufferedWriter(java.io.Writer out, int size) { }	
}
