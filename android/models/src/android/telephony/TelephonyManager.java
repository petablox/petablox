class TelephonyManager
{
	@STAMP(flows = {@Flow(from="$getDeviceSoftwareVersion",to="@return")})
    public  java.lang.String getDeviceSoftwareVersion() 
	{ 
		return new String();
	}

    @STAMP(flows = {@Flow(from="$getDeviceId",to="@return")})    
    public  java.lang.String getDeviceId()
    { 
		return new String();
    }
	
	@STAMP(flows = {@Flow(from="$getNetworkCountryIso",to="@return")})
	public  java.lang.String getNetworkCountryIso() 
	{ 
		return new String();
	}

	@STAMP(flows = {@Flow(from="$getSimSerialNumber",to="@return")})
	public  java.lang.String getSimSerialNumber() 
	{ 
		return new String();
	}

	@STAMP(flows = {@Flow(from="$SimOperator",to="@return")})
	public  java.lang.String getSimOperator() 
	{ 
		return new String();
	}
	
	@STAMP(flows = {@Flow(from="$SimOperatorName",to="@return")})
	public  java.lang.String getSimOperatorName() 
	{ 
		return new String();
	}
	
	@STAMP(flows = {@Flow(from="$SimCountryIso",to="@return")})
	public  java.lang.String getSimCountryIso() 
	{ 
		return new String();
	}

    @STAMP(flows = {@Flow(from="$getSubscriberId",to="@return")})    
	public  java.lang.String getSubscriberId() 
	{ 
		return new String();
	}

    @STAMP(flows = {@Flow(from="$getLine1Number",to="@return")})    
	public  java.lang.String getLine1Number() 
	{
		return new String();
	}

	@STAMP(flows = {@Flow(from="$getVoiceMailNumber",to="@return")})
	public  java.lang.String getVoiceMailNumber() 
	{ 
		return new String();
	}

	@STAMP(flows = {@Flow(from="$getVoiceMailNumber",to="@return")})
        public  java.lang.String getVoiceMailAlphaTag() { 
	    return new String(); 
	}

	@STAMP(flows = {@Flow(from="$incomingCallNumber",to="@return")})
	private String incomingCallNumber()
	{
		return new String();
	}

	@STAMP(flows = {@Flow(from="$NetworkOperatorName",to="@return")})
	public  java.lang.String getNetworkOperatorName() 
	{ 
		return new String();
	}

	@STAMP(flows = {@Flow(from="$NetworkOperator",to="@return")})
	public  java.lang.String getNetworkOperator() 
	{ 
		return new String();
	}


	public  void listen(final android.telephony.PhoneStateListener listener, int events) 
	{
		listener.onCallStateChanged(0, incomingCallNumber());
		listener.onCellLocationChanged(getCellLocation());
	}
	
	private static TelephonyManager telephonyManager = new TelephonyManager();
	public static TelephonyManager getInstance()
	{
		return telephonyManager;
	}

    public  android.telephony.CellLocation getCellLocation() {
	int i = 10;
	if (i > 0) {
	    return new android.telephony.cdma.CdmaCellLocation();
	} else {
	    return new android.telephony.gsm.GsmCellLocation();
	}
    }


}
