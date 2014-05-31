package util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import rendering.RenderUtil;

public class GIUtil {

	private static final double REFLECTIVITY = 0.5;
	private float[] vertices = new float[0];
	private float[] uvs = new float[0];
	private float[] normals = new float[0];
	private float[][][] lookup;
	private boolean[][] bitmap;
	private int TEXTURE_SIZE;
	private int currentLookupIndex;
	private boolean[][] emmisive;

	public GIUtil(int texturesize, float[] vertices) {
		this(texturesize, new ProxyBakeable(vertices, new float[] { 1, 0, 0 },
				false));
	}

	public GIUtil(int texturesize, Bakeable... bs) {
		this.TEXTURE_SIZE = texturesize;
		lookup = new float[TEXTURE_SIZE][TEXTURE_SIZE][9];
		bitmap = new boolean[TEXTURE_SIZE][TEXTURE_SIZE];
		emmisive = new boolean[TEXTURE_SIZE][TEXTURE_SIZE];
		for (Bakeable b : bs) {
			add(b);
		}
	}

	public void saveTextureAtlasToFile() {
		BufferedImage bi = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE,
				BufferedImage.TYPE_INT_RGB);
		int[] colorData = new int[TEXTURE_SIZE * TEXTURE_SIZE];
		int[] normalData = new int[TEXTURE_SIZE * TEXTURE_SIZE];
		int[] positionData = new int[TEXTURE_SIZE * TEXTURE_SIZE];
		for (int x = 0; x < TEXTURE_SIZE; x++)
			for (int y = 0; y < TEXTURE_SIZE; y++) {
				int col = ((int) (lookup[x][y][6] * 255)) << 16;
				col |= ((int) (lookup[x][y][7] * 255)) << 8;
				col |= ((int) (lookup[x][y][8] * 255));
				colorData[x + (TEXTURE_SIZE - y - 1) * TEXTURE_SIZE] = col;
				int norm = ((int) ((lookup[x][y][3] + 1.0f) / 2.0f * 255)) << 16;
				norm |= ((int) ((lookup[x][y][4] + 1.0f) / 2.0f * 255)) << 8;
				norm |= ((int) ((lookup[x][y][5] + 1.0f) / 2.0f * 255));
				normalData[x + (TEXTURE_SIZE - y - 1) * TEXTURE_SIZE] = norm;
				int pos = ((int) ((lookup[x][y][0]) / 40.0f * 255)) << 16;
				pos |= ((int) ((lookup[x][y][1]) / 40.0f * 255)) << 8;
				pos |= ((int) ((lookup[x][y][2]) / 40.0f * 255));
				positionData[x + (TEXTURE_SIZE - y - 1) * TEXTURE_SIZE] = pos;
			}
		try {
			bi.setRGB(0, 0, TEXTURE_SIZE, TEXTURE_SIZE, colorData, 0,
					TEXTURE_SIZE);
			ImageIO.write(bi, "png", Util.generateScreenshotFile());
			bi.setRGB(0, 0, TEXTURE_SIZE, TEXTURE_SIZE, normalData, 0,
					TEXTURE_SIZE);
			ImageIO.write(bi, "png", Util.generateScreenshotFile());
			bi.setRGB(0, 0, TEXTURE_SIZE, TEXTURE_SIZE, positionData, 0,
					TEXTURE_SIZE);
			ImageIO.write(bi, "png", Util.generateScreenshotFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void add(Bakeable b) {
		float[] vertices = b.getVertices();
		float[] uvs = new float[(vertices.length / 3) * 2];
		float[] normals = new float[vertices.length];

		for (int i = 0; i < vertices.length; i += 9) {
			// Log.log(this, (float) 100 * i / vertices.length + "%");
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
			fit(newuvs, triangle, normal, b);
			// Log.log(this, newuvs);
			int count = (i / 3) * 2;
			for (int j = 0; j < 3; j++) {
				uvs[count++] = newuvs[j].x;
				uvs[count++] = newuvs[j].y;
			}
		}
		this.vertices = RenderUtil.merge(this.vertices, vertices);
		this.normals = RenderUtil.merge(this.normals, normals);
		this.uvs = RenderUtil.merge(this.uvs, uvs);
	}

	private void fit(Vector2f[] newuvs, Vector3f[] triangle, Vector3f normal,
			Bakeable b) {
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
					rasterize(newuvs, x, y, maxx, maxy, triangle, normal, b);
					return;
				}
			}
		Log.err(this, "no free space found for triangle");
	}

