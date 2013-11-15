package physics;

import game.Game;
import game.GameLoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ode4j.math.DVector3;
import org.ode4j.ode.DBallJoint;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DHingeJoint;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSliderJoint;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.OdeMath;

import util.Log;
import world.GameObject;
import world.GameObjectType;
import world.Joint;
import world.LocomotionData;

public class OdePhysics implements AbstractPhysics {
	protected static final int MAX_CONTACTS = 100;
	public static final float ODE_SCALE = 1f;
	public static final float STEPSIZE = 0.02f / 60f;
	private DSpace space;
	private DWorld world;
	private DJointGroup contactJoints = OdeHelper.createJointGroup();
	private Map<GameObject, DGeom> bodyMap = new HashMap<GameObject, DGeom>();
	private Map<Joint, DJoint> jointMap = new HashMap<Joint, DJoint>();
	private List<LocomotionData> locomoted = new ArrayList<LocomotionData>();
	private Map<DGeom, Object[]> collisionMap = new HashMap<DGeom, Object[]>();
	private DNearCallback collideCallBack = new DNearCallback() {

		public void call(Object arg0, DGeom o1, DGeom o2) {
			Object[] collWatch = collisionMap.get(o1);
			DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
			for (int i = 0; i < MAX_CONTACTS; i++) {
				DContact contact = contacts.get(i);
				contact.surface.mode = OdeMath.dContactApprox1;
				contact.surface.mu = 400;
			}
			int contactNum = OdeHelper.collide(o1, o2, 10,
					contacts.getGeomBuffer());
			double maxDepth = 0;
			DVector3 maxPos = null;
			for (int i = 0; i < contactNum; i++) {
				if (Math.abs(contacts.get(i).geom.depth) > maxDepth) {
					maxDepth = Math.abs(contacts.get(i).geom.depth);
					maxPos = contacts.get(i).geom.pos.scale(1.0f / ODE_SCALE);
				}
				DContactJoint joint = OdeHelper.createContactJoint(world,
						contactJoints, contacts.get(i));
				joint.attach(o1.getBody(), o2.getBody());
			}
			if (collWatch != null && collWatch[0] == o2 && maxDepth > 0.001) {
				Game.INSTANCE.loop.renderer.addDebugLine(
						maxPos.toFloatArray(),
						new float[] { (float) maxPos.get(0),
								(float) (maxPos.get(1) + maxDepth * 1000),
								(float) maxPos.get(2) });
				((CollisionCallback) collWatch[1]).call(
						(GameObject) collWatch[2], (GameObject) collWatch[3]);
			}
		}
	};

	public OdePhysics() {
		OdeHelper.initODE2(0);
		world = OdeHelper.createWorld();
		world.setGravity(0, -0.5, 0);
		float kd = 1;
		float kp = 1000f;
		world.setCFM(1.0f / (STEPSIZE * kp + kd));
		world.setERP(STEPSIZE * kp / (STEPSIZE * kp + kd));
		// world.setAutoDisableFlag(true);
		// world.setContactMaxCorrectingVel(0.1);
		// world.setContactSurfaceLayer(0.001);
		space = OdeHelper.createSimpleSpace();
	}

	@Override
	public void dispose() {
		destroy();
		world.destroy();
		OdeHelper.closeODE();
	}

	@Override
	public void update(Map<String, List<GameObject>> objs) {
		addNewGeomJoints(objs);

		for (int i = 0; i < GameLoop.TICKS_PER_SECOND/2; i++) {
			updateForces();
			space.collide(null, collideCallBack);
			for (LocomotionData loc : locomoted) {
				OdeLocomotion.locomote(jointMap, loc, bodyMap.get(loc.body)
						.getBody());
			}
			world.step(STEPSIZE);
			contactJoints.empty();
			updateRotPos();
		}

	}

	private void updateRotPos() {
		for (GameObject go : bodyMap.keySet()) {
			DGeom geom = bodyMap.get(go);
			go.setPos(geom.getPosition().toFloatArray(), 1.0f / ODE_SCALE);
			go.rotationMatrix.m00 = (float) geom.getRotation().get00();
			go.rotationMatrix.m01 = (float) geom.getRotation().get01();
			go.rotationMatrix.m02 = (float) geom.getRotation().get02();
			go.rotationMatrix.m10 = (float) geom.getRotation().get10();
			go.rotationMatrix.m11 = (float) geom.getRotation().get11();
			go.rotationMatrix.m12 = (float) geom.getRotation().get12();
			go.rotationMatrix.m20 = (float) geom.getRotation().get20();
			go.rotationMatrix.m21 = (float) geom.getRotation().get21();
			go.rotationMatrix.m22 = (float) geom.getRotation().get22();
		}
		DVector3 vec = new DVector3();
		for (Joint joint : jointMap.keySet()) {
			DJoint j = jointMap.get(joint);
			switch (joint.jointType) {
			case HINGE:
				((DHingeJoint) j).getAnchor(vec);
				joint.angleRate = (float) ((DHingeJoint) j).getAngleRate();
				break;
			case BALL:
				((DBallJoint) j).getAnchor(vec);
				break;
			default:
				break;
			}
			joint.color[1] = joint.color[2] = 1 - Math.abs(joint.angleRate) / 5;
			joint.setPos(vec.toFloatArray(), 1.0f / ODE_SCALE);
		}
	}

