package com.google.android.maps;

public abstract class ItemizedOverlay<Item extends OverlayItem> extends com.google.android.maps.Overlay implements com.google.android.maps.Overlay.Snappable
{
  public  ItemizedOverlay(android.graphics.drawable.Drawable param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected static android.graphics.drawable.Drawable boundCenterBottom(android.graphics.drawable.Drawable param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected static android.graphics.drawable.Drawable boundCenter(android.graphics.drawable.Drawable param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected abstract Item createItem(int param1);

  public abstract int size();

  public com.google.android.maps.GeoPoint getCenter()
  {
    throw new RuntimeException("Stub!");
  }

  protected int getIndexToDraw(int param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void draw(android.graphics.Canvas param1, com.google.android.maps.MapView param2, boolean param3)
  {
    throw new RuntimeException("Stub!");
  }

  public int getLatSpanE6()
  {
    throw new RuntimeException("Stub!");
  }

  public int getLonSpanE6()
  {
    throw new RuntimeException("Stub!");
  }

  protected void populate()
  {
    throw new RuntimeException("Stub!");
  }

  protected void setLastFocusedIndex(int param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void setFocus(Item param1)
  {
    throw new RuntimeException("Stub!");
  }

  public com.google.android.maps.OverlayItem getFocus()
  {
    throw new RuntimeException("Stub!");
  }

  public int getLastFocusedIndex()
  {
    throw new RuntimeException("Stub!");
  }

  public Item getItem(int param1)
  {
    throw new RuntimeException("Stub!");
  }

  public Item nextFocus(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTap(com.google.android.maps.GeoPoint param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onSnapToItem(int param1, int param2, android.graphics.Point param3, com.google.android.maps.MapView param4)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTrackballEvent(android.view.MotionEvent param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onKeyUp(int param1, android.view.KeyEvent param2, com.google.android.maps.MapView param3)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTouchEvent(android.view.MotionEvent param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  protected boolean hitTest(Item param1, android.graphics.drawable.Drawable param2, int param3, int param4)
  {
    throw new RuntimeException("Stub!");
  }

  public void setOnFocusChangeListener(com.google.android.maps.ItemizedOverlay.OnFocusChangeListener param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void setDrawFocusedItem(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  protected boolean onTap(int param1)
  {
    throw new RuntimeException("Stub!");
  }

  public interface OnFocusChangeListener
  {

    public abstract void onFocusChanged(com.google.android.maps.ItemizedOverlay param1, com.google.android.maps.OverlayItem param2);


  }


}

