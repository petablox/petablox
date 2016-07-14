package java.lang;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

public class FakeProcess extends Process
{
	public FakeProcess() {}

	public void destroy() {}

	@STAMP(flows = {@Flow(from="$PROCESS.ExitValue",to="@return")})
	public int exitValue() { return 0; }

	@STAMP(flows = {@Flow(from="$PROCESS.ErrorStream",to="@return")})
	public java.io.InputStream getErrorStream() 
	{ 
		return new java.io.StampInputStream();
	}

	@STAMP(flows = {@Flow(from="$PROCESS.InputStream",to="@return")})
	public java.io.InputStream getInputStream() 
	{ 
		return new java.io.StampInputStream();
	}

	@STAMP(flows = {@Flow(from="!PROCESS.OutputStream",to="@return")})
	public java.io.OutputStream getOutputStream() 
	{ 
		return new java.io.StampOutputStream(); 
	}

	public int waitFor() throws java.lang.InterruptedException { return 0; }
}
