class MockPackageManager
{
	@STAMP(flows={@Flow(from="$InstalledPackages",to="@return")})
	public  java.util.List<android.content.pm.PackageInfo> getInstalledPackages(int flags) { 
		java.util.List<android.content.pm.PackageInfo> ret = new java.util.ArrayList<android.content.pm.PackageInfo>();
		ret.add(new android.content.pm.PackageInfo());
		return ret;
	}
}