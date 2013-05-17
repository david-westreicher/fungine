package world;

import javax.vecmath.Matrix3f;

import settings.Settings;

public class Camera extends GameObject {
	public float oldZoom = 1;
	public float zoom = 1;
	public float focus;

	static {
		new GameObjectType("cam");
	}

	public Camera() {
		super("cam");
		// setPos(Settings.WIDTH / 2, Settings.HEIGHT / 2, 500);
		// rotation[2] = (float) (Math.PI / 2);
	}

	public boolean zoomChanged() {
		if (oldZoom != zoom) {
			oldZoom = zoom;
			return true;
		}
		return false;
	}

	@Override
	public void beforeUpdate() {
		if (Settings.VR)
			setTo(oldPos, pos);
		else
			super.beforeUpdate();
	}

	public void setRotation(Matrix3f matrix) {
		rotationMatrix.set(matrix);
	}

}
