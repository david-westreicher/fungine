package util;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4d;

import world.GameObject;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class MathHelper {
	public static class Tansformation {
		public Quat4f rotation = new Quat4f();
		public Vector3f translation = new Vector3f();

		public void transform(Vector3f pos) {
			// pos.negate();
			Matrix3f m = new Matrix3f();
			m.set(rotation);
			// Log.log(this, m);
			// m.transpose();
			m.transform(pos);
			pos.add(translation);
		}

		@Override
		public String toString() {
			return "Tansformation [rotation=" + rotation + ", translation="
					+ translation + "]";
		}
	}

	private static Tansformation tempTransform = new Tansformation();
	private static Vector3f tmpVector = new Vector3f();
	private static Vector3f tmp2Vector = new Vector3f();
	private static Matrix3f tmpMatrix = new Matrix3f();
	private static final float[] tmpDist = new float[] { 0, 0, 0 };
	private static double[] tmpDist2 = new double[] { 0, 0, 0 };

	public static Tansformation getTransformation(float realPoints[][],
			float currPoints[][]) {
		realPoints = clone(realPoints);
		currPoints = clone(currPoints);
		float[] pCentroid = mulOld(sum(realPoints), 1.0f / realPoints.length);
		float[] qCentroid = mulOld(sum(currPoints), 1.0f / currPoints.length);
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
		Matrix3f tmpMatrix = new Matrix3f();
		tmpMatrix.set(tempTransform.rotation);
		tempTransform.translation.set(pCentroid);
		tmpMatrix.transform(tempTransform.translation);
		tempTransform.translation.negate();
		tempTransform.translation.add(new Vector3f(qCentroid));
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
		EigenvalueDecomposition eigen = new EigenvalueDecomposition(new Matrix(
				toArray(m)));
		double[] eigenVals = eigen.getRealEigenvalues();
		Matrix eigenVal = eigen.getV();
		double largestEigenValue = Double.MIN_VALUE;
		int largestEigenValueIndex = 0;
		for (int i = 0; i < 4; i++) {
			if (eigenVals[i] > largestEigenValue) {
				largestEigenValue = eigenVals[i];
				largestEigenValueIndex = i;
			}
		}
		// Log.log(MathHelper.class, eigenVals);
		// Log.log(MathHelper.class, largestEigenValueIndex);
		return new float[] { (float) eigenVal.get(0, largestEigenValueIndex),
				(float) eigenVal.get(1, largestEigenValueIndex),
				(float) eigenVal.get(2, largestEigenValueIndex),
				(float) eigenVal.get(3, largestEigenValueIndex) };
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

	private static float[] mulOld(float[] fs, float f) {
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

	public static void set(float[] tmp2, double... set) {
		for (int i = 0; i < tmp2.length; i++)
			tmp2[i] = (float) set[i];
	}

	public static void set(float[] tmp2, float... set) {
		for (int i = 0; i < tmp2.length; i++)
			tmp2[i] = set[i];
	}

	public static void add(float[] acc, double[] seperate, double f) {
		int max = Math.min(acc.length, seperate.length);
		for (int i = 0; i < max; i++)
			acc[i] += seperate[i] * f;
	}

	public static void add(double[] acc, double[] seperate, double f) {
		int max = Math.min(acc.length, seperate.length);
		for (int i = 0; i < max; i++)
			acc[i] += seperate[i] * f;
	}

	public static void add(float[] acc, float[] seperate, float f) {
		int max = Math.min(acc.length, seperate.length);
		for (int i = 0; i < max; i++)
			acc[i] += seperate[i] * f;
	}

	public static void normalize(float[] diff) {
		mul(diff, 1.0f / length(diff));
	}

	public static float length(float[] diff) {
		float length = 0;
		for (int i = 0; i < diff.length; i++) {
			length += diff[i] * diff[i];
		}
		return (float) Math.sqrt(length);
	}

	public static double length(double[] diff) {
		double length = 0;
		for (int i = 0; i < diff.length; i++) {
			length += diff[i] * diff[i];
		}
		return Math.sqrt(length);
	}

	public static float lengthSquared(float[] diff) {
		float length = 0;
		for (int i = 0; i < diff.length; i++) {
			length += diff[i] * diff[i];
		}
		return length;
	}

	public static double lengthSquared(double[] diff) {
		float length = 0;
		for (int i = 0; i < diff.length; i++) {
			length += diff[i] * diff[i];
		}
		return length;
	}

	public static float[] distance(float[] pos, float[] pos2) {
		for (int i = 0; i < tmpDist.length; i++) {
			tmpDist[i] = pos[i] - pos2[i];
		}
		return tmpDist;
	}

	public static double[] distance(double[] pos, double[] pos2) {
		tmpDist2 = tmpDist2.clone();
		for (int i = 0; i < tmpDist2.length; i++) {
			tmpDist2[i] = pos[i] - pos2[i];
		}
		return tmpDist2;
	}

	public static void mul(float[] vel, float f) {
		for (int i = 0; i < vel.length; i++)
			vel[i] *= f;
	}

	public static void mul(double[] vel, double f) {
		for (int i = 0; i < vel.length; i++)
			vel[i] *= f;
	}

	public static float[] interp(float[] pos, float[] oldPos, float interp,
			boolean smoothstep) {
		float res[] = new float[3];
		if (smoothstep)
			interp = ((interp) * (interp) * (3 - 2 * (interp)));
		for (int i = 0; i < 3; i++)
			res[i] = pos[i] * interp + (oldPos[i] * (1 - interp));
		return res;
	}

	public static float toDegree(float f) {
		return (float) (f * 180 / Math.PI);
	}

	public static float[] to4x4Matrix(Matrix3f m) {
		float matrix[] = new float[16];
		matrix[0] = m.m00;
		matrix[4] = m.m01;
		matrix[8] = m.m02;
		matrix[1] = m.m10;
		matrix[5] = m.m11;
		matrix[9] = m.m12;
		matrix[2] = m.m20;
		matrix[6] = m.m21;
		matrix[10] = m.m22;
		matrix[15] = 1;
		return matrix;
	}

	public static void HSLtoRGB(float h, float s, float l, float[] color) {
		if (s == 0) {
			color[0] = l;
			color[1] = l;
			color[2] = l;
		} else {
			float q = l < 0.5 ? (l * (1 + s)) : (l + s - l * s);
			float p = 2 * l - q;
			color[0] = hue2rgb(p, q, h + 1f / 3f);
			color[1] = hue2rgb(p, q, h);
			color[2] = hue2rgb(p, q, h - 1f / 3f);
		}
	}

	private static float hue2rgb(float p, float q, float t) {
		if (t < 0)
			t += 1;
		if (t > 1)
			t -= 1;
		if (t < 1f / 6f)
			return p + (q - p) * 6 * t;
		if (t < 1f / 2f)
			return q;
		if (t < 2f / 3f)
			return p + (q - p) * (2f / 3f - t) * 6;
		return p;
	}

	public static Vector2f[] getProjectedTriangle(Vector3f triangle[]) {
		Vector3f v1 = triangle[0];
		Vector3f v2 = triangle[1];
		Vector3f v3 = triangle[2];
		Vector2f[] uvs = new Vector2f[] { new Vector2f(), new Vector2f(),
				new Vector2f() };
		Vector3f tmp = new Vector3f();
		tmp.sub(v1, v2);
		float d1 = tmp.length();
		tmp.sub(v2, v3);
		float d2 = tmp.length();
		tmp.sub(v3, v1);
		float d3 = tmp.length();
		uvs[0].set(0, 0);
		uvs[1].set(d1, 0);
		float x = (d2 * d2 - d3 * d3 - d1 * d1) / (-2 * d1);
		float y = (float) Math.sqrt(d3 * d3 - x * x);
		uvs[2].set(x, y);
		return uvs;
	}

	public static Vector3f findNormal(Vector3f[] triangle) {
		Vector3f v0 = triangle[0];
		Vector3f v1 = triangle[1];
		Vector3f v2 = triangle[2];
		Vector3f normal = new Vector3f();
		Vector3f tmp1 = new Vector3f();
		Vector3f tmp2 = new Vector3f();
		tmp1.sub(v0, v2);
		tmp2.sub(v1, v2);
		normal.cross(tmp1, tmp2);
		normal.normalize();
		normal.y *= -1;
		return normal;
	}

	public static Vector3f findMid(Vector3f[] triangle) {
		Vector3f tmp = new Vector3f();
		for (Vector3f vec : triangle)
			tmp.add(vec);
		tmp.x /= triangle.length;
		tmp.y /= triangle.length;
		tmp.z /= triangle.length;
		return tmp;
	}

	public static Vector3f findMid(Vector2f[] triangle) {
		Vector3f tmp = new Vector3f();
		for (Vector2f vec : triangle) {
			tmp.x += vec.x;
			tmp.y += vec.y;
		}
		tmp.x /= triangle.length;
		tmp.y /= triangle.length;
		tmp.z = 0;
		return tmp;
	}

	public static void translate(float[] fish, int x, int y, int z) {
		for (int i = 0; i < fish.length; i += 3) {
			fish[i + 0] += x;
			fish[i + 1] += y;
			fish[i + 2] += z;
		}
	}

	public static float[] toFloat(double[] color) {
		float ret[] = new float[color.length];
		for (int i = 0; i < color.length; i++)
			ret[i] = (float) color[i];
		return ret;
	}
}