	private void addNewGeomJoints(Map<String, List<GameObject>> objs) {
		List<LocomotionData> newLocomotions = new ArrayList<LocomotionData>();
		for (List<GameObject> gos : objs.values()) {
			GameObjectType type = GameObjectType.getType(gos.get(0).getType());
			if (type.shape != null) {
				for (GameObject go : gos) {
					if (bodyMap.get(go) == null) {
						DGeom geom = type.shape.getOdeShape(world, space, go);
						bodyMap.put(go, geom);
						if (go.physics.locomotion != null) {
							Log.log(this, "Added Locomotion");
							newLocomotions.add(go.physics.locomotion);
						}
					}
				}
			}
		}
		for (LocomotionData l : newLocomotions) {
			// bodyMap.get(l.body).getBody().addForce(0, 0, 100);
			collisionWatch(l.footA, l.floor, l.collisionCallback);
			collisionWatch(l.footB, l.floor, l.collisionCallback);
		}
		locomoted.addAll(newLocomotions);
		List<GameObject> gos = objs.get(Joint.JOINT_OBJECT_TYPE_NAME);
		if (gos != null)
			for (GameObject go : gos) {
				Joint joint = (Joint) go;
				if (jointMap.get(joint) == null) {
					jointMap.put(joint, createJoint(world, joint));
				}
			}
	}

	private void collisionWatch(GameObject go1, GameObject go2,
			CollisionCallback cc) {
		collisionMap.put(bodyMap.get(go1), new Object[] { bodyMap.get(go2), cc,
				go1, go2 });
		collisionMap.put(bodyMap.get(go2), new Object[] { bodyMap.get(go1), cc,
				go2, go1 });
	}

	private void updateForces() {
		for (Joint joint : jointMap.keySet()) {
			DJoint j = jointMap.get(joint);
			if (joint.physics.force[0] != 0 || joint.physics.force[1] != 0
					|| joint.physics.force[2] != 0) {
				switch (joint.jointType) {
				case HINGE:
					((DHingeJoint) j).addTorque(joint.physics.force[0]);
					break;
				case SLIDER:
					((DSliderJoint) j).addForce(joint.physics.force[0]);
					break;
				default:
					break;
				}
			}
		}
		for (GameObject go : bodyMap.keySet()) {
			if (go.physics.force[0] != 0 || go.physics.force[1] != 0
					|| go.physics.force[2] != 0) {
				DGeom geom = bodyMap.get(go);
				geom.setPosition(go.physics.force[0] * ODE_SCALE,
						go.physics.force[1] * ODE_SCALE, go.physics.force[2]
								* ODE_SCALE);
				geom.getBody().setForce(0, 0, 0);
				geom.getBody().setAngularVel(0, 0, 0);
				geom.getBody().setLinearVel(0, 0, 0);
				go.physics.resetForce();
			}
		}
	}

	private DJoint createJoint(DWorld world, Joint joint) {
		DJoint j;
		switch (joint.jointType) {
		default:
		case BALL:
			j = OdeHelper.createBallJoint(world);
			break;
		case HINGE:
			j = OdeHelper.createHingeJoint(world);
			break;
		case SLIDER:
			j = OdeHelper.createSliderJoint(world);
		}
		j.attach(bodyMap.get(joint.body[0]).getBody(),
				bodyMap.get(joint.body[1]).getBody());
		if (joint.hingeAxis != null)
			((DHingeJoint) j).setAxis(joint.hingeAxis[0], joint.hingeAxis[1],
					joint.hingeAxis[2]);
		if (joint.hingeAnchor != null)
			((DHingeJoint) j).setAnchor(joint.hingeAnchor[0] * ODE_SCALE,
					joint.hingeAnchor[1] * ODE_SCALE, joint.hingeAnchor[2]
							* ODE_SCALE);
		if (joint.ballAnchor != null)
			((DBallJoint) j).setAnchor(joint.ballAnchor[0] * ODE_SCALE,
					joint.ballAnchor[1] * ODE_SCALE, joint.ballAnchor[2]
							* ODE_SCALE);
		if (joint.sliderAxis != null) {
			((DSliderJoint) j).setAxis(joint.sliderAxis[0],
					joint.sliderAxis[1], joint.sliderAxis[2]);
		}
		if (joint.sliderLength != 0) {
			((DSliderJoint) j).setParamHiStop(joint.sliderLength * ODE_SCALE);
		}
		if (joint.minStop != null) {
			((DHingeJoint) j).setParamLoStop(joint.minStop);
		}
		if (joint.maxStop != null) {
			((DHingeJoint) j).setParamHiStop(joint.maxStop);
		}
		return j;
	}

	@Override
	public void restart() {
		destroy();
		space = OdeHelper.createSimpleSpace();
	}

	private void destroy() {
		for (DGeom body : bodyMap.values()) {
			if (body.getBody() != null)
				body.getBody().destroy();
			body.destroy();
		}
		for (DJoint joint : jointMap.values()) {
			joint.disable();
		}
		space.destroy();
		contactJoints.empty();
		bodyMap.clear();
		jointMap.clear();
		locomoted.clear();
		collisionMap.clear();
	}

	public static interface CollisionCallback {
		void call(GameObject collWatch, GameObject collWatch2);
	}
}
