class NdefMessage 
{
    @STAMP(flows={@Flow(from="record",to="this"),@Flow(from="records",to="this")})
    public NdefMessage(android.nfc.NdefRecord record, android.nfc.NdefRecord... records) { }

    @STAMP(flows={@Flow(from="records",to="this")})
    public NdefMessage(android.nfc.NdefRecord[] records) { }

    @STAMP(flows={@Flow(from="data",to="this")})
    public NdefMessage(byte[] data) {}
}

