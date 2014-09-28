package rendering;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import shader.ShaderScript;
import shader.ShaderUtil;
import util.GLUtil;
import util.Log;

import com.jogamp.common.nio.Buffers;

//TODO interleaved VBO's
//TODO indexed VBO's (drawElements)
//TODO set cull backfaces (yes/no)
//TODO shadowShader (no cols/normals/textures ...)
//TODO integrate textures
public class RenderInformation {
	private static Map<Integer, ShaderScript> shaders = new HashMap<Integer, ShaderScript>();

	private ArrayList<VBO> vbos = new ArrayList<VBO>();
	private ShaderScript shader;
	private int numOfVertices;
	private int drawType = GL3.GL_TRIANGLES;

	public void add(String name, float[] data, int perVertexSize) {
		vbos.add(new VBOFloat(name, perVertexSize, data));
	}

	public void setDrawType(int drawType) {
		this.drawType = drawType;
	}

	public void render(GL2GL3 gl, GLUtil glutil) {
		if (shader == null) {
			init(gl);
			return;
		}

		shader.execute(gl);
		ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
				glutil.getModelViewProjection(), true);
		int attrib = 0;
		for (VBO vbo : vbos) {
			gl.glBindBuffer(vbo.getType(), vbo.gpuBuffer);
			gl.glEnableVertexAttribArray(attrib);
			gl.glVertexAttribPointer(attrib, vbo.perVertexSize, GL3.GL_FLOAT,
					false, 0, 0);
			attrib++;
		}
		gl.glDrawArrays(drawType, 0, numOfVertices);
		for (int i = 0; i < vbos.size(); i++)
			gl.glDisableVertexAttribArray(i);
		shader.end(gl);
	}

	public void init(GL2GL3 gl) {
		numOfVertices = vbos.get(0).getNumOfVertices();
		for (VBO vbo : vbos) {
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
		sb.append("out vec3 col;\n");
		sb.append("uniform mat4 modelviewprojection;\n");
		sb.append("void main(){\n");
		if (hasVBO("color"))
			sb.append("\tcol = color;\n");
		else
			sb.append("\tcol = vec3(1,1,1);\n");
		sb.append("\tgl_Position = modelviewprojection*vec4(vertex,1.0);\n");
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
		private String name;
		protected int perVertexSize;
		private int gpuBuffer = -1;
		private Buffer data;

		public VBO(String name, int perVertexSize) {
			this.name = name;
			this.perVertexSize = perVertexSize;
		}

		public void init(GL2GL3 gl) {
			if (gpuBuffer > -1)
				return;
			int dataBuffer[] = new int[1];
			gl.glGenBuffers(1, dataBuffer, 0);
			int type = getType();
			gl.glBindBuffer(type, dataBuffer[0]);
			gl.glBufferData(type, data.capacity() * getGPUSize(), getData(),
					GL3.GL_STATIC_DRAW);
			gl.glBindBuffer(type, 0);
			gpuBuffer = dataBuffer[0];
		}

		protected abstract int getType();

		protected abstract int getGPUSize();

		public int getNumOfVertices() {
			return data.capacity() / perVertexSize;
		}

		protected Buffer getData() {
			this.data.rewind();
			return data;
		}

		public void dispose(GL2GL3 gl) {
			Log.log(this, "disposing");
			gl.glDeleteBuffers(1, new int[] { gpuBuffer }, 0);
		}
	}

	private static class VBOFloat extends VBO {

		public VBOFloat(String name, int perVertexSize, float[] data) {
			super(name, perVertexSize);
			super.data = FloatBuffer.wrap(data);
		}

		@Override
		protected int getGPUSize() {
			return Buffers.SIZEOF_FLOAT;
		}

		@Override
		protected int getType() {
			return GL3.GL_ARRAY_BUFFER;
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
