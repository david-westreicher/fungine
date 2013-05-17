package util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import rendering.ModelRenderer;

public class VoxelTerrainRenderer extends ModelRenderer {

	private static final int WIDTH = 150;
	private static final int HEIGHT = WIDTH;
	private static final int DEPTH = WIDTH;
	private static final float SCALE = 0.05f;
	private static final float THRESHOLD = 0.1f;

	public VoxelTerrainRenderer() {
		generate();
	}

	private void generate() {
		int points = 0;
		List<Float> verticeList = new ArrayList<Float>();
		for (int x = 0; x < WIDTH; x++)
			for (int y = 0; y < HEIGHT; y++)
				for (int z = 0; z < DEPTH; z++) {
					float intensity = getIntensity(x, y, z);
					// Log.log(this, x, y, z, intensity);
					if (intensity > THRESHOLD && intensity < THRESHOLD + 0.2) {
						verticeList
								.add((float) ((float) x / WIDTH - 0.5f + Math
										.random() / WIDTH));
						verticeList
								.add((float) ((float) y / HEIGHT - 0.5f + Math
										.random() / WIDTH));
						verticeList
								.add((float) ((float) z / DEPTH - 0.5f + Math
										.random() / WIDTH));
						points++;
					}
					// vertices.put((float) Math.random())
					// .put((float) Math.random())
					// .put((float) Math.random());
				}
		Log.log(this, "number of points: " + points);
		FloatBuffer vertices = FloatBuffer.allocate(points * 3);
		for (Float f : verticeList)
			vertices.put(f);
		vertices.rewind();
		// FloatBuffer colors = FloatBuffer.allocate(points * 3);
		// FloatBuffer normals = FloatBuffer.allocate(points * 3);
		// for (int x = 0; x < WIDTH; x++)
		// for (int y = 0; y < HEIGHT; y++)
		// for (int z = 0; z < DEPTH; z++) {
		// float intensity = getIntensity(x, y, z);
		// if (intensity > THRESHOLD) {
		// intensity = 1 - (intensity * 1 / THRESHOLD - 1)
		// / (1 / THRESHOLD - 1);
		// colors.put(intensity).put(intensity).put(intensity);
		// }
		// }
		// colors.rewind();
		// normals.rewind();
		init(vertices, null, null, null, null, null);
	}

	private float getIntensity(float x, float y, float z) {
		return (float) Noise.noise(x * SCALE, y * SCALE, z * SCALE);
	}
}
