class ProcessBuilder
{
	@STAMP(flows={@Flow(from="command",to="!PROCESS")})
	public  ProcessBuilder(java.lang.String... command) { }

	@STAMP(flows={@Flow(from="command",to="!PROCESS")})
	public  ProcessBuilder(java.util.List<java.lang.String> command) { }
	
	@STAMP(flows={@Flow(from="command",to="!PROCESS")})
	public  java.lang.ProcessBuilder command(java.lang.String... command) { return this; }

	@STAMP(flows={@Flow(from="command",to="!PROCESS")})
	public  java.lang.ProcessBuilder command(java.util.List<java.lang.String> command) { return this; }

    public java.lang.Process start() throws java.io.IOException {
		return new FakeProcess();
    }

}
