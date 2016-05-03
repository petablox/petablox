class CallLog
{
	static {
		CONTENT_URI = taintedCallLog();
	}

	@STAMP(flows = {@Flow(from="$CallLog",to="@return")})
	private static android.net.Uri taintedCallLog()
	{
		return new android.net.StampUri("");
	}

	class Calls {
		static {
			CONTENT_URI = taintedCallLog();
			CONTENT_FILTER_URI = taintedCallLog();
		}
		
		@STAMP(flows = {@Flow(from="$CallLog.LastOutgoingCall",to="@return")})
		public static  java.lang.String getLastOutgoingCall(android.content.Context context)
		{
			return new String();
		}
	}

}