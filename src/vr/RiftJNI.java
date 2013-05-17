package vr;

import settings.Settings;
import util.Log;

public class RiftJNI {

	static {
		if (Settings.VR) {
			System.loadLibrary("rift");
		}
	}

	public native static void start();

	public native static double[] getData();

	public native static void end();

	public static void main(String[] args) throws InterruptedException {
		start();
		Thread.sleep(1000);
		int i = 0;
		while (i++ < 1000000) {
			// Thread.sleep(1000);
			long start = System.nanoTime();
			double[] data = getData();
			Log.log(RiftJNI.class, System.nanoTime() - start);
			if (data != null && data.length == 4)
				Log.log(RiftJNI.class, data[0], data[1], data[2], data[3]);
			else
				Log.log(RiftJNI.class, "error", data, data.getClass());
		}
		end();
	}

}
