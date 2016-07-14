class Uri {

    @STAMP(flows = { @Flow(from = "uriString", to = "@return") })
    public static android.net.Uri parse(java.lang.String uriString) {
        return new StampUri(uriString);
    }

    @STAMP(flows = { @Flow(from = "baseUri", to = "@return") })
    public static android.net.Uri withAppendedPath(android.net.Uri baseUri, java.lang.String pathSegment) {
        return new StampUri();
    }

    @STAMP(flows = { @Flow(from = "file", to = "@return") })
    public static android.net.Uri fromFile(java.io.File file) {
        return new StampUri();
    }
}

