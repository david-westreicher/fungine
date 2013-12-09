package rendering;

import javax.media.opengl.GL2;

public class NiceRenderer extends RenderUpdater {

	public NiceRenderer() {
		super.executeInOpenGLContext(new GLRunnable() {
			@Override
			public void run(GL2 gl) {
				textures.createGBuffer(gl, "gBuffer");
			}
		});
	}

	public void renderObjects() {
	}

	@Override
	public void initShaderUniforms() {
		
	}

	@Override
	public void endShaderUniforms() {
		
	}
}
