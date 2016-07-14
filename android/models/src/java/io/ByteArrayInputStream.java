class ByteArrayInputStream
{
	@STAMP(flows = {@Flow(from="buf",to="this")})
	public  ByteArrayInputStream(byte[] buf) {}

	@STAMP(flows = {@Flow(from="buf",to="this")})
	public  ByteArrayInputStream(byte[] buf, int offset, int length) {}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public synchronized  int read() { return 0; }

	@STAMP(flows = {@Flow(from="this",to="buffer")})
	public synchronized  int read(byte[] buffer, int offset, int length) { return 0; }
}
