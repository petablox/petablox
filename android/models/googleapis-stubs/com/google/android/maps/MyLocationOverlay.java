package com.google.android.maps;

public class MyLocationOverlay extends com.google.android.maps.Overlay implements android.hardware.SensorListener, android.location.LocationListener, com.google.android.maps.Overlay.Snappable
{

  public  MyLocationOverlay(android.content.Context param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized boolean enableCompass()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized void disableCompass()
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isCompassEnabled()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized boolean enableMyLocation()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized void disableMyLocation()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized void onSensorChanged(int param1, float[] param2)
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized void onLocationChanged(android.location.Location param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void onStatusChanged(java.lang.String param1, int param2, android.os.Bundle param3)
  {
    throw new RuntimeException("Stub!");
  }

  public void onProviderEnabled(java.lang.String param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void onProviderDisabled(java.lang.String param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onSnapToItem(int param1, int param2, android.graphics.Point param3, com.google.android.maps.MapView param4)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTap(com.google.android.maps.GeoPoint param1, com.google.android.maps.MapView param2)
  {
    throw new RuntimeException("Stub!");
  }

  protected boolean dispatchTap()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized boolean draw(android.graphics.Canvas param1, com.google.android.maps.MapView param2, boolean param3, long param4)
  {
    throw new RuntimeException("Stub!");
  }

  protected void drawMyLocation(android.graphics.Canvas param1, com.google.android.maps.MapView param2, android.location.Location param3, com.google.android.maps.GeoPoint param4, long param5)
  {
    throw new RuntimeException("Stub!");
  }

  protected void drawCompass(android.graphics.Canvas param1, float param2)
  {
    throw new RuntimeException("Stub!");
  }

  public com.google.android.maps.GeoPoint getMyLocation()
  {
    throw new RuntimeException("Stub!");
  }

  public android.location.Location getLastFix()
  {
    throw new RuntimeException("Stub!");
  }

  public float getOrientation()
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isMyLocationEnabled()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized boolean runOnFirstFix(java.lang.Runnable param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void onAccuracyChanged(int param1, int param2)
  {
    throw new RuntimeException("Stub!");
  }

   class NameAndDate
  {
    public java.lang.String name = null;
    public long date = 0;

    public  NameAndDate(java.lang.String param1)
    {
      throw new RuntimeException("Stub!");
    }


  }


}