	private void rasterize(Vector2f[] newuvs, int x, int y, float maxx,
			float maxy, Vector3f[] position, Vector3f normal, Bakeable b) {
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
		Matrix4f t = MathHelper.getTransformation(realPoints, currPoints).matrix;
		// Vector4f test = new Vector4f(newuvs[0].x, newuvs[0].y, 0, 1);
		// t.transform(test);
		// Log.log(this, newuvs);
		// Log.log(this, position);
		// Log.log(this, position[0], test, t);
		boolean isEmissive = b.isEmmisive();
		float[] color = b.getColor();
		Vector4f pos = new Vector4f();
		for (int x2 = x; x2 <= maxx; x2++) {
			for (int y2 = y; y2 <= maxy; y2++) {
				bitmap[x2][y2] = true;
				emmisive[x2][y2] = isEmissive;
				pos.x = x2 + 0.5f;
				pos.y = y2 + 0.5f;
				pos.z = 0;
				pos.w = 1;
				t.transform(pos);
				lookup[x2][y2][0] = pos.x;
				lookup[x2][y2][1] = pos.y;
				lookup[x2][y2][2] = pos.z;
				lookup[x2][y2][3] = normal.x;
				lookup[x2][y2][4] = normal.y;
				lookup[x2][y2][5] = normal.z;
				lookup[x2][y2][6] = color[0];
				lookup[x2][y2][7] = color[1];
				lookup[x2][y2][8] = color[2];
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
		} while (!bitmap[x][y] || emmisive[x][y]);
	}

	public FloatBuffer getFloatTexture() {
		FloatBuffer textureBuffer = FloatBuffer.allocate(TEXTURE_SIZE
				* TEXTURE_SIZE * 4);
		for (int i = 0; i < TEXTURE_SIZE; i++) {
			for (int j = 0; j < TEXTURE_SIZE; j++) {
				// textureBuffer.put((lookup[j][i][3] + 1) / 2);
				// textureBuffer.put((lookup[j][i][4] + 1) / 2);
				// textureBuffer.put((lookup[j][i][5] + 1) / 2);
				if (emmisive[j][i]) {
					textureBuffer.put(lookup[j][i][6]);
					textureBuffer.put(lookup[j][i][7]);
					textureBuffer.put(lookup[j][i][8]);
				} else {
					textureBuffer.put(0);
					textureBuffer.put(0);
					textureBuffer.put(0);
				}
				textureBuffer.put(1);
			}
		}
		textureBuffer.rewind();
		return textureBuffer;
	}

	public static interface Bakeable {
		public float[] getVertices();

		public boolean isEmmisive();

		public float[] getColor();
	}

	public static class ProxyBakeable implements Bakeable {

		private float[] vertices;
		private float[] color;
		private boolean isEmmisive;

		public ProxyBakeable(float[] vertices, float[] color, boolean isEmmsivie) {
			this.vertices = vertices;
			this.color = color;
			this.isEmmisive = isEmmsivie;
		}

		public ProxyBakeable(float[] box) {
			this(box, new float[] { 0.5f, 0.5f, 0.5f }, false);
		}

		public ProxyBakeable(float[] box, float[] color) {
			this(box, color, false);
		}

		public ProxyBakeable(float[] box, boolean b) {
			// this(box, new float[] { (float) Math.random(),
			// (float) Math.random(), (float) Math.random() }, b);
			this(box, new float[] { 0.5f, 0.5f, 0.5f }, b);
		}

		@Override
		public float[] getVertices() {
			return vertices;
		}

		@Override
		public float[] getColor() {
			return color;
		}

		@Override
		public boolean isEmmisive() {
			return isEmmisive;
		}

	}

	public FloatBuffer radiosity(double[] color) {
		int x = currentLookupIndex / TEXTURE_SIZE;
		int y = currentLookupIndex % TEXTURE_SIZE;
		MathHelper.mul(color, REFLECTIVITY / 255.0);
		float[] colorf = MathHelper.toFloat(color);
		colorf[0] *= lookup[x][y][6];
		colorf[1] *= lookup[x][y][7];
		colorf[2] *= lookup[x][y][8];
		colorf[3] = 1;
		return FloatBuffer.wrap(colorf);
	}

}
