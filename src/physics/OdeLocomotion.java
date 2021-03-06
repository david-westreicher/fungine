package physics;

import java.util.Map;

import javax.vecmath.Vector3f;

import org.ode4j.ode.DBody;
import org.ode4j.ode.DHingeJoint;
import org.ode4j.ode.DJoint;

import world.GameObject;
import world.Joint;
import world.Joint.Type;
import world.LocomotionData;

public class OdeLocomotion {
	private static final double KP = 4;
	private static final double KD = KP / 1.5;
	private static final float CV = 3000f;
	private static final float CD = 0;
	private static float calcAnlge;
	private static float angleRate;
	private static Vector3f tmp2 = new Vector3f();
	private static Vector3f up = new Vector3f(0, 1, 0);
	private static float maxTorque;

	public static void locomote(Map<Joint, DJoint> jointMap,
			LocomotionData loc, DBody dBody) {
		Joint stanceHip = null;
		float swingHipTorque = 0;
		float torsoTorque = 0;
		for (Joint j : loc.joints)
			if (j.jointType == Type.DUMMY) {
				float target = loc.getCurrentTarget(j);
				torsoTorque = propDerivControl(j, target, null, dBody, null);
			}
		for (Joint j : loc.joints)
			if (loc.isStanceHip(j)) {
				stanceHip = j;
			} else {
				float target = loc.getCurrentTarget(j);
				if (loc.isSwingHip(j)) {
					target = feedBack(loc, j, target);
					swingHipTorque = propDerivControl(j, target,
							jointMap.get(j), dBody, loc.body);
				} else
					propDerivControl(j, target, jointMap.get(j), null, null);
			}
		((DHingeJoint) jointMap.get(stanceHip))
				.addTorque(capTorque(-swingHipTorque - torsoTorque));
		loc.step();
	}

	private static float feedBack(LocomotionData loc, Joint j,
			float currentTarget) {
		float v = loc.getV();
		float d = loc.getD();
		// if (Game.INSTANCE.loop.tick % (Game.INSTANCE.loop.TICKS_PER_SECOND /
		// 2) == 0)
		// Log.log(OdeLocomotion.class, loc.currentState, d, v, calcAnlge,
		// maxTorque);
		if (loc.currentState % 2 == 0)
			return currentTarget + CD * d * Math.signum(CV) + CV * v;
		else
			return currentTarget;// + 0 * d + CV * v;
	}

	private static float propDerivControl(Joint j, double currentTarget,
			DJoint odeJoint, DBody dBody, GameObject body) {
		switch (j.jointType) {
		case DUMMY:
			float angle = calcAngle(j.body[0]);
			angleRate = (calcAnlge - angle);
			calcAnlge = angle;
			return (float) (KP * (currentTarget - angle) - KD * angleRate);
		case HINGE:
			DHingeJoint hinge = ((DHingeJoint) odeJoint);
			float torque = 0;
			double angle1 = hinge.getAngle() + (body == null ? 0 : calcAnlge);
			double angleRate1 = hinge.getAngleRate()
					+ (body == null ? 0 : angleRate);
			torque = (float) (KP * (currentTarget - angle1) - KD * angleRate1);
			hinge.addTorque(capTorque(torque));
			return torque;
		default:
			return 0;
		}
	}

	private static double capTorque(float torque) {
		if (Math.abs(torque) > maxTorque)
			maxTorque = Math.abs(torque);
		return Math.min(Math.max(-40, torque), 40);
	}

	private static float calcAngle(GameObject go) {
		tmp2.set(0, 0, 1);
		go.rotationMatrix.transform(tmp2);
		// Game.INSTANCE.loop.renderer.addDebugLine(go.pos, new float[] {
		// tmp2.x + go.pos[0], tmp2.y + go.pos[1], tmp2.z + go.pos[2] });
		float dot = up.dot(tmp2);
		return (float) Math.asin(Math.abs(dot)) * Math.signum(dot);
	}
}
