package physics;

import org.ode4j.math.DMatrix3;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DMassC;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import util.MathHelper;
import world.GameObject;

public abstract class AbstractCollisionShape {

	public abstract DGeom getOdeShape(DWorld world, DSpace space, GameObject go);

	public static abstract class OdeShape extends AbstractCollisionShape {
		protected float ODE_SCALE = OdePhysics.ODE_SCALE;

		@Override
		public DGeom getOdeShape(DWorld world, DSpace space, GameObject go) {
			DGeom geom = getGeom(space, go);
			MathHelper.setRotationMatrix(go.rotationMatrix, go.rotation);
			go.updateRotation();
			if (go.physics.mass != 0) {
				DBody body = OdeHelper.createBody(world);
				body.setMass(getMass(go));
				body.setPosition(go.pos[0] * ODE_SCALE, go.pos[1] * ODE_SCALE,
						go.pos[2] * ODE_SCALE);
				body.setRotation(new DMatrix3(go.rotationMatrixArray[0],
						go.rotationMatrixArray[1], go.rotationMatrixArray[2],
						go.rotationMatrixArray[3], go.rotationMatrixArray[4],
						go.rotationMatrixArray[5], go.rotationMatrixArray[6],
						go.rotationMatrixArray[7], go.rotationMatrixArray[8]));
				body.setLinearVel(go.physics.force[0], go.physics.force[1],
						go.physics.force[2]);
				go.physics.resetForce();
				geom.setBody(body);
			} else {
				geom.setPosition(go.pos[0] * ODE_SCALE, go.pos[1] * ODE_SCALE,
						go.pos[2] * ODE_SCALE);
				geom.setRotation(new DMatrix3(go.rotationMatrixArray[0],
						go.rotationMatrixArray[1], go.rotationMatrixArray[2],
						go.rotationMatrixArray[3], go.rotationMatrixArray[4],
						go.rotationMatrixArray[5], go.rotationMatrixArray[6],
						go.rotationMatrixArray[7], go.rotationMatrixArray[8]));
			}
			return geom;
		}

		protected abstract DMassC getMass(GameObject go);

		protected abstract DGeom getGeom(DSpace space, GameObject go);
	}

	public static class SphereShape extends OdeShape {

		@Override
		protected DMassC getMass(GameObject go) {
			DMass mass = OdeHelper.createMass();
			mass.setSphereTotal(go.physics.mass, ODE_SCALE * go.size[0] / 2);
			return mass;
		}

		@Override
		protected DGeom getGeom(DSpace space, GameObject go) {
			return OdeHelper.createSphere(space, ODE_SCALE * go.size[0] / 2);
		}

	}

	public static class BoxShape extends OdeShape {

		@Override
		protected DMassC getMass(GameObject go) {
			DMass mass = OdeHelper.createMass();
			mass.setBoxTotal(go.physics.mass, go.size[0] * ODE_SCALE,
					go.size[1] * ODE_SCALE, go.size[2] * ODE_SCALE);
			return mass;
		}

		@Override
		protected DGeom getGeom(DSpace space, GameObject go) {
			return OdeHelper.createBox(space, go.size[0] * ODE_SCALE,
					go.size[1] * ODE_SCALE, go.size[2] * ODE_SCALE);
		}
	}

	public static class CapsuleShape extends OdeShape {

		@Override
		protected DMassC getMass(GameObject go) {
			DMass mass = OdeHelper.createMass();
			mass.setCapsuleTotal(go.physics.mass, 2,
					ODE_SCALE * go.size[1] / 4, ODE_SCALE * go.size[2]
							- ODE_SCALE * go.size[1] / 2);
			return mass;
		}

		@Override
		protected DGeom getGeom(DSpace space, GameObject go) {
			go.size[0] = go.size[1];
			return OdeHelper.createCapsule(space, ODE_SCALE * go.size[1] / 4,
					ODE_SCALE * go.size[2] - ODE_SCALE * go.size[1] / 2);
		}
	}
}
