/* Thread structure:

  t1
  |
  |     t2
  |______
p1|     |p2h
  |     |
  |
  |
  |     t3
  |______
p2|     |p3h 
  |     |      t4
  |     |_______
  |   p3|      |p4h
  |     |      |
  |
  |
  |     t5
  |______    
p4|     |p5h 
  |     | 

*/

public class T {
	public static void main(String[] args) {
		final Thread t2 = new Thread() {
			public void run() {
				// point p2h:
				System.out.println("t2");
			}
		};
		final Thread t4 = new Thread() {
			public void run() {
				// point p4h:
				System.out.println("t4");
			}
		};
		final Thread t3 = new Thread() {
			public void run() {
				// point p3h:
				System.out.println("t3");
				t4.start();
			}
		};
		final Thread t5 = new Thread() {
			public void run() {
				// point p5h:
				System.out.println("t5");
			}
		};
		t2.start();
		t3.start();
		t5.start();
	}
}
