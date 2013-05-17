package physics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.vecmath.Vector3f;

import util.VoxelWorld;

public class IntersectionTest {
	private static float[] c = new float[] { 10, 10 };
	private static float[] d = new float[] { 0, 0 };
	private static float min;
	private static float max;

	public static boolean isIntersecting(float[] corner1, float[] corner2,
			float[] c, float[] d) {
		float[] a = new float[] { corner1[0], corner1[1] };
		float[] b = new float[] { corner1[0], corner2[1] };
		if (doubleCheck(a, b, c, d))
			return true;
		/*
		 * a = new float[] { corner1[0], corner2[1] }; b = new float[] {
		 * corner2[0], corner2[1] }; if (doubleCheck(a, b, c, d)) return true; a
		 * = new float[] { corner2[0], corner2[1] }; b = new float[] {
		 * corner2[0], corner1[1] }; if (doubleCheck(a, b, c, d)) return true;
		 */
		a = new float[] { corner2[0], corner1[1] };
		b = new float[] { corner1[0], corner1[1] };
		if (doubleCheck(a, b, c, d))
			return true;
		return false;
	}

	private static boolean doubleCheck(float[] a, float[] b, float[] c,
			float[] d) {
		return check(a, b, c, d) && check(c, d, a, b);
	}

	private static boolean check(float[] a, float[] b, float[] c, float[] d) {
		float[] normal = normalize(new float[] { -b[1] + a[1], b[0] - a[0] });
		float[] vc = new float[] { a[0] - c[0], a[1] - c[1] };
		float[] vd = new float[] { a[0] - d[0], a[1] - d[1] };
		boolean check = (normal[0] * vc[0] + normal[1] * vc[1])
				* (normal[0] * vd[0] + normal[1] * vd[1]) <= 0;
		return check;
	}

	private static float[] normalize(float[] vec) {
		float length = length(vec);
		for (int i = 0; i < vec.length; i++)
			vec[i] /= length;
		return vec;
	}

	private static float length(float[] vec) {
		float sumSquared = 0;
		for (int i = 0; i < vec.length; i++)
			sumSquared += vec[i] * vec[i];
		return (float) Math.sqrt(sumSquared);
	}

	public static void main(String[] args) {
		for (int k = 0; k < 20; k++) {
			d[0] = k;
			for (int i = 0; i < 20; i++) {
				for (int j = 0; j < 20; j++) {
					if (isIntersecting(new float[] { i, j }, new float[] {
							i + 1, j + 1 }, c, d))
						System.out.print("  ");
					else
						System.out.print("x ");
				}
				System.out.print("\n");
			}
			System.out.println("+++++++++++++++++++++++++++");
		}
	}

	public static boolean isIntersecting(float[] fs, float[] fs2,
			FloatBuffer vertices, IntBuffer[] indices, int t, int indice) {
		if (IntersectionTest.isIntersecting(fs, fs2,
				new float[] { vertices.get(indices[indice].get(t) * 3 + 0),
						vertices.get(indices[indice].get(t) * 3 + 1) },
				new float[] { vertices.get(indices[indice].get(t + 1) * 3 + 0),
						vertices.get(indices[indice].get(t + 1) * 3 + 1) }))
			return true;
		if (IntersectionTest.isIntersecting(fs, fs2,
				new float[] { vertices.get(indices[indice].get(t + 1) * 3 + 0),
						vertices.get(indices[indice].get(t + 1) * 3 + 1) },
				new float[] { vertices.get(indices[indice].get(t + 2) * 3 + 0),
						vertices.get(indices[indice].get(t + 2) * 3 + 1) }))
			return true;
		if (IntersectionTest.isIntersecting(fs, fs2,
				new float[] { vertices.get(indices[indice].get(t + 2) * 3 + 0),
						vertices.get(indices[indice].get(t + 2) * 3 + 1) },
				new float[] { vertices.get(indices[indice].get(t + 0) * 3 + 0),
						vertices.get(indices[indice].get(t + 0) * 3 + 1) }))
			return true;
		return false;
	}

	public static void minmax(float x0, float x1, float x2) {
		min = max = x0;
		if (x1 < min)
			min = x1;
		if (x1 > max)
			max = x1;
		if (x2 < min)
			min = x2;
		if (x2 > max)
			max = x2;
	}

	public static boolean planeBoxOverlap(Vector3f normal, Vector3f vert,
			Vector3f maxbox) {
		Vector3f vmin, vmax;

		float signs[] = new float[] { 1, 1, 1 };
		if (normal.x <= 0.0f)
			signs[0] = -1;
		if (normal.y <= 0.0f)
			signs[1] = -1;
		if (normal.z <= 0.0f)
			signs[2] = -1;

		Vector3f sign = new Vector3f(signs[0], signs[1], signs[2]);
		vmin = scale(sign, maxbox);
		vmin.add(vert);
		vmin.negate();
		vmax = scale(sign, maxbox);
		vmax.sub(vert);

		if (dot(normal, vmin) > 0.0f)
			return false;
		if (dot(normal, vmax) >= 0.0f)
			return true;

		return false;
	}

