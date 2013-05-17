package rendering;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2;

import util.Voxel;

import com.jogamp.common.nio.Buffers;

public class ChunkRenderer extends ModelRenderer {

	private Voxel v;
	public boolean[][][] voxels;
	public boolean valid = true;

	public ChunkRenderer(int size) {
		v = new Voxel(size);
		voxels = v.voxels;
		FloatBuffer[] vertNorms = v.generateMesh();
		init(vertNorms[0], vertNorms[1], null, null, null, null);
	}

	public ChunkRenderer(String file) {
		v = new Voxel(file);
		voxels = v.voxels;
		FloatBuffer[] vertNorms = v.generateMesh();
		init(vertNorms[0], vertNorms[1], null, null, null, null);
	}

	public void updateMesh(GL2 gl) {
		FloatBuffer[] vertNorms = v.generateMesh();
		this.vertexCount = vertNorms[0].capacity() / 3;
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboVertices[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertNorms[0].capacity()
				* Buffers.SIZEOF_FLOAT, vertNorms[0], isStatic());
		if (vertNorms[1] != null) {
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboNormals[0]);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertNorms[1].capacity()
					* Buffers.SIZEOF_FLOAT, vertNorms[1], isStatic());
		}
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
	}

	@Override
	protected int isStatic() {
		return GL2.GL_DYNAMIC_DRAW;
	}

}
