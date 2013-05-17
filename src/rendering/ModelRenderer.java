package rendering;

import game.Game;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.Log;
import util.Material;
import util.ObjLoader;
import world.GameObject;
import world.GameObjectType;

import com.jogamp.common.nio.Buffers;

public class ModelRenderer extends GameObjectRenderer {
	private static final int NUM_RENDERED_INST = 30;

	public int drawMode = GL2.GL_TRIANGLES;;
	protected int[] vboVertices = new int[1];
	private int[] vboIndices;
	protected int[] vboNormals = new int[1];
	private int[] vboColors = new int[1];
	private int[] vboUvs = new int[1];
	// vboBones[0] = float4(weights), vboBones[1] = int4(boneIndices);
	private int[] vboBones = new int[2];

	protected int vertexCount;
	protected int[] indexCounts;
	private static ShaderScript transformShader;
	private static ShaderScript simpleTransform;

	private FloatBuffer[] transformBuffers;
	private boolean hasNormals;
	private boolean hasColors;
	private boolean hasIndices;
	private boolean hasUVs;
	protected List<Material> materials;
	private static Material currentMaterial;
	private String name;
	private RenderState renderState;
	private boolean depthOnly;
	private boolean hasBones;
	public boolean doubleSided = false;

	public ModelRenderer() {
		super(true);
	}

	public ModelRenderer(String s, boolean flippedCullface) {
		this(s, flippedCullface, false, false);
	}

	public ModelRenderer(String s, boolean flippedCullface, boolean voxelize) {
		this(s, flippedCullface, voxelize, false);
	}

	public ModelRenderer(String s, boolean flippedCullface, boolean voxelize,
			boolean engineFolder) {
		this(new ObjLoader(s, flippedCullface, voxelize, engineFolder));
		if (voxelize) {
			Log.log(this, "voxelizing: " + s);
		}
		this.name = s;
		UberManager.getShader(Shader.TRANSFORM_TEXTURE);
		UberManager.getShader(Shader.TRANSFORM_SIMPLE);
	}

	public ModelRenderer(final FloatBuffer vertices, final FloatBuffer normals,
			FloatBuffer colors) {
		super(true);
		init(vertices, normals, colors, null, null, null);
	}

	public ModelRenderer(FloatBuffer vertices, FloatBuffer normals,
			FloatBuffer uvs, List<Material> materials, FloatBuffer weights,
			FloatBuffer boneIndices, IntBuffer[] indices) {
		super(true);
		this.materials = materials;
		init(vertices, normals, null, uvs, weights, boneIndices, indices);
		// TODO Auto-generated constructor stub
	}