	private static Vector3f scale(Vector3f sign, Vector3f maxbox) {
		Vector3f v = new Vector3f(sign.x * maxbox.x, sign.y * maxbox.y, sign.z
				* maxbox.z);
		return v;
	}

	private static float dot(Vector3f v1, Vector3f v2) {
		return v1.dot(v2);
	}

	public static boolean axisTestX01(float a, float b, float fa, float fb,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f boxhalfsize) {
		float p0 = a * v0.y - b * v0.z;
		float p2 = a * v2.y - b * v2.z;
		min = Math.min(p0, p2);
		max = Math.max(p0, p2);
		float rad = fa * boxhalfsize.y + fb * boxhalfsize.z;
		if (min > rad || max < -rad)
			return false;
		return true;
	}

	public static boolean axisTestX2(float a, float b, float fa, float fb,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f boxhalfsize) {
		float p0 = a * v0.y - b * v0.z;
		float p1 = a * v1.y - b * v1.z;
		min = Math.min(p0, p1);
		max = Math.max(p0, p1);
		float rad = fa * boxhalfsize.y + fb * boxhalfsize.z;
		if (min > rad || max < -rad)
			return false;
		return true;
	}

	public static boolean axisTestY02(float a, float b, float fa, float fb,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f boxhalfsize) {
		float p0 = -a * v0.x + b * v0.z;
		float p2 = -a * v2.x + b * v2.z;
		min = Math.min(p0, p2);
		max = Math.max(p0, p2);
		float rad = fa * boxhalfsize.x + fb * boxhalfsize.z;
		if (min > rad || max < -rad)
			return false;
		return true;
	}

	public static boolean axisTestY1(float a, float b, float fa, float fb,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f boxhalfsize) {
		float p0 = -a * v0.x + b * v0.z;
		float p1 = -a * v1.x + b * v1.z;
		min = Math.min(p0, p1);
		max = Math.max(p0, p1);
		float rad = fa * boxhalfsize.x + fb * boxhalfsize.z;
		if (min > rad || max < -rad)
			return false;
		return true;
	}

	public static boolean axisTestZ12(float a, float b, float fa, float fb,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f boxhalfsize) {
		float p1 = a * v1.x - b * v1.y;
		float p2 = a * v2.x - b * v2.y;
		min = Math.min(p1, p2);
		max = Math.max(p1, p2);
		float rad = fa * boxhalfsize.x + fb * boxhalfsize.y;
		if (min > rad || max < -rad)
			return false;
		return true;
	}

	public static boolean axisTestZ0(float a, float b, float fa, float fb,
			Vector3f v0, Vector3f v1, Vector3f v2, Vector3f boxhalfsize) {
		float p1 = a * v0.x - b * v0.y;
		float p2 = a * v1.x - b * v1.y;
		min = Math.min(p1, p2);
		max = Math.max(p1, p2);
		float rad = fa * boxhalfsize.x + fb * boxhalfsize.y;
		if (min > rad || max < -rad)
			return false;
		return true;
	}

	public static boolean triBoxOverlap(Vector3f boxcenter,
			Vector3f boxhalfsize, Vector3f[] tri) {
		// use separating axis theorem to test overlap between triangle and box
		// need to test for overlap in these directions:
		// 1) the {x,y,z}-directions (actually, since we use the AABB of the
		// triangle
		// we do not even need to test these)
		// 2) normal of the triangle
		// 3) crossproduct(edge from tri, {x,y,z}-directin)
		// this gives 3x3=9 more tests
		Vector3f v0 = new Vector3f(), v1 = new Vector3f(), v2 = new Vector3f();
		float fex, fey, fez;
		Vector3f normal = new Vector3f(), e0 = new Vector3f(), e1 = new Vector3f(), e2 = new Vector3f();

		// This is the fastest branch on Sun.
		// move everything so that the boxcenter is in (0,0,0)
		v0.sub(tri[0], boxcenter);
		v1.sub(tri[1], boxcenter);
		v2.sub(tri[2], boxcenter);

		// Compute triangle edges.
		e0.sub(v1, v0);
		e1.sub(v2, v1);
		e2.sub(v0, v2);

		// Bullet 3:
		// test the 9 tests first (this was faster)
		fex = Math.abs(e0.x);
		fey = Math.abs(e0.y);
		fez = Math.abs(e0.z);
		if (!axisTestX01(e0.z, e0.y, fez, fey, v0, v1, v2, boxhalfsize))
			return false;
		if (!axisTestY02(e0.z, e0.x, fez, fex, v0, v1, v2, boxhalfsize))
			return false;
		if (!axisTestZ12(e0.y, e0.x, fey, fex, v0, v1, v2, boxhalfsize))
			return false;

		fex = Math.abs(e1.x);
		fey = Math.abs(e1.y);
		fez = Math.abs(e1.z);
		if (!axisTestX01(e1.z, e1.y, fez, fey, v0, v1, v2, boxhalfsize))
			return false;
		if (!axisTestY02(e1.z, e1.x, fez, fex, v0, v1, v2, boxhalfsize))
			return false;
		if (!axisTestZ0(e1.y, e1.x, fey, fex, v0, v1, v2, boxhalfsize))
			return false;

		fex = Math.abs(e2.x);
		fey = Math.abs(e2.y);
		fez = Math.abs(e2.z);
		if (!axisTestX2(e2.z, e2.y, fez, fey, v0, v1, v2, boxhalfsize))
			return false;
		if (!axisTestY1(e2.z, e2.x, fez, fex, v0, v1, v2, boxhalfsize))
			return false;
		if (!axisTestZ12(e2.y, e2.x, fey, fex, v0, v1, v2, boxhalfsize))
			return false;

		// Bullet 1:
		// first test overlap in the {x,y,z}-directions
		// find min, max of the triangle each direction, and test for overlap in
		// that direction -- this is equivalent to testing a minimal AABB around
		// the triangle against the AABB

		// test in X-direction
		minmax(v0.x, v1.x, v2.x);
		if (min > boxhalfsize.x || max < -boxhalfsize.x)
			return false;
		// test in Y-direction
		minmax(v0.y, v1.y, v2.y);
		if (min > boxhalfsize.y || max < -boxhalfsize.y)
			return false;
		// test in Z-direction
		minmax(v0.z, v1.z, v2.z);
		if (min > boxhalfsize.z || max < -boxhalfsize.z)
			return false;

		// Bullet 2:
		// test if the box intersects the plane of the triangle
		// compute plane equation of triangle: normal*x+d=0
		normal.cross(e0, e1);
		return planeBoxOverlap(normal, v0, boxhalfsize);
	}

