package rendering;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import shader.ShaderScript;
import shader.ShaderUtil;
import util.GLUtil;
import util.Log;
import world.GameObject;

import com.jogamp.common.nio.Buffers;

//TODO interleaved VBO's
//TODO indexed VBO's (drawElements)
//TODO set cull backfaces (yes/no)
//TODO shadowShader (no cols/normals/textures ...)
//TODO integrate textures
public class RenderInformation {

	private static Map<Integer, ShaderScript> shaders = new HashMap<Integer, ShaderScript>();
	private static final int MAX_INSTANCES = 10000;

	private List<VBOFloat> vbos = new ArrayList<VBOFloat>();
	private ShaderScript shader;
	private int numOfVertices;
	private int drawType = GL3.GL_TRIANGLES;
	private static InstanceVBO instanceBuffer;

	public void add(String name, float[] data, int perVertexSize) {
		vbos.add(new VBOFloat(name, perVertexSize, data));
	}

	public void setDrawType(int drawType) {
		this.drawType = drawType;
	}

	public void render(GL3 gl, GLUtil glutil, List<GameObject> gos) {
		if (shader == null) {
			if (instanceBuffer == null) {
				instanceBuffer = new InstanceVBO("instance");
				instanceBuffer.init(gl);
			}
			init(gl);
			return;
		}

		shader.execute(gl);
		ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
				glutil.getModelViewProjection(), true);
		int attrib = 0;
		for (VBO vbo : vbos)
			vbo.bind(attrib++, gl);
		int currentInstance = 0;
		do {
			int renderThisRound = Math.min(gos.size() - currentInstance,
					MAX_INSTANCES);
			instanceBuffer.bind(
					attrib,
					(GL3) gl,
					gos.subList(currentInstance, currentInstance
							+ renderThisRound));
			gl.glDrawArraysInstanced(drawType, 0, numOfVertices,
					renderThisRound);
			currentInstance += renderThisRound;
		} while (currentInstance < gos.size());
		for (int i = 0; i < vbos.size() + instanceBuffer.perInstanceSize / 3; i++)
			gl.glDisableVertexAttribArray(i);
		shader.end(gl);
	}

	public void init(GL2GL3 gl) {
		numOfVertices = vbos.get(0).getNumOfVertices();
		for (VBOFloat vbo : vbos) {
			if (vbo.getNumOfVertices() != numOfVertices)
				throw new RuntimeException("VBO " + vbo.name
						+ " has not the same amount of vertices: "
						+ vbo.getNumOfVertices() + "!=" + numOfVertices);
			vbo.init(gl);
		}
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
		for (VBO vbo : vbos)
			sb.append("layout(location = " + (location++) + ") in vec"
					+ vbo.perVertexSize + " " + vbo.name + ";\n");
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instancePos;\n");
		sb.append("layout(location = " + (location++)
				+ ") in vec3 instanceScale;\n");
		sb.append("layout(location = " + (location++)
				+ ") in mat3 instanceRotation;\n");
		sb.append("out vec3 col;\n");
		sb.append("uniform mat4 modelviewprojection;\n");
		sb.append("void main(){\n");
		if (hasVBO("color"))
			sb.append("\tcol = color;\n");
		else
			sb.append("\tcol = vec3(1,1,1);\n");
		sb.append("\tvec3 transformed = (transpose(instanceRotation)*vertex)*instanceScale+instancePos;\n");
		sb.append("\tgl_Position = modelviewprojection*vec4(transformed,1.0);\n");
		sb.append("}\n");

		// FRAGMENT shader
		sb.append("//fragment\n#version 330\n");
		sb.append("in vec3 col;\n");
		sb.append("out vec4 outputColor;\n");
		sb.append("void main(){\n");
		sb.append("\toutputColor = vec4(col, 1.0f);\n");
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
		return descr;
	}

	private boolean hasVBO(String name) {
		for (VBO vbo : vbos)
			if (vbo.name.equals(name))
				return true;
		return false;
	}

	private abstract static class VBO {
		protected String name;
		protected int perVertexSize;
		private int gpuBuffer = -1;
		private Buffer data;
		protected boolean isStatic = true;
		protected int gpuSize = Buffers.SIZEOF_FLOAT;
		protected int type = GL3.GL_FLOAT;
		protected int arrayType = GL3.GL_ARRAY_BUFFER;

		public VBO(String name, int perVertexSize) {
			this.name = name;
			this.perVertexSize = perVertexSize;
		}

		public void bind(int attrib, GL2GL3 gl) {
			gl.glBindBuffer(arrayType, gpuBuffer);
			gl.glEnableVertexAttribArray(attrib);
			gl.glVertexAttribPointer(attrib, perVertexSize, type, false, 0, 0);
		}

		public void init(GL2GL3 gl) {
			if (gpuBuffer > -1)
				return;
			int dataBuffer[] = new int[1];
			gl.glGenBuffers(1, dataBuffer, 0);
			gl.glBindBuffer(arrayType, dataBuffer[0]);
			gl.glBufferData(arrayType, data.capacity() * gpuSize, getData(),
					isStatic ? GL3.GL_STATIC_DRAW : GL3.GL_DYNAMIC_DRAW);
			gl.glBindBuffer(arrayType, 0);
			gpuBuffer = dataBuffer[0];
		}

		protected Buffer getData() {
			this.data.rewind();
			return data;
		}

		protected void dispose(GL2GL3 gl) {
			Log.log(this, "disposing");
			gl.glDeleteBuffers(1, new int[] { gpuBuffer }, 0);
		}
	}

	private static class VBOFloat extends VBO {

		public VBOFloat(String name, int perVertexSize, float[] data) {
			super(name, perVertexSize);
			super.data = FloatBuffer.wrap(data);
		}

		private int getNumOfVertices() {
			return super.data.capacity() / perVertexSize;
		}
	}

	public static class InstanceVBO extends VBO {

		private int perInstanceSize;

		public InstanceVBO(String name) {
			super(name, 3 + 3 + 9);
			super.isStatic = false;
			this.perInstanceSize = perVertexSize;
			super.data = FloatBuffer.allocate(MAX_INSTANCES * perInstanceSize);
		}

		public void bind(int attrib, GL3 gl, List<GameObject> gos) {
			gl.glBindBuffer(arrayType, super.gpuBuffer);
			for (int i = 0; i < 5; i++) {
				gl.glEnableVertexAttribArray(attrib + i);
				gl.glVertexAttribPointer(attrib + i, 3, type, false,
						perInstanceSize * gpuSize, i * 3 * gpuSize);
				gl.glVertexAttribDivisor(attrib + i, 1);
			}
			super.data.rewind();
			for (GameObject go : gos) {
				((FloatBuffer) super.data).put(go.pos);
				((FloatBuffer) super.data).put(go.size);
				((FloatBuffer) super.data).put(go.rotationMatrixArray);
			}
			gl.glBufferSubData(arrayType, 0, gos.size() * perInstanceSize
					* gpuSize, getData());
		}
	}

	public void dispose(GL2GL3 gl) {
		Log.log(this, "disposing");
		for (VBO vbo : vbos)
			vbo.dispose(gl);
		if (shader != null)
			shader.deleteShader(gl);
	}

}
