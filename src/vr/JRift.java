package vr;

import javax.vecmath.Matrix3f;

import util.Log;
import util.RepeatedRunnable;
import vr.VRFactory.VR;
import de.fruitfly.ovr.HMDInfo;
import de.fruitfly.ovr.OculusRift;

public class JRift implements VR {
	private Matrix3f m = new Matrix3f();
	private Matrix3f temp = new Matrix3f();
	private float[] rot = new float[3];

	public JRift() {
		m.setIdentity();
		new RepeatedRunnable("JRift") {

			private OculusRift or;

			@Override
			protected void onStopped() {
				//TODO causes thread to never terminate: or.destroy();
			}

			@Override
			protected void onStarted() {
				or = new OculusRift();
				or.init();
				HMDInfo hmdInfo = or.getHMDInfo();
				Log.log(this, hmdInfo);
			}

			@Override
			protected void executeRepeatedly() {
				if (or.isInitialized()) {
					or.poll();
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					synchronized (m) {
						rot[0] = or.getPitch();
						rot[1] = or.getYaw();
						rot[2] = or.getRoll();
						m.setIdentity();
						temp.rotY(rot[1]);
						m.mul(temp);
						temp.rotX(rot[0]);
						m.mul(temp);
						temp.rotZ(rot[2]);
						m.mul(temp);
					}
				}
			}
		}.start();
	}

	@Override
	public float[] getRotation() {
		synchronized (m) {
			return rot;
		}
	}

	@Override
	public Matrix3f getMatrix() {
		synchronized (m) {
			return m;
		}
	}
}
