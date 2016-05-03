class ObjectOutputStream
{
	@STAMP(flows = {@Flow(from="output",to="this")})
 	public ObjectOutputStream(java.io.OutputStream output) throws java.io.IOException { 
	}

	@STAMP(flows = {@Flow(from="object",to="!this")})
	public final void writeObject(java.lang.Object object) throws java.io.IOException {
		ObjectInputStream.object = object;
 	}
}
