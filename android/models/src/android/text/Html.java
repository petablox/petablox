package android.text;

class Html {

    @STAMP(flows = { @Flow(from = "source", to = "@return") })
    public static android.text.Spanned fromHtml(java.lang.String source) {
        return new android.text.SpannableString(source);
    }

    @STAMP(flows = { @Flow(from = "source", to = "@return") })
    public static android.text.Spanned fromHtml(java.lang.String source, android.text.Html.ImageGetter imageGetter, android.text.Html.TagHandler tagHandler) {
        return new android.text.SpannableString(source);
    }

    @STAMP(flows = { @Flow(from = "text", to = "@return") })
    public static java.lang.String toHtml(android.text.Spanned text) {
		return new String();
    }

    @STAMP(flows = { @Flow(from = "text", to = "@return") })
    public static java.lang.String escapeHtml(java.lang.CharSequence text) {
		return new String();
    }
}

