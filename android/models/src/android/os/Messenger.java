class Messenger
{
	//this is probably temporary
	@STAMP(flows = {@Flow(from="message",to="!Messenger")})
	public  void send(android.os.Message message) throws android.os.RemoteException { 
	}
}