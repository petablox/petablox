class Socket
{
	@STAMP(flows={@Flow(from="proxy",to="!SOCKET")})
	public  Socket(java.net.Proxy proxy) 
	{ 
	}

	@STAMP(flows={@Flow(from="dstName",to="!SOCKET")})
	public  Socket(java.lang.String dstName, int dstPort) throws java.net.UnknownHostException, java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="dstName",to="!SOCKET")})
	public  Socket(java.lang.String dstName, int dstPort, java.net.InetAddress localAddress, int localPort) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="hostName",to="!SOCKET")})
	public  Socket(java.lang.String hostName, int port, boolean streaming) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="dstAddress",to="!SOCKET")})
	public  Socket(java.net.InetAddress dstAddress, int dstPort) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="dstAddress",to="!SOCKET")})
	public  Socket(java.net.InetAddress dstAddress, int dstPort, java.net.InetAddress localAddress, int localPort) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="addr",to="!SOCKET")})
	public  Socket(java.net.InetAddress addr, int port, boolean streaming) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="remoteAddr",to="!SOCKET")})
	public  void connect(java.net.SocketAddress remoteAddr) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="remoteAddr",to="!SOCKET")})
	public  void connect(java.net.SocketAddress remoteAddr, int timeout) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="!SOCKET",to="@return")})
    public java.io.OutputStream getOutputStream() throws java.io.IOException 
	{
		return new java.io.StampOutputStream();
    }

}