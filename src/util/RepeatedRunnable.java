package util;

public abstract class RepeatedRunnable extends Stoppable {
	public RepeatedRunnable(String name) {
		super(name);
	}

	private boolean pause = false;

	@Override
	public void run() {
		Log.log(this, " started!");
		onStarted();
		while (running) {
			if (pause) {
				synchronized (this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			executeRepeatedly();
		}
		Log.log(this, "terminated!");
		onStopped();
	}

	protected void onStopped() {
	}

	protected void onStarted() {
	}

	protected abstract void executeRepeatedly();

	public void startPause() {
		Log.log(this, " paused!");
		pause = true;
	}

	public void endPause() {
		Log.log(this, " pause ended!");
		synchronized (this) {
			this.notify();
		}
		pause = false;
	}

	public boolean isPausing() {
		return pause;
	}

}
