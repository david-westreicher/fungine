package game;

import javax.media.opengl.GL;

import manager.UberManager;
import rendering.RenderUpdater;
import shader.Shader;
import shader.ShaderScript;

public class TestRenderer extends RenderUpdater {
	public TestRenderer() {
		super.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				createShadowFob(0, width, height);
			}
		});
	}

	public void renderObjects() {
		if (fobs.size() == 0
				|| !UberManager.areShaderInitialized(shader.Shader.values()))
			return;

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fobs.get(0)[1]);
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		super.renderObjects(true);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		super.startOrthoRender();
		ShaderScript depthShader = UberManager.getShader(Shader.DEPTH);
		if (depthShader != null) {
			depthShader.execute(gl);
			ShaderScript.setUniformTexture("depth", 0, fobs.get(0)[0]);
			ShaderScript.setUniform("zNear", ZNEAR);
			ShaderScript.setUniform("zFar", ZFar);
			drawQuad(1);
			depthShader.end(gl);
		}
		super.endOrthoRender();
	}

	private void drawQuad(float scale) {
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4f(1, 1, 1, 1);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(0, 0, 0);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(0, height * scale, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(width * scale, height * scale, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(width * scale, 0, 0);
		gl.glEnd();
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_BLEND);
	}
}
