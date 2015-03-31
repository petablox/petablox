package joeq.Runtime;

import jwutil.util.Assert.DebugDelegate;

public class DebugImpl implements DebugDelegate {
        public void write(byte[] msg, int size) {
            for (int i=0; i<size; ++i)
                System.err.print((char) msg[i]);
        }

        public void write(String msg) {
            System.err.print(msg);
        }

        public void writeln(byte[] msg, int size) {
            write(msg, size);
            System.err.println();
        }

        public void writeln(String msg) {
            System.err.println(msg);
        }

        public void die(int code) {
			throw new RuntimeException();
        }
}

