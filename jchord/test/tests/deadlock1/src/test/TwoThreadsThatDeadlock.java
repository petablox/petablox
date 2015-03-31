package test;

public class TwoThreadsThatDeadlock {
		static Object lock = getLock();
		static Object lock1 = getLock1();
        public static class MyThread1 extends Thread {
                public void run() {
                        synchronized (lock) {
                                System.err.println(Thread.currentThread()
                                                + " Acquired the lock " + lock);
                                synchronized (lock1) {
                                        System.err.println(Thread.currentThread()
                                                        + " Acquired the lock " + lock1);
                                }
                        }
                }
        }

        public static class MyThread2 extends Thread {
                public void run() {
                        synchronized (lock1) {
                                System.err.println(Thread.currentThread()
                                                + " Acquired the lock " + lock);
                                synchronized (lock) {
                                        System.err.println(Thread.currentThread()
                                                        + " Acquired the lock " + lock1);
                                }
                        }
                }
        }

        public static Object getLock() {
                Object lock = new Object(); // "First";
                return lock;
        }

        public static Object getLock1() {
                Object lock = new Object(); // "Second";
                return lock;
        }

        public static void main(String[] args) throws Exception {
                Thread t1 = new MyThread1();
                Thread t2 = new MyThread2();
                t1.start();
                t2.start();
                t1.join();
                t2.join();
        }
}
