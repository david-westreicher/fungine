package util;

public class TickCounter {

	public float fps = 0;
	private long ticks;
	private long last;

	public void tick() {
		ticks++;
		long now = System.currentTimeMillis();
		long delta = now - last;
		if (delta > 100) {
			fps = (float) (ticks * 1000) / (float) delta;
			ticks = 0;
			last = now;
		}
	}

}
