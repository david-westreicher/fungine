package vr;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3f;

import util.RepeatedRunnable;
import vr.VRFactory.VR;

public class RiftFetcher extends RepeatedRunnable implements VR {

	private Matrix3f m = new Matrix3f();
	private Matrix3f modifiable = new Matrix3f();
	private Matrix3f tempM = new Matrix3f();
	private Quat4d q = new Quat4d();
	private float[] rot = new float[3];
	private boolean start = true;
	private Vector3f viewVec = new Vector3f();

	public RiftFetcher() {
		super("RiftFetcher");
		tempM.setIdentity();
		tempM.rotX(-(float) Math.PI / 2);
		start();
	}

	public Matrix3f getMatrix() {
		synchronized (m) {
			return modifiable;
		}
	}

	public float[] getRotation() {
		synchronized (m) {
			return rot;
		}
	}

	@Override
	protected void executeRepeatedly() {
		if (start) {
			start = false;
			RiftJNI.start();
		}
		double[] quat = RiftJNI.getData();
		q.set(quat);
		synchronized (m) {
			m.set(q);
			m.mul(tempM);
			modifiable.set(m);
			viewVec.set(0, 0, -1);
			m.transform(viewVec);
			rot[1] = (float) (-Math.PI / 2 - Math.atan2(viewVec.z, viewVec.x));
			rot[0] = (float) (Math.atan2(viewVec.y,
					Math.sqrt(viewVec.z * viewVec.z + viewVec.x * viewVec.x)));
			viewVec.set(0, 1, 0);
			m.transform(viewVec);
			rot[2] = (float) (Math.random() * Math.PI);// (Math.atan2(viewVec.y,
														// viewVec.x) - Math.PI
														// / 2);
		}

	}

	protected void onStopped() {
		RiftJNI.end();
	}
}
