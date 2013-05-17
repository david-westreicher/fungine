package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import physics.IntersectionTest;
import rendering.RenderUpdater;
import rendering.VoxelWorldRenderer;
import world.GameObject;
import world.GameObjectType;
import algorithms.SimplexNoise;

public class VoxelWorld extends GameObject {

	public static final String VOXEL_OBJECT_TYPE_NAME = "VoxelWorld";
	public static int CHUNKSIZE = 1;
	private static VoxelWorldRenderer voxelWorldRenderer;
	public transient final List<Chunk> chunks = new ArrayList<Chunk>();
	public boolean minecraft;

	public static final class GameObjectVoxel {
		public GameObject go;
		public int[] pos;
		private Chunk chunk;

		public GameObjectVoxel(Chunk chunk, GameObject go, int[] pos) {
			this.go = go;
			this.chunk = chunk;
			this.pos = pos;
		}

		public boolean isActive() {
			return chunk.voxels[pos[0]][pos[1]][pos[2]] > 0.8;
		}
	}

	public final class Chunk {
		public float pos[];
		public final float voxels[][][] = new float[16][16][16];
		public boolean valid = true;
		public List<GameObjectVoxel> objs;
		public List<GameObjectVoxel> visibleObjs;

		public void set(float b) {
			for (int i = 0; i < 16; i++)
				for (int j = 0; j < 16; j++)
					for (int k = 0; k < 16; k++)
						voxels[i][j][k] = b;
		}

		public void addObject(GameObject go, int[] pos) {
			if (objs == null) {
				objs = new ArrayList<GameObjectVoxel>();
				visibleObjs = new ArrayList<GameObjectVoxel>();
			}
			objs.add(new GameObjectVoxel(this, go, pos));
		}

		public void updateVisibleObjects() {
			if (visibleObjs != null) {
				visibleObjs.clear();
				for (GameObjectVoxel go : objs) {
					if (go.isActive())
						visibleObjs.add(go);
				}
			}
		}
	}

	public VoxelWorld(boolean minecraft, int chunksize) {
		super(null);
		VoxelWorld.CHUNKSIZE = chunksize;
		createInstance(chunksize);
		setType(VOXEL_OBJECT_TYPE_NAME);
		this.minecraft = minecraft;
		this.setType(VOXEL_OBJECT_TYPE_NAME);
		for (int i = 0; i < VoxelWorld.CHUNKSIZE; i++)
			for (int j = 0; j < VoxelWorld.CHUNKSIZE; j++)
				for (int k = 0; k < VoxelWorld.CHUNKSIZE; k++) {
					Chunk c = new Chunk();
					c.pos = new float[] { i, j, k };
					c.set(0.0f);
					chunks.add(c);
				}
		explode((int) ((16 * CHUNKSIZE / 2) / 4.0),
				(int) ((16 * CHUNKSIZE / 2) / 4.0),
				(int) ((16 * CHUNKSIZE / 2) / 4.0), 10, true);
		updateVoxels();

	}

	private static void createInstance(int chunksize) {
		GameObjectType voxelType = GameObjectType
				.getType(VOXEL_OBJECT_TYPE_NAME);
		if (voxelType == null) {
			voxelType = new GameObjectType(VOXEL_OBJECT_TYPE_NAME);
		}
		voxelWorldRenderer = new VoxelWorldRenderer();
		voxelType.renderer = voxelWorldRenderer;
	}

	public void setNoise() {
		for (int i = 0; i < CHUNKSIZE * 16; i++) {
			for (int j = 0; j < CHUNKSIZE * 16; j++) {
				for (int k = 0; k < CHUNKSIZE * 16; k++) {
					setVoxel(i, j, k, (float) SimplexNoise.noise(i / 40.0f,
							j / 20.0f, k / 20.0f) / 2 + 0.6f);
				}
			}
		}
		updateVoxels();
	}

	public void loadFromFile(String file) {
		Voxel voxels = new Voxel(CHUNKSIZE * 16);
		ObjLoader ojb = new ObjLoader(file, false, false);
		voxels.indices = ojb.indices;
		voxels.vertices = ojb.vertices;
		voxels.voxelize(file, CHUNKSIZE * 16);
		for (int i = 0; i < CHUNKSIZE * 16; i++) {
			for (int j = 0; j < CHUNKSIZE * 16; j++) {
				for (int k = 0; k < CHUNKSIZE * 16; k++) {
					setVoxel(i, j, k, voxels.voxels[i][j][k] ? 1 : 0);
				}
			}
		}
		updateVoxels();
	}

