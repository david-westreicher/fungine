package rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import rendering.VBO.IndexVBO;
import rendering.VBO.InstanceVBO;
import rendering.VBO.VBOFloat;
import rendering.VBO.VertexAttribute;
import rendering.material.Material;
import shader.ShaderScript;
import shader.ShaderUtil;
import util.GLUtil;
import util.Log;
import util.ObjLoader;
import world.GameObject;

//TODO interleaved VBO's
//TODO shadowShader (no cols/normals/textures ...)
public class RenderInformation {
	private static Map<Integer, ShaderScript> shaders = new HashMap<Integer, ShaderScript>();
	private static Map<String, ObjLoader> objMap = new HashMap<String, ObjLoader>();
	private static InstanceVBO instanceBuffer;
	private List<VBOFloat> vertexAttribs = new ArrayList<VBOFloat>();
	private IndexVBO[] multiIndices = null;
	private ShaderScript shader;
	private int numOfVertices = -1;
	private int drawType = GL3.GL_TRIANGLES;
	private boolean wireFrame = false;
	private List<Material> materials;
	private int description;

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
		this.materials = materials;
	}

	public void render(GL3 gl, GLUtil glutil, List<GameObject> gos) {
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
		ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
				glutil.getModelViewProjection(), true);
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
				for (IndexVBO indices : multiIndices) {
					indices.bind(gl);
					activateMaterial(materialIndex++, gl);
					gl.glDrawElementsInstanced(drawType,
							indices.getIndicesNum(), GL3.GL_UNSIGNED_INT, null,
							renderThisRound);
				}
			}
			currentInstance += renderThisRound;
		} while (currentInstance < gos.size());
	}

	private void activateMaterial(int materialIndex, GL3 gl) {
		if (materials != null && materialIndex < materials.size())
			materials.get(materialIndex).activate(gl);
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
		ShaderUtil.compileFromString(gl, getShaderSource(), description + "",
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
				+ ") in vec3 instanceScale;\n");
		sb.append("layout(location = " + (location++)
				+ ") in mat3 instanceRotation;\n");

		if (hasVBO(VertexAttribute.COLOR))
			sb.append("out vec3 col;\n");
		if (hasVBO(VertexAttribute.UV))
			sb.append("out vec2 uvCoord;\n");
		sb.append("void main(){\n");
		if (hasVBO(VertexAttribute.COLOR))
			sb.append("\tcol = " + VertexAttribute.COLOR + ";\n");
		else if (!hasTexture)
			sb.append("\tcol = vec3(0.5f,0.5f,0.5f);\n");
		if (hasVBO(VertexAttribute.UV))
			sb.append("\tuvCoord=" + VertexAttribute.UV + ";\n");
		sb.append("\tvec3 transformed = (transpose(instanceRotation)*"
				+ VertexAttribute.POSITION + ")*instanceScale+instancePos;\n");
		sb.append("\tgl_Position = modelviewprojection*vec4(transformed,1.0);\n");
		sb.append("}\n");

		// FRAGMENT shader
		sb.append("//fragment\n#version 330\n");
		if (hasVBO(VertexAttribute.COLOR))
			sb.append("in vec3 col;\n");
		if (hasVBO(VertexAttribute.UV))
			sb.append("in vec2 uvCoord;\n");
		if (hasTexture)
			sb.append("uniform sampler2D tex;\n");
		sb.append("out vec4 outputColor;\n");
		sb.append("void main(){\n");
		if (hasVBO(VertexAttribute.COLOR) && !hasTexture)
			sb.append("\toutputColor = vec4(col, 1.0f);\n");
		if (hasTexture)
			sb.append("\toutputColor = texture(tex, uvCoord);\n");
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
		return descr;
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
		ri.setIndexed(multiIndices);
		// Log.log(RenderInformation.class, Arrays.toString(multiIndices[0]));
		ri.setDrawType(GL3.GL_TRIANGLES);
		ri.setMaterials(obj.materials);
		return ri;
	}

}