	public static int[][] getLastIntersectingVoxels(float[] start, float[] end,
			VoxelWorld voxelWorld) {
		int[] xyz = new int[] { voxelWorld.posToVoxel(start[0]),
				voxelWorld.posToVoxel(start[1]),
				voxelWorld.posToVoxel(start[2]) };
		int[] xyzFirst = new int[] { xyz[0], xyz[1], xyz[2] };
		Vector3f startV = new Vector3f(start);
		Vector3f endV = new Vector3f(end);
		Vector3f direction = new Vector3f();
		direction.sub(endV, startV);
		direction.normalize();
		Vector3f step = new Vector3f(Math.signum(direction.x),
				Math.signum(direction.y), Math.signum(direction.z));

		Vector3f cellBoundary = new Vector3f(xyz[0] + (step.x > 0 ? 1 : 0),
				xyz[1] + (step.y > 0 ? 1 : 0), xyz[2] + (step.z > 0 ? 1 : 0));

		Vector3f tMax = new Vector3f((direction.x == 0 ? Float.MAX_VALUE
				: ((cellBoundary.x - start[0] * 4) / direction.x)),
				(direction.y == 0 ? Float.MAX_VALUE
						: ((cellBoundary.y - start[1] * 4) / direction.y)),
				(direction.z == 0 ? Float.MAX_VALUE
						: ((cellBoundary.z - start[2] * 4) / direction.z)));
		Vector3f tDelta = new Vector3f((direction.x == 0 ? Float.MAX_VALUE
				: (step.x / direction.x)), (direction.y == 0 ? Float.MAX_VALUE
				: (step.y / direction.x)), (direction.z == 0 ? Float.MAX_VALUE
				: (step.z / direction.x)));
		/*
		 * Log.log(IntersectionTest.class, start);
		 * Log.log(IntersectionTest.class, end); Log.log(IntersectionTest.class,
		 * xyz); Log.log(IntersectionTest.class, step);
		 */
		for (int i = 0; i < 5; i++) {
			// Return it.
			if (voxelWorld.getVoxelValue(xyz[0], xyz[1], xyz[2]) > 0.5) {
				break;
			}
			xyzFirst[0] = xyz[0];
			xyzFirst[1] = xyz[1];
			xyzFirst[2] = xyz[2];
			// Do the next step.
			if (tMax.x < tMax.y && tMax.x < tMax.z) {
				// tMax.X is the lowest, an YZ cell boundary plane is nearest.
				xyz[0] += step.x;
				tMax.x += tDelta.x;
			} else if (tMax.y < tMax.z) {
				// tMax.Y is the lowest, an XZ cell boundary plane is nearest.
				xyz[1] += step.y;
				tMax.y += tDelta.y;
			} else {
				// tMax.Z is the lowest, an XY cell boundary plane is nearest.
				xyz[2] += step.z;
				tMax.z += tDelta.z;
			}
		}
		/*
		 * Log.log(IntersectionTest.class, "first");
		 * Log.log(IntersectionTest.class, xyzFirst);
		 * Log.log(IntersectionTest.class, "next");
		 * Log.log(IntersectionTest.class, xyz);
		 */
		return new int[][] { xyzFirst, xyz };
	}
}
