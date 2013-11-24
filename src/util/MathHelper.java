package util;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import world.GameObject;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class MathHelper {
	public static class Tansformation {
		public Quat4f rotation = new Quat4f();
		public Vector3f translation = new Vector3f();
	}

	private static Tansformation tempTransform = new Tansformation();
	private static Vector3f tmpVector = new Vector3f();
	private static Vector3f tmp2Vector = new Vector3f();
	private static Matrix3f tmpMatrix = new Matrix3f();

	public static Tansformation getTransformation(float realPoints[][],
			float currPoints[][]) {
		realPoints = clone(realPoints);
		currPoints = clone(currPoints);
		float[] pCentroid = mul(sum(realPoints), 1.0f / realPoints.length);
		float[] qCentroid = mul(sum(currPoints), 1.0f / currPoints.length);
		float pPrime[][] = addOld(realPoints, pCentroid, -1);
		float qPrime[][] = addOld(currPoints, qCentroid, -1);
		Matrix4f m = new Matrix4f();
		Matrix4f p = new Matrix4f();
		Matrix4f q = new Matrix4f();
		for (int i = 0; i < pPrime.length; i++) {
			setToP(pPrime[i], p);
			setToQ(qPrime[i], q);
			p.transpose();
			p.mul(q);
			m.add(p);
		}
		float quat[] = findLargestEigenVector(m);
		tempTransform.rotation.set(quat[1], quat[2], quat[3], quat[0]);
		tempTransform.rotation.normalize();
		tempTransform.translation.set(qCentroid);
		return tempTransform;
	}

	private static float[][] clone(float[][] currPoints) {
		float[][] clone = new float[currPoints.length][currPoints[0].length];
		for (int i = 0; i < clone.length; i++)
			for (int j = 0; j < clone[0].length; j++)
				clone[i][j] = currPoints[i][j];
		return clone;
	}

	private static float[] findLargestEigenVector(Matrix4f m) {
		Matrix eigenVal = new EigenvalueDecomposition(new Matrix(toArray(m)))
				.getV();
		return new float[] { (float) eigenVal.get(0, 0),
				(float) eigenVal.get(1, 0), (float) eigenVal.get(2, 0),
				(float) eigenVal.get(3, 0) };
	}

	private static double[][] toArray(Matrix4f m) {
		double[][] arr = new double[4][4];
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				arr[i][j] = m.getElement(i, j);
		return arr;
	}

	private static void setToP(float[] fs, Matrix4f p) {
		p.setZero();
		p.m01 = -fs[0];
		p.m02 = -fs[1];
		p.m03 = -fs[2];
		p.m10 = fs[0];
		p.m12 = fs[2];
		p.m13 = -fs[1];
		p.m20 = fs[1];
		p.m21 = -fs[2];
		p.m23 = fs[0];
		p.m30 = fs[2];
		p.m31 = fs[1];
		p.m32 = -fs[0];
	}

	private static void setToQ(float[] fs, Matrix4f q) {
		q.setZero();
		q.m01 = -fs[0];
		q.m02 = -fs[1];
		q.m03 = -fs[2];
		q.m10 = fs[0];
		q.m12 = -fs[2];
		q.m13 = fs[1];
		q.m20 = fs[1];
		q.m21 = fs[2];
		q.m23 = -fs[0];
		q.m30 = fs[2];
		q.m31 = -fs[1];
		q.m32 = fs[0];
	}

	private static float[][] addOld(float[][] realPoints, float[] pCentroid,
			float f) {
		for (int i = 0; i < realPoints.length; i++)
			for (int j = 0; j < pCentroid.length; j++) {
				realPoints[i][j] += pCentroid[j] * f;
			}
		return realPoints;
	}

	private static float[] mul(float[] fs, float f) {
		for (int i = 0; i < fs.length; i++)
			fs[i] *= f;
		return fs;
	}

	private static float[] sum(float[][] realPoints) {
		float sum[] = new float[realPoints[0].length];
		for (int i = 0; i < realPoints.length; i++)
			for (int j = 0; j < sum.length; j++)
				sum[j] += realPoints[i][j];
		return sum;
	}

	public static void computeRelativeTransform(GameObject gameObject,
			GameObject child) {
		float[] relPos = (float[]) child.get("relPos");
		if (relPos == null) {
			child.set("parent", gameObject);
			relPos = new float[] { child.pos[0], child.pos[1], child.pos[2] };
			child.set("relPos", relPos);
		}
		Matrix3f relRot = (Matrix3f) child.get("relRot");
		if (relRot == null) {
			child.updateRotation();
			relRot = (Matrix3f) child.rotationMatrix.clone();
			child.set("relRot", relRot);
		}
		tmp2Vector.set(relPos);
		gameObject.rotationMatrix.transform(tmp2Vector);
		tmpVector.set(gameObject.pos);
		tmpVector.add(tmp2Vector);
		child.pos[0] = tmpVector.x;
		child.pos[1] = tmpVector.y;
		child.pos[2] = tmpVector.z;
		child.rotationMatrix.mul(gameObject.rotationMatrix, relRot);
	}

	public static float[] getEulerFromVector(float vec[]) {
		tmpVector.set(vec);
		tmpVector.normalize();
		return new float[] {
				(float) (-Math.atan2(
						tmpVector.y,
						Math.sqrt(tmpVector.x * tmpVector.x + tmpVector.z
								* tmpVector.z))),
				(float) (Math.atan2(tmpVector.x, tmpVector.z)), 0 };
	}

	public static void setRotationMatrix(Matrix3f rotationMatrix,
			float[] rotation) {
		rotationMatrix.setIdentity();
		tmpMatrix.rotY(rotation[1]);
		rotationMatrix.mul(tmpMatrix);
		tmpMatrix.rotX(rotation[0]);
		rotationMatrix.mul(tmpMatrix);
		tmpMatrix.rotZ(rotation[2]);
		rotationMatrix.mul(tmpMatrix);
	}

	public static void clamp(float[] vel, float min, float max) {
		for (int i = 0; i < vel.length; i++)
			vel[i] = Math.min(Math.max(vel[i], min), max);
	}

	public static float clamp(double d, double min, double max) {
		return (float) Math.min(Math.max(d, min), max);
	}

	public static void set(double[] tmp2, double... set) {
		for (int i = 0; i < tmp2.length; i++)
			tmp2[i] = set[i];
	}

	public static void set(float[] tmp2, float... set) {
		for (int i = 0; i < tmp2.length; i++)
			tmp2[i] = set[i];
	}

	public static void add(float[] acc, double[] seperate, double f) {
		for (int i = 0; i < acc.length; i++)
			acc[i] += seperate[i] * f;
	}

	public static void add(float[] acc, float[] seperate, float f) {
		for (int i = 0; i < acc.length; i++)
			acc[i] += seperate[i] * f;
	}

}
