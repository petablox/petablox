package java.net;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

public class StampURLOutputStream extends java.io.OutputStream
{
	@STAMP(flows={@Flow(from="buffer",to="!INTERNET")})
	public  void write(byte[] buffer) throws java.io.IOException 
	{ 
	}
	
	@STAMP(flows={@Flow(from="buffer",to="!INTERNET")})
	public  void write(byte[] buffer, int offset, int count) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="oneByte",to="!INTERNET")})
	public void write(int oneByte) throws java.io.IOException
	{
	}
}
