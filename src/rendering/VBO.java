package rendering;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import util.Log;
import world.GameObject;

import com.jogamp.common.nio.Buffers;

public abstract class VBO {
	public enum VertexAttribute {
		POSITION, COLOR, UV;
	};

	private int gpuBuffer = -1;
	private Buffer data;
	protected boolean isStatic = true;
	protected int gpuSize = Buffers.SIZEOF_FLOAT;
	protected int type = GL3.GL_FLOAT;
	protected int arrayType = GL3.GL_ARRAY_BUFFER;

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

		protected VertexAttribute name;
		protected int perVertexSize;

		public VBOFloat(VertexAttribute name, int perVertexSize, float[] data) {
			this.perVertexSize = perVertexSize;
			this.name = name;
			super.data = FloatBuffer.wrap(data);
		}

		public int getNumOfVertices() {
			return super.data.capacity() / perVertexSize;
		}

		public void bind(int attrib, GL3 gl) {
			gl.glBindBuffer(arrayType, super.gpuBuffer);
			gl.glEnableVertexAttribArray(attrib);
			gl.glVertexAttribPointer(attrib, perVertexSize, type, false, 0, 0);
			gl.glVertexAttribDivisor(attrib, 0);
		}
	}

	public static class InstanceVBO extends VBO {
		public static final int MAX_INSTANCES = 10000;
		public static int PER_INSTANCE_SIZE = 3 + 3 + 9;
		public static final float[] dataCache = new float[MAX_INSTANCES
				* PER_INSTANCE_SIZE];
		private int instancesToRender = -1;

		public InstanceVBO() {
			super.isStatic = false;
			super.data = FloatBuffer
					.allocate(MAX_INSTANCES * PER_INSTANCE_SIZE);
		}

		public int bind(int attrib, GL3 gl, List<GameObject> gos) {
			gl.glBindBuffer(arrayType, super.gpuBuffer);
			for (int i = 0; i < 5; i++) {
				gl.glEnableVertexAttribArray(attrib + i);
				gl.glVertexAttribPointer(attrib + i, 3, type, false,
						PER_INSTANCE_SIZE * gpuSize, i * 3 * gpuSize);
				gl.glVertexAttribDivisor(attrib + i, 1);
			}
			// TODO use for static objects
			// if (instancesToRender > -1)
			// return instancesToRender;
			instancesToRender = 0;
			FloatBuffer instanceBuffer = ((FloatBuffer) super.data);
			int dataCacheIndex = 0;
			for (GameObject go : gos) {
				if (go.render) {
					for (int i = 0; i < 3; i++) {
						dataCache[dataCacheIndex++] = go.pos[i];
					}
					for (int i = 0; i < 3; i++) {
						dataCache[dataCacheIndex++] = go.size[i];
					}
					dataCache[dataCacheIndex++] = go.rotationMatrix.m00;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m01;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m02;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m10;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m11;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m12;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m20;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m21;
					dataCache[dataCacheIndex++] = go.rotationMatrix.m22;
					instancesToRender++;
				}
			}
			instanceBuffer.rewind();
			instanceBuffer.put(dataCache, 0, dataCacheIndex);
			instanceBuffer.rewind();
			gl.glBufferSubData(arrayType, 0, instancesToRender
					* PER_INSTANCE_SIZE * gpuSize, instanceBuffer);
			return instancesToRender;
		}
	}

	public static class IndexVBO extends VBO {

		private int indicesNum;

		public IndexVBO(int[] indices) {
			gpuSize = Buffers.SIZEOF_INT;
			type = GL3.GL_INT;
			arrayType = GL3.GL_ELEMENT_ARRAY_BUFFER;
			indicesNum = indices.length;
			super.data = IntBuffer.wrap(indices);
		}

		public void bind(GL3 gl) {
			gl.glBindBuffer(arrayType, super.gpuBuffer);
		}

		public int getIndicesNum() {
			return indicesNum;
		}

	}
}
