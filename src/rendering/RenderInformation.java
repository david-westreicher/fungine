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
import rendering.material.Material;
import shader.ShaderScript;
import shader.ShaderUtil;
import util.GLUtil;
import util.Log;
import util.MathHelper;
import util.ObjLoader;
import world.GameObject;

//TODO interleaved VBO's
//TODO shadowShader (no cols/normals/textures ...)
//TODO integrate textures
public class RenderInformation {

	private static Map<Integer, ShaderScript> shaders = new HashMap<Integer, ShaderScript>();
	private static Map<String, ObjLoader> objMap = new HashMap<String, ObjLoader>();
	private static InstanceVBO instanceBuffer;
	private List<VBOFloat> vertexAttribs = new ArrayList<VBOFloat>();
	private IndexVBO[] multiIndices = null;
	private ShaderScript shader;
	private int numOfVertices = 0;
	private int drawType = GL3.GL_TRIANGLES;
	private boolean wireFrame = false;
	private List<Material> materials;

	public void addVA(String name, float[] data, int perVertexSize) {
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
		if (numOfVertices == 0) {
			init(gl);
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
		int currentInstance = 0;
		do {
			int renderThisRound = Math.min(gos.size() - currentInstance,
					InstanceVBO.MAX_INSTANCES);
			instanceBuffer.bind(
					attrib,
					(GL3) gl,
					gos.subList(currentInstance, currentInstance
							+ renderThisRound));
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
		for (int i = 0; i < vertexAttribs.size()
				+ instanceBuffer.perInstanceSize / 3; i++)
			gl.glDisableVertexAttribArray(i);
		if (materials != null)
			gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
		shader.end(gl);
		if (wireFrame)
			gl.glPolygonMode(GL3.GL_FRONT_AND_BACK, GL3.GL_FILL);
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
		final int description = getDescription();
		shader = shaders.get(description);
		if (shader == null)
			ShaderUtil.compileFromString(gl, getShaderSource(), description
					+ "", new ShaderUtil.ShaderCompiledListener() {
				@Override
				public void shaderCompiled(ShaderScript shaderprogram) {
					shaders.put(description, shaderprogram);
					RenderInformation.this.shader = shaderprogram;
				}
			});
	}

	private String getShaderSource() {
		StringBuilder sb = new StringBuilder();
		// VERTEX shader
		sb.append("#version 330\n");
		int location = 0;
		// sb.append("layout(location = 1) in vec3 color;\n");
		for (VBOFloat vbo : vertexAttribs)
			sb.append("layout(location = " + (location++) + ") in vec"
					+ vbo.perVertexSize + " " + vbo.name + ";\n");
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instancePos;\n");
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instanceScale;\n");
		sb.append("layout(location = " + (location++)
				+ ") in mat3 instanceRotation;\n");
		if (hasVBO("color"))
			sb.append("out vec3 col;\n");
		if (hasVBO("uv"))
			sb.append("out vec2 uvCoord;\n");
		sb.append("uniform mat4 modelviewprojection;\n");
		sb.append("void main(){\n");
		if (hasVBO("color"))
			sb.append("\tcol = color;\n");
		else if (!hasVBO("uv") || materials == null)
			sb.append("\tcol = vec3(0.5f,0.5f,0.5f);\n");
		if (hasVBO("uv"))
			sb.append("\tuvCoord=uv;\n");
		sb.append("\tvec3 transformed = (transpose(instanceRotation)*vertex)*instanceScale+instancePos;\n");
		sb.append("\tgl_Position = modelviewprojection*vec4(transformed,1.0);\n");
		sb.append("}\n");

		// FRAGMENT shader
		sb.append("//fragment\n#version 330\n");
		if (hasVBO("color"))
			sb.append("in vec3 col;\n");
		if (hasVBO("uv"))
			sb.append("in vec2 uvCoord;\n");
		if (materials != null)
			sb.append("uniform sampler2D tex;\n");
		sb.append("out vec4 outputColor;\n");
		sb.append("void main(){\n");
		if (hasVBO("color"))
			sb.append("\toutputColor = vec4(col, 1.0f);\n");
		if (hasVBO("uv") && materials != null)
			sb.append("\toutputColor = texture(tex, uvCoord);\n");
		sb.append("}\n");
		Log.log(this, sb.toString());
		return sb.toString();
	}

	private int getDescription() {
		int descr = 0;
		// vertices
		if (hasVBO("vertex"))
			descr |= 1 << 0;
		if (hasVBO("color"))
			descr |= 1 << 1;
		if (hasVBO("uv"))
			descr |= 1 << 2;
		return descr;
	}

	private boolean hasVBO(String name) {
		for (VBOFloat vbo : vertexAttribs)
			if (vbo.name.equals(name))
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
			obj = new ObjLoader(objFile, false, false);
			objMap.put(objFile, obj);
		}
		RenderInformation ri = new RenderInformation();
		float[] verts = MathHelper.toArray(obj.correctVertices);
		ri.addVA("vertex", verts, 3);
		int[][] multiIndices = new int[obj.indices.length][];
		for (int i = 0; i < obj.indices.length; i++)
			multiIndices[i] = obj.indices[i].array();
		ri.addVA("uv", MathHelper.toArray(obj.correctUVs), 2);
		ri.setIndexed(multiIndices);
		ri.setDrawType(GL3.GL_TRIANGLES);
		ri.setMaterials(obj.materials);
		return ri;
	}

}
