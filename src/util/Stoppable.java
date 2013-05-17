package util;

import java.util.ArrayList;
import java.util.List;

public class Stoppable extends Thread {
	private static List<Stoppable> allThreads = new ArrayList<Stoppable>();
	protected boolean running = true;

	public Stoppable(String name) {
		super(name);
		allThreads.add(this);
	}

	public void stopThread() {
		synchronized (this) {
			this.notifyAll();
		}
		running = false;
	}

	public static void stopAll() {
		Log.log(Stoppable.class, "Stopping all Threads");
		for (Stoppable r : allThreads) {
			Log.log(Stoppable.class, "Stopping " + r.getClass().getSimpleName()
					+ ":" + r.toString());
			r.stopThread();
		}
		allThreads.clear();
	}
}
