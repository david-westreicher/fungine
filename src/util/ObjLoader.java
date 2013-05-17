package util;

import game.Game;
import io.IO;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import physics.IntersectionTest;
import settings.Settings;
import world.GameObject;

public class ObjLoader {

	public class Group {

		public String name;
		public int start;
		public int end;
		public Material material;

		public Group(String name, int start) {
			this.name = name;
			this.start = start;
		}

		@Override
		public String toString() {
			return "Group [name=" + name + ", start=" + start + ", end=" + end
					+ ", material=" + material + "]";
		}

	}

	public Map<String, Integer> indicesMap = new HashMap<String, Integer>();
	public List<Float> correctVertices = new ArrayList<Float>();
	public List<Float> correctUVs = new ArrayList<Float>();
	public List<Float> correctNormals = new ArrayList<Float>();
	public List<Integer> correctIndices = new ArrayList<Integer>();
	public List<Float> correctWeights = new ArrayList<Float>();
	public List<Float> correctBoneIndices = new ArrayList<Float>();
	public List<Float> normalList = new ArrayList<Float>();
	public List<Float> verticesList = new ArrayList<Float>();
	private List<Float> uvsList = new ArrayList<Float>();
	private List<Integer> indicesList = new ArrayList<Integer>();
	private List<Float> weightsList = new ArrayList<Float>();
	private List<Float> boneIndicesList = new ArrayList<Float>();
	private List<Group> groups = new ArrayList<Group>();
	public FloatBuffer vertices;
	public IntBuffer[] indices;
	public FloatBuffer normals;
	public FloatBuffer uvs;
	public FloatBuffer weights;
	public FloatBuffer boneIndices;
	public int vertexCount;
	private boolean hasNormals;
	private boolean hasUV;
	private Group currentGroup;
	private MaterialLibrary materialLibrary;
	public List<Material> materials;
	private boolean hasWeights = false;
	private boolean flippedCullface;
	private String name;

	public ObjLoader(String s, boolean flippedCullface, boolean engineFolder) {
		this(s, flippedCullface, false, false);
	}

	public ObjLoader(String s, boolean flippedCullface, boolean voxelize,
			boolean engineFolder) {
		Log.log(this, "starting to parse: " + s);
		this.flippedCullface = flippedCullface;
		this.name = s;
		BufferedReader br = IO.read(engineFolder ? Settings.ENGINE_FOLDER
				: Settings.RESSOURCE_FOLDER, s);
		String line;
		int lineNum = 0;
		try {
			while ((line = br.readLine()) != null && !line.startsWith("e")) {
				// if (lineNum++ % 10000 == 0)
				// Log.log(this, "line number: " + lineNum);
				String[] split = line.split("\\s+");
				if (line.startsWith("mtllib")) {
					materialLibrary = new MaterialLibrary(s.substring(0,
							s.lastIndexOf("/") + 1)
							+ split[1]);
				}
				if (line.startsWith("usemtl") && materialLibrary != null) {
					Material newMaterial = materialLibrary
							.getMaterial(split[1]);
					if (currentGroup == null || currentGroup.material != null) {
						newGroup(currentGroup == null ? "" : currentGroup.name
								+ "," + newMaterial.name);
					}
					currentGroup.material = newMaterial;
				}
				if (line.startsWith("g ") || line.startsWith("o ")) {
					newGroup(split[1]);
				}
				if (line.startsWith("w ")) {
					hasWeights = true;
					for (int i = 0; i < (split.length - 1) / 2; i++) {
						weightsList.add(Float.parseFloat(split[2 * i + 1]));
						boneIndicesList.add(Float
								.parseFloat(split[2 * (i + 1)]));
					}
					for (int i = (split.length - 1) / 2; i < 4; i++) {
						weightsList.add(-1f);
						boneIndicesList.add(-1f);
					}
				}
				if (line.startsWith("v ")) {
					for (int i = 0; i < 3; i++) {
						verticesList.add(Float.parseFloat(split[i + 1]));
					}
				}
				if (line.startsWith("vn")) {
					hasNormals = true;
					for (int i = 0; i < 3; i++) {
						normalList.add(Float.parseFloat(split[i + 1]));
					}
				}
				if (line.startsWith("vt")) {
					hasUV = true;

					for (int i = 0; i < 2; i++) {
						uvsList.add(Float.parseFloat(split[i + 1]));
					}
				}
				if (line.startsWith("f")) {
					if (split.length == 4) {
						// for (int i = 0; i < 3; i++) {
						put(split[1]);
						put(split[flippedCullface ? 3 : 2]);
						put(split[flippedCullface ? 2 : 3]);
						indicesList
								.add(Integer.parseInt(split[1].split("/")[0]) - 1);
						indicesList.add(Integer
								.parseInt(split[flippedCullface ? 3 : 2]
										.split("/")[0]) - 1);
						indicesList.add(Integer
								.parseInt(split[flippedCullface ? 2 : 3]
										.split("/")[0]) - 1);
						// }
					}
					if (split.length == 5) {
						put(split[1]);
						put(split[2]);
						put(split[3]);
						indicesList
								.add(Integer.parseInt(split[1].split("/")[0]) - 1);
						indicesList
								.add(Integer.parseInt(split[2].split("/")[0]) - 1);
						indicesList
								.add(Integer.parseInt(split[3].split("/")[0]) - 1);

						put(split[1]);
						put(split[3]);
						put(split[4]);

						indicesList
								.add(Integer.parseInt(split[1].split("/")[0]) - 1);
						indicesList
								.add(Integer.parseInt(split[3].split("/")[0]) - 1);
						indicesList
								.add(Integer.parseInt(split[4].split("/")[0]) - 1);
					}
				}
			}
			if (currentGroup != null) {
				currentGroup.end = correctIndices.size() - 1;
			}
			// for (Group g : groups)
			// Log.log(this, g);

			Collections.sort(groups, new Comparator<Group>() {
				@Override
				public int compare(Group o1, Group o2) {
					if (o1.material == null || o1.material.name == null
							|| o2.material == null || o2.material.name == null)
						return 0;
					return o1.material.name.compareTo(o2.material.name);
				}
			});

			if (hasNormals || hasUV) {
				normalize(correctVertices);
				// Log.log(this, "normallist: " + normalList);
				// Log.log(this, "normals: " + correctNormals);
				// Log.log(this, "vertices: " + correctVertices);
				// Log.log(this, "indices: " + correctIndices);
				vertices = fillFloat(correctVertices);
				/*
				 * Log.log(this, "unsorted"); for (Group g : groups) {
				 * Log.log(this, g); }
				 */

				indices = fillInt(correctIndices, groups);

				weights = fillFloat(correctWeights);
				boneIndices = fillFloat(correctBoneIndices);
				normals = fillFloat(correctNormals);
				if (hasUV)
					uvs = fillFloat(correctUVs);
			} else {
				normalize(verticesList);
				vertices = fillFloat(verticesList);
				indices = new IntBuffer[] { fillInt(indicesList) };
				if (hasUV)
					uvs = fillFloat(uvsList);
			}

			if (voxelize) {
				voxelize();
			}

			materials = new ArrayList<Material>();
			if (materialLibrary != null)
				for (Material m : materialLibrary.getMaterials())
					Log.log(this, m);
			for (Group g : groups) {
				// Log.log(this, g.name, g.material);
				materials.add(g.material);
			}
			/*
			 * normals = IntBuffer.allocate(normalList.size()); for (Integer i :
			 * normalList) Log.log(this, verticesList.size() / 3,
			 * indicesList.size() / 3, polCount);
			 */
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void voxelize() {
		Voxel v = new Voxel(indices, vertices);
		v.voxelize(name, 200);
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

	private void newGroup(String name) {
		int last = correctIndices.size();
		if (currentGroup != null) {
			currentGroup.end = last - 1;
		}
		currentGroup = new Group(name, last);
		groups.add(currentGroup);
	}

	private IntBuffer fillInt(List<Integer> indicesList) {
		return fillInt(indicesList, 0, indicesList.size() - 1);
	}

	private IntBuffer[] fillInt(List<Integer> indicesList, List<Group> groups) {
		IntBuffer indices[] = new IntBuffer[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			Group g = groups.get(i);
			indices[i] = fillInt(indicesList, g.start, g.end);
		}
		return indices;
	}

	private IntBuffer fillInt(List<Integer> arr, int start, int end) {
		IntBuffer vertices = IntBuffer.allocate(end - start + 1);
		for (int i = start; i <= end; i++) {
			vertices.put(arr.get(i));
		}
		vertices.rewind();
		return vertices;
	}

	private FloatBuffer fillFloat(List<Float> arr) {
		if (arr == null || arr.size() == 0)
			return null;
		FloatBuffer vertices = FloatBuffer.allocate(arr.size());
		for (Float f : arr)
			vertices.put(f);
		vertices.rewind();
		return vertices;
	}

	private void put(String val) {
		Integer index = indicesMap.get(val);
		if (index == null) {
			index = indicesMap.size();
			indicesMap.put(val, index);
			if (hasUV) {
				String uvIndexString = val.split("/")[1];
				int uvIndex;
				if (uvIndexString.length() == 0)
					uvIndex = 0;
				else
					uvIndex = Integer.parseInt(uvIndexString) - 1;
				for (int i = 0; i < 2; i++) {
					correctUVs.add(uvsList.get(uvIndex * 2 + i));
				}
			}
			if (hasNormals) {
				int normalIndex = Integer.parseInt(val.split("/")[2]) - 1;
				for (int i = 0; i < 3; i++)
					correctNormals.add(normalList.get(normalIndex * 3 + i));
			}
			int vertexIndex = Integer.parseInt(val.split("/")[0]) - 1;
			for (int i = 0; i < 3; i++)
				correctVertices.add(verticesList.get(vertexIndex * 3 + i));

			if (hasWeights)
				for (int i = 0; i < 4; i++) {
					correctWeights.add(weightsList.get(vertexIndex * 4 + i));
					correctBoneIndices.add(boneIndicesList.get(vertexIndex * 4
							+ i));
				}
		}
		correctIndices.add(index);
	}

	public static void normalize(List<Float> vertices) {
		// xyz minMax bounds
		float[][] bounds = new float[][] { new float[2], new float[2],
				new float[2] };
		int i = 0;
		for (Float v : vertices) {
			check(v, bounds[i++ % 3]);
		}
		// move with map[0], scale with map[1]
		float[][] map = new float[3][];
		for (i = 0; i < 3; i++) {
			float length = bounds[i][1] - bounds[i][0];
			map[i] = new float[] { -bounds[i][0] - length / 2, length };
		}
		float length = Math.max(Math.max(map[0][1], map[1][1]), map[2][1]);
		// Log.log(ObjLoader.class, "normalizing: " + length);
		for (i = 0; i < vertices.size(); i++) {
			Float curr = vertices.get(i);
			float[] m = map[i % 3];
			vertices.set(i, (curr + m[0]) / length);
		}
	}

	private float dist(float x, float y, float z) {

		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public static void check(Float v, float[] xMM) {
		if (xMM[0] > v)
			xMM[0] = v;
		if (xMM[1] < v)
			xMM[1] = v;
	}

}
