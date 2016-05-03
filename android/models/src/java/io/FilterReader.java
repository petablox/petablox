class FilterReader 
{
    @STAMP(flows = { @Flow(from = "in", to = "this") })
    protected FilterReader(java.io.Reader in) { }
}