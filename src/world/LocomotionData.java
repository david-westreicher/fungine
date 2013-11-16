package world;

import game.Game;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import physics.OdePhysics;
import physics.OdePhysics.CollisionCallback;
import util.Log;

public class LocomotionData {
	public GameObject body;
	public Joint hipA;
	public Joint hipB;
	public Joint[] joints;
	public GameObject footA;
	public GameObject footB;
	public GameObject floor;
	public int currentState = 0;
	private Joint torsoDummy;
	private List<State> stateVals = new ArrayList<State>();
	private Joint swingHip;
	private Joint stanceHip;
	private GameObject swingFoot;
	private GameObject stanceFoot;
	private Vector3f com = new Vector3f();
	private Vector3f oldCom = new Vector3f();
	private Vector3f tmp = new Vector3f();
	private Vector3f tmp2 = new Vector3f();
	public CollisionCallback collisionCallback = new CollisionCallback() {
		@Override
		public void call(GameObject go1, GameObject go2) {
			if (go1 == swingFoot || go2 == swingFoot) {
				if (currentState % 2 == 1)
					if (go1 == footA || go2 == footA) {
						((ContactObjective) stateVals.get(1).objective).contactHappened = true;
					} else {
						((ContactObjective) stateVals.get(3).objective).contactHappened = true;
					}
			}
		}
	};

	public LocomotionData(GameObject body, Joint jA, Joint jB, Joint jKneeA,
			Joint jKneeB, GameObject jFootA, GameObject jFootB, Joint jHeelA,
			Joint jHeelB, GameObject floor) {
		this.body = body;
		this.hipA = jA;
		this.hipB = jB;
		this.footA = jFootA;
		this.footB = jFootB;
		torsoDummy = new Joint();
		torsoDummy.jointType = Joint.Type.DUMMY;
		torsoDummy.body[0] = body;
		this.joints = new Joint[] { torsoDummy, jA, jB, jKneeA, jKneeB, jHeelA,
				jHeelB };
		this.floor = floor;
		initState();
		getV();
		getV();
	}

	public void addState(float jA, float jB, float jKneeA, float jKneeB,
			float jHeelA, float jHeelB, float time) {
		Objective objective = (time != 0) ? new TimeObjective((int) (time
				* (0.02f / 60f) / (OdePhysics.STEPSIZE)))
				: new ContactObjective();
		stateVals.add(new State(new float[] { -0.f, jA, jB, jKneeA, jKneeB,
				jHeelA, jHeelB }, objective));
	}

	public float getCurrentTarget(Joint j) {
		int index = 0;
		for (; index < joints.length; index++) {
			if (j == joints[index])
				break;
		}
		return stateVals.get(currentState).target[index];
	}

	public void step() {
		State state = stateVals.get(currentState);
		// Log.log(this, currentState);
		if (state.objective()) {
			state.reset();
			currentState = (currentState + 1) % stateVals.size();
			initState();
		}
	}

	private void initState() {
		if (currentState / 2 == 0) {
			swingHip = hipA;
			stanceHip = hipB;
			stanceFoot = footB;
			swingFoot = footA;
		} else {
			swingHip = hipB;
			stanceHip = hipA;
			stanceFoot = footA;
			swingFoot = footB;
		}
	}

	public boolean isStanceHip(Joint j) {
		return stanceHip == j;
	}

	public boolean isSwingHip(Joint j) {
		return swingHip == j;
	}

	public float getD() {
		tmp.set(0, 0, -1);
		stanceFoot.rotationMatrix.transform(tmp);
		tmp.add(new Vector3f(stanceFoot.pos));
		Game.INSTANCE.loop.renderer.addDebugLine(new float[] { tmp.x, tmp.y,
				tmp.z }, new float[] { tmp.x, tmp.y + 1, tmp.z });
		tmp.sub(com);
		tmp.scale(-1);
		tmp.y = 0;
		tmp2.set(0, 0, 1);
		body.rotationMatrix.transform(tmp2);
		tmp2.y = 0;
		tmp2.normalize();
		float result = tmp.dot(tmp2);
		tmp2.scale(-result);
		Game.INSTANCE.loop.renderer.addDebugLine(
				new float[] { com.x, 0, com.z }, new float[] { com.x + tmp2.x,
						0, com.z + tmp2.z });
		return result;
	}

	public float getV() {
		com.set((hipA.pos[0] + hipB.pos[0]) / 2,
				(hipA.pos[1] + hipB.pos[1]) / 2,
				(hipA.pos[2] + hipB.pos[2]) / 2);
		Vector3f debug = (Vector3f) com.clone();
		tmp.set(com);
		tmp.sub(oldCom);
		tmp.y = 0;
		Game.INSTANCE.loop.renderer.addDebugLine(new float[] { debug.x,
				debug.y, debug.z }, new float[] { debug.x + tmp.x * 1000,
				debug.y + tmp.y, debug.z + tmp.z * 1000 });
		oldCom.set(com);
		tmp2.set(0, 0, 1);
		body.rotationMatrix.transform(tmp2);
		tmp2.y = 0;
		return tmp.length() * Math.signum(tmp.dot(tmp2));
	}

	public static class State {
		public float[] target;
		public Objective objective;

		public State(float[] fs, Objective objective) {
			this.target = fs;
			this.objective = objective;
		}

		public boolean objective() {
			return objective.isMet();
		}

		public void reset() {
			objective.reset();
		}
	}

	public static interface Objective {
		boolean isMet();

		void reset();
	}

	public static class TimeObjective implements Objective {
		public int ticks;
		public int oldTick;

		public TimeObjective(int ticks) {
			this.ticks = ticks;
			oldTick = ticks;
			Log.log(this, ticks);
		}

		@Override
		public boolean isMet() {
			// Log.log(this, ticks);
			return --ticks <= 0;
		}

		@Override
		public void reset() {
			ticks = oldTick;
		}
	}

	public static class ContactObjective implements Objective {
		public boolean contactHappened = false;

		@Override
		public boolean isMet() {
			return contactHappened;
		}

		@Override
		public void reset() {
			contactHappened = false;
		}

	}
}
