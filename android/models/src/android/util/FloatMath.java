package android.util;

class FloatMath {

    @STAMP(flows = { @Flow(from = "value", to = "@return") })
    public static native float ceil(float value);
}

