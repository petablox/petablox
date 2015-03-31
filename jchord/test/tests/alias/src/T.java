public class T {
	int f;
	static {
		(new Thread() { public void run() { } }).start();
	}
	public static void main(String[] args) {
		T t1 = getNew();
		T t2 = getNew();
		int i = t1.f;
		t2.f = i;

		T t3 = new T();
		T t4 = new T();
		int j = t3.f;
		t4.f = j;

		T t5 = new T();
		T t6 = t5;
		int k = t5.f;
		t6.f = k;
	}
	public static T getNew() {
		return new T();
	}
}
