public class CdmaCellLocation{

    @STAMP(flows = {@Flow(from="$CDMA_LOCATION",to="@return")})    
    public  int getBaseStationId() { return 0; }

    @STAMP(flows = {@Flow(from="$CDMA_LOCATION",to="@return")})    
    public  int getBaseStationLatitude() { return 0; }

    @STAMP(flows = {@Flow(from="$CDMA_LOCATION",to="@return")})    
    public  int getBaseStationLongitude() { return 0; }

    @STAMP(flows = {@Flow(from="$CDMA_SYSTEM_ID",to="@return")})    
    public  int getSystemId() { return 0; }

    @STAMP(flows = {@Flow(from="$CDMA_NETWORK_ID",to="@return")})    
    public  int getNetworkId() { return 0; }

}