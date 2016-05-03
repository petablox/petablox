class WifiManager
{
	@STAMP(flows={@Flow(from="$WifiManager.getDhcpInfo", to="@return")})
	public  android.net.DhcpInfo getDhcpInfo() 
	{ 
		return new android.net.DhcpInfo();
	}
}