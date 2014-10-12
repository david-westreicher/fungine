package rendering;

import game.Game;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import util.WorkerPool;
import util.WorkerPool.WorkerImpl;
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

	public void dispose(GL2GL3 gl) {
		// Log.log(this, "disposing");
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
		public static final int MAX_INSTANCES = 1000;
		// pos, color,size, rot
		public static final int PER_INSTANCE_SIZE = 3 + 3 + 3 + 9;
		public final float[] instanceData = new float[MAX_INSTANCES
				* PER_INSTANCE_SIZE];
		private WorkerImpl wi;

		// TODO maybe nicer way for instancedata?

		public InstanceVBO() {
			super.isStatic = false;
			super.data = FloatBuffer.wrap(instanceData);
			this.wi = new WorkerPool.WorkerImpl() {
				@Override
				public void update(List<GameObject> gos, int subListStart,
						int subListEnd) {
					updateInstanceData(gos, subListStart, subListEnd);
				}
			};
		}

		protected void updateInstanceData(List<GameObject> gos,
				int subListStart, int subListEnd) {
			for (int i = subListStart; i < subListEnd; i++) {
				GameObject go = gos.get(i);
				int dataCacheIndex = i * PER_INSTANCE_SIZE;
				for (int j = 0; j < 3; j++) {
					instanceData[dataCacheIndex++] = go.pos[j];
				}
				for (int j = 0; j < 3; j++) {
					instanceData[dataCacheIndex++] = go.color[j];
				}
				for (int j = 0; j < 3; j++) {
					instanceData[dataCacheIndex++] = go.size[j];
				}
				instanceData[dataCacheIndex++] = go.rotationMatrix.m00;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m01;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m02;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m10;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m11;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m12;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m20;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m21;
				instanceData[dataCacheIndex++] = go.rotationMatrix.m22;
			}
		}

		// TODO fix flickering bug when using multiple RenderInformation, maybe
		// because glBufferSubData is non blocking-> cpu hasn't uploaded the
		// buffer yet, but we modify it again with the next renderinformation
		// -> fix by uploading instancedata once at the beginning for all gos
		// http://stackoverflow.com/questions/24220583/why-glbuffersubdata-need-to-wait-until-the-vbo-is-not-used-by-gldrawelements
		public void bind(int attrib, GL3 gl, List<GameObject> gos) {
			gl.glBindBuffer(arrayType, super.gpuBuffer);
			for (int i = 0; i < PER_INSTANCE_SIZE / 3; i++) {
				gl.glEnableVertexAttribArray(attrib + i);
				gl.glVertexAttribPointer(attrib + i, 3, type, false,
						PER_INSTANCE_SIZE * gpuSize, i * 3 * gpuSize);
				gl.glVertexAttribDivisor(attrib + i, 1);
			}
			Game.workerPool.execute(gos, wi);
			FloatBuffer instanceBuffer = ((FloatBuffer) super.data);
			instanceBuffer.rewind();
			gl.glBufferSubData(arrayType, 0, gos.size() * PER_INSTANCE_SIZE
					* gpuSize, instanceBuffer);
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

		public static void unbind(GL3 gl) {
			gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

	}
}
