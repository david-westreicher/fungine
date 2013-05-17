package test;

public class CountMS {
	private long start;

	public CountMS() {
	}

	public void reset() {
		start = System.currentTimeMillis();
	}

	public void stop() {
		System.out.println("stopping " + (System.currentTimeMillis() - start));
	}

}
