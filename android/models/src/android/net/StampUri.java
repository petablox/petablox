package android.net;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

/*
  created this class because android.net.Uri is abstract
 */
public class StampUri extends Uri
{
	public StampUri(){}

    public StampUri(String s) { throw new RuntimeException("Stub!"); }

    public boolean isHierarchical()
    {
		throw new RuntimeException("Stub!");
    }

    public  boolean isOpaque() { throw new RuntimeException("Stub!"); }

    public   boolean isRelative()
    {
		throw new RuntimeException("Stub!");
    }

    public  boolean isAbsolute() { throw new RuntimeException("Stub!"); }

    public java.lang.String getScheme()
    {
		throw new RuntimeException("Stub!");
    }
	
    public   java.lang.String getSchemeSpecificPart()
    {
		throw new RuntimeException("Stub!");
    }
	
    public   java.lang.String getEncodedSchemeSpecificPart()
    {
		throw new RuntimeException("Stub!");
    }

    public   java.lang.String getAuthority()
    {
		throw new RuntimeException("Stub!");
    }

    public   java.lang.String getEncodedAuthority()
    {
		throw new RuntimeException("Stub!");
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getUserInfo()
    {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getEncodedUserInfo()
    {
		return new String();
    }

    public   java.lang.String getHost()
    {
		throw new RuntimeException("Stub!");
    }

    public   int getPort()
    {
		throw new RuntimeException("Stub!");
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getPath()
    {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getEncodedPath()
    {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getQuery()
    {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getEncodedQuery()
    {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getFragment()
    {
		return new String();
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getEncodedFragment()
    {
		return new String();
    }


    public   java.util.List<java.lang.String> getPathSegments()
    {
		java.util.List<java.lang.String> ret = new java.util.ArrayList<java.lang.String>();
		ret.add(getLastPathSegment());
		return ret;
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
    public   java.lang.String getLastPathSegment()
    {
		return new String();
    }

    public  int compareTo(android.net.Uri other) 
    { 
		throw new RuntimeException("Stub!"); 
    }

    public   android.net.Uri.Builder buildUpon()
    {
		throw new RuntimeException("Stub!");
    }

    public void writeToParcel(android.os.Parcel p, int i)
    {
		throw new RuntimeException("Stub!");
    }
	
	public int describeContents()
	{
		throw new RuntimeException("Stub!");
	}
	
	public String toString(){
		return new String();
	}
}