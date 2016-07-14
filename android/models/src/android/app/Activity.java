import android.telephony.TelephonyManager;
import android.location.LocationManager;
import android.content.ClipboardManager;

public class Activity
{
	protected android.content.Intent intent;
	public android.view.StampLayoutInflater stamp_inflater;

    public  Activity() 
	{
		this.intent = new android.content.Intent();
	}
	
	public void callCallbacks()
	{
		this.onCreate(null);
		this.onStart();
		this.onRestart();
		this.onPause();
		this.onResume();
		this.onPostResume();
		this.onStop();
		this.onDestroy();
		this.onRestoreInstanceState(null);
		this.onPostCreate(null);
		this.onNewIntent(null);
		this.onSaveInstanceState(null);
		this.onUserLeaveHint();
		this.onCreateThumbnail(null, null);
		this.onCreateDescription();
		this.onRetainNonConfigurationInstance();
		this.onConfigurationChanged(null);
		this.onLowMemory();
		this.onKeyDown(0, null);
		this.onKeyLongPress(0, null);
		this.onKeyUp(0, null);
		this.onKeyMultiple(0, 0, null);
		this.onBackPressed();
		this.onTouchEvent(null);
		this.onTrackballEvent(null);
		this.onUserInteraction();
		this.onWindowAttributesChanged(null);
		this.onContentChanged();
		this.onWindowFocusChanged(false);
		this.onAttachedToWindow();
		this.onDetachedFromWindow();
		this.onCreatePanelMenu(0, null);
		this.onPreparePanel(0, null, null);
		this.onMenuOpened(0, null);
		this.onMenuItemSelected(0, null);
		this.onPanelClosed(0, null);
		this.onCreateOptionsMenu(null);
		this.onPrepareOptionsMenu(null);
		this.onOptionsItemSelected(null);
		this.onOptionsMenuClosed(null);
		this.onCreateContextMenu(null, null, null);
		this.dispatchKeyEvent(null);
		this.dispatchKeyShortcutEvent(null);
		this.dispatchTouchEvent(null);
		this.dispatchTrackballEvent(null);
		this.dispatchGenericMotionEvent(null);
		this.dispatchPopulateAccessibilityEvent(null);
		this.onCreatePanelView(0);
		this.onSearchRequested();
		this.onWindowStartingActionMode(null);
		this.onActionModeStarted(null);
		this.onActionModeFinished(null);
	}

    public final  android.database.Cursor managedQuery(android.net.Uri uri, java.lang.String[] projection, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String sortOrder) 
    { 
		return getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

	public  java.lang.Object getSystemService(java.lang.String name) 
	{ 
		if(name.equals(TELEPHONY_SERVICE))
			return TelephonyManager.getInstance();
		else if(name.equals(LOCATION_SERVICE))
			return LocationManager.getInstance();
		else if(name.equals(CLIPBOARD_SERVICE))
			return ClipboardManager.getInstance();
		else if(name.equals(LAYOUT_INFLATER_SERVICE))
			return android.view.LayoutInflater.from(this);
		else 
			return null;//TODO
	}
	
	@STAMP(flows = {@Flow(from="intent", to="!Activity")})
	public  void startActivityForResult(android.content.Intent intent, int requestCode) 
	{ 
		this.onActivityResult(0, 0, new android.content.Intent());
	}

	@STAMP(flows = {@Flow(from="intent", to="!Activity"), @Flow(from="options", to="!Activity")})
	public  void startActivityForResult(android.content.Intent intent, int requestCode, android.os.Bundle options) 
	{ 
		this.onActivityResult(0, 0, new android.content.Intent());
	}

	public  void startActivity(android.content.Intent intent) 
	{ 
	}
	
	public  void startActivity(android.content.Intent intent, android.os.Bundle options) 
	{ 
	}

	@STAMP(flows = {@Flow(from="intent",to="!Activity")})
	public  boolean startActivityIfNeeded(android.content.Intent intent, int requestCode) 
	{  
		return false;
	}

	@STAMP(flows = {@Flow(from="intent",to="!Activity"),@Flow(from="options",to="!Activity")})
	public  boolean startActivityIfNeeded(android.content.Intent intent, int requestCode, android.os.Bundle options) 
	{  
		return false;
	}

	@STAMP(flows = {@Flow(from="intent",to="!Activity")})
	public  boolean startNextMatchingActivity(android.content.Intent intent) 
	{  
		return false;
	}

	@STAMP(flows = {@Flow(from="intent",to="!Activity"),@Flow(from="options",to="!Activity")})
	public  boolean startNextMatchingActivity(android.content.Intent intent, android.os.Bundle options) 
	{  
		return false;
	}

	@STAMP(flows = {@Flow(from="intent",to="!Activity")})
	public  void startActivityFromChild(android.app.Activity child, android.content.Intent intent, int requestCode) {  }

	@STAMP(flows = {@Flow(from="intent",to="!Activity"),@Flow(from="options",to="!Activity")})
	public  void startActivityFromChild(android.app.Activity child, android.content.Intent intent, int requestCode, android.os.Bundle options) {  }

	@STAMP(flows = {@Flow(from="intent",to="!Activity")})
	public  void startActivityFromFragment(android.app.Fragment fragment, android.content.Intent intent, int requestCode) {  }

	@STAMP(flows = {@Flow(from="intent",to="!Activity"),@Flow(from="options",to="!Activity")})
	public  void startActivityFromFragment(android.app.Fragment fragment, android.content.Intent intent, int requestCode, android.os.Bundle options) {  }

	public final void showDialog (int id)
	{
	    this.onCreateDialog(id);
	}

    public final void runOnUiThread(final java.lang.Runnable action) {
		action.run();
    }

	@STAMP(flows = {@Flow(from="$Activity.GetIntent", to="@return")})
    public android.content.Intent getIntent() {
		return this.intent;
    }

	public void setIntent(android.content.Intent newIntent) {
		this.intent = newIntent;
	}
	
	public  void setContentView(int layoutResID) { 
		//dont make it a stub
	}

	public  void setContentView(android.view.View view) { 
		this.stamp_inflater = view.stamp_inflater;
	}

	public  void setContentView(android.view.View view, android.view.ViewGroup.LayoutParams params) { 
		this.stamp_inflater = view.stamp_inflater;
	}

	public  void addContentView(android.view.View view, android.view.ViewGroup.LayoutParams params) { 
		this.stamp_inflater = view.stamp_inflater;
	}

	public  android.view.LayoutInflater getLayoutInflater() {
		return this.stamp_inflater;
	}

}
