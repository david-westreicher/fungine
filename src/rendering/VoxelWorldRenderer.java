package rendering;

import game.Game;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.VoxelWorld;
import util.VoxelWorld.GameObjectVoxel;
import world.GameObject;
import world.GameObjectType;
import algorithms.MarchingCube;
import algorithms.Minecraft;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;

public class VoxelWorldRenderer extends GameObjectRenderer {

	private ShaderScript voxelShader;
	private ShaderScript oldShader;
	private VoxelWorld world;
	private ChunkR chunks[][][] = new ChunkR[VoxelWorld.CHUNKSIZE][VoxelWorld.CHUNKSIZE][VoxelWorld.CHUNKSIZE];
	private boolean depthOnly;
	private ShaderScript transformShader;
	private ShaderScript voxelDepth;
	private int sortTick;
	private List<ChunkInfo> sortedChunks = new ArrayList<ChunkInfo>();
	private List<ChunkInfo> visibleChunks = new ArrayList<ChunkInfo>();
	public static int VISIBLE_CHUNKS;
	private static Vector3f tmp = new Vector3f(0, 0, 0);

	public VoxelWorldRenderer() {
		RenderUpdater.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				UberManager.getTexture("img/stone.jpg");
				// UberManager.getTexture("img/stone.jpg");
			}
		});
		for (int i = 0; i < VoxelWorld.CHUNKSIZE; i++)
			for (int j = 0; j < VoxelWorld.CHUNKSIZE; j++)
				for (int k = 0; k < VoxelWorld.CHUNKSIZE; k++) {
					chunks[i][j][k] = new ChunkR(new float[16][16][16]);
					ChunkInfo ci = new ChunkInfo();
					ci.renderer = chunks[i][j][k];
					ci.pos = new Vector3f(i, j, k);
					sortedChunks.add(ci);
				}
	}

	public boolean isSimple() {
		return true;
	}

	@Override
	public void draw(GL2 gl, List<GameObject> gos, float interp) {
		if (gos.size() == 0)
			return;
		if (gos.size() > 1)
			throw new RuntimeException("Just render 1 voxel object!");
		if (world != null)
			draw(gl);
		world = (VoxelWorld) gos.get(0);
	}

	@Override
	public void init(GL2 gl) {
		this.voxelShader = UberManager.getShader(Shader.VOXEL);
		voxelDepth = UberManager.getShader(Shader.VOXEL_DEPTH);
		depthOnly = ((RenderUpdater) Game.INSTANCE.loop.renderer).renderState.depthOnly;

		calculateVisibleChunks();
		// render voxel content
		if (world != null) {
			Map<String, List<GameObject>> gameObjects = new HashMap<String, List<GameObject>>();
			for (ChunkInfo ci : visibleChunks) {
				int i = (int) ci.pos.x;
				int j = (int) ci.pos.y;
				int k = (int) ci.pos.z;
				List<GameObjectVoxel> objs = world.getChunk(i, j, k).visibleObjs;
				if (objs != null) {
					for (GameObjectVoxel v : objs) {
						List<GameObject> list = gameObjects.get(v.go.getType());
						if (list == null) {
							list = new ArrayList<GameObject>();
							gameObjects.put(v.go.getType(), list);
						}
						list.add(v.go);
					}

				}
			}
			((RenderUpdater) Game.INSTANCE.loop.renderer)
					.renderObjects(gameObjects);
		}

		// deactivate old shader if necesary and activate voxel shader
		if (voxelShader == null || depthOnly)
			return;
		oldShader = ShaderScript.getActiveShader(gl);
		if (oldShader != null) {
			oldShader.end(gl);
		}
		if (Game.WIREFRAME)
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		else
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		voxelShader.execute(gl);
		((RenderUpdater) Game.INSTANCE.loop.renderer).initShaderUniforms();
		Texture stone = UberManager.getTexture("img/stone.jpg");
		if (stone != null)
			ShaderScript.setUniformTexture("triplanar", 0,
					stone.getTextureObject(gl));
		Texture stonen = UberManager.getTexture("img/stone.jpg");
		if (stonen != null)
			ShaderScript.setUniformTexture("triplanarNorm", 0,
					stone.getTextureObject(gl));
	}

	private void calculateVisibleChunks() {
		visibleChunks.clear();
		final Vector3f camPos = new Vector3f(Game.INSTANCE.cam.pos);
		camPos.scale(0.25f);
		// camPos.add(new Vector3f(0.5f, 0.5f, 0.5f));
		Vector3f camDir = new Vector3f(0, 0, 1);
		Game.INSTANCE.cam.rotationMatrix.transform(camDir);
		for (ChunkInfo ci : sortedChunks) {
			tmp.set(ci.pos);
			tmp.sub(camPos, tmp);
			float length = tmp.length();
			ci.distance = length;
			if (length > 5)
				continue;
			if (length > 6)
				break;
			tmp.normalize();
			float dot = tmp.dot(camDir);
			if (dot < 1 - RenderUpdater.FOV_Y / 180 && length > 5)
				continue;
			visibleChunks.add(ci);
		}
		VISIBLE_CHUNKS = visibleChunks.size();

		if (sortTick++ % 3000 == 0)
			Collections.sort(sortedChunks);
	}

	@Override
	public void draw(GL2 gl) {
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		if (!depthOnly) {
			gl.glColor3fv(world.color, 0);
			gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			GameObjectType got = GameObjectType.getType(world.getType());
			ShaderScript.setUniform("shininess", got.shininess);
			ShaderScript.setUniform("reflective", got.reflective);
		} else {
			voxelDepth.execute(gl);
		}
		ShaderScript.setUniform("scale", 4.0f);
		for (ChunkInfo ci : visibleChunks) {
			int i = (int) ci.pos.x;
			int j = (int) ci.pos.y;
			int k = (int) ci.pos.z;
			if (chunks[i][j][k].vertexCount == 0)
				continue;
			ShaderScript.setUniform("chunkPos", world.getChunk(i, j, k).pos);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, chunks[i][j][k].vboVertices[0]);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
			if (!depthOnly) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER,
						chunks[i][j][k].vboNormals[0]);
				gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
			}
			gl.glDrawArrays(chunks[i][j][k].drawMode, 0,
					chunks[i][j][k].vertexCount);
		}
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		if (!depthOnly) {
			gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		} else {
			voxelDepth.end(gl);
		}
	}

	@Override
	public void end(GL2 gl) {
		if (depthOnly || voxelShader == null)
			return;
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		((RenderUpdater) Game.INSTANCE.loop.renderer).endShaderUniforms();
		voxelShader.end(gl);
		if (oldShader != null) {
			oldShader.execute(gl);
			((RenderUpdater) Game.INSTANCE.loop.renderer).initShaderUniforms();
		}
	}

	@Override
	public void drawSimple(GL2 gl) {
	}

	public static final class ChunkInfo implements Comparable<ChunkInfo> {
		public ChunkR renderer;
		public Vector3f pos;
		protected float distance;

		@Override
		public int compareTo(ChunkInfo o) {
			return Float.compare(distance, o.distance);
		}
	}

	public class ChunkR extends ModelRenderer {

		public ChunkR(float voxels[][][]) {
			FloatBuffer[] vertNorms = getMesh(voxels, null, null);
			init(vertNorms[0], vertNorms[1], null, null, null, null);
		}

		private FloatBuffer[] getMesh(float voxels[][][], float[] pos,
				VoxelWorld world) {
			if (world != null && world.minecraft)
				return Minecraft.minecraftMesh(16, voxels);
			return MarchingCube.polygonise(16, voxels, pos, world);
		}

		public void updateMesh(float voxels[][][], float[] pos,
				VoxelWorld world, GL2 gl) {
			int i = (int) pos[0];
			int j = (int) pos[1];
			int k = (int) pos[2];
			if (world != null)
				world.getChunk(i, j, k).updateVisibleObjects();
			FloatBuffer[] vertNorms = getMesh(voxels, pos, world);
			this.vertexCount = vertNorms[0].capacity() / 3;
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertices[0]);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertNorms[0].capacity()
					* Buffers.SIZEOF_FLOAT, vertNorms[0], isStatic());
			if (vertNorms[1] != null) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboNormals[0]);
				gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertNorms[1].capacity()
						* Buffers.SIZEOF_FLOAT, vertNorms[1], isStatic());
			}
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		}

		@Override
		protected int isStatic() {
			return GL2.GL_DYNAMIC_DRAW;
		}

	}

	public void update(float[] pos, float[][][] voxels) {
		chunks[(int) pos[0]][(int) pos[1]][(int) pos[2]].updateMesh(voxels,
				pos, world, RenderUpdater.gl);
	}

}
