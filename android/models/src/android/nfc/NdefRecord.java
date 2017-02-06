class NdefRecord
{
	private NdefRecord(){}

	@STAMP(flows={@Flow(from="payload",to="this")})
	public  NdefRecord(short tnf, byte[] type, byte[] id, byte[] payload) { throw new RuntimeException("Stub!"); }

	@STAMP(flows={@Flow(from="data",to="this")})
	public  NdefRecord(byte[] data) throws android.nfc.FormatException { throw new RuntimeException("Stub!"); }

	@STAMP(flows={@Flow(from="uri",to="@return")})
	public static  android.nfc.NdefRecord createUri(android.net.Uri uri) { return new NdefRecord(); }

	@STAMP(flows={@Flow(from="uriString",to="@return")})
	public static  android.nfc.NdefRecord createUri(java.lang.String uriString) { return new NdefRecord(); }

	@STAMP(flows={@Flow(from="mimeData",to="@return")})
	public static  android.nfc.NdefRecord createMime(java.lang.String mimeType, byte[] mimeData) { return new NdefRecord(); }

	@STAMP(flows={@Flow(from="data",to="@return")})
	public static  android.nfc.NdefRecord createExternal(java.lang.String domain, java.lang.String type, byte[] data) { return new NdefRecord(); }

}