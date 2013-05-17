package rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import util.Material;
import browser.Browser;

public class TerrainRenderer extends ModelRenderer {
	public static final int WIDTH = 1300;
	public static final int HEIGHT = 1300;
	private static final long SEED = 151654654;
	public static float NOISE = 0;
	private static Vector3f[][] heightMap;

	private FloatBuffer vertices;
	private FloatBuffer normals;
	private FloatBuffer uvs;
	private IntBuffer indices;
	private FloatBuffer colors;
	private Random r;

	public TerrainRenderer() {
		generate();
		this.materials = new ArrayList<Material>();
		Material m = new Material(Browser.TEXTURE_NAME);
		m.texture = Browser.TEXTURE_NAME;
		m.displacementMap = Browser.TEXTURE_NAME;
		materials.add(m);
	}

	public void generate() {
		r = new Random(SEED);
		computeHeightMap();
		vertices = getVertices();
		indices = getIndices();
		normals = getNormals();
		// colors = getColors();
		uvs = getUVs();
		init(vertices, normals, colors, uvs, null, null, indices);
		vertices.clear();
		normals.clear();
		indices.clear();
		// colors.clear();
		vertices = null;
		normals = null;
		indices = null;
		colors = null;
	}

	private FloatBuffer getUVs() {
		FloatBuffer buf = FloatBuffer.allocate((WIDTH + 1) * (HEIGHT + 1) * 2);
		for (int i = 0; i < WIDTH + 1; i++) {
			for (int j = 0; j < HEIGHT + 1; j++) {
				buf.put((float) i / (WIDTH + 1));
				buf.put((float) j / (HEIGHT + 1));
			}
		}
		buf.rewind();
		return buf;
	}

	private FloatBuffer getColors() {
		FloatBuffer buf = FloatBuffer.allocate((WIDTH + 1) * (HEIGHT + 1) * 3);
		for (int i = 0; i < WIDTH + 1; i++) {
			for (int j = 0; j < HEIGHT + 1; j++) {
				// buf.put(0);
				// buf.put(Math.max(heightMap[i][j].y * 10, 0));
				// buf.put(heightMap[i][j].y * -5);// 0.1f - heightMap[i][j].y
				// // * 10);
				buf.put(1);
				buf.put(1);
				buf.put(1);
			}
		}
		buf.rewind();
		return buf;
	}

	private void computeHeightMap() {
		heightMap = new Vector3f[WIDTH + 1][HEIGHT + 1];
		for (int i = 0; i < WIDTH + 1; i++)
			for (int j = 0; j < HEIGHT + 1; j++)
				heightMap[i][j] = new Vector3f(i, 0, j);
		int log = 0;
		int exp = 1;
		while (exp < WIDTH) {
			exp *= 2;
			log++;
		}
		displace(heightMap[0][0], heightMap[WIDTH][0], heightMap[0][HEIGHT],
				heightMap[WIDTH][HEIGHT], 1 / 2f, log);
	}

	private void displace(Vector3f a, Vector3f b, Vector3f c, Vector3f d,
			float noise, int depth) {
		if (depth == 0)
			return;
		Vector3f ePos = new Vector3f();
		ePos.add(a);
		ePos.add(b);
		ePos.add(c);
		ePos.add(d);
		ePos.scale(1 / 4f);
		float height = ePos.y + noise * (r.nextFloat() - 0.5f) * NOISE;
		Vector3f e = heightMap[(int) ePos.x][(int) ePos.z];
		e.y = height;
		Vector3f f = displace(a, c, noise, height);
		Vector3f g = displace(a, b, noise, height);
		Vector3f h = displace(b, d, noise, height);
		Vector3f i = displace(c, d, noise, height);
		float newNoise = (noise / 2);
		displace(a, g, f, e, newNoise, depth - 1);
		displace(g, b, e, h, newNoise, depth - 1);
		displace(f, e, c, i, newNoise, depth - 1);
		displace(e, h, i, d, newNoise, depth - 1);
	}

