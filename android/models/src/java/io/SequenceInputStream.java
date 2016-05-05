class SequenceInputStream 
{
    @STAMP(flows={@Flow(from="s1",to="this"), @Flow(from="s2",to="this")})
    public SequenceInputStream(java.io.InputStream s1, java.io.InputStream s2) {}
	
}
