
import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

class CalendarContract
{
    static {
	CONTENT_URI = taintedUri();
    }

    class Instances
    {
	static { 
	    CONTENT_URI = taintedUri();
	    CONTENT_BY_DAY_URI = taintedUri();
	    CONTENT_SEARCH_URI = taintedUri();
	    CONTENT_SEARCH_BY_DAY_URI = taintedUri();
	}

	@STAMP(flows = {@Flow(from="$CALENDAR",to="@return")})
	public static final  android.database.Cursor query(android.content.ContentResolver cr, java.lang.String[] projection, long begin, long end) {
	    return new android.test.mock.MockCursor();
	}

	@STAMP(flows = {@Flow(from="$CALENDAR",to="@return")})
	public static final  android.database.Cursor query(android.content.ContentResolver cr, java.lang.String[] projection, long begin, long end, java.lang.String searchQuery) {
	    return new android.test.mock.MockCursor();
	}
    }

    class Calendars
    {
	static { 
	    CONTENT_URI = taintedUri();
	}
    }	

    class Attendees
    {
	static { 
	    CONTENT_URI = taintedUri();
	}

	@STAMP(flows = {@Flow(from="$CALENDAR.attendees",to="@return")})
	public static final  android.database.Cursor query(android.content.ContentResolver cr, long eventId, java.lang.String[] projection) { 
	    return new android.test.mock.MockCursor();  
	}
    }

    class Reminders
    {
	static { 
	    CONTENT_URI = taintedUri();
	}

	@STAMP(flows = {@Flow(from="$CALENDAR.reminder",to="@return")})
	public static final  android.database.Cursor query(android.content.ContentResolver cr, long eventId, java.lang.String[] projection) { 
	    return new android.test.mock.MockCursor(); 
	}
    }

    @STAMP(flows = {@Flow(from="$CALENDAR",to="@return")})
	private static android.net.Uri taintedUri()
    {
	return new android.net.StampUri("");
    }
}