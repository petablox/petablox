// ANSIColorStream.java, created Tue Sep 30 14:22:48 PDT 2003 by gback
// Copyright (C) 2001-3 Godmar Back <gback@cs.utah.edu, @stanford.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;
import java.io.PrintStream;
import java.io.FilterOutputStream;
import java.io.IOException;

/**
 * ANSIColorStream provides ANSI-color streams.
 *
 * Comes in handy when outputting to a terminal, simply use
 * ANSIColorStream.red.println("in red") to print a line in red.
 *
 * Set ANSIColorStream.OFF to true to turn this off.
 *
 * @see jwutil.io.ANSIColorStream#OFF
 * @author Godmar Back <gback@cs.utah.edu, @stanford.edu>
 */
public class ANSIColorStream extends PrintStream {
    public static boolean OFF = false;
    public static final int RESET = 0;
    public static final int BRIGHT = 1;
    public static final int DIM = 2;
    public static final int UNDERLINE = 3;
    public static final int BLINK = 4;
    public static final int REVERSE = 7;
    public static final int HIDDEN = 8;
    public static final int BLACK = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int YELLOW = 3;
    public static final int BLUE = 4;
    public static final int MAGENTA = 5;
    public static final int CYAN = 6;
    public static final int GRAY = 7;
    public static final int WHITE = 8;
    public static ANSIColorStream blue = new ANSIColorStream(System.out,
            ANSIColorStream.BLUE);
    public static ANSIColorStream red = new ANSIColorStream(System.out,
            ANSIColorStream.RED);
    public static ANSIColorStream green = new ANSIColorStream(System.out,
            ANSIColorStream.GREEN);
    public static ANSIColorStream cyan = new ANSIColorStream(System.out,
            ANSIColorStream.CYAN);
    public ANSIColorStream(final PrintStream out, final int fgColor) {
        this(out, fgColor, WHITE);
    }
    public ANSIColorStream(final PrintStream pout, final int fgColor,
            final int bgColor) {
        super(new FilterOutputStream(pout) {
            private void setColor() {
                ((PrintStream) out).print("\033[0;" + (30 + fgColor) + ";"
                        + (40 + bgColor) + "m");
            }
            private void resetColor() {
                ((PrintStream) out).print("\033[00m");
            }
            public void write(int b) throws IOException {
                if (OFF) {
                    out.write(b);
                } else {
                    setColor();
                    out.write(b);
                    resetColor();
                }
            }
            public void write(byte[] b, int off, int len) throws IOException {
                if (OFF) {
                    out.write(b, off, len);
                } else {
                    setColor();
                    out.write(b, off, len);
                    resetColor();
                }
            }
        });
    }
    public static void main(String av[]) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 9; j++) {
                PrintStream out = new ANSIColorStream(System.out, i, j);
                out.print(av.length > 0 ? av[0] : "testing");
            }
            System.out.println();
        }
    }
}
