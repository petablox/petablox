class StringWriter {

    @STAMP(flows = { @Flow(from = "chars", to = "this") })
    public void write(char[] chars, int offset, int count) {
    }

    @STAMP(flows = { @Flow(from = "oneChar", to = "this") })
    public void write(int oneChar) {
    }

    @STAMP(flows = { @Flow(from = "str", to = "this") })
    public void write(java.lang.String str) {
    }

    @STAMP(flows = { @Flow(from = "str", to = "this") })
    public void write(java.lang.String str, int offset, int count) {
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public java.lang.StringBuffer getBuffer() {
        return new StringBuffer();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
	public java.lang.String toString() {
		return new String();
    }

    @STAMP(flows = { @Flow(from = "c", to = "this") })
    public java.io.StringWriter append(char c) {
		return this;
    }

    @STAMP(flows = { @Flow(from = "csq", to = "this") })
    public java.io.StringWriter append(java.lang.CharSequence csq) {
		return this;
    }

    @STAMP(flows = { @Flow(from = "csq", to = "this") })
    public java.io.StringWriter append(java.lang.CharSequence csq, int start, int end) {
		return this;
    }
}

