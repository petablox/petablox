class OutputStream
{
	@STAMP(flows={@Flow(from="buffer",to="!this")})
	public  void write(byte[] buffer) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="buffer",to="!this")})
	public  void write(byte[] buffer, int offset, int count) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="oneByte",to="!this")})
	public abstract  void write(int oneByte) throws java.io.IOException;
}