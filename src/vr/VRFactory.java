package vr;

import javax.vecmath.Matrix3f;

import com.sun.jna.Platform;

public class VRFactory {

	public interface VR {

		float[] getRotation();

		Matrix3f getMatrix();

	}

	public static VR createVR() {
		if (Platform.isWindows())
			return new JRift();
		return new RiftFetcher();
	}

}
