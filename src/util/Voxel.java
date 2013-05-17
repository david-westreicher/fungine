package util;

import game.Game;
import io.IO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.vecmath.Vector3f;

import physics.IntersectionTest;
import world.GameObject;

public class Voxel {

	public IntBuffer[] indices;
	public FloatBuffer vertices;
	private float stepSize;
	private int steps;
	public boolean[][][] voxels;

	public Voxel(IntBuffer[] indices, FloatBuffer vertices) {
		this.indices = indices;
		this.vertices = vertices;
	}

	public Voxel(String file) {
		voxels = read(file);
		steps = voxels == null ? 200 : voxels.length - 1;
		stepSize = 1.0f / (steps);
	}

	public Voxel(int size) {
		voxels = new boolean[size][size][size];
		steps = size - 1;
		stepSize = 1.0f / (steps);
	}

	public static void save(boolean[][][] voxels, String file) {
		Log.log(Voxel.class, "saving voxel object: " + file);
		try {
			BufferedWriter bw = IO.getWriteBuffer(file);
			int xLength = voxels.length;
			int yLength = voxels[0].length;
			int zLength = voxels[0][0].length;
			bw.write(xLength + "\n");
			bw.write(yLength + "\n");
			bw.write(zLength + "\n");
			for (int i = 0; i < xLength; i++) {
				for (int j = 0; j < yLength; j++) {
					StringBuilder sb = new StringBuilder();
					for (int k = 0; k < zLength; k++) {
						sb.append(voxels[i][j][k] ? "x" : " ");
					}
					sb.append("\n");
					bw.write(sb.toString());
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean[][][] read(String file) {
		try {
			Log.log(Voxel.class, "reading voxel object: " + file);
			BufferedReader br = IO.read(file);
			if (br == null)
				return null;
			int steps = Integer.parseInt(br.readLine());
			br.readLine();
			br.readLine();
			boolean[][][] voxels = new boolean[steps][steps][steps];
			for (int i = 0; i < steps; i++) {
				for (int j = 0; j < steps; j++) {
					String line = br.readLine();
					char[] chars = new char[line.length()];
					line.getChars(0, line.length(), chars, 0);
					for (int k = 0; k < steps; k++) {
						voxels[i][j][k] = chars[k] == 'x';
					}
				}
			}
			br.close();
			return voxels;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void voxelize(String name, int defaultSteps) {
		long start = System.currentTimeMillis();
		Log.log(this, "starting voxelization");
		boolean voxels[][][] = Voxel.read(name + ".vox");
		steps = voxels == null ? defaultSteps : voxels.length - 1;
		stepSize = 1.0f / (steps);
		if (voxels == null) {
			voxels = new boolean[steps + 1][steps + 1][steps + 1];
			Vector3f boxhalfsize = new Vector3f(stepSize / 2, stepSize / 2,
					stepSize / 2);
			for (int indice = 0; indice < indices.length; indice++) {
				for (int t = 0; t < indices[indice].capacity(); t += 3) {
					Vector3f[] triangle = getTriangle(indice, t);
					int minMax[][] = convert(getMinMax(triangle), steps);
					for (int i = minMax[0][0]; i <= minMax[0][1]; i++) {
						for (int j = minMax[1][0]; j <= minMax[1][1]; j++) {
							for (int k = minMax[2][0]; k <= minMax[2][1]; k++) {
								if (!voxels[i][j][k]) {
									boolean result = isIntersecting(i, j, k,
											triangle, boxhalfsize, stepSize);
									if (result)
										voxels[i][j][k] = true;
								}
							}
						}
					}
				}
				Log.log(this, indice + "/" + indices.length
						+ " indices generated");
			}
			Voxel.save(voxels, name + ".vox");
			Log.log(this, "voxels created");
		}
		this.voxels = voxels;

		Log.log(this, "voxelization ended", System.currentTimeMillis() - start);
	}

	public void generateMesh1() {
		boolean marked[][][] = new boolean[steps + 1][steps + 1][steps + 1];
		for (int i = 0; i < steps; i++) {
			for (int j = 0; j < steps; j++) {
				for (int k = 0; k < steps; k++) {
					if (voxels[i][j][k] && !marked[i][j][k]) {
						marked[i][j][k] = true;
						int to = j;
						boolean expand = true;
						while (expand) {

						}
						while (to + 1 < steps && voxels[i][to + 1][k]
								&& !marked[i][to + 1][k]) {
							to++;
							marked[i][to][k] = true;
						}

						GameObject cube = Game.INSTANCE.factory
								.createGameObject("Cube");
						cube.setPos(
								(i * stepSize - 0.5f + stepSize / 2) * 10,
								((((float) j + to) / 2) * stepSize - 0.5f + stepSize / 2) * 10,
								(k * stepSize - 0.5f + stepSize / 2) * 10);
						cube.setSize(stepSize * 10, stepSize * 10
								* (to - j + 1), stepSize * 10);
						cube.setColor(0.5f, 0.5f, 0.5f);
						Game.INSTANCE.world.add(cube);
					}
				}
			}
		}
	}

	public FloatBuffer[] generateMesh() {
		return null;
		// return Minecraft.minecraftMesh(steps, voxels);
		// return MarchingCube.polygonise(this, worldI, worldJ, worldK);
	}

	private int[][] convert(float[][] minMax, int steps) {
		int[][] minMaxI = new int[3][2];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 2; j++) {
				minMaxI[i][j] = (int) ((minMax[i][j] + 0.5f) * steps);
			}
		}
		return minMaxI;
	}

	private float[][] getMinMax(Vector3f[] triangle) {
		float[][] minMax = new float[3][2];
		for (int i = 0; i < 3; i++) {
			minMax[i][0] = Integer.MAX_VALUE;
			minMax[i][1] = 0;
		}
		for (int i = 0; i < 3; i++) {
			if (minMax[0][0] > triangle[i].x)
				minMax[0][0] = triangle[i].x;
			if (minMax[0][1] < triangle[i].x)
				minMax[0][1] = triangle[i].x;
			if (minMax[1][0] > triangle[i].y)
				minMax[1][0] = triangle[i].y;
			if (minMax[1][1] < triangle[i].y)
				minMax[1][1] = triangle[i].y;
			if (minMax[2][0] > triangle[i].z)
				minMax[2][0] = triangle[i].z;
			if (minMax[2][1] < triangle[i].z)
				minMax[2][1] = triangle[i].z;
		}
		return minMax;
	}

	private Vector3f[] getTriangle(int indice, int t) {
		Vector3f[] triangle = new Vector3f[] {
				new Vector3f(vertices.get(indices[indice].get(t) * 3 + 0),
						vertices.get(indices[indice].get(t) * 3 + 1),
						vertices.get(indices[indice].get(t) * 3 + 2)),
				new Vector3f(vertices.get(indices[indice].get(t + 1) * 3 + 0),
						vertices.get(indices[indice].get(t + 1) * 3 + 1),
						vertices.get(indices[indice].get(t + 1) * 3 + 2)),
				new Vector3f(vertices.get(indices[indice].get(t + 2) * 3 + 0),
						vertices.get(indices[indice].get(t + 2) * 3 + 1),
						vertices.get(indices[indice].get(t + 2) * 3 + 2)) };
		return triangle;
	}

	private boolean isIntersecting(int i, int j, int k, Vector3f[] triangle,
			Vector3f boxhalfsize, float stepSize) {
		boolean result = IntersectionTest.triBoxOverlap(new Vector3f(i
				* stepSize - 0.5f + stepSize / 2, j * stepSize - 0.5f
				+ stepSize / 2, k * stepSize - 0.5f + stepSize / 2),
				boxhalfsize, triangle);
		return result;
	}

	private void oldVoxelize() {
		int steps = 100;
		float stepSize = 1.0f / (steps);
		boolean voxels[][] = new boolean[steps + 1][steps + 1];
		for (int indice = 0; indice < indices.length; indice++) {
			for (int t = 0; t < indices[indice].capacity(); t += 3) {
				float minx = Math.min(Math.min(
						vertices.get(indices[indice].get(t) * 3 + 0),
						vertices.get(indices[indice].get(t + 1) * 3 + 0)),
						vertices.get(indices[indice].get(t + 2) * 3 + 0));
				float maxx = Math.max(Math.max(
						vertices.get(indices[indice].get(t) * 3 + 0),
						vertices.get(indices[indice].get(t + 1) * 3 + 0)),
						vertices.get(indices[indice].get(t + 2) * 3 + 0));
				float miny = Math.min(Math.min(
						vertices.get(indices[indice].get(t) * 3 + 1),
						vertices.get(indices[indice].get(t + 1) * 3 + 1)),
						vertices.get(indices[indice].get(t + 2) * 3 + 1));
				float maxy = Math.max(Math.max(
						vertices.get(indices[indice].get(t) * 3 + 1),
						vertices.get(indices[indice].get(t + 1) * 3 + 1)),
						vertices.get(indices[indice].get(t + 2) * 3 + 1));
				minx = (minx + 0.5f) * steps;
				maxx = (maxx + 0.5f) * steps;
				miny = (miny + 0.5f) * steps;
				maxy = (maxy + 0.5f) * steps;
				for (int i = (int) minx; i <= maxx; i++) {
					for (int j = (int) miny; j <= maxy; j++) {
						float x = i * stepSize - 0.5f;
						float y = j * stepSize - 0.5f;

						boolean result = voxels[i][j];
						if (!result
								&& IntersectionTest.isIntersecting(new float[] {
										x, y }, new float[] { x + stepSize,
										y + stepSize }, vertices, indices, t,
										indice)) {
							voxels[i][j] = true;
						}
					}
				}
			}
		}
		for (int i = 0; i < steps; i++) {
			for (int j = 0; j < steps; j++) {
				if (voxels[i][j]) {
					GameObject cube = Game.INSTANCE.factory
							.createGameObject("Cube");
					cube.setPos((i * stepSize - 0.5f + stepSize / 2) * 10, (j
							* stepSize - 0.5f - stepSize / 2) * 10, 0);
					cube.setSize(stepSize * 10, stepSize * 10, stepSize * 10);
					Game.INSTANCE.world.add(cube);
					System.out.print("x ");
				} else {
					System.out.print("  ");
				}
			}
			System.out.print("\n");
		}
	}
}
