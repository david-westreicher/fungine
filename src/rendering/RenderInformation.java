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
	public enum InstanceData {
		POSITION, SCALE, ROTATION
	};

	private static Map<Integer, ShaderScript> shaders = new HashMap<Integer, ShaderScript>();
	private static final int MAX_INSTANCES = 200000;

	private ArrayList<VBO> vbos = new ArrayList<VBO>();
	private ShaderScript shader;
	private int numOfVertices;
	private int drawType = GL3.GL_TRIANGLES;
	private static VBOInstanceAttr instanceBufferPos;
	private static VBOInstanceAttr instanceBufferScale;
	private static VBOInstanceAttr instanceBufferRotation;

	public void add(String name, float[] data, int perVertexSize) {
		vbos.add(new VBOFloat(name, perVertexSize, data));
	}

	public void setDrawType(int drawType) {
		this.drawType = drawType;
	}

	public void render(GL3 gl, GLUtil glutil, List<GameObject> gos) {
		if (shader == null) {
			if (instanceBufferPos == null) {
				instanceBufferPos = new VBOInstanceAttr("instancePos",
						InstanceData.POSITION);
				instanceBufferPos.init(gl);
				instanceBufferScale = new VBOInstanceAttr("instanceScale",
						InstanceData.SCALE);
				instanceBufferScale.init(gl);
				instanceBufferRotation = new VBOInstanceAttr(
						"instanceRotation", InstanceData.ROTATION);
				instanceBufferRotation.init(gl);
			}
			init(gl);
			return;
		}

		shader.execute(gl);
		int attrib = 0;
		for (VBO vbo : vbos)
			vbo.bind(attrib++, gl);
		instanceBufferPos.bind(attrib++, (GL3) gl, gos);
		instanceBufferScale.bind(attrib++, (GL3) gl, gos);
		instanceBufferRotation.bind(attrib++, (GL3) gl, gos);
		// for (GameObject go : gos) {
		// glutil.glPushMatrix();
		// glutil.glTranslatef(go.pos[0], go.pos[1], go.pos[2]);
		// glutil.scale(go.size[0], go.size[1], go.size[2]);
		// glutil.multiply(go.rotationMatrix);
		ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
				glutil.getModelViewProjection(), true);
		// gl.glDrawArrays(drawType, 0, numOfVertices);
		gl.glDrawArraysInstanced(drawType, 0, numOfVertices, gos.size());
		// glutil.glPopMatrix();
		// }
		for (int i = 0; i < vbos.size() + 1; i++)
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
		sb.append("layout(location = " + (location++) + ") in vec"
				+ instanceBufferPos.perVertexSize + " "
				+ instanceBufferPos.name + ";\n");
		sb.append("layout(location = " + (location++) + ") in vec"
				+ instanceBufferScale.perVertexSize + " "
				+ instanceBufferScale.name + ";\n");
		sb.append("layout(location = " + (location++) + ") in mat3" + " "
				+ instanceBufferRotation.name + ";\n");
		sb.append("out vec3 col;\n");
		sb.append("uniform mat4 modelviewprojection;\n");
		sb.append("void main(){\n");
		if (hasVBO("color"))
			sb.append("\tcol = color;\n");
		else
			sb.append("\tcol = vec3(1,1,1);\n");
		sb.append("\tgl_Position = modelviewprojection*vec4((transpose(instanceRotation)*vertex)*instanceScale+instancePos,1.0);\n");
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

		public VBO(String name, int perVertexSize) {
			this.name = name;
			this.perVertexSize = perVertexSize;
		}

		public void bind(int attrib, GL2GL3 gl) {
			gl.glBindBuffer(getArrayType(), gpuBuffer);
			gl.glEnableVertexAttribArray(attrib);
			gl.glVertexAttribPointer(attrib, perVertexSize, getType(), false,
					0, 0);
		}

		public void init(GL2GL3 gl) {
			if (gpuBuffer > -1)
				return;
			int dataBuffer[] = new int[1];
			gl.glGenBuffers(1, dataBuffer, 0);
			int arrayType = getArrayType();
			gl.glBindBuffer(arrayType, dataBuffer[0]);
			gl.glBufferData(arrayType, data.capacity() * getGPUSize(),
					getData(), isStatic() ? GL3.GL_STATIC_DRAW
							: GL3.GL_DYNAMIC_DRAW);
			gl.glBindBuffer(arrayType, 0);
			gpuBuffer = dataBuffer[0];
		}

		protected abstract int getArrayType();

		protected abstract int getGPUSize();

		protected abstract int getType();

		protected int getNumOfVertices() {
			return data.capacity() / perVertexSize;
		}

		protected boolean isStatic() {
			return true;
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

		@Override
		protected int getGPUSize() {
			return Buffers.SIZEOF_FLOAT;
		}

		@Override
		protected int getArrayType() {
			return GL3.GL_ARRAY_BUFFER;
		}

		@Override
		protected int getType() {
			return GL3.GL_FLOAT;
		}
	}

	public class VBOInstanceAttr extends VBO {

		private InstanceData id;

		public VBOInstanceAttr(String name, InstanceData id) {
			super(name, id == InstanceData.ROTATION ? 9 : 3);
			this.id = id;
			super.data = FloatBuffer.wrap(new float[MAX_INSTANCES
					* perVertexSize]);
		}

		@Override
		protected int getArrayType() {
			return GL3.GL_ARRAY_BUFFER;
		}

		@Override
		protected int getGPUSize() {
			return Buffers.SIZEOF_FLOAT;
		}

		@Override
		protected int getType() {
			return GL3.GL_FLOAT;
		}

		protected boolean isStatic() {
			return false;
		}

		public void bind(int attrib, GL3 gl, List<GameObject> gos) {
			if (id == InstanceData.ROTATION) {
				gl.glBindBuffer(getArrayType(), super.gpuBuffer);
				for (int i = 0; i < 3; i++) {
					gl.glEnableVertexAttribArray(attrib + i);
					gl.glVertexAttribPointer(attrib + i, 3, getType(), false,
							9 * getGPUSize(), i * 3 * getGPUSize());
					gl.glVertexAttribDivisor(attrib + i, 1);
				}
			} else {
				super.bind(attrib, gl);
				gl.glVertexAttribDivisor(attrib, 1);
			}
			super.data.rewind();
			switch (id) {
			case POSITION:
				for (GameObject go : gos)
					((FloatBuffer) super.data).put(go.pos);
				break;
			case ROTATION:
				for (GameObject go : gos)
					((FloatBuffer) super.data).put(go.rotationMatrixArray);
				break;
			case SCALE:
				for (GameObject go : gos)
					((FloatBuffer) super.data).put(go.size);
				break;
			}
			super.data.rewind();
			gl.glBufferSubData(getArrayType(), 0, gos.size() * perVertexSize
					* getGPUSize(), getData());
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
