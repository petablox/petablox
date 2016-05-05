class FilterOutputStream
{
	@STAMP(flows={@Flow(from="out",to="this")})	
    public FilterOutputStream(java.io.OutputStream out) {
		this.out = out;
    }
}