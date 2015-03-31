package test;

public class Lock {
	int state;
	public Lock() {
		state = 0;  //unlocked
	}

	public void Lock() {
		state = 1;
	}
	public void UnLock() {
		state = 0;
	}
}
