package android.webkit;

public class StampWebSettings extends WebSettings
{
	public void setAllowUniversalAccessFromFileURLs(boolean flag){ }
	public void setAllowFileAccessFromFileURLs(boolean flag){ }
	public boolean getAllowUniversalAccessFromFileURLs(){ return false; }
	public boolean getAllowFileAccessFromFileURLs(){ return false; }
}