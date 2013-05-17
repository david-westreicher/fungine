package rendering;

import game.Game;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import shader.ShaderScript;

public class VoxelRenderer extends RenderUpdater {
	private static final float SKYBOX_SCALE = 500;
	private ShaderScript renderDepth;
	private ShaderScript renderVoxel;

	public VoxelRenderer() {
		// renderDepth = new ShaderScript("shader\\renderDepth.glsl");
		// renderVoxel = new ShaderScript("shader\\renderVoxel.glsl");
		super.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				createShadowFob(0, width, height);
			}
		});
		super.excludeGameObjectFromRendering("Skybox-b");
		super.excludeGameObjectFromRendering("Skybox-d");
		super.excludeGameObjectFromRendering("Skybox-f");
		super.excludeGameObjectFromRendering("Skybox-l");
		super.excludeGameObjectFromRendering("Skybox-r");
		super.excludeGameObjectFromRendering("Skybox-u");
	}

	protected void renderObjects() {
		renderSkyBox();
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fobs.get(0)[1]);
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		gl.glColorMask(false, false, false, false);
		renderVoxel.execute(gl);
		ShaderScript.setUniform("time", (float) Game.INSTANCE.loop.tick);
		super.renderObjects();
		renderVoxel.end(gl);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glColorMask(true, true, true, true);
		super.startOrthoRender();
		renderDepth.execute(gl);
		ShaderScript.setUniform("zNear", ZNear);
		ShaderScript.setUniform("zFar", ZFar);
		ShaderScript.setUniformTexture("depth", 0, fobs.get(0)[0]);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glColor4f(1, 1, 1, 1);

		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(0, 0, 0);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(0, height, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(width, height, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(width, 0, 0);
		gl.glEnd();

		gl.glDisable(GL.GL_TEXTURE_2D);
		renderDepth.end(gl);
		super.endOrthoRender();
	}

	private void renderSkyBox() {
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		super.setupLook(new float[] { 0, 0, 0 }, cam.rotationMatrix);
		gl.glScalef(SKYBOX_SCALE, SKYBOX_SCALE, SKYBOX_SCALE);
		super.renderExcludedObjects();
		gl.glPopMatrix();
		gl.glEnable(GL2.GL_DEPTH_TEST);
	}
}
