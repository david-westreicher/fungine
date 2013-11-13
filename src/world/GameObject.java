package world;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import physics.BulletPhysics;
import util.Log;
import util.MathHelper;
import util.MathHelper.Tansformation;

import com.bulletphysics.dynamics.RigidBody;

public class GameObject extends VariableHolder {


	public float[] bbox = new float[6];
	public float[] pos = new float[3];
	public float[] oldPos = new float[3];
	public float[] size = new float[] { 10, 10, 10 };
	public float[] rotation = new float[3];
	public float[] color = new float[] { (float) Math.random(),
			(float) Math.random(), (float) Math.random() };
	public transient PhysicsData physics = null;
	private String type;
	public float[] rotationMatrixArray = new float[9];
	public Matrix3f rotationMatrix = new Matrix3f(1, 0, 0, 0, 1, 0, 0, 0, 1);
	public float alpha = 1;
	public boolean marked;
	public boolean render = true;
	public float angle;
	private Quat4f quaternion;


	public GameObject(String name) {
		setType(name);
	}

	public String getType() {
		return type;
	}

	public void beforeUpdate() {
		updateRotation();
		setTo(oldPos, pos);
	}

	public void updateRotation() {
		GameObjectType type = GameObjectType.getType(this.type);
		if (quaternion != null) {
			rotationMatrix.set(quaternion);
		} else if (type != null && type.shape == null && get("parent") == null) {
			MathHelper.setRotationMatrix(rotationMatrix, rotation);
		}
		updateRotationMatrixArray();
	}

	private void updateRotationMatrixArray() {
		rotationMatrixArray[0] = rotationMatrix.m00;
		rotationMatrixArray[1] = rotationMatrix.m01;
		rotationMatrixArray[2] = rotationMatrix.m02;
		rotationMatrixArray[3] = rotationMatrix.m10;
		rotationMatrixArray[4] = rotationMatrix.m11;
		rotationMatrixArray[5] = rotationMatrix.m12;
		rotationMatrixArray[6] = rotationMatrix.m20;
		rotationMatrixArray[7] = rotationMatrix.m21;
		rotationMatrixArray[8] = rotationMatrix.m22;
	}

	protected void setTo(float[] oldPos2, float[] pos2) {
		for (int i = 0; i < oldPos2.length; i++)
			oldPos2[i] = pos2[i];
	}

	public void updateBbox() {
		bbox[0] = pos[0] - size[0] / 2;
		bbox[1] = pos[0] + size[0] / 2;
		bbox[2] = pos[1] - size[1] / 2;
		bbox[3] = pos[1] + size[1] / 2;
		bbox[4] = pos[2] - size[2] / 2;
		bbox[5] = pos[2] + size[2] / 2;
	}

	public void setQuaternion(float[] v) {
		if (quaternion == null)
			quaternion = new Quat4f();
		quaternion.set(v);
	}

	public void setTransformation(float realPoints[][], float currPoints[][]) {
		Tansformation transform = MathHelper.getTransformation(realPoints,
				currPoints);
		if (quaternion == null)
			quaternion = new Quat4f();
		quaternion.set(transform.rotation);
		pos[0] = transform.translation.x;
		pos[1] = transform.translation.y;
		pos[2] = transform.translation.z;
		updateRotation();
	}

	public void setPos(float x, float y) {
		pos[0] = x;
		pos[1] = y;
		oldPos[0] = pos[0];
		oldPos[1] = pos[1];
	}

	public void setPos(float x, float y, float z) {
		setPos(x, y);
		pos[2] = z;
		oldPos[2] = pos[2];
	}

	public void setPos(float[] otherPos) {
		setTo(pos, otherPos);
		setTo(oldPos, pos);
	}

	public void setColor(float r, float g, float b) {
		color[0] = r;
		color[1] = g;
		color[2] = b;
	}

	public void setColor(int rgb) {
		Color rgbC = new Color(rgb);
		color[0] = (float) rgbC.getRed() / 255f;
		color[1] = (float) rgbC.getGreen() / 255f;
		color[2] = (float) rgbC.getBlue() / 255f;
	}

	public void setRotation(float x, float y, float z) {
		rotation[0] = x;
		rotation[1] = y;
		rotation[2] = z;
	}

	public void setRotation(float rot[]) {
		rotation[0] = rot[0];
		rotation[1] = rot[1];
		rotation[2] = rot[2];
	}

	public void setSize(float x, float y, float z) {
		size[0] = x;
		size[1] = y;
		size[2] = z;
	}

	public void setType(String name) {
		GameObjectType goType = GameObjectType.getType(name);
		if (goType == null) {
			Log.err(this, "GameObjectType " + name + " doesn't exist!");
			return;
		}
		if (goType.shape != null || name.equals(Joint.JOINT_OBJECT_TYPE_NAME))
			physics = new PhysicsData();
		vals.putAll(goType.getVars());
		this.type = name;
	}

	public void computeRelativeTransform(GameObject child) {
		MathHelper.computeRelativeTransform(this, child);
	}

	public void setLinearVelocity(float x, float y) {
		RigidBody rigidBody = getRigidBody();
		if (rigidBody != null) {
			Vector3f vel = rigidBody.getLinearVelocity(new Vector3f());
			rigidBody.setLinearVelocity(new Vector3f(x, vel.y, y));
		}
	}

	public void setLinearVelocity(float x, float y, float z) {
		RigidBody rigidBody = getRigidBody();
		Vector3f vel = rigidBody.getLinearVelocity(new Vector3f());
		rigidBody.setLinearVelocity(new Vector3f((x == 0) ? vel.x : x,
				(y == 0) ? vel.y : y, (z == 0) ? vel.z : z));
	}

	public RigidBody getRigidBody() {
		return BulletPhysics.ids.get(this);
	}

	public void setRotation(Quat4f o) {
		rotationMatrix.set(o);
		updateRotationMatrixArray();
	}

	public GameObjectType getGameObjectType() {
		return GameObjectType.getType(type);
	}

	@Override
	public String toString() {
		return "GameObject [pos=" + Arrays.toString(pos) + ", color="
				+ Arrays.toString(color) + "]";
	}

	public void setQuaternion(double x, double y, double z, double w) {
		if (quaternion == null)
			quaternion = new Quat4f();
		quaternion.set((float) x, (float) y, (float) z, (float) w);
	}

	public void setPos(float[] p, float s) {
		setPos(p[0] * s, p[1] * s, p[2] * s);
	}

}
