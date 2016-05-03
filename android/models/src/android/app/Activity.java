import android.telephony.TelephonyManager;
import android.location.LocationManager;

import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;
 
public class Activity
{
    public  Activity() 
	{
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						Activity.this.onCreate(null);
						Activity.this.onStart();
						Activity.this.onRestart();
						Activity.this.onPause();
						Activity.this.onResume();
						Activity.this.onPostResume();
						Activity.this.onStop();
						Activity.this.onDestroy();
						Activity.this.onRestoreInstanceState(null);
						Activity.this.onPostCreate(null);
						Activity.this.onNewIntent(null);
						Activity.this.onSaveInstanceState(null);
						Activity.this.onUserLeaveHint();
						Activity.this.onCreateThumbnail(null, null);
						Activity.this.onCreateDescription();
						Activity.this.onRetainNonConfigurationInstance();
						Activity.this.onConfigurationChanged(null);
						Activity.this.onLowMemory();
						Activity.this.onKeyDown(0, null);
						Activity.this.onKeyLongPress(0, null);
						Activity.this.onKeyUp(0, null);
						Activity.this.onKeyMultiple(0, 0, null);
						Activity.this.onBackPressed();
						Activity.this.onTouchEvent(null);
						Activity.this.onTrackballEvent(null);
						Activity.this.onUserInteraction();
						Activity.this.onWindowAttributesChanged(null);
						Activity.this.onContentChanged();
						Activity.this.onWindowFocusChanged(false);
						Activity.this.onAttachedToWindow();
						Activity.this.onDetachedFromWindow();
						Activity.this.onCreatePanelMenu(0, null);
						Activity.this.onPreparePanel(0, null, null);
						Activity.this.onMenuOpened(0, null);
						Activity.this.onMenuItemSelected(0, null);
						Activity.this.onPanelClosed(0, null);
						Activity.this.onCreateOptionsMenu(null);
						Activity.this.onPrepareOptionsMenu(null);
						Activity.this.onOptionsItemSelected(null);
						Activity.this.onOptionsMenuClosed(null);
						Activity.this.onCreateContextMenu(null, null, null);
						Activity.this.dispatchKeyEvent(null);
						Activity.this.dispatchKeyShortcutEvent(null);
						Activity.this.dispatchTouchEvent(null);
						Activity.this.dispatchTrackballEvent(null);
						Activity.this.dispatchGenericMotionEvent(null);
						Activity.this.dispatchPopulateAccessibilityEvent(null);
						Activity.this.onCreatePanelView(0);
						Activity.this.onSearchRequested();
						Activity.this.onWindowStartingActionMode(null);
						Activity.this.onActionModeStarted(null);
						Activity.this.onActionModeFinished(null);
					}
				});
	}



    public final  android.database.Cursor managedQuery(android.net.Uri uri, java.lang.String[] projection, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String sortOrder) 
    { 
		return getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

	public  android.view.View findViewById(int id) 
	{ 
		return edu.stanford.stamp.harness.ViewFactory.findViewById(null, id);
	}


	public  java.lang.Object getSystemService(java.lang.String name) 
	{ 
		if(name.equals(TELEPHONY_SERVICE))
			return TelephonyManager.getInstance();
		else if(name.equals(LOCATION_SERVICE))
			return LocationManager.getInstance();
		else
			return null;//TODO
	}
	
	@STAMP(flows = {@Flow(from="!Activity",to="intent")})
	public  void startActivityForResult(android.content.Intent intent, int requestCode) 
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						Activity.this.onActivityResult(0, 0, new android.content.Intent());
					}
				});
	}

	@STAMP(flows = {@Flow(from="!Activity",to="intent")})
	public  void startActivityForResult(android.content.Intent intent, int requestCode, android.os.Bundle options) 
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						Activity.this.onActivityResult(0, 0, new android.content.Intent());
					}
				});
	}
	
	public final void showDialog (int id)
	{
	    this.onCreateDialog(id);
	}

    public final void runOnUiThread(final java.lang.Runnable action) {
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						action.run();
					}
				});
    }

}
