package java.lang;

class Boolean {

    @STAMP(flows = { @Flow(from = "string", to = "this") })
    public Boolean(java.lang.String string) {
    }

    @STAMP(flows = { @Flow(from = "value", to = "this") })
    public Boolean(boolean value) {
    }

    @STAMP(flows = { @Flow(from = "o", to = "@return"), @Flow(from = "this", to = "@return") })
    public boolean equals(java.lang.Object o) {
		return true;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public boolean booleanValue() {
        return true;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public int hashCode() {
        return 1231;
    }

    @STAMP(flows = { @Flow(from = "string", to = "@return") })
    public static java.lang.Boolean valueOf(java.lang.String string) {
        return new Boolean(true);
    }

    @STAMP(flows = { @Flow(from = "b", to = "@return") })
    public static java.lang.Boolean valueOf(boolean b) {
        return new Boolean(b);
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public java.lang.String toString() {
		return new String();
    }

    @STAMP(flows = { @Flow(from = "s", to = "@return") })
    public static boolean parseBoolean(java.lang.String s) {
		return true;
    }

    @STAMP(flows = { @Flow(from = "value", to = "@return") })
    public static java.lang.String toString(boolean value) {
		return new String();
    }

}