	private Vector3f displace(Vector3f a, Vector3f c, float noise, float e) {
		Vector3f ePos = new Vector3f();
		ePos.add(a);
		ePos.add(c);
		ePos.scale(1 / 2f);
		float newE = ePos.y;
		heightMap[(int) ePos.x][(int) ePos.z].y = newE;
		return heightMap[(int) ePos.x][(int) ePos.z];
	}

	private FloatBuffer getNormals() {
		int size = (WIDTH + 1) * (HEIGHT + 1) * 3;
		List<Vector3f> normals = new ArrayList<Vector3f>(size);
		FloatBuffer buf = FloatBuffer.allocate(size);
		for (int i = 0; i < indices.capacity(); i += 3) {
			int i0 = indices.get(i);
			int i1 = indices.get(i + 1);
			int i2 = indices.get(i + 2);
			Vector3f v0 = getVertex(i0);
			Vector3f v1 = getVertex(i1);
			Vector3f v2 = getVertex(i2);
			v1.sub(v0);
			v2.sub(v0);
			Vector3f normal = new Vector3f();
			normal.cross(v1, v2);
			normal.normalize();
			addTo(normals, normal, i0);
			addTo(normals, normal, i1);
			addTo(normals, normal, i2);
		}
		for (Vector3f n : normals) {
			n.normalize();
		}
		for (Vector3f n : normals) {
			buf.put(n.x);
			buf.put(n.y);
			buf.put(n.z);
		}
		indices.rewind();
		buf.rewind();
		return buf;
	}

	private void addTo(List<Vector3f> normals, Vector3f normal, int i) {
		while (normals.size() < i + 1)
			normals.add(new Vector3f());
		Vector3f currNormal = normals.get(i);
		currNormal.add(normal);
	}

	private Vector3f getVertex(int i) {
		Vector3f vec = (Vector3f) heightMap[i / (HEIGHT + 1)][i % (HEIGHT + 1)]
				.clone();
		vec.y *= WIDTH + 1;
		return vec;
	}

	private IntBuffer getIndices() {
		IntBuffer buf = IntBuffer.allocate(WIDTH * HEIGHT * 6);
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				// v0
				int first = (HEIGHT + 1) * i + j;
				// v1
				int second = first + HEIGHT + 1;
				buf.put(first);
				buf.put(second + 1);
				buf.put(second);
				buf.put(first);
				buf.put(first + 1);
				buf.put(second + 1);
			}

		}
		buf.rewind();
		return buf;
	}

	private FloatBuffer getVertices() {
		FloatBuffer buf = FloatBuffer.allocate((WIDTH + 1) * (HEIGHT + 1) * 3);
		float terrainOffset = 1.0f / (2 * (WIDTH + 1));
		for (int i = 0; i < WIDTH + 1; i++) {
			for (int j = 0; j < HEIGHT + 1; j++) {
				buf.put((float) i / (WIDTH + 1) - 0.5f + terrainOffset);
				buf.put(heightMap[i][j].y);
				buf.put((float) j / (HEIGHT + 1) - 0.5f + terrainOffset);
			}
		}
		buf.rewind();
		return buf;
	}

	protected int isStatic() {
		return GL2.GL_DYNAMIC_DRAW;
	}

	public static float[] getHeightField() {
		float[] heights = new float[(WIDTH + 1) * (HEIGHT + 1)];
		for (int i = 0; i < WIDTH + 1; i++)
			for (int j = 0; j < HEIGHT + 1; j++)
				heights[i * (WIDTH + 1) + j] = heightMap[j][i].y;
		return heights;
	}

	@Override
	public void init(GL2 gl) {
		// gl.glDisable(GL2.GL_CULL_FACE);
		super.init(gl);
	}

	@Override
	public void end(GL2 gl) {
		super.end(gl);
		// gl.glEnable(GL2.GL_CULL_FACE);
	}
}
