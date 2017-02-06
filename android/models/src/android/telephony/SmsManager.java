class SmsManager
{
	@STAMP(flows={@Flow(from="text",to="!sendTextMessage"), @Flow(from="text",to="!destinationAddress")})
	public  void sendTextMessage(java.lang.String destinationAddress, java.lang.String scAddress, java.lang.String text, android.app.PendingIntent sentIntent, android.app.PendingIntent deliveryIntent) 
	{ 
	}

	public  java.util.ArrayList<java.lang.String> divideMessage(java.lang.String text) 
	{ 
		java.util.ArrayList<java.lang.String> result = new java.util.ArrayList<java.lang.String>();
		result.add(text);
		return result;
	}

	@STAMP(flows={@Flow(from="parts",to="!sendMultipartTextMessage"),@Flow(from="parts",to="!destinationAddress")})
	public  void sendMultipartTextMessage(java.lang.String destinationAddress, java.lang.String scAddress, java.util.ArrayList<java.lang.String> parts, java.util.ArrayList<android.app.PendingIntent> sentIntents, java.util.ArrayList<android.app.PendingIntent> deliveryIntents) 
	{ 
	}

	@STAMP(flows={@Flow(from="data",to="!sendDataMessage"), @Flow(from="data",to="!destinationAddress")})
	public  void sendDataMessage(java.lang.String destinationAddress, java.lang.String scAddress, short destinationPort, byte[] data, android.app.PendingIntent sentIntent, android.app.PendingIntent deliveryIntent) 
	{ 
	}

	private static SmsManager smsManager = new SmsManager();

	public static  android.telephony.SmsManager getDefault() 
	{ 
		return smsManager;
	}
}
