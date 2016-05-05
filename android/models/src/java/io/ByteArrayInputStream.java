class ByteArrayInputStream
{
	@STAMP(flows = {@Flow(from="buf",to="this")})
	public  ByteArrayInputStream(byte[] buf) {}

	@STAMP(flows = {@Flow(from="buf",to="this"), @Flow(from="offset",to="this"), @Flow(from="length",to="this")})
	public  ByteArrayInputStream(byte[] buf, int offset, int length) {}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public synchronized  int read() { return 0; }

	@STAMP(flows = {@Flow(from="this",to="buffer"), @Flow(from="length",to="this")})
	public synchronized  int read(byte[] buffer, int offset, int length) { return 0; }
	
	@STAMP(origin="dr-modelgen-safe",flows = {@Flow(from="this",to="@return")})
	public synchronized int	available() { return 0; }
	
	@STAMP(origin="dr-modelgen",flows = {@Flow(from="byteCount",to="this"),@Flow(from="byteCount",to="@return"),@Flow(from="this",to="@return")})
	public synchronized long skip(long byteCount) { return 0; }
	
}
