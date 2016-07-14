package java.lang;

class Character {

    @STAMP(flows = { @Flow(from = "c", to = "@return") })
    public static char toLowerCase(char c) {
        return 'a';
    }

    @STAMP(flows = { @Flow(from = "codePoint", to = "@return") })
    public static int toLowerCase(int codePoint) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "c", to = "@return") })
    public static char toUpperCase(char c) {
        return 'a';
    }

    @STAMP(flows = { @Flow(from = "codePoint", to = "@return") })
    public static int toUpperCase(int codePoint) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "c", to = "@return") })
    public static char toTitleCase(char c) {
        return 'a';
    }

    @STAMP(flows = { @Flow(from = "codePoint", to = "@return") })
    public static int toTitleCase(int codePoint) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "codePoint", to = "dst") })
    public static int toChars(int codePoint, char[] dst, int dstIndex) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "codePoint", to = "@return") })
    public static char[] toChars(int codePoint) {
        return new char[0];
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public char charValue() {
        return 'a';
    }

    @STAMP(flows = { @Flow(from = "value", to = "this") })
    public Character(char value) {
    }

    @STAMP(flows = { @Flow(from = "seq", to = "@return") })
    public static int codePointAt(java.lang.CharSequence seq, int index) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "seq", to = "@return") })
    public static int codePointAt(char[] seq, int index) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "seq", to = "@return") })
    public static int codePointAt(char[] seq, int index, int limit) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "c", to = "@return") })
    public static int digit(char c, int radix) {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "codePoint", to = "@return") })
    public static int digit(int codePoint, int radix) {
        return 0;
    }

    public static java.lang.Character valueOf(char c) {
		return new Character(c);
    }
}

