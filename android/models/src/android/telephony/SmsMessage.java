class SmsMessage
{
	@STAMP(flows={@Flow(from="pdu",to="@return")})
	public static  android.telephony.SmsMessage createFromPdu(byte[] pdu) 
	{ 
		return new SmsMessage(); 
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getServiceCenterAddress() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getOriginatingAddress() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getDisplayOriginatingAddress() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getMessageBody() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getDisplayMessageBody() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getPseudoSubject() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  long getTimestampMillis() 
	{ 
		return 0L;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isEmail() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getEmailBody() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String getEmailFrom() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int getProtocolIdentifier() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isReplace() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isCphsMwiMessage() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isMWIClearMessage() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isMWISetMessage() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isMwiDontStore() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  byte[] getUserData() 
	{ 
		return new byte[1];
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  byte[] getPdu() 
	{ 
		return new byte[1];
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int getStatusOnSim() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int getStatusOnIcc() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int getIndexOnSim() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int getIndexOnIcc() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int getStatus() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isStatusReportMessage() 
	{ 
		return false;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean isReplyPathPresent() 
	{ 
		return false;
	}
}