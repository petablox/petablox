package android.os;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

public class SystemProperties
{

	@STAMP(flows = {@Flow(from="$SystemProperties.get",to="@return")})
    public static String get(String key, String def) 
	{
        return new String();
    }
}
