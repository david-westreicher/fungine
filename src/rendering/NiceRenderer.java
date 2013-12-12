package rendering;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import world.Camera;
import world.GameObject;

public class NiceRenderer extends RenderUpdater {
	private static final int[] gbufferDrawBuffer = new int[] {
			GL3.GL_COLOR_ATTACHMENT0, GL3.GL_COLOR_ATTACHMENT1,
			GL3.GL_COLOR_ATTACHMENT2 };

	public NiceRenderer() {
		super.executeInOpenGLContext(new GLRunnable() {
			@Override
			public void run(GL2 gl) {
				textures.createGBuffer(gl, "gBuffer");
			}
		});
	}

	public void renderObjects() {
		// * render into gbuffer
		// * * for every gameobjecttype:
		// * * * activate got shader, in=list of gameobjects, out: drawn gbuffer
		// * * * * for every object of gameobject
		// * * * * * render gameobject
		// * * * * * * if emmisive -> render to light buffer
		// * * * deactivate got shader
		// * render lights
		// * * activate deferred light shader wo. shadows
		// * * * for every non-shadow-light:
		// * * * * render light
		// * * deactivate shader
		// * * for every shadow-light:
		// * * * for every gameobjecttype:
		// * * * * activate got vertex shader
		// * * * * * render all objects into shadowmap
		// * * * * deactivate vertex shader
		// * * * activate deferred light shader with shadows
		// * * * * render light
		// * * * deactivate shader
		// * ssao
		// * mix results so far
		// * * add albedo + lights + ssao
		// * postrocessing
		// * * DOF
		// * * WARP
		// * * TONEMAP
		renderIntoGBuffer();
		for (String type : renderObjs.keySet()) {
			if (type.equals(Camera.CAM_OBJECT_TYPE_NAME))
				continue;
			for (GameObject go : renderObjs.get(type)) {
			}
		}
	}

	private void renderIntoGBuffer() {
		bindGBuffer();
		gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		// draw all objects gbuffer style
		gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	}

	private void bindGBuffer() {
		gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER,
				textures.getTextureInformation("gBuffer")[0]);
		gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
				GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D,
				textures.getTextureInformation("gBuffer")[1], 0);
		gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
				GL3.GL_COLOR_ATTACHMENT1, GL3.GL_TEXTURE_2D,
				textures.getTextureInformation("gBuffer")[2], 0);
		gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
				GL3.GL_COLOR_ATTACHMENT2, GL3.GL_TEXTURE_2D,
				textures.getTextureInformation("gBuffer")[3], 0);
		gl3.glDrawBuffers(3, gbufferDrawBuffer, 0);
	}

	@Override
	public void initShaderUniforms() {

	}

	@Override
	public void endShaderUniforms() {

	}
}
