package rendering;

import game.Game;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import shader.ShaderScript;
import util.Log;

import com.jogamp.common.nio.Buffers;

public class DOFRenderer extends RenderUpdater {

	private List<Integer[]> fobs = new ArrayList<Integer[]>();
	private ShaderScript sscript;
	private ShaderScript sscript2;

	public DOFRenderer() {
		// sscript = new ShaderScript("shader\\glow.glsl");
		// sscript2 = new ShaderScript("shader\\blur.glsl");
		super.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				createTex(0);
				createShadowFob(1);
			}
		});
	}

	@Override
	protected void renderObjects() {
		boolean POST_PROCESS = false;
		if (!POST_PROCESS) {
			super.renderObjects();
			/*
			 * rttStart(fobs.get(0)[1]); gl.glEnable(GL.GL_BLEND);
			 * gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			 * super.renderObjects(); gl.glDisable(GL.GL_BLEND); rttEnd();
			 * sscript.execute(gl); sscript.setUniform("time", (float)
			 * Game.INSTANCE.loop.tick); sscript.setUniform("resolution", new
			 * float[] { 800, 600 }); sscript.setUniform("size", 6);
			 * 
			 * startOrthoRender(); drawTexture(fobs.get(0)[0]);
			 * endOrthoRender(); sscript.end(gl);
			 */
		} else {
			rttStart(fobs.get(0)[1]);
			super.renderObjects();
			rttEnd();

			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fobs.get(1)[1]);
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glColorMask(false, false, false, false);
			super.renderObjects();
			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
			super.startOrthoRender();
			{
				gl.glColorMask(true, true, true, true);
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

				sscript2.execute(gl);
				sscript2.setUniform("focus", Game.INSTANCE.cam.focus);
				sscript2.setUniform("zNear", RenderUpdater.ZNear);
				sscript2.setUniform("zFar", RenderUpdater.ZFar);
				sscript2.setUniformTexture("depthTex", 0, fobs.get(1)[0]);
				sscript2.setUniformTexture("ambTex", 1, fobs.get(0)[0]);
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
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
				gl.glDisable(GL.GL_BLEND);

				sscript2.end(gl);
			}
			super.endOrthoRender();
		}

	}

	private void rttStart(int buf) {
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, buf);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
	}

	private void rttEnd() {
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
	}

	private void drawTexture(int tex, float scale) {
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tex);
		gl.glColor4f(1, 1, 1, 1);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(0, 0, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(width * scale, 0, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(width * scale, height * scale, 0);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(0, height * scale, 0);
		gl.glEnd();
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_BLEND);
	}

	//
	// private void createTex(int fobNum) {
	// int[] fboId = new int[1];
	// int[] texId = new int[1];
	// int[] depId = new int[1];
	// gl.glGenFramebuffers(1, fboId, 0);
	// gl.glGenTextures(1, texId, 0);
	// gl.glGenRenderbuffers(1, depId, 0);
	//
	// gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depId[0]);
	// gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT32,
	// width, height);
	// ;
	//
	// gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);
	// gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
	// GL.GL_LINEAR);
	// gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
	// GL.GL_LINEAR);
	// gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
	// gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
	// gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, width, height, 0,
	// GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
	// Buffers.newDirectByteBuffer(width * height * 4));
	//
	// gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);
	// gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
	// GL.GL_TEXTURE_2D, texId[0], 0);
	// gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
	// GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, depId[0]);
	//
	// gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	// Integer[] fob = new Integer[] { texId[0], fboId[0], depId[0] };
	// if (fobs.size() < fobNum)
	// fobs.set(fobNum, fob);
	// else
	// fobs.add(fob);
	// int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
	// if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
	// gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
	// Log.log(this, "Frame buffer object successfully created");
	// } else {
	// throw new IllegalStateException("Frame Buffer Oject not created.");
	// }
	// }

	private void createShadowFob(int fobNum) {
		int[] fboId = new int[1];
		int[] texId = new int[1];
		gl.glGenFramebuffers(1, fboId, 0);
		gl.glGenTextures(1, texId, 0);

		gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
		// gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, width, height, 0,
		// GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
		// Buffers.newDirectByteBuffer(width * height * 4));
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT, width,
				height, 0, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_BYTE,
				Buffers.newDirectByteBuffer(width * height * 4));

		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);

		gl.glDrawBuffer(GL2.GL_NONE);
		gl.glReadBuffer(GL2.GL_NONE);

		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
				GL.GL_TEXTURE_2D, texId[0], 0);

		Integer[] fob = new Integer[] { texId[0], fboId[0] };
		if (fobs.size() < fobNum)
			fobs.set(fobNum, fob);
		else
			fobs.add(fob);
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			Log.log(this, "Frame buffer object successfully created");
		} else {
			throw new IllegalStateException("Frame Buffer Oject not created.");
		}
	}

}
