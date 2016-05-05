package java.net;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

public class StampURLInputStream extends java.io.InputStream
{
    @STAMP(flows = {@Flow(from="this",to="@return")})
	private int taintInt() { return 0; }

	public int read() throws java.io.IOException {
		return taintInt();
	}
}
