class GeoPoint
{
  
  @STAMP(flows={@Flow(from="param1",to="this"),@Flow(from="param2",to="this")})  
  public  GeoPoint(int param1, int param2) {}

  @STAMP(flows={@Flow(from="this",to="@return")})
  public int getLatitudeE6() { return 13000000; }

  @STAMP(flows={@Flow(from="this",to="@return")})
  public int getLongitudeE6() { return 13000000; }

  @STAMP(flows={@Flow(from="this",to="@return")})
  public java.lang.String toString() { return new String(); }

  @STAMP(flows={@Flow(from="this",to="@return")})
  public int hashCode() { return 424242; }


}

