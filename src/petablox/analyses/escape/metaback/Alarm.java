package petablox.analyses.escape.metaback;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Alarm {
	private Timer timer;
	private volatile boolean isTimedOut;
	
	public Alarm(int milliseconds) {
		isTimedOut = false;
		timer = new Timer();
		timer.schedule(new Task(), milliseconds);
	}
	
	public boolean isTimedOut(){
		return isTimedOut;
	}
	
	public void cancel(){
		timer.cancel();
	}
	
	private class Task extends TimerTask{

		@Override
		public void run() {
			isTimedOut=true;
		}
		
	}
}