	public void explode(float x, float y, float z, int size, boolean add) {
		long start = System.currentTimeMillis();
		int[] randPos = new int[] { posToVoxel(x), posToVoxel(y), posToVoxel(z) };
		float sizeHalf = (float) size / 2;
		Random gauss = new Random();
		for (int i = randPos[0] - size / 2; i < randPos[0] + size / 2; i++)
			for (int j = randPos[1] - size / 2; j < randPos[1] + size / 2; j++)
				for (int k = randPos[2] - size / 2; k < randPos[2] + size / 2; k++) {
					float xDist = (randPos[0] - i);
					float yDist = (randPos[1] - j);
					float zDist = (randPos[2] - k);
					float dist = (float) Math.sqrt(xDist * xDist + yDist
							* yDist + zDist * zDist)
							/ ((float) Math.sqrt(2 * sizeHalf * sizeHalf));
					if (!minecraft)
						minmaxToVoxel(i, j, k, dist * dist * 2.0f, add);
					else if (dist < 0.8f || dist * Math.random() < 0.1)
						setVoxel(i, j, k, add ? 1.0f : 0.0f);
				}
		Log.log(this, System.currentTimeMillis() - start
				+ " explosion time at:", x, y, z, size);
		Log.log(this, randPos);
		updateVoxels();
	}

	private void updateVoxels() {
		final List<Chunk> toUpdate = new ArrayList<Chunk>();
		for (Chunk c : chunks) {
			if (!c.valid) {
				toUpdate.add(c);
			}
		}
		RenderUpdater.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < Math.min(toUpdate.size(), 6); i++) {
					Chunk c = toUpdate.get(i);
					voxelWorldRenderer.update(c.pos, c.voxels);
					c.valid = true;
				}
			}
		});
		for (int i = Math.min(toUpdate.size(), 6); i < toUpdate.size(); i++) {
			final Chunk c = toUpdate.get(i);
			RenderUpdater.queue(new Runnable() {
				@Override
				public void run() {
					voxelWorldRenderer.update(c.pos, c.voxels);
				}
			});
			c.valid = true;
		}
	}

	private void minmaxToVoxel(int i, int j, int k, float val, boolean add) {
		if (i >= CHUNKSIZE * 16 || j >= CHUNKSIZE * 16 || k >= CHUNKSIZE * 16
				|| i < 0 || j < 0 || k < 0)
			return;
		Chunk chunk = getChunk(i / 16, j / 16, k / 16);
		if (add)
			chunk.voxels[i % 16][j % 16][k % 16] += Math.max((1 - val) / 20.0f,
					0);
		else
			chunk.voxels[i % 16][j % 16][k % 16] = Math.min(val,
					chunk.voxels[i % 16][j % 16][k % 16]);
		chunk.valid = false;
	}

	public void setVoxel(int i, int j, int k, float val) {
		if (i >= CHUNKSIZE * 16 || j >= CHUNKSIZE * 16 || k >= CHUNKSIZE * 16
				|| i < 0 || j < 0 || k < 0)
			return;
		Chunk chunk = getChunk(i / 16, j / 16, k / 16);
		chunk.voxels[i % 16][j % 16][k % 16] = val;
		chunk.valid = false;
	}

	public Chunk getChunk(int i, int j, int k) {
		int index = (i * CHUNKSIZE + j) * CHUNKSIZE + k;
		Chunk c = index >= chunks.size() ? chunks.get(0) : chunks.get(index);
		return c;
	}

	public boolean isInVoxel(GameObject go) {
		return isInVoxel(go.pos);
	}

	public void addGameObject(GameObject go, float pos[]) {
		Chunk c = getChunk(pos);
		c.addObject(go, new int[] { posToVoxel(pos[0]) % 16,
				posToVoxel(pos[1]) % 16, posToVoxel(pos[2]) % 16 });
	}

	public float[] reflectLine(float[] start, float[] end) {
		Vector3f startV = new Vector3f(start);
		startV.sub(new Vector3f(end));
		// Log.log(this, "speed", startV);
		// startV.normalize();
		int[][] voxels = IntersectionTest.getLastIntersectingVoxels(start, end,
				this);
		int voxelStart[] = voxels[0];
		int voxelEnd[] = voxels[1];
		if (voxelStart[0] - voxelEnd[0] != 0) {
			startV.x *= -1;
		}
		if (voxelStart[1] - voxelEnd[1] != 0) {
			startV.y *= -1;
		}
		if (voxelStart[2] - voxelEnd[2] != 0) {
			startV.z *= -1;
		}
		// Log.log(this, "speedReflect", startV);
		return new float[] { -startV.x, -startV.y, -startV.z };
	}

	public int posToVoxel(float f) {
		return (int) (f * 4);
	}

	private Chunk getChunk(float[] pos) {
		return getChunk((int) (pos[0] / 4), (int) (pos[1] / 4),
				(int) (pos[2] / 4));
	}

	public boolean isInVoxel(float[] pos) {
		return getVoxelValue(posToVoxel(pos[0]), posToVoxel(pos[1]),
				posToVoxel(pos[2])) > 0.5f;
	}

	public void setVoxel(float[] pos, float val) {
		setVoxel(posToVoxel(pos[0]), posToVoxel(pos[1]), posToVoxel(pos[2]),
				val);
	}

	public float getVoxelValue(int i, int j, int k) {
		if (i >= CHUNKSIZE * 16 || j >= CHUNKSIZE * 16 || k >= CHUNKSIZE * 16
				|| i < 0 || j < 0 || k < 0)
			return 0.0f;
		Chunk chunk = getChunk(i / 16, j / 16, k / 16);
		return chunk.voxels[i % 16][j % 16][k % 16];
	}

	public String toString() {
		return "Voxelworld: " + super.toString();
	}
}