	protected void init(final FloatBuffer vertices, final FloatBuffer normals,
			final FloatBuffer colors, final FloatBuffer uvs,
			final FloatBuffer weights, final FloatBuffer boneIndices,
			final IntBuffer... multiIndices) {

		hasNormals = normals != null;
		hasColors = colors != null;
		hasIndices = multiIndices != null && multiIndices.length > 0;
		hasUVs = uvs != null;
		vertexCount = vertices.capacity() / 3;
		hasBones = weights != null && weights.capacity() > 0;

		if (hasBones) {
			Log.log(this, "Weights: " + weights.toString());
			Log.log(this, "BoneIndices: " + boneIndices.toString());
		}
		Log.log(this, "Vertices: " + vertexCount);
		Log.log(this, "multiindices: " + multiIndices.length);
		// multiple index buffers
		if (hasIndices) {
			vboIndices = new int[multiIndices.length];
			indexCounts = new int[multiIndices.length];
			int i = 0;
			for (IntBuffer indices : multiIndices) {
				// Log.log(this, "indices: " + indices);
				indexCounts[i++] = indices.capacity();
			}
		}

		if (hasNormals)
			Log.log(this, "Normals: " + normals.capacity() / 3);
		if (hasUVs)
			Log.log(this, "UVS: " + uvs.capacity() / 2);
		RenderUpdater.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				GL2 gl = RenderUpdater.gl;
				if (vboVertices[0] == 0)
					gl.glGenBuffers(1, vboVertices, 0);
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertices[0]);
				gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertexCount * 3
						* Buffers.SIZEOF_FLOAT, vertices, isStatic());
				// if (vboIndices[0] == 0)
				if (hasIndices) {
					gl.glGenBuffers(vboIndices.length, vboIndices, 0);
					for (int i = 0; i < vboIndices.length; i++) {
						gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER,
								vboIndices[i]);
						gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER,
								indexCounts[i] * Buffers.SIZEOF_INT,
								multiIndices[i], isStatic());
					}
				}
				if (hasNormals) {
					if (vboNormals[0] == 0)
						gl.glGenBuffers(1, vboNormals, 0);
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboNormals[0]);
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, normals.capacity()
							* Buffers.SIZEOF_FLOAT, normals, isStatic());
				}
				if (hasUVs) {
					if (vboUvs[0] == 0)
						gl.glGenBuffers(1, vboUvs, 0);
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboUvs[0]);
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, uvs.capacity()
							* Buffers.SIZEOF_FLOAT, uvs, isStatic());
				}
				if (hasColors) {
					if (vboColors[0] == 0)
						gl.glGenBuffers(1, vboColors, 0);
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboColors[0]);
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, colors.capacity()
							* Buffers.SIZEOF_FLOAT, colors, isStatic());
				}
				if (hasBones) {
					gl.glGenBuffers(2, vboBones, 0);
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboBones[0]);
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, weights.capacity()
							* Buffers.SIZEOF_FLOAT, weights, isStatic());
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboBones[1]);
					gl.glBufferData(GL2.GL_ARRAY_BUFFER, boneIndices.capacity()
							* Buffers.SIZEOF_FLOAT, boneIndices, isStatic());
				}
			}
		});
		allocateBuffers(4, NUM_RENDERED_INST * 3);
	}

	protected int isStatic() {
		return GL2.GL_STATIC_DRAW;
	}

	public ModelRenderer(ObjLoader loader) {
		this(loader.vertices, loader.normals, loader.uvs, loader.materials,
				loader.weights, loader.boneIndices, loader.indices);
	}

	@Override
	public void init(GL2 gl) {
		transformShader = UberManager.getShader(Shader.TRANSFORM_TEXTURE);
		simpleTransform = UberManager.getShader(Shader.TRANSFORM_SIMPLE);
		depthOnly = ((RenderUpdater) Game.INSTANCE.loop.renderer).renderState.depthOnly;
		if (doubleSided)
			gl.glDisable(GL2.GL_CULL_FACE);
		// Optimize!!!!
		if (!depthOnly) {
			gl.glColor4f(1, 1, 1, 1);
			if (Game.WIREFRAME)
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
			else
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		}
		if (hasIndices)
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER,
					vboIndices[getIndexNumberToRender() % vboIndices.length]);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertices[0]);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
		if (!depthOnly) {
			if (hasNormals) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboNormals[0]);
				gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
				gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
			}
			if (hasColors) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboColors[0]);
				gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
				gl.glColorPointer(3, GL2.GL_FLOAT, 0, 0);
			}

		} else {
			if (simpleTransform != null) {
				simpleTransform.execute(gl);
				ShaderScript
						.setUniform("time", (float) Game.INSTANCE.loop.tick);
			}
		}
		if (hasUVs) {
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboUvs[0]);
			gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
			gl.glClientActiveTexture(GL2.GL_TEXTURE0);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);
		}
	}

	@Override
	public void end(GL2 gl) {
		if (doubleSided)
			gl.glEnable(GL2.GL_CULL_FACE);
		if (!depthOnly)
			Material.deactivate(gl);
		else if (ShaderScript.getActiveShader(gl) != null)
			ShaderScript.setUniform("hasMask", false);
		if (hasIndices)
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		if (hasUVs)
			gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		if (!depthOnly) {
			if (hasNormals)
				gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
			if (hasColors)
				gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		} else {
			if (simpleTransform != null)
				simpleTransform.end(gl);
		}
	}

	@Override
	public void draw(GL2 gl, List<GameObject> gos, float interp) {
		if (!ShaderScript.isShaderActivated() && transformShader != null) {
			transformShader.execute(gl);
		}
		if (hasBones) {
			int weightsLocation = gl.glGetAttribLocation(
					ShaderScript.getActiveShader(gl).shaderNum, "weights");
			int boneIndicesLocation = gl.glGetAttribLocation(
					ShaderScript.getActiveShader(gl).shaderNum, "boneIndices");
			if (weightsLocation >= 0 && boneIndicesLocation >= 0) {
				gl.glEnableVertexAttribArray(weightsLocation);
				gl.glEnableVertexAttribArray(boneIndicesLocation);
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboBones[0]);
				gl.glVertexAttribPointer(weightsLocation, 4, GL2.GL_FLOAT,
						false, 0, 0);
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboBones[1]);
				gl.glVertexAttribPointer(boneIndicesLocation, 4, GL2.GL_FLOAT,
						false, 0, 0);
			} else {
				// Log.err("Shader :" + ShaderScript.getActiveShader(gl)
				// + " has no attribute: weights,boneIndices");
			}
			ShaderScript.setUniformMatrix4("bones",
					TestSkinningRenderer.bonesUniform, true);
		}
		ShaderScript.setUniform("interp", RenderUpdater.interp);

		GameObjectType got = GameObjectType.getType(gos.get(0).getType());
		if (!depthOnly)
			if (gos.size() > 0) {
				renderState = got.renderState;
				ShaderScript.setUniform("shininess", got.shininess);
				ShaderScript.setUniform("reflective", got.reflective);
			}
		ShaderScript.setUniform("hasAirShader", got.airShader);
		renderInstanced(gl, gos);

		if (ShaderScript.isShaderActivated(transformShader)
				&& transformShader != null)
			transformShader.end(gl);
		currentMaterial = null;
	}

	private void renderInstanced(GL2 gl, List<GameObject> gos) {
		int start = 0;
		while (start < gos.size()) {
			int instancesNum = 0;
			rewindBuffers();
			for (int i = start; i < Math.min(gos.size(), start
					+ NUM_RENDERED_INST); i++) {
				instancesNum++;
				GameObject go = gos.get(i);
				putBuffer(go.size, go.pos, go.oldPos, go.color,
						go.rotationMatrixArray);
			}
			rewindBuffers();
			setUniforms(transformBuffers[0], transformBuffers[1],
					transformBuffers[2], transformBuffers[3],
					transformBuffers[4]);
			if (hasIndices) {
				for (int i = 0; i < indexCounts.length; i++) {
					if (renderState != null
							&& renderState.materials
							&& ((RenderUpdater) Game.INSTANCE.loop.renderer).renderState.materials)
						activateMaterial(gl, i);
					gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIndices[i]);
					gl.glDrawElementsInstanced(drawMode, indexCounts[i],
							GL2.GL_UNSIGNED_INT, null, instancesNum);
				}
			} else
				gl.glDrawArraysInstanced(drawMode, 0, vertexCount, instancesNum);
			start += NUM_RENDERED_INST;
		}
	}

	protected void activateMaterial(GL2 gl, int i) {
		if (materials == null) {
			return;
		}
		Material material = materials.get(i);
		if (!depthOnly) {
			if (currentMaterial != material) {
				gl.glColor4f(1, 1, 1, 1);
				material.activate(gl);
				currentMaterial = material;
			}
		} else {
			if (material != null)
				material.activateMaskMap(gl);
		}
	}

	protected void setUniforms(FloatBuffer scale, FloatBuffer translate,
			FloatBuffer translateOld, FloatBuffer color,
			FloatBuffer rotationMatrix) {
		ShaderScript.setUniform3fv("scaleArr", scale);
		ShaderScript.setUniform3fv("translateArr", translate);
		ShaderScript.setUniform3fv("translateOldArr", translateOld);
		ShaderScript.setUniform3fv("colorArr", color);
		ShaderScript
				.setUniformMatrix3("rotationMatrices", rotationMatrix, true);
	}

	protected int getIndexNumberToRender() {
		return 0;
	}

	private void putBuffer(float[]... puts) {
		for (int i = 0; i < transformBuffers.length; i++)
			transformBuffers[i].put(puts[i]);
	}

	private void rewindBuffers() {
		for (int i = 0; i < transformBuffers.length; i++)
			transformBuffers[i].rewind();
	}

	private void allocateBuffers(int num, int size) {
		transformBuffers = new FloatBuffer[num + 1];
		for (int i = 0; i < num; i++)
			transformBuffers[i] = FloatBuffer.allocate(size);
		// rotation matrix
		transformBuffers[num] = FloatBuffer.allocate(NUM_RENDERED_INST * 9);
	}

	@Override
	public void draw(GL2 gl) {
		draw(gl, new ArrayList<GameObject>() {
			{
				add(RenderUpdater.cgo);
			}
		}, 0);
	}

	@Override
	public void drawSimple(GL2 gl) {
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertices[0]);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

		if (hasIndices)
			for (int i = 0; i < indexCounts.length; i++) {
				gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, vboIndices[i]);
				gl.glDrawElements(drawMode, indexCounts[i],
						GL2.GL_UNSIGNED_INT, 0);
			}
		else {
			gl.glDrawArraysInstanced(drawMode, 0, vertexCount, 1);
		}
		if (hasIndices)
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
	}

	public List<Material> getMaterials() {
		return materials;
	}

	public String getName() {
		return this.name;
	}
}
