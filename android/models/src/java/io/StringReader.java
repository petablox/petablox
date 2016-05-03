package java.io;

class StringReader {

    @STAMP(flows = { @Flow(from = "str", to = "this") })
    public StringReader(java.lang.String str) {
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public int read() throws java.io.IOException {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "buf"), @Flow(from="len",to="this"), @Flow(from="len",to="@return") })
    public int read(char[] buf, int offset, int len) throws java.io.IOException {
        return 0;
    }
}

