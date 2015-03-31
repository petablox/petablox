package test;

public class Test {
	public static Lock lock3 = null;
	public static void main(String[] args) {
/*
		Lock lock1 = new Lock();
		Lock lock2 = new Lock();
		lock3 = lock1;
		lock2.Lock();
		lock3.Lock();
		for(int i=0;i<4;i++)
		{
			lock3.UnLock();
			lock3.Lock();
		}
*/
		A a = new A();
		a.f.Lock();
		a.f.UnLock();
	}
}

class A {
	Lock f;
	A() { f = new Lock(); }
}

