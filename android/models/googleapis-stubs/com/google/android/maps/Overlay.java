package com.google.android.maps;

public abstract class Overlay
{

  public  Overlay()
  {
    throw new RuntimeException("Stub!");
  }

  protected static void drawAt(android.graphics.Canvas param1, android.graphics.drawable.Drawable param2, int param3, int param4, boolean param5)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTouchEvent(android.view.MotionEvent param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTrackballEvent(android.view.MotionEvent param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onKeyDown(int param1, android.view.KeyEvent param2, com.google.android.maps.MapView param3)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onKeyUp(int param1, android.view.KeyEvent param2, com.google.android.maps.MapView param3)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTap(com.google.android.maps.GeoPoint param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  public void draw(android.graphics.Canvas param1, com.google.android.maps.MapView param2, boolean param3)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean draw(android.graphics.Canvas param1, com.google.android.maps.MapView param2, boolean param3, long param4)
  {
    throw new RuntimeException("Stub!");
  }

  public interface Snappable
  {

    public abstract boolean onSnapToItem(int param1, int param2, android.graphics.Point param3, com.google.android.maps.MapView param4);


  }


}

