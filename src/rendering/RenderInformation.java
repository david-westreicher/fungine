package rendering;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import rendering.material.Material;
import rendering.material.Material.Map;
import rendering.util.VBO;
import rendering.util.VBO.IndexVBO;
import rendering.util.VBO.InstanceVBO;
import rendering.util.VBO.VBOFloat;
import rendering.util.VBO.VertexAttribute;
import shader.ShaderScript;
import shader.ShaderUtil;
import util.Log;
import util.ObjLoader;
import world.GameObject;

//TODO interleaved VBO's
//TODO shadowShader (no cols/normals/textures ...)
public class RenderInformation {
	private static HashMap<Integer, ShaderScript> shaders = new HashMap<Integer, ShaderScript>();
	private static HashMap<String, ObjLoader> objMap = new HashMap<String, ObjLoader>();
	private static InstanceVBO instanceBuffer;
	private List<VBOFloat> vertexAttribs = new ArrayList<VBOFloat>();
	private IndexVBO[] multiIndices = null;
	private ShaderScript shader;
	private int numOfVertices = -1;
	private int drawType = GL3.GL_TRIANGLES;
	private boolean wireFrame = false;
	private List<Material> materials;
	private int description;
	private Material currentMaterial;
	private boolean gammaCorrection;

	public void addVA(VBO.VertexAttribute name, float[] data, int perVertexSize) {
		vertexAttribs.add(new VBOFloat(name, perVertexSize, data));
	}

	public void setWireframe(boolean wf) {
		this.wireFrame = wf;
	}

	public void setIndexed(int[]... multiIndices) {
		this.multiIndices = new IndexVBO[multiIndices.length];
		for (int i = 0; i < multiIndices.length; i++)
			this.multiIndices[i] = new IndexVBO(multiIndices[i]);
	}

	public void setDrawType(int drawType) {
		this.drawType = drawType;
	}

	public void setMaterials(List<Material> materials) {
		this.materials = (materials == null || materials.size() == 0 ? null
				: materials);
	}

	public void render(GL3 gl, FloatBuffer mvp, List<GameObject> gos) {
		if (instanceBuffer == null) {
			instanceBuffer = new InstanceVBO();
			instanceBuffer.init(gl);
		}
		if (numOfVertices == -1) {
			init(gl);
			return;
		}
		shader = shaders.get(description);
		if (shader == null) {
			compileShader(gl);
			return;
		}
		if (wireFrame)
			gl.glPolygonMode(GL3.GL_FRONT_AND_BACK, GL3.GL_LINE);
		shader.execute(gl);
		ShaderScript.setUniformMatrix4(gl, "modelviewprojection", mvp, true);
		int attrib = 0;
		for (VBOFloat vbo : vertexAttribs)
			vbo.bind(attrib++, gl);
		if (gos.size() > 0)
			renderInstanced(gl, attrib, gos);
		for (int i = 0; i < vertexAttribs.size()
				+ InstanceVBO.PER_INSTANCE_SIZE / 3; i++)
			gl.glDisableVertexAttribArray(i);
		shader.end(gl);
		if (wireFrame)
			gl.glPolygonMode(GL3.GL_FRONT_AND_BACK, GL3.GL_FILL);
	}

	private void renderInstanced(GL3 gl, int attrib, List<GameObject> gos) {
		int currentInstance = 0;
		do {
			int renderThisRound = Math.min(gos.size() - currentInstance,
					InstanceVBO.MAX_INSTANCES);
			List<GameObject> gosRound = gos.subList(currentInstance,
					currentInstance + renderThisRound);
			instanceBuffer.bind(attrib, (GL3) gl, gosRound);
			if (multiIndices == null)
				gl.glDrawArraysInstanced(drawType, 0, numOfVertices,
						renderThisRound);
			else {
				int materialIndex = 0;
				currentMaterial = null;
				for (IndexVBO indices : multiIndices) {
					indices.bind(gl);
					activateMaterial(materialIndex++, gl);
					gl.glDrawElementsInstanced(drawType,
							indices.getIndicesNum(), GL3.GL_UNSIGNED_INT, null,
							renderThisRound);
				}
				IndexVBO.unbind(gl);
			}
			currentInstance += renderThisRound;
		} while (currentInstance < gos.size());
	}

