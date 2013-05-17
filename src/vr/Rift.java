package vr;

import rendering.RenderUpdater;

public class Rift {
	public static final float HScreenSize = 0.14976f;
	public static final float VScreenSize = 0.0935f;
	public static final float VScreenCenter = VScreenSize / 2;
	public static final float EyeToScreenDistance = 0.041f;
	public static final float LensSeparationDistance = 0.064f;
	public static final float InterpupillaryDistance = 0.064f;
	public static final float DistortionK = 0.1f;
	public static final float HResolution = 1280;
	public static final float VResolution = 800;
	private static float fov;
	private static float h;

	public static float getH() {
		if (h == 0f)
			h = 4 * (HScreenSize / 4 - LensSeparationDistance / 2)
					/ HScreenSize;
		return h;
	}

	public static float getFOV() {
		if (fov == 0)
			fov = (float) (2.0f * Math
					.atan(VScreenCenter / EyeToScreenDistance) * 180 / Math.PI);
		return fov;
	}

	public static float getDip() {
		return RenderUpdater.EYE_GAP / 2;
	}

}
