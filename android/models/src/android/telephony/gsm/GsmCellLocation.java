class GsmCellLocation
{
    @STAMP(flows = {@Flow(from="$FINE_LOCATION",to="@return")})    
    public  int getLac() { return 0; }

    @STAMP(flows = {@Flow(from="$FINE_LOCATION",to="@return")})    
    public  int getCid() { return 0; }

    @STAMP(flows = {@Flow(from="$FINE_LOCATION",to="@return")})     
    public  int getPsc() { return 0; }
}