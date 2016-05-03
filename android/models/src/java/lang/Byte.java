package java.lang;

class Byte {

    @STAMP(flows = { @Flow(from = "value", to = "this") })
    public Byte(byte value) {
    }

    @STAMP(flows = { @Flow(from = "string", to = "this") })
    public Byte(java.lang.String string) throws java.lang.NumberFormatException {
    }

    @STAMP(flows = { @Flow(from = "string", to = "@return") })
    public static byte parseByte(java.lang.String string) throws java.lang.NumberFormatException {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "string", to = "@return") })
    public static byte parseByte(java.lang.String string, int radix) throws java.lang.NumberFormatException {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "string", to = "@return") })
    public static java.lang.Byte valueOf(java.lang.String string) throws java.lang.NumberFormatException {
        return new Byte((byte)0);
    }

    @STAMP(flows = { @Flow(from = "string", to = "@return") })
    public static java.lang.Byte valueOf(java.lang.String string, int radix) throws java.lang.NumberFormatException {
        return new Byte((byte)0);
    }

    @STAMP(flows = { @Flow(from = "b", to = "@return") })
    public static java.lang.Byte valueOf(byte b) {
        return new Byte((byte)0);
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public byte byteValue() {
		return 0;
    }

    @STAMP(flows = { @Flow(from = "value", to = "@return") })
	public static java.lang.String toString(byte value) {
		return new String();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public double doubleValue() {
		return 0.0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public float floatValue() {
		return 0.0f;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public int intValue() {
		return 0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public long longValue() {
		return 0L;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public short shortValue() {
		return (short) 0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public java.lang.String toString() {
		return new String();
    }

}

