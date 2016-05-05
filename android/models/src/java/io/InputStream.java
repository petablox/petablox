class InputStream
{
    @STAMP(flows = {@Flow(from="this",to="buffer")})
	public  int read(byte[] buffer) throws java.io.IOException 
	{ 
		return 0; 
	}

    @STAMP(flows = {@Flow(from="this",to="buffer")})
	public  int read(byte[] buffer, int offset, int length) throws java.io.IOException 
	{ 
		return 0; 
	}

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public abstract  int read() throws java.io.IOException;
}