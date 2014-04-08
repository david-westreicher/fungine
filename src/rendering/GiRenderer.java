package rendering;

import javax.media.opengl.GL2;

import util.Util;

public class GiRenderer extends RenderUpdater {

	private float[] vertices;
	private float[] uvs;

	public GiRenderer() {
		vertices = new float[] {};
		uvs = new float[vertices.length];
		for (int i = 0; i < vertices.length; i++) {
		}
	}

	@Override
	protected void renderObjects() {
		for (float i = 0; i < 200; i++)
			RenderUtil.drawTexture(gl, glutil, (i % 20) / 5,
					(float) ((int) i / 20) / 5, 0, 0.2f, 0.2f,
					textures.getTextureInformation("debugTexture")[0], 0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	@Override
	public void initShaderUniforms() {

	}

	@Override
	public void endShaderUniforms() {

	}

}