	private void activateMaterial(int materialIndex, GL3 gl) {
		if (materials != null && materialIndex < materials.size()) {
			Material newMaterial = materials.get(materialIndex);
			if (currentMaterial != newMaterial) {
				currentMaterial = newMaterial;
				newMaterial.activate(gl);
			}
		}
	}

	public void init(GL2GL3 gl) {
		numOfVertices = vertexAttribs.get(0).getNumOfVertices();
		for (VBOFloat vbo : vertexAttribs) {
			if (vbo.getNumOfVertices() != numOfVertices)
				throw new RuntimeException("VBO " + vbo.name
						+ " has not the same amount of vertices: "
						+ vbo.getNumOfVertices() + "!=" + numOfVertices);
			vbo.init(gl);
		}
		if (multiIndices != null)
			for (IndexVBO indices : multiIndices)
				indices.init(gl);
		description = getDescription();
	}

	private void compileShader(GL3 gl) {
		ShaderUtil.compileFromString(gl, getShaderSource(), description + "-"
				+ descriptionToString(),
				new ShaderUtil.ShaderCompiledListener() {
					@Override
					public void shaderCompiled(ShaderScript shaderprogram) {
						shaders.put(description, shaderprogram);
						RenderInformation.this.shader = shaderprogram;
					}
				});
	}

	private String getShaderSource() {
		boolean hasTexture = hasVBO(VertexAttribute.UV) && materials != null;
		StringBuilder sb = new StringBuilder();
		// #################
		// VERTEX shader
		// #################
		sb.append("#version 330\n");

		// UNIFORMS
		sb.append("uniform mat4 modelviewprojection;\n");

		// VERTEX ATTRIBUTES
		int location = 0;
		for (VBOFloat vbo : vertexAttribs)
			sb.append("layout(location = " + (location++) + ") in vec"
					+ vbo.perVertexSize + " " + vbo.name + ";\n");

		// INSTANCE ATTRIBUTES
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instancePos;\n");
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instanceColor;\n");
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instanceScale;\n");
		sb.append("layout(location = " + (location++)
				+ ") in mat3 instanceRotation;\n");

		sb.append("out vec3 col;\n");
		if (hasVBO(VertexAttribute.NORMAL))
			sb.append("out vec3 normalVec;\n");
		if (hasVBO(VertexAttribute.UV))
			sb.append("out vec2 uvCoord;\n");
		sb.append("void main(){\n");
		if (hasVBO(VertexAttribute.NORMAL))
			sb.append("\tnormalVec = normalize(transpose(instanceRotation)*"
					+ VertexAttribute.NORMAL + ");\n");
		if (hasVBO(VertexAttribute.COLOR))
			sb.append("\tcol = " + VertexAttribute.COLOR + "*instanceColor;\n");
		else
			sb.append("\tcol = instanceColor;\n");
		if (hasVBO(VertexAttribute.UV))
			sb.append("\tuvCoord=" + VertexAttribute.UV + ";\n");
		sb.append("\tvec3 transformed = (transpose(instanceRotation)*"
				+ VertexAttribute.POSITION + ")*instanceScale+instancePos;\n");
		sb.append("\tgl_Position = modelviewprojection*vec4(transformed,1.0);\n");
		sb.append("}\n");

		// #################
		// FRAGMENT shader
		// #################
		sb.append(ShaderUtil.FRAGMENT_SPLITTER + "\n");
		sb.append("#version 330\n");
		sb.append("in vec3 col;\n");
		if (hasVBO(VertexAttribute.UV))
			sb.append("in vec2 uvCoord;\n");
		if (hasVBO(VertexAttribute.NORMAL))
			sb.append("in vec3 normalVec;\n");

		// TEXTURES
		for (Map m : Material.mapValues)
			if (hasMap(m))
				sb.append("uniform sampler2D " + m + ";\n");

		sb.append("layout(location = 0) out vec3 outputColor;\n");
		sb.append("layout(location = 1) out vec3 outputNormal;\n");
		sb.append("void main(){\n");

		if (hasVBO(VertexAttribute.NORMAL)) {
			if (hasMap(Map.NORMAL_MAP)) {
				sb.append("\tvec3 normalMap = texture2D(" + Map.NORMAL_MAP
						+ ",uvCoord).rgb-vec3(0.5);\n");
				sb.append("\tnormalMap.z*=0.25;\n");
				sb.append("\toutputNormal = normalize(normalVec+normalMap);\n");
				// sb.append("\toutputNormal = normalMap;\n");
			} else
				sb.append("\toutputNormal = normalVec;\n");
		} else
			sb.append("\toutputNormal = vec3(1,0,0);\n");

		if (hasTexture)
			sb.append("\toutputColor = texture(" + Map.COLOR_MAP
					+ ", uvCoord).rgb*col;\n");
		else
			sb.append("\toutputColor = col;\n");
		if (gammaCorrection)
			sb.append("\toutputColor = pow(outputColor,vec3(2.2));\n");
		sb.append("}\n");
		Log.log(this, sb.toString());
		return sb.toString();
	}

