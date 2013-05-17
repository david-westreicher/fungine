package algorithms;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Minecraft {

	public static FloatBuffer[] minecraftMesh(int steps, float[][][] voxels) {
		// Sweep over 3-axes
		long startTime = System.currentTimeMillis();
		int[] dims = new int[] { steps, steps, steps };
		List<int[]> quads = new ArrayList<int[]>();
		List<float[]> normals = new ArrayList<float[]>();
		for (int d = 0; d < 3; ++d) {
			int i, j, k, l, w, h, u = (d + 1) % 3, v = (d + 2) % 3;
			int[] x = new int[] { 0, 0, 0 };
			int[] q = new int[] { 0, 0, 0 };
			boolean mask[] = new boolean[(dims[u] + 1) * (dims[v] + 1)];
			q[d] = 1;
			for (x[d] = -1; x[d] < dims[d];) {
				// Compute mask
				int n = 0;
				for (x[v] = 0; x[v] < dims[v]; ++x[v])
					for (x[u] = 0; x[u] < dims[u]; ++x[u]) {
						mask[n++] = (0 <= x[d] ? f(x[0], x[1], x[2], voxels)
								: false) != (x[d] < dims[d] - 1 ? f(
								x[0] + q[0], x[1] + q[1], x[2] + q[2], voxels)
								: false);
					}
				// Increment x[d]
				++x[d];
				// Generate mesh for mask using lexicographic ordering
				n = 0;
				for (j = 0; j < dims[v]; ++j)
					for (i = 0; i < dims[u];) {
						if (mask[n]) {
							// Compute width
							for (w = 1; mask[n + w] && i + w < dims[u]; ++w) {
							}
							// Compute height (this is slightly awkward
							boolean done = false;
							for (h = 1; j + h < dims[v]; ++h) {
								for (k = 0; k < w; ++k) {
									if (!mask[n + k + h * dims[u]]) {
										done = true;
										break;
									}
								}
								if (done) {
									break;
								}
							}
							// Add quad
							x[u] = i;
							x[v] = j;
							int[] du = new int[] { 0, 0, 0 };
							int[] dv = new int[] { 0, 0, 0 };
							du[u] = w;
							dv[v] = h;
							int[] x1 = new int[] { x[0], x[1], x[2] };
							int[] x2 = new int[] { x[0] + du[0], x[1] + du[1],
									x[2] + du[2] };
							int[] x3 = new int[] { x[0] + du[0] + dv[0],
									x[1] + du[1] + dv[1], x[2] + du[2] + dv[2] };
							int[] x4 = new int[] { x[0] + dv[0], x[1] + dv[1],
									x[2] + dv[2] };
							int[] plus = new int[] { 0, 0, 0 };
							plus[d] = -1;
							boolean flipped = !f(x[0] + plus[0],
									x[1] + plus[1], x[2] + plus[2], voxels);
							float[] normal = new float[] { 0, 0, 0 };
							normal[d] = flipped ? -1 : 1;
							if (!flipped) {
								quads.add(x1);
								quads.add(x2);
								quads.add(x3);
								quads.add(x1);
								quads.add(x3);
								quads.add(x4);
							} else {
								quads.add(x1);
								quads.add(x3);
								quads.add(x2);
								quads.add(x1);
								quads.add(x4);
								quads.add(x3);
							}
							for (int o = 0; o < 6; o++) {
								normals.add(normal);
							}
							// Zero-out mask
							for (l = 0; l < h; ++l)
								for (k = 0; k < w; ++k) {
									mask[n + k + l * dims[u]] = false;
								}
							// Increment counters and continue
							i += w;
							n += w;
						} else {
							++i;
							++n;
						}
					}
			}
		}
		// Log.log(this, System.currentTimeMillis() - startTime
		// + " for generating mesh from voxels");
		List<Float> vertices = new ArrayList<Float>(quads.size() * 3);
		for (int[] coord : quads)
			for (Integer i : coord)
				vertices.add((float) i / steps);
		FloatBuffer fb = FloatBuffer.allocate(vertices.size());
		for (Float f : vertices)
			fb.put(f);
		fb.rewind();
		FloatBuffer normalfb = FloatBuffer.allocate(normals.size() * 3);
		for (float[] fs : normals)
			for (float f : fs)
				normalfb.put(f);
		normalfb.rewind();
		return new FloatBuffer[] { fb, normalfb };
	}

	private static boolean f(int i, int j, int k, float[][][] voxels) {
		if (i < 0 || j < 0 || k < 0 || i >= voxels.length || j >= voxels.length
				|| k >= voxels.length)
			return false;
		return voxels[i][j][k] > 0.5;
	}
}
