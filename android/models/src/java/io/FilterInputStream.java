class FilterInputStream 
{
    @STAMP(flows = { @Flow(from = "in", to = "this") })
    protected FilterInputStream(java.io.InputStream in) { }
}