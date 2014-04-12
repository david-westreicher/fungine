package util;

import java.nio.FloatBuffer;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import util.MathHelper.Tansformation;

public class GIUtil {

	private float[] vertices;
	private float[] uvs;
	private float[] normals;
	private float[][][] lookup;
	private boolean[][] bitmap;
	private int TEXTURE_SIZE;
	private int currentLookupIndex;

	public GIUtil(int texturesize, float[] vertices) {
		this.TEXTURE_SIZE = texturesize;
		this.vertices = vertices;
		uvs = new float[(vertices.length / 3) * 2];
		normals = new float[vertices.length];
		lookup = new float[TEXTURE_SIZE][TEXTURE_SIZE][6];
		bitmap = new boolean[TEXTURE_SIZE][TEXTURE_SIZE];

		for (int i = 0; i < vertices.length; i += 9) {
			Log.log(this, (float) 100 * i / vertices.length + "%");
			float x0 = vertices[i + 0];
			float y0 = vertices[i + 1];
			float z0 = vertices[i + 2];
			float x1 = vertices[i + 3];
			float y1 = vertices[i + 4];
			float z1 = vertices[i + 5];
			float x2 = vertices[i + 6];
			float y2 = vertices[i + 7];
			float z2 = vertices[i + 8];
			Vector3f v0 = new Vector3f(x0, y0, z0);
			Vector3f v1 = new Vector3f(x1, y1, z1);
			Vector3f v2 = new Vector3f(x2, y2, z2);
			Vector3f[] triangle = new Vector3f[] { v0, v1, v2 };
			Vector3f normal = MathHelper.findNormal(triangle);
			normals[i + 0] = normal.x;
			normals[i + 1] = normal.y;
			normals[i + 2] = normal.z;
			normals[i + 3] = normal.x;
			normals[i + 4] = normal.y;
			normals[i + 5] = normal.z;
			normals[i + 6] = normal.x;
			normals[i + 7] = normal.y;
			normals[i + 8] = normal.z;
			Vector2f[] newuvs = MathHelper.getProjectedTriangle(triangle);
			// Log.log(this, newuvs);
			fit(newuvs, triangle, normal);
			// Log.log(this, newuvs);
			int count = (i / 3) * 2;
			for (int j = 0; j < 3; j++) {
				uvs[count++] = newuvs[j].x;
				uvs[count++] = newuvs[j].y;
			}
		}
	}

	private void fit(Vector2f[] newuvs, Vector3f[] triangle, Vector3f normal) {
		Vector2f translation = new Vector2f();
		Vector2f size = new Vector2f();
		translation.x = -Math.min(0, newuvs[2].x);
		size.x = (int) (Math.max(newuvs[1].x, newuvs[2].x) + translation.x) + 1;
		size.y = (int) (Math.max(0, newuvs[2].y)) + 1;
		for (int x = 0; x < TEXTURE_SIZE - size.x; x++)
			for (int y = 0; y < TEXTURE_SIZE - size.y; y++) {
				boolean found = true;
				float maxx = Math.min(x + size.x + 2, TEXTURE_SIZE - 1);
				float maxy = Math.min(y + size.y + 2, TEXTURE_SIZE - 1);
				loop: for (int x2 = x; x2 <= maxx; x2++)
					for (int y2 = y; y2 <= maxy; y2++) {
						if (bitmap[x2][y2]) {
							found = false;
							break loop;
						}
					}
				if (found) {
					translation.x += x + 1;
					translation.y += y + 1;
					for (Vector2f uv : newuvs) {
						uv.add(translation);
					}
					rasterize(newuvs, x, y, maxx, maxy, triangle, normal);
					return;
				}
			}
		Log.err(this, "no free space found for triangle");
	}

	private void rasterize(Vector2f[] newuvs, int x, int y, float maxx,
			float maxy, Vector3f[] position, Vector3f normal) {
		float[][] currPoints = new float[3][3];
		float[][] realPoints = new float[3][3];
		for (int i = 0; i < 3; i++) {
			realPoints[i][0] = newuvs[i].x;
			realPoints[i][1] = newuvs[i].y;
			realPoints[i][2] = 0;
			currPoints[i][0] = position[i].x;
			currPoints[i][1] = position[i].y;
			currPoints[i][2] = position[i].z;
		}
		Tansformation t = MathHelper.getTransformation(realPoints, currPoints);
		Vector3f test = new Vector3f(newuvs[0].x, newuvs[0].y, 0);
		t.transform(test);
		// Log.log(this, newuvs);
		// Log.log(this, position);
		// Log.log(this, position[0], test, t);
		Vector3f pos = new Vector3f();
		for (int x2 = x; x2 <= maxx; x2++) {
			for (int y2 = y; y2 <= maxy; y2++) {
				bitmap[x2][y2] = true;
				pos.x = x2 + 0.5f;
				pos.y = y2 + 0.5f;
				pos.z = 0;
				t.transform(pos);
				lookup[x2][y2][0] = pos.x;
				lookup[x2][y2][1] = pos.y;
				lookup[x2][y2][2] = pos.z;
				lookup[x2][y2][3] = normal.x;
				lookup[x2][y2][4] = normal.y;
				lookup[x2][y2][5] = normal.z;
			}
		}
	}

	public FloatBuffer getGPUData() {
		FloatBuffer verticeUVs = FloatBuffer.allocate(vertices.length
				+ uvs.length + normals.length);
		for (int i = 0; i < vertices.length / 3; i++) {
			verticeUVs.put(uvs[i * 2 + 0]);
			verticeUVs.put(uvs[i * 2 + 1]);
			verticeUVs.put(vertices[i * 3 + 0]);
			verticeUVs.put(vertices[i * 3 + 1]);
			verticeUVs.put(vertices[i * 3 + 2]);
			verticeUVs.put(normals[i * 3 + 0]);
			verticeUVs.put(normals[i * 3 + 1]);
			verticeUVs.put(normals[i * 3 + 2]);
		}
		verticeUVs.rewind();
		return verticeUVs;
	}

	public int triangleNum() {
		return vertices.length / 3;
	}

	public void getPosNormal(Vector3f pos, Vector3f normal, Vector2f uvpos) {
		int x = currentLookupIndex / TEXTURE_SIZE;
		int y = currentLookupIndex % TEXTURE_SIZE;
		uvpos.set(x, y);
		float[] posNormals = lookup[x][y];
		pos.set(posNormals[0], posNormals[1], posNormals[2]);
		normal.set(posNormals[3], posNormals[4], posNormals[5]);
	}

	public void getNext() {
		int x;
		int y;
		do {
			// currentLookupIndex = ((int) (Math.random() * TEXTURE_SIZE *
			// TEXTURE_SIZE))
			// % (TEXTURE_SIZE * TEXTURE_SIZE);
			currentLookupIndex = (currentLookupIndex + 1)
					% (TEXTURE_SIZE * TEXTURE_SIZE);
			x = currentLookupIndex / TEXTURE_SIZE;
			y = currentLookupIndex % TEXTURE_SIZE;
		} while (!bitmap[x][y]);
	}
}
