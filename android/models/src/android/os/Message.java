class Message
{
	public  Message() { }

	public static  android.os.Message obtain() { return new Message(); }
	
	public static  android.os.Message obtain(android.os.Message orig) { return orig;  }

	public static  android.os.Message obtain(android.os.Handler h) { 
		Message m = new Message(); 
		m.handler = h;
		return m;
	}
	
	public static  android.os.Message obtain(android.os.Handler h, java.lang.Runnable callback) {
		Message m = new Message(); 
		m.handler = h;
		return m;
	}

	@STAMP(flows={@Flow(from="what",to="@return")})
	public static  android.os.Message obtain(android.os.Handler h, int what) { 
		Message m = new Message(); 
		m.handler = h;
		m.what = what;
		return m;
	}

	@STAMP(flows={@Flow(from="what",to="@return"),@Flow(from="obj",to="@return")})
	public static  android.os.Message obtain(android.os.Handler h, int what, java.lang.Object obj) {  
		Message m = new Message(); 
		m.handler = h;
		m.what = what;
		m.obj = obj;
		return m;
	}

	@STAMP(flows={@Flow(from="what",to="@return"),@Flow(from="arg1",to="@return"),@Flow(from="arg2",to="@return")})
	public static  android.os.Message obtain(android.os.Handler h, int what, int arg1, int arg2) {
		Message m = new Message(); 
		m.handler = h;
		m.what = what;
		m.arg1 = arg1;
		m.arg2 = arg2;
		return m;
	}

	@STAMP(flows={@Flow(from="what",to="@return"),@Flow(from="arg1",to="@return"),@Flow(from="arg2",to="@return"),@Flow(from="obj",to="@return")})
	public static  android.os.Message obtain(android.os.Handler h, int what, int arg1, int arg2, java.lang.Object obj) {  
		Message m = new Message(); 
		m.handler = h;
		m.what = what;
		m.arg1 = arg1;
		m.arg2 = arg2;
		m.obj = obj;
		return m;
	}

	@STAMP(flows={@Flow(from="o",to="this")})
	public  void copyFrom(android.os.Message o) {
		this.handler = o.handler;
		this.what = o.what;
		this.arg1 = o.arg1;
		this.arg2 = o.arg2;
		this.obj = o.obj;
	}
	
	public  void setTarget(android.os.Handler target) {
		this.handler = target;
	}

	public  android.os.Handler getTarget() {  
		return this.handler;
	}

	public  android.os.Bundle getData() {
		return (Bundle) obj;
	}

	public  android.os.Bundle peekData() {
		return (Bundle) obj;
	}

	@STAMP(flows={@Flow(from="data",to="this")})
	public  void setData(android.os.Bundle data) {
		this.obj = data;
	}

	public  void sendToTarget() {  
		handler.handleMessage(this);
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String toString() { return new String(); }

	Handler handler;
}