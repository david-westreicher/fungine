package vr;

import javax.vecmath.Matrix3f;

import settings.Settings;

public class VRFactory {

	public interface VR {

		float[] getRotation();

		Matrix3f getMatrix();

	}

	public static VR createVR() {
		if (Settings.IS_WINDOWS)
			return new JRift();
		return new RiftFetcher();
	}

}
