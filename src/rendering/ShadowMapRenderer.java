package rendering;

import game.Game;

import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import shader.ShaderScript;
import util.Log;
import world.GameObject;
import world.PointLight;

import com.jogamp.common.nio.Buffers;

public class ShadowMapRenderer extends RenderUpdater {

	private static final int SHADOW_MAP_SIZE = 4096;
	private static final float SKYBOX_SCALE = 500;
	public static boolean POST_PROCESS = false;
	public static float OFFSET_A = 0;
	public static float OFFSET_B = 0;
	public static float FOV = 80;
	private FloatBuffer lightMatrix = Buffers.newDirectFloatBuffer(16);
	private FloatBuffer projectionMatrix = Buffers.newDirectFloatBuffer(16);
	private FloatBuffer modelViewMatrix = Buffers.newDirectFloatBuffer(16);
	private double[] bias = new double[] { 0.5, 0.0, 0.0, 0.0, 0.0, 0.5, 0.0,
			0.0, 0.0, 0.0, 0.5, 0.0, 0.5, 0.5, 0.5, 1.0 };
	private ShaderScript renderDepth;
	private ShaderScript shadowMapScript;
	private ShaderScript textureShader;
	private ShaderScript dofShader;

	public ShadowMapRenderer() {
		// shadowMapScript = new ShaderScript("shader\\shadowMap.glsl");
		// renderDepth = new ShaderScript("shader\\renderDepth.glsl");
		// textureShader = new ShaderScript("shader\\textureShader.glsl");
		// dofShader = new ShaderScript("shader\\blur.glsl");
		super.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				// blend map
				createTex(0);
				// shadow map
				createShadowFob(1, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
				// depth map
				createShadowFob(2, width, height);
				// debug map
				createTex(3, width, height / 4);
				// dof shader map
				createTex(4);
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
		if (fobs.size() == 5) {
			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fobs.get(2)[1]);
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glColorMask(false, false, false, false);
			super.renderObjects();
			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
			gl.glColorMask(true, true, true, true);

			if (Game.DEBUG) {
				rttStart(fobs.get(3)[1], true);
				rttEnd();
			}
			if (POST_PROCESS) {
				rttStart(fobs.get(4)[1], true);
				rttEnd();
			}

			rttStart(fobs.get(0)[1], false);
			renderSkyBox();
			rttEnd();
			List<GameObject> lights = renderObjs
					.get(PointLight.LIGHT_OBJECT_TYPE_NAME);
			int lightNum = 0;
			if (lights != null) {
				renderStrings.add("Lights    :  " + lights.size());
				boolean isFirst = true;
				for (GameObject cam : lights) {
					createShadowMap(cam);
					// rttStart(fobs.get(0)[1], true);
					gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fobs.get(0)[1]);
					gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
					{
						gl.glEnable(GL.GL_BLEND);
						gl.glBlendFunc(GL.GL_SRC_ALPHA,
								GL.GL_ONE_MINUS_SRC_ALPHA);
						setModelViewMatrix();
						shadowMapScript.execute(gl);
						ShaderScript.setUniform("interp", interp);
						ShaderScript.setUniformTexture("shadowTexture", 0,
								fobs.get(1)[0]);
						ShaderScript.setUniform("lightPos", cam.pos);
						// gl.glPolygonOffset(OFFSET_A, OFFSET_B);
						// gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
						super.renderObjects();
						// gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
						shadowMapScript.end(gl);
						gl.glDisable(GL.GL_BLEND);
					}
					rttEnd();
					super.startOrthoRender();
					{
						if (POST_PROCESS)
							gl.glBindFramebuffer(GL.GL_FRAMEBUFFER,
									fobs.get(4)[1]);
						{
							gl.glEnable(GL.GL_BLEND);
							gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
							gl.glBlendEquation(GL2.GL_FUNC_ADD);
							{
								drawTexture(0, 0, width, height,
										fobs.get(0)[0], false);
							}
							gl.glDisable(GL.GL_BLEND);
						}
						if (POST_PROCESS)
							rttEnd();
						if (Game.DEBUG) {
							gl.glBindFramebuffer(GL.GL_FRAMEBUFFER,
									fobs.get(3)[1]);
							renderDepth.execute(gl);
							ShaderScript.setUniform("zNear",
									RenderUpdater.ZNear);
							ShaderScript.setUniform("zFar", RenderUpdater.ZFar);
							ShaderScript.setUniformTexture("depth", 0,
									fobs.get(1)[0]);
							drawQuad(lightNum * height / 4, 3 * height / 4,
									height / 4, height / 4, true);
							renderDepth.end(gl);
							rttEnd();
						}
						lightNum++;
					}
					super.endOrthoRender();
					isFirst = false;
				}
			} else {
				renderStrings.add("Lights    :  " + 0);
				super.renderObjects();
			}

			if (POST_PROCESS || Game.DEBUG) {
				super.startOrthoRender();
				gl.glDisable(GL2.GL_DEPTH_TEST);
				if (POST_PROCESS) {
					dofShader.execute(gl);
					// float distanceFromCam =
					// PhysicsTest.getInstance().rayTest(
					// cam, ZFar);
					ShaderScript.setUniform("time",
							(float) Game.INSTANCE.loop.tick / 100);
					ShaderScript.setUniform("focus", cam.focus);
					ShaderScript.setUniform("zNear", ZNear);
					ShaderScript.setUniform("zFar", ZFar);
					ShaderScript.setUniform("aspectRatio", (float) width
							/ height);
					ShaderScript.setUniformTexture("depthTex", 0,
							fobs.get(2)[0]);
					ShaderScript.setUniformTexture("ambTex", 1, fobs.get(4)[0]);
					// gl.glEnable(GL.GL_BLEND);
					// gl.glBlendFunc(GL.GL_SRC_ALPHA,
					// GL.GL_ONE_MINUS_SRC_ALPHA);
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
					// gl.glDisable(GL.GL_BLEND);

					dofShader.end(gl);
				}
				if (Game.DEBUG) {
					drawTexture(0, 0, width, height / 4, fobs.get(3)[0], true);
				}
				gl.glEnable(GL2.GL_DEPTH_TEST);
				super.endOrthoRender();
			}
		}
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

