package util;

public abstract class RepeatedThreadExact extends Stoppable {
	private int fps;
	private int nanoLength;
	private int frameCount;
	private long startTime;
	private boolean pause;

	public RepeatedThreadExact(int fps, String name) {
		super(name);
		setFPS(fps);
	}

	public void setFPS(int fps) {
		this.fps = fps;
		nanoLength = 1000000000 / fps;
		reset();
	}

	public void run() {
		Log.log(this, " started!");
		running = true;
		pause = false;
		reset();

		while (running) {
			executeRepeatedly(0);
			frameCount++;
			while ((System.nanoTime() - startTime) / nanoLength < frameCount
					&& !pause && running) {
				Thread.yield();
			}
			if (frameCount > 5 * fps) {
				reset();
			}
			if (pause) {
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Log.log(this, " terminated!");
	}

	private void reset() {
		startTime = System.nanoTime();
		frameCount = 0;
	}

	public void startPause() {
		pause = true;
	}

	public void endPause() {
		pause = false;
		synchronized (this) {
			this.notify();
		}
	}

	protected abstract void executeRepeatedly(float deltaTime);
}
