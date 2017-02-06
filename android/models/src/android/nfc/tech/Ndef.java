public class Ndef 
{
    @STAMP(flows = {@Flow(from="msg",to="!NDEFMESSAGE")})
    public void writeNdefMessage(android.nfc.NdefMessage msg) {}
}
