package java.io;

class RandomAccessFile {

    @STAMP(flows = { @Flow(from = "file", to = "this") })
    public RandomAccessFile(java.io.File file, java.lang.String mode) throws java.io.FileNotFoundException {
    }

    @STAMP(flows = { @Flow(from = "fileName", to = "this") })
    public RandomAccessFile(java.lang.String fileName, java.lang.String mode) throws java.io.FileNotFoundException {
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public final java.io.FileDescriptor getFD() throws java.io.IOException {
        return new FileDescriptor();
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public int read() throws java.io.IOException {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "buffer") })
    public int read(byte[] buffer) throws java.io.IOException {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "buffer") })
    public int read(byte[] buffer, int byteOffset, int byteCount) throws java.io.IOException {
        return 0;
    }

    @STAMP(flows = { @Flow(from = "this", to = "@return") })
    public final boolean readBoolean() throws java.io.IOException {
        return true;
    }
}

