package rendering;

import game.Game;

import javax.media.opengl.GL;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;

import com.jogamp.opengl.util.texture.Texture;

public class ShaderTestRenderer extends RenderUpdater {

	private Texture testImage;
	private ShaderScript renderTexture;

	@Override
	protected void renderObjects() {
		renderTexture = UberManager.getShader(Shader.TEXTURE);
		super.renderObjects();
		if (testImage == null)
			testImage = UberManager.getTexture("img/red.png");
		if (testImage != null) {
			startOrthoRender();
			renderTexture.execute(gl);
			ShaderScript.setUniformTexture("tex", 0,
					testImage.getTextureObject(gl));
			drawQuad(0);
			renderTexture.end(gl);
			endOrthoRender();
		}
	}

	private void drawQuad(int num) {
		float scale = 0.5f;
		if (num == 3 && !Game.DEBUG) {
			num = 0;
			scale = 1;
		}
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4f(1, 1, 1, 1);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f((num % 2) * width * scale, (num / 2) * height * scale, 0);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f((num % 2) * width * scale, (num / 2) * height * scale
				+ height * scale, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f((num % 2) * width * scale + width * scale, (num / 2)
				* height * scale + height * scale, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f((num % 2) * width * scale + width * scale, (num / 2)
				* height * scale, 0);
		gl.glEnd();
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_BLEND);
	}

}
