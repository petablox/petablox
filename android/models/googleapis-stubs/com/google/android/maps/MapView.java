package com.google.android.maps;

public class MapView extends android.view.ViewGroup
{

  public  MapView(android.content.Context param1, java.lang.String param2)
  {
	  super(param1);
  }

  public  MapView(android.content.Context param1, android.util.AttributeSet param2)
  {
	  super(param1, param2);
  }

  public  MapView(android.content.Context param1, android.util.AttributeSet param2, int param3)
  {
	  super(param1, param2, param3);
  }

  protected void onSizeChanged(int param1, int param2, int param3, int param4)
  {
    throw new RuntimeException("Stub!");
  }

  protected void onDetachedFromWindow()
  {
    throw new RuntimeException("Stub!");
  }

  public void computeScroll()
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isOpaque()
  {
    throw new RuntimeException("Stub!");
  }

  protected void onDraw(android.graphics.Canvas param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected void onMeasure(int param1, int param2)
  {
    throw new RuntimeException("Stub!");
  }

  public void onWindowFocusChanged(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void onFocusChanged(boolean param1, int param2, android.graphics.Rect param3)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onKeyDown(int param1, android.view.KeyEvent param2)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onKeyUp(int param1, android.view.KeyEvent param2)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTrackballEvent(android.view.MotionEvent param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTouchEvent(android.view.MotionEvent param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected void onVisibilityChanged(android.view.View param1, int param2)
  {
    throw new RuntimeException("Stub!");
  }

  protected com.google.android.maps.MapView.LayoutParams generateDefaultLayoutParams()
  {
    throw new RuntimeException("Stub!");
  }

  protected void onLayout(boolean param1, int param2, int param3, int param4, int param5)
  {
    throw new RuntimeException("Stub!");
  }

  protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void displayZoomControls(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean canCoverCenter()
  {
    throw new RuntimeException("Stub!");
  }

  public void preLoad()
  {
    throw new RuntimeException("Stub!");
  }

  public int getZoomLevel()
  {
    throw new RuntimeException("Stub!");
  }

  public void setSatellite(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isSatellite()
  {
    throw new RuntimeException("Stub!");
  }

  public void setTraffic(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isTraffic()
  {
    throw new RuntimeException("Stub!");
  }

  public void setStreetView(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isStreetView()
  {
    throw new RuntimeException("Stub!");
  }

  public com.google.android.maps.GeoPoint getMapCenter()
  {
    throw new RuntimeException("Stub!");
  }

  public com.google.android.maps.MapController getController()
  {
    throw new RuntimeException("Stub!");
  }

  public java.util.List getOverlays()
  {
    throw new RuntimeException("Stub!");
  }

  public int getLatitudeSpan()
  {
    throw new RuntimeException("Stub!");
  }

  public int getLongitudeSpan()
  {
    throw new RuntimeException("Stub!");
  }

  public void setReticleDrawMode(com.google.android.maps.MapView.ReticleDrawMode param1)
  {
    throw new RuntimeException("Stub!");
  }

  public int getMaxZoomLevel()
  {
    throw new RuntimeException("Stub!");
  }

  public void onSaveInstanceState(android.os.Bundle param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void onRestoreInstanceState(android.os.Bundle param1)
  {
    throw new RuntimeException("Stub!");
  }

  public android.view.View getZoomControls()
  {
    throw new RuntimeException("Stub!");
  }

  public android.widget.ZoomButtonsController getZoomButtonsController()
  {
    throw new RuntimeException("Stub!");
  }

  public void setBuiltInZoomControls(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public com.google.android.maps.Projection getProjection()
  {
    throw new RuntimeException("Stub!");
  }

  public class LayoutParams extends android.view.ViewGroup.LayoutParams
  {
    public static final int MODE_MAP = 0;
    public static final int MODE_VIEW = 0;
    public int mode = 0;
    public com.google.android.maps.GeoPoint point = null;
    public int x = 0;
    public int y = 0;
    public int alignment = 0;
    public static final int LEFT = 0;
    public static final int RIGHT = 0;
    public static final int TOP = 0;
    public static final int BOTTOM = 0;
    public static final int CENTER_HORIZONTAL = 0;
    public static final int CENTER_VERTICAL = 0;
    public static final int CENTER = 0;
    public static final int TOP_LEFT = 0;
    public static final int BOTTOM_CENTER = 0;

    public  LayoutParams(int param1, int param2, com.google.android.maps.GeoPoint param3, int param4)
    {
		super(param1, param2);
    }

    public  LayoutParams(int param1, int param2, com.google.android.maps.GeoPoint param3, int param4, int param5, int param6)
    {
		super(param1, param2);
    }

    public  LayoutParams(int param1, int param2, int param3, int param4, int param5)
    {
		super(param1, param2);
    }

    public  LayoutParams(android.content.Context param1, android.util.AttributeSet param2)
    {
		super(param1, param2);
    }

    public  LayoutParams(android.view.ViewGroup.LayoutParams param1)
    {
		super(param1);
    }

    public java.lang.String debug(java.lang.String param1)
    {
      throw new RuntimeException("Stub!");
    }


  }

  public enum ReticleDrawMode
  {
    DRAW_RETICLE_OVER,
    DRAW_RETICLE_UNDER,
    DRAW_RETICLE_NEVER
  }

   class Repainter implements android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestListener
  {

    public void onComplete(android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequest param1)
    {
      throw new RuntimeException("Stub!");
    }

    public void onNetworkError(int param1, boolean param2, java.lang.String param3)
    {
      throw new RuntimeException("Stub!");
    }


  }


}

