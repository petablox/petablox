class InetSocketAddress
{
	@STAMP(flows={@Flow(from="address",to="this")})
	public  InetSocketAddress(java.net.InetAddress address, int port) 
	{ 
	}

	@STAMP(flows={@Flow(from="host",to="this")})
	public  InetSocketAddress(java.lang.String host, int port) 
	{ 
	}

	public static  java.net.InetSocketAddress createUnresolved(java.lang.String host, int port) 
	{ 
		return new InetSocketAddress(host, port);
	}

}