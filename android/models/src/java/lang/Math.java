class Math
{
	@STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double abs(double d);

    @STAMP(flows={@Flow(from="f",to="@return")})
	public static native  float abs(float f);

    @STAMP(flows={@Flow(from="i",to="@return")})
	public static native  int abs(int i);

    @STAMP(flows={@Flow(from="l",to="@return")})
	public static native  long abs(long l);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double acos(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double asin(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double atan(double d);

    @STAMP(flows={@Flow(from="y",to="@return"),@Flow(from="x",to="@return")})
	public static native  double atan2(double y, double x);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double cbrt(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double ceil(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double cos(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double cosh(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double exp(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double expm1(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double floor(double d);

    @STAMP(flows={@Flow(from="y",to="@return"),@Flow(from="x",to="@return")})
	public static native  double hypot(double x, double y);

    @STAMP(flows={@Flow(from="y",to="@return"),@Flow(from="x",to="@return")})
	public static native  double IEEEremainder(double x, double y);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double log(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double log10(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double log1p(double d);

    @STAMP(flows={@Flow(from="d1",to="@return"),@Flow(from="d2",to="@return")})
	public static  double max(double d1, double d2) { return 0.0; }

    @STAMP(flows={@Flow(from="f1",to="@return"),@Flow(from="f2",to="@return")})
	public static  float max(float f1, float f2) { return 0.0f; }

    @STAMP(flows={@Flow(from="i1",to="@return"),@Flow(from="i2",to="@return")})
	public static native  int max(int i1, int i2);

    @STAMP(flows={@Flow(from="l1",to="@return"),@Flow(from="l2",to="@return")})
	public static  long max(long l1, long l2) { return 0L; }

    @STAMP(flows={@Flow(from="d1",to="@return"),@Flow(from="d2",to="@return")})
	public static  double min(double d1, double d2) { return 0.0; }

    @STAMP(flows={@Flow(from="f1",to="@return"),@Flow(from="f2",to="@return")})
	public static  float min(float f1, float f2) { return 0.0f; }
	
    @STAMP(flows={@Flow(from="i1",to="@return"),@Flow(from="i2",to="@return")})
	public static native  int min(int i1, int i2);
	
    @STAMP(flows={@Flow(from="l1",to="@return"),@Flow(from="l2",to="@return")})
	public static  long min(long l1, long l2) { return 0L; }

    @STAMP(flows={@Flow(from="y",to="@return"),@Flow(from="x",to="@return")})
	public static native  double pow(double x, double y);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double rint(double d);

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static  long round(double d) { return 0L; }

    @STAMP(flows={@Flow(from="f",to="@return")})
	public static  int round(float f) { return 0; }

    @STAMP(flows={@Flow(from="d",to="@return")})
	public static  double signum(double d) { return 0.0; }

    @STAMP(flows={@Flow(from="f",to="@return")})
	public static  float signum(float f) { return 0.0f; }

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double sin(double d);

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double sinh(double d);

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double sqrt(double d);

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double tan(double d);

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static native  double tanh(double d);

	@STAMP(flows={@Flow(from="angdeg",to="@return")})
	public static  double toRadians(double angdeg) { return 0.0; }

	@STAMP(flows={@Flow(from="angrad",to="@return")})
	public static  double toDegrees(double angrad) { return 0.0; }

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static  double ulp(double d) { return 0.0; }

	@STAMP(flows={@Flow(from="f",to="@return")})
	public static  float ulp(float f) { return 0.0f; }

	@STAMP(flows={@Flow(from="magnitude",to="@return"),@Flow(from="sign",to="@return")})
	public static  double copySign(double magnitude, double sign) { return 0.0; }

	@STAMP(flows={@Flow(from="magnitude",to="@return"),@Flow(from="sign",to="@return")})
	public static  float copySign(float magnitude, float sign) { return 0.0f; }

	@STAMP(flows={@Flow(from="f",to="@return")})
	public static  int getExponent(float f) { return 0; }

	public static  int getExponent(double d) { return 0; }

	@STAMP(flows={@Flow(from="start",to="@return"),@Flow(from="direction",to="@return")})
	public static  double nextAfter(double start, double direction) { return 0.0; }

	@STAMP(flows={@Flow(from="start",to="@return"),@Flow(from="direction",to="@return")})
	public static  float nextAfter(float start, double direction) { return 0.0f; }

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static  double nextUp(double d) { return 0.0; }
	
	@STAMP(flows={@Flow(from="f",to="@return")})
	public static  float nextUp(float f) { return 0.0f; }

	@STAMP(flows={@Flow(from="d",to="@return"),@Flow(from="scaleFactor",to="@return")})
	public static  double scalb(double d, int scaleFactor) { return 0.0; }

	@STAMP(flows={@Flow(from="d",to="@return"),@Flow(from="scaleFactor",to="@return")})
	public static  float scalb(float d, int scaleFactor) { return 0.0f; }
}