	private int getDescription() {
		int descr = 0;
		if (hasVBO(VertexAttribute.POSITION))
			descr |= 1 << 0;
		if (hasVBO(VertexAttribute.COLOR))
			descr |= 1 << 1;
		if (hasVBO(VertexAttribute.UV))
			descr |= 1 << 2;
		if (hasVBO(VertexAttribute.NORMAL))
			descr |= 1 << 3;
		if (hasMap(Map.NORMAL_MAP))
			descr |= 1 << 4;
		if (hasMap(Map.COLOR_MAP))
			descr |= 1 << 5;
		return descr;
	}

	private boolean hasMap(Map map) {
		if (materials == null)
			return false;
		// TODO set shaderuniform hasMapFactor = 1|0;
		Set<Map> hasMaps = new HashSet<Map>();
		for (Material m : materials)
			if (m.has(map))
				hasMaps.add(map);
		for (Map hasMap : hasMaps)
			for (Material m : materials)
				if (!m.has(hasMap)) {
					m.set(hasMap, "img/pano_b.jpg");
					// Log.err(this, m + "doesn't have " + map);
				}

		return materials.get(0).has(map);
	}

	private String descriptionToString() {
		StringBuilder sb = new StringBuilder();
		if ((description & (1 << 0)) > 0)
			sb.append(VertexAttribute.POSITION + ", ");
		if ((description & (1 << 1)) > 0)
			sb.append(VertexAttribute.COLOR + ", ");
		if ((description & (1 << 2)) > 0)
			sb.append(VertexAttribute.UV + ", ");
		if ((description & (1 << 3)) > 0)
			sb.append(VertexAttribute.NORMAL + ", ");
		if ((description & (1 << 4)) > 0)
			sb.append(Map.NORMAL_MAP + ", ");
		if ((description & (1 << 5)) > 0)
			sb.append(Map.COLOR_MAP + ", ");
		return sb.toString();
	}

	private boolean hasVBO(VertexAttribute name) {
		for (VBOFloat vbo : vertexAttribs)
			if (vbo.name == name)
				return true;
		return false;
	}

	public void dispose(GL2GL3 gl) {
		Log.log(this, "disposing");
		shaders.remove(getDescription());
		for (VBO vbo : vertexAttribs)
			vbo.dispose(gl);
		if (multiIndices != null)
			for (IndexVBO indices : multiIndices)
				indices.dispose(gl);
		if (shader != null)
			shader.deleteShader(gl);
	}

	public static RenderInformation fromObj(String objFile) {
		ObjLoader obj = objMap.get(objFile);
		if (obj == null) {
			try {
				obj = new ObjLoader(objFile, false, false);
			} catch (RuntimeException e) {
				e.printStackTrace();
				return null;
			}
			objMap.put(objFile, obj);
		}
		RenderInformation ri = new RenderInformation();
		float[] verts = obj.vertices.array();

		ri.addVA(VertexAttribute.POSITION, verts, 3);
		int[][] multiIndices = new int[obj.indices.length][];
		for (int i = 0; i < obj.indices.length; i++)
			multiIndices[i] = obj.indices[i].array();
		if (obj.correctUVs.size() > 0)
			ri.addVA(VertexAttribute.UV, obj.uvs.array(), 2);
		if (obj.correctNormals.size() > 0)
			ri.addVA(VertexAttribute.NORMAL, obj.normals.array(), 3);
		ri.setIndexed(multiIndices);
		ri.setDrawType(GL3.GL_TRIANGLES);
		ri.setMaterials(obj.materials);
		ri.setGammaCorrection(true);
		return ri;
	}

	public void setGammaCorrection(boolean b) {
		gammaCorrection = b;
	}

}