	private void drawTexture(int x, int y, int width, int height, int tex,
			boolean useBlending) {
		textureShader.execute(gl);
		ShaderScript.setUniformTexture("tex", 0, tex);
		drawQuad(x, y, width, height, useBlending);
		textureShader.end(gl);
	}

	private void drawQuad(int x, int y, int width, int height,
			boolean useBlending) {
		gl.glEnable(GL.GL_TEXTURE_2D);
		if (useBlending) {
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		}
		gl.glColor4f(1, 1, 1, 1);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(x, y, 0);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(x, y + height, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(x + width, y + height, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(x + width, y, 0);
		gl.glEnd();
		gl.glDisable(GL.GL_TEXTURE_2D);
		if (useBlending)
			gl.glDisable(GL.GL_BLEND);
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
		gl.glVertex3f(height * scale, height * scale, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(height * scale, 0, 0);
		gl.glEnd();
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_BLEND);
	}

	private void setModelViewMatrix() {
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelViewMatrix);
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glActiveTexture(GL.GL_TEXTURE6);
		gl.glLoadIdentity();
		gl.glMultMatrixf(modelViewMatrix);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	private void setTextureMatrix() {
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, lightMatrix);
		gl.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, projectionMatrix);
		gl.glMatrixMode(GL.GL_TEXTURE);
		gl.glActiveTexture(GL.GL_TEXTURE7);
		gl.glLoadIdentity();
		gl.glLoadMatrixd(bias, 0);
		gl.glMultMatrixf(projectionMatrix);
		gl.glMultMatrixf(lightMatrix);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
	}

	private void createShadowMap(GameObject go) {
		gl.glColorMask(false, false, false, false);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fobs.get(1)[1]);
		{
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glCullFace(GL.GL_FRONT);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			glu.gluPerspective(FOV, 1, ZNear, ZFar);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			super.setupLook(go);
			setTextureMatrix();
			gl.glPolygonOffset(OFFSET_A, OFFSET_B);
			gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
			super.renderObjects();
			gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
			gl.glPopMatrix();
			gl.glCullFace(GL.GL_BACK);
		}
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glColorMask(true, true, true, true);

		super.setProjection(width, height);
	}

	private void drawTex(int tex, float scale) {
		textureShader.execute(gl);
		ShaderScript.setUniformTexture("tex", 0, tex);
		drawQuad(scale);
		textureShader.end(gl);
	}

	private void rttStart(int buf, boolean clearDepth) {
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, buf);
		int bits = GL.GL_COLOR_BUFFER_BIT;
		if (clearDepth)
			bits |= GL.GL_DEPTH_BUFFER_BIT;
		gl.glClear(bits);
	}

	private void rttEnd() {
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
	}

	public void dispose() {
		Log.log(this, "Deleting shaders");
		/*
		 * renderDepth.deleteShader(gl); shadowMapScript.deleteShader(gl);
		 * Log.log(this, "Deleting textures"); gl.glDeleteTextures(2, new int[]
		 * { fobs.get(0)[0], fobs.get(1)[0] }, 0); Log.log(this,
		 * "Deleting render buffers"); gl.glDeleteRenderbuffers(1, new int[] {
		 * fobs.get(0)[2] }, 0); Log.log(this, "Deleting frame buffers");
		 * gl.glDeleteFramebuffers(2, new int[] { fobs.get(0)[1], fobs.get(1)[1]
		 * }, 0);
		 */
		super.dispose();
	}

}
