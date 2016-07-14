class DatagramPacket
{
	public  DatagramPacket(byte[] data, int length) 
	{
		transferTaint(data[0]);
	}

	public  DatagramPacket(byte[] data, int offset, int length) 
	{ 
		transferTaint(data[0]);
	}

	public  DatagramPacket(byte[] data, int offset, int length, java.net.InetAddress host, int aPort) 
	{ 
		transferTaint(data[0]);
	}

	public  DatagramPacket(byte[] data, int length, java.net.InetAddress host, int port) 
	{ 
		transferTaint(data[0]);
	}

	public  DatagramPacket(byte[] data, int length, java.net.SocketAddress sockAddr) throws java.net.SocketException 
	{
		transferTaint(data[0]);
	}

	public  DatagramPacket(byte[] data, int offset, int length, java.net.SocketAddress sockAddr) throws java.net.SocketException 
	{ 
		transferTaint(data[0]);
	}

	public synchronized  void setData(byte[] data, int offset, int byteCount) 
	{ 
		transferTaint(data[0]);
	}

	public synchronized  void setData(byte[] buf) 
	{ 
		transferTaint(buf[0]);
	}

	@STAMP(flows={@Flow(from="data",to="this")})
	private void transferTaint(byte data)
	{
	}
}