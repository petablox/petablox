import java.util.Random;

public class T {
	static final int NUM_THREADS = 10;
	static int[] g = new int[NUM_THREADS];
	static Object lock = new Object();
	static int result;
	public static void main(String[] a) throws Exception {
		Thread[] threads = new Thread[NUM_THREADS];
		final Random rg = new Random();
		for (int i = 0; i < NUM_THREADS; i++) {
			Thread t = new Thread() {
				public void run() {
					synchronized (lock) {
						int k = rg.nextInt(NUM_THREADS);
						g[k]++;
					}
				}
			};
			threads[i] = t;
		}
		for (int i = 0; i < NUM_THREADS; i++)
			threads[i].start();
		for (int i = 0; i < NUM_THREADS; i++)
			threads[i].join();
		result = g[5];   
		System.out.println("RESULT: " + result);
	}
}

