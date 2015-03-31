// Convert.java, created Wed Mar  5  0:26:31 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.util;

/**
 * Utility methods to convert between primitive data types.
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Convert.java,v 1.2 2004/10/08 07:12:25 joewhaley Exp $
 */
public abstract class Convert {
    
    /**
     * Convert two bytes to a char.
     * 
     * @param b1 first byte
     * @param b2 second byte
     * @return char result
     */
    public static final char twoBytesToChar(byte b1, byte b2) {
        return (char) ((b1 << 8) | (b2 & 0xFF));
    }

    /**
     * Convert two bytes to a short.
     * 
     * @param b1 first byte
     * @param b2 second byte
     * @return short result
     */
    public static final short twoBytesToShort(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
    }

    /**
     * Convert two chars to an int.
     * 
     * @param c1 first char
     * @param c2 second char
     * @return int result
     */
    public static final int twoCharsToInt(char c1, char c2) {
        return (c1 << 16) | c2;
    }

    /**
     * Convert four bytes to an int.
     */
    public static final int fourBytesToInt(byte b1, byte b2, byte b3, byte b4) {
        return (b1 << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | (b4 & 0xFF);
    }

    /**
     * Convert eight bytes to a long.
     */
    public static final long eightBytesToLong(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
        int hi = fourBytesToInt(b1, b2, b3, b4);
        int lo = fourBytesToInt(b5, b6, b7, b8);
        return twoIntsToLong(lo, hi);
    }

    /**
     * Convert two bytes at the given position in an array to a char.
     */
    public static final char twoBytesToChar(byte[] b, int i) {
        return Convert.twoBytesToChar(b[i], b[i + 1]);
    }

    /**
     * Convert two bytes at the given position in an array to a short.
     */
    public static final short twoBytesToShort(byte[] b, int i) {
        return twoBytesToShort(b[i], b[i + 1]);
    }

    /**
     * Convert four bytes at the given position in an array to an int.
     */
    public static final int fourBytesToInt(byte[] b, int i) {
        return fourBytesToInt(b[i], b[i + 1], b[i + 2], b[i + 3]);
    }

    /**
     * Convert eight bytes at the given position in an array to a long.
     */
    public static final long eightBytesToLong(byte[] b, int i) {
        return eightBytesToLong(b[i], b[i + 1], b[i + 2], b[i + 3], b[i + 4], b[i + 5], b[i + 6], b[i + 7]);
    }

    /**
     * Convert two ints to a long.
     */
    public static final long twoIntsToLong(int lo, int hi) {
        return (((long) lo) & 0xFFFFFFFFL) | ((long) hi << 32);
    }

    /**
     * Convert a char to two bytes, putting the result at the given position in
     * the given array.
     */
    public static final void charToTwoBytes(char i, byte[] b, int index) {
        b[index] = (byte) (i >> 8);
        b[index + 1] = (byte) (i);
    }

    /**
     * Convert an int to four bytes, putting the result at the given position in
     * the given array.
     */
    public static final void intToFourBytes(int i, byte[] b, int index) {
        b[index] = (byte) (i >> 24);
        b[index + 1] = (byte) (i >> 16);
        b[index + 2] = (byte) (i >> 8);
        b[index + 3] = (byte) (i);
    }

    /**
     * Convert a long to eight bytes, putting the result at the given position
     * in the given array.
     */
    public static final void longToEightBytes(long i, byte[] b, int index) {
        b[index] = (byte) (i >> 56);
        b[index + 1] = (byte) (i >> 48);
        b[index + 2] = (byte) (i >> 40);
        b[index + 3] = (byte) (i >> 32);
        b[index + 4] = (byte) (i >> 24);
        b[index + 5] = (byte) (i >> 16);
        b[index + 6] = (byte) (i >> 8);
        b[index + 7] = (byte) (i);
    }
    
    /**
     * Convenience function for getting a Boolean.  Acts exactly like Boolean.valueOf(),
     * but JDK1.3 doesn't have that function.
     * 
     * @see java.lang.Boolean#valueOf(boolean)
     * @param b  boolean
     * @return  Boolean object
     */
    public static final Boolean getBoolean(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }
    
    /**
     * Unwraps the given object to a boolean.
     * 
     * @param value  object to unwrap
     * @return  boolean value of object
     * @throws IllegalArgumentException  if value is not a Boolean object
     */
    public static boolean unwrapToBoolean(Object value) throws IllegalArgumentException {
        if (value instanceof Boolean) return ((Boolean)value).booleanValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to boolean");
    }
    
    /**
     * Unwraps the given object to a byte.
     * 
     * @param value  object to unwrap
     * @return  byte value of object
     * @throws IllegalArgumentException  if value is not a Byte object
     */
    public static byte unwrapToByte(Object value) throws IllegalArgumentException {
        if (value instanceof Byte) return ((Byte)value).byteValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to byte");
    }
    
    /**
     * Unwraps the given object to a char.
     * 
     * @param value  object to unwrap
     * @return  char value of object
     * @throws IllegalArgumentException  if value is not a Character object
     */
    public static char unwrapToChar(Object value) throws IllegalArgumentException {
        if (value instanceof Character) return ((Character)value).charValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to char");
    }
    
    /**
     * Unwraps the given object to a short.
     * 
     * @param value  object to unwrap
     * @return  short value of object
     * @throws IllegalArgumentException  if value is not a Short or Byte object
     */
    public static short unwrapToShort(Object value) throws IllegalArgumentException {
        if (value instanceof Short) return ((Short)value).shortValue();
        else if (value instanceof Byte) return ((Byte)value).shortValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to short");
    }
    
    /**
     * Unwraps the given object to an int.
     * 
     * @param value  object to unwrap
     * @return  int value of object
     * @throws IllegalArgumentException  if value is not an Integer, Byte, Character, or Short object
     */
    public static int unwrapToInt(Object value) throws IllegalArgumentException {
        if (value instanceof Integer) return ((Integer)value).intValue();
        else if (value instanceof Byte) return ((Byte)value).intValue();
        else if (value instanceof Character) return (int)((Character)value).charValue();
        else if (value instanceof Short) return ((Short)value).intValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to int");
    }
    
    /**
     * Unwraps the given object to a long.
     * 
     * @param value  object to unwrap
     * @return  long value of object
     * @throws IllegalArgumentException  if value is not a Long, Integer, Byte, Character, or Short object
     */
    public static long unwrapToLong(Object value) throws IllegalArgumentException {
        if (value instanceof Long) return ((Long)value).longValue();
        else if (value instanceof Integer) return ((Integer)value).longValue();
        else if (value instanceof Byte) return ((Byte)value).longValue();
        else if (value instanceof Character) return (long)((Character)value).charValue();
        else if (value instanceof Short) return ((Short)value).longValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to long");
    }
    
    /**
     * Unwraps the given object to a float.
     * 
     * @param value  object to unwrap
     * @return  float value of object
     * @throws IllegalArgumentException  if value is not a Float, Long, Integer, Byte, Character, or Short object
     */
    public static float unwrapToFloat(Object value) throws IllegalArgumentException {
        if (value instanceof Float) return ((Float)value).floatValue();
        else if (value instanceof Integer) return ((Integer)value).floatValue();
        else if (value instanceof Long) return ((Long)value).floatValue();
        else if (value instanceof Byte) return ((Byte)value).floatValue();
        else if (value instanceof Character) return (float)((Character)value).charValue();
        else if (value instanceof Short) return ((Short)value).floatValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to float");
    }
    
    /**
     * Unwraps the given object to a double.
     * 
     * @param value  object to unwrap
     * @return  double value of object
     * @throws IllegalArgumentException  if value is not a Double, Float, Long, Integer, Byte, Character, or Short object
     */
    public static double unwrapToDouble(Object value) throws IllegalArgumentException {
        if (value instanceof Double) return ((Double)value).doubleValue();
        else if (value instanceof Float) return ((Float)value).doubleValue();
        else if (value instanceof Integer) return ((Integer)value).doubleValue();
        else if (value instanceof Long) return ((Long)value).doubleValue();
        else if (value instanceof Byte) return ((Byte)value).doubleValue();
        else if (value instanceof Character) return (double)((Character)value).charValue();
        else if (value instanceof Short) return ((Short)value).doubleValue();
        else throw new IllegalArgumentException((value==null?null:value.getClass())+" cannot be converted to double");
    }

}
