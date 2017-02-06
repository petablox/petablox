package com.google.android.maps;

 class NetworkConnectivityListener
{

  public  NetworkConnectivityListener()
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized void startListening(android.content.Context param1)
  {
    throw new RuntimeException("Stub!");
  }

  public synchronized void stopListening()
  {
    throw new RuntimeException("Stub!");
  }

  public void registerHandler(android.os.Handler param1, int param2)
  {
    throw new RuntimeException("Stub!");
  }

  public void unregisterHandler(android.os.Handler param1)
  {
    throw new RuntimeException("Stub!");
  }

  public com.google.android.maps.NetworkConnectivityListener.State getState()
  {
    throw new RuntimeException("Stub!");
  }

  public android.net.NetworkInfo getNetworkInfo()
  {
    throw new RuntimeException("Stub!");
  }

  public android.net.NetworkInfo getOtherNetworkInfo()
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isFailover()
  {
    throw new RuntimeException("Stub!");
  }

  public java.lang.String getReason()
  {
    throw new RuntimeException("Stub!");
  }

  public enum State
  {
    UNKNOWN,
    CONNECTED,
    NOT_CONNECTED
  }

   class ConnectivityBroadcastReceiver extends android.content.BroadcastReceiver
  {

    public void onReceive(android.content.Context param1, android.content.Intent param2)
    {
      throw new RuntimeException("Stub!");
    }


  }


}

