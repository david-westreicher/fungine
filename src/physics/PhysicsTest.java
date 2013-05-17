package physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import util.Log;
import world.GameObject;
import world.GameObjectType;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

public class PhysicsTest {
	public List<LocalRayResult> results = new ArrayList<LocalRayResult>();

	public static Map<GameObject, RigidBody> ids = new HashMap<GameObject, RigidBody>();
	public static final float PHYSICS_SCALE = 1f / 100f;
	private static PhysicsTest INSTANCE = null;

	private static DiscreteDynamicsWorld dynamicsWorld;
	private long last;
	private static Tuple3f tmpVector3f = new Vector3f();
	private static Quat4f o = new Quat4f();
	private static AxisAngle4f aa = new AxisAngle4f();
	private static Transform trans = new Transform();

	public PhysicsTest() {
		INSTANCE = this;
		CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
		CollisionDispatcher dispatcher = new CollisionDispatcher(
				collisionConfiguration);
		Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
		Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);
		int maxProxies = 1024;
		AxisSweep3 overlappingPairCache = new AxisSweep3(worldAabbMin,
				worldAabbMax, maxProxies);
		SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher,
				overlappingPairCache, solver, collisionConfiguration);
		dynamicsWorld.setGravity(new Vector3f(0, -5, 0));
		// addFloor();
	}

	private void addFloor() {// create a few basic rigid bodies
		CollisionShape groundShape = new BoxShape(new Vector3f(500.f, 10.f,
				500.f));
		// keep track of the shapes, we release memory at exit.
		// make sure to re-use collision shapes among rigid bodies whenever
		// possible!
		ObjectArrayList<CollisionShape> collisionShapes = new ObjectArrayList<CollisionShape>();

		collisionShapes.add(groundShape);

		Transform groundTransform = new Transform();
		groundTransform.setIdentity();
		groundTransform.origin.set(new Vector3f(0.f, -10.f, 0.f));

		{
			float mass = 0f;

			// rigidbody is dynamic if and only if mass is non zero,
			// otherwise static
			boolean isDynamic = (mass != 0f);

			Vector3f localInertia = new Vector3f(0, 0, 0);
			if (isDynamic) {
				groundShape.calculateLocalInertia(mass, localInertia);
			}

			// using motionstate is recommended, it provides interpolation
			// capabilities, and only synchronizes 'active' objects
			DefaultMotionState myMotionState = new DefaultMotionState(
					groundTransform);
			RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
					mass, myMotionState, groundShape, localInertia);
			RigidBody body = new RigidBody(rbInfo);
			// body.setFriction(0.5f);

			// add the body to the dynamics world
			dynamicsWorld.addRigidBody(body);
		}
	}

	public void update(Map<String, List<GameObject>> objs) {
		for (String s : objs.keySet()) {
			CollisionShape cs = GameObjectType.getType(s).shape;
			if (cs != null)
				for (GameObject go : objs.get(s)) {
					if (!ids.containsKey(go)) {
						ids.put(go, createRigid(cs, go));
					}
				}
		}

		for (List<GameObject> list : objs.values())
			for (GameObject go : list) {
				updateForce(go, ids.get(go));
			}

		// for (int i = 0; i < 3; i++)
		float delta = 0.03f;// ((float) (System.currentTimeMillis() - last) /
							// 1000f);
		// dynamicsWorld.rayTest(new Vector3f(0,0,0), new Vector3f(0,100,0), new
		// RayResultCallback() {
		// @Override
		// public float addSingleResult(LocalRayResult rayResult, boolean arg1)
		// {
		// Log.log(this, rayResult.hitNormalLocal);
		// Log.log(this, rayResult.hitFraction);
		// return 0;
		// }
		// });
		// KinematicCharacterController kc = new
		// KinematicCharacterController(null, null, delta);
		// dynamicsWorld.addAction(kc);
		dynamicsWorld.stepSimulation(delta, 5);
		last = System.currentTimeMillis();

		for (List<GameObject> list : objs.values())
			for (GameObject go : list) {
				updateTransformation(go, ids.get(go));
			}
	}

	private void updateForce(GameObject go, RigidBody rigidBody) {
		if (rigidBody != null) {
			boolean hasForce = false;
			for (int i = 0; i < 3; i++)
				if (go.force[i] != 0)
					hasForce = true;
			if (hasForce) {
				rigidBody.applyCentralForce(new Vector3f(go.force));
				go.resetForce();
			}
		}
	}

	private void updateTransformation(GameObject go, RigidBody body) {
		if (body != null) {

			body.getWorldTransform(trans);
			trans.getRotation(o);
			aa.set(o);
			go.setRotation(o);
			go.angle = aa.angle;
			go.rotation[0] = aa.x;
			go.rotation[1] = aa.y;
			go.rotation[2] = aa.z;
			// Util.toEuler(aa.x, aa.y, aa.z, aa.angle, go.rotation);
			// go.beforeUpdate();
			// Util.toEuler(trans.basis, go.rotation);

			go.pos[0] = trans.origin.x / PHYSICS_SCALE;
			go.pos[1] = trans.origin.y / PHYSICS_SCALE;
			go.pos[2] = trans.origin.z / PHYSICS_SCALE;
			// System.out.printf("world pos = %f,%f,%f\n", trans.origin.x,
			// trans.origin.y, trans.origin.z);
		}

	}

	private RigidBody createRigid(CollisionShape cs, GameObject go) {
		Transform startTransform = new Transform();
		startTransform.setIdentity();

		float mass = go.fixed ? 0.0f : 1.0f;

		// rigidbody is dynamic if and only if mass is non zero,
		// otherwise static
		boolean isDynamic = (mass != 0f);

		Vector3f localInertia = new Vector3f(0, 0, 0);
		if (isDynamic) {
			cs.calculateLocalInertia(mass, localInertia);
		}

		startTransform.origin.set(new Vector3f(go.pos[0] * PHYSICS_SCALE,
				go.pos[1] * PHYSICS_SCALE, go.pos[2] * PHYSICS_SCALE));

		// using motionstate is recommended, it provides
		// interpolation capabilities, and only synchronizes
		// 'active' objects
		MotionState myMotionState = new DefaultMotionState(startTransform);

		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass,
				myMotionState, cs, localInertia);
		RigidBody body = new RigidBody(rbInfo);
		body.applyCentralForce(new Vector3f(go.force));
		body.setFriction(go.friction);

		go.resetForce();
		if (cs instanceof CapsuleShape) {
			Log.log(this, "capsule found");
			body.setSleepingThresholds(0, 0);
			body.setAngularFactor(0);
		}

		try {
			dynamicsWorld.addRigidBody(body);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return body;
	}

	public float rayTest(GameObject go, float rayLength) {
		Vector3f start = new Vector3f(go.pos);
		start.scale(PHYSICS_SCALE);
		go.rotationMatrix.transform(tmpVector3f);
		// Vector3f rot = go.getEyeVector();
		// rot.scale(rayLength);
		Vector3f end = new Vector3f(go.pos);
		// end.add(rot);
		end.add(new Vector3f(tmpVector3f.x * rayLength, tmpVector3f.y
				* rayLength, tmpVector3f.z * rayLength));
		end.scale(PHYSICS_SCALE);

		results.clear();
		// /rot.scale(PHYSICS_SCALE);
		dynamicsWorld.rayTest(start, end, new RayResultListener(ids.get(go)));
		float min = 1;
		for (LocalRayResult result : results) {
			if (result.hitFraction < min)
				min = result.hitFraction;
		}
		return min;
	}

	public static PhysicsTest getInstance() {
		return INSTANCE;
	}

	public class RayResultListener extends RayResultCallback {

		private CollisionObject rayTestCallerRigid;

		public RayResultListener(CollisionObject go) {
			this.rayTestCallerRigid = go;
		}

		@Override
		public float addSingleResult(LocalRayResult result, boolean arg1) {
			if (result.collisionObject != rayTestCallerRigid)
				results.add(result);
			return 0;
		}

	}

	public void dispose() {
		dynamicsWorld.destroy();
	}
}
