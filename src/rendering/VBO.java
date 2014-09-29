package rendering;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import util.Log;
import world.GameObject;

import com.jogamp.common.nio.Buffers;

public abstract class VBO {
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

	public void bind(int attrib, GL3 gl) {
		gl.glBindBuffer(arrayType, gpuBuffer);
		gl.glEnableVertexAttribArray(attrib);
		gl.glVertexAttribPointer(attrib, perVertexSize, type, false, 0, 0);
		gl.glVertexAttribDivisor(attrib, 0);
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

	public static class VBOFloat extends VBO {

		public VBOFloat(String name, int perVertexSize, float[] data) {
			super(name, perVertexSize);
			super.data = FloatBuffer.wrap(data);
		}

		public int getNumOfVertices() {
			return super.data.capacity() / perVertexSize;
		}
	}

	public static class InstanceVBO extends VBO {
		public static final int MAX_INSTANCES = 10000;
		public int perInstanceSize;

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
}
