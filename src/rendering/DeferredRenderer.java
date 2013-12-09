package rendering;

import game.Game;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import manager.UberManager;
import rendering.model.ModelRenderer;
import settings.Settings;
import shader.ShaderScript;
import util.Log;
import world.GameObject;
import world.PointLight;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;

public class DeferredRenderer extends SimpleRenderer {
	private static final float SKYBOX_SCALE = 10;
	private static final boolean RENDER_SKYBOX = true;
	protected static final int SHADOW_MAP_SIZE = 512;
	private static boolean HATCHED = false;
	public static boolean DEPTH_FIRST = false;
	public static float SHADOW_FOV = 90;
	public static float SSAO_STRENGTH = 0.43f;
	public static boolean SSAO = true;
	public static boolean DEBUG = false;
	public static float AMBIENT = 0.2f;
	public static boolean DOF = false;
	public static float BLUR = 0.1f;
	private double[] bias = new double[] { 0.5, 0.0, 0.0, 0.0, 0.0, 0.5, 0.0,
			0.0, 0.0, 0.0, 0.5, 0.0, 0.5, 0.5, 0.5, 1.0 };
	private ShaderScript deferredRenderer;
	private ShaderScript textureShader;
	private ShaderScript deferredLighting;
	private ShaderScript hBlur;
	private ShaderScript vBlur;
	private ShaderScript bokeh;
	private ShaderScript ssaoShader;
	private ShaderScript skyboxShader;
	private ShaderScript hatchShader;
	private LightRenderer lightRenderer;
	private static float[] tmpArray = new float[3];
	private FloatBuffer lightMatrix = Buffers.newDirectFloatBuffer(16);
	private FloatBuffer projectionMatrix = Buffers.newDirectFloatBuffer(16);
	public Texture cubeMap;
	private float[] nullVector = new float[] { 0, 0, 0 };
	private static final int[] gbufferDrawBuffer = new int[] {
			GL2.GL_COLOR_ATTACHMENT0, GL2.GL_COLOR_ATTACHMENT1,
			GL2.GL_COLOR_ATTACHMENT2 };

	private final class LightRenderer extends ModelRenderer {
		private List<PointLight> shadowLights = new ArrayList<PointLight>();

		// private ModelRenderer coneRenderer;

		public LightRenderer() {
			super("obj/lowpolySphere.obj", false, false, true);
			// coneRenderer = new ModelRenderer("obj/lowpolyCone.obj");
		}

		public void draw(GL2 gl, List<GameObject> lights) {
			gl.glFrontFace(GL2.GL_CW);
			shadowLights.clear();
			super.init(gl);
			// ugly fix -> shadowMap uniform has to be set once
			ShaderScript.setUniformTexture(gl, "shadowMap", 3,
					textures.getTextureInformation("shadowmap")[0]);
			for (GameObject go : lights) {
				PointLight l = (PointLight) go;
				if (!l.shadow) {
					setUniforms(l);
					gl.glDrawElements(GL2.GL_TRIANGLES, indexCounts[0],
							GL2.GL_UNSIGNED_INT, 0);
				} else
					shadowLights.add(l);
			}
			super.end(gl);
			excludeGameObjectFromRendering(PointLight.LIGHT_OBJECT_TYPE_NAME);
			for (GameObject go : shadowLights) {
				PointLight l = (PointLight) go;
				gl.glEnable(GL2.GL_DEPTH_TEST);
				gl.glBindFramebuffer(GL.GL_FRAMEBUFFER,
						textures.getTextureInformation("shadowmap")[1]);
				renderDepthIntoFBO(l, l.radius * 1.05f);
				gl.glDisable(GL2.GL_DEPTH_TEST);
				gl.glBindFramebuffer(GL.GL_FRAMEBUFFER,
						textures.getTextureInformation("1")[1]);
				ShaderScript.setUniformTexture(gl, "shadowMap", 3,
						textures.getTextureInformation("shadowmap")[0]);
				boolean wireframesetting = RenderUpdater.WIREFRAME;
				RenderUpdater.WIREFRAME = false;
				super.init(gl);
				setUniforms(l);
				gl.glDrawElements(GL2.GL_TRIANGLES, indexCounts[0],
						GL2.GL_UNSIGNED_INT, 0);
				super.end(gl);
				RenderUpdater.WIREFRAME = wireframesetting;
				// coneRenderer.init(gl);
				// gl.glDrawElements(GL2.GL_TRIANGLES,
				// coneRenderer.indexCounts[0], GL2.GL_UNSIGNED_INT, 0);
				// coneRenderer.end(gl);
			}
			includeGameObjectFromRendering(PointLight.LIGHT_OBJECT_TYPE_NAME);
			gl.glFrontFace(GL2.GL_CCW);
		}

		private void setUniforms(PointLight l) {
			ShaderScript.setUniform3fv(gl, "lightPos", interpolatePos(l));
			ShaderScript.setUniform3fv(gl, "lightColor", l.color);
			ShaderScript.setUniform(gl, "lightRadius", l.radius);
			ShaderScript.setUniform(gl, "hasShadowMap", l.shadow);
			ShaderScript.setUniform(gl, "hasFallof", l.fallof);
		}
	}

	public DeferredRenderer() {
		super.executeInOpenGLContext(new GLRunnable() {

			@Override
			public void run(GL2 gl) {
				UberManager.getTexture("img/random.png", true);
				createGBuffer();
				textures.createTex(gl, "1");
				textures.createTex(gl, "2");
				textures.createTex(gl, "3");
				textures.createShadowFob(gl, "shadowmap", SHADOW_MAP_SIZE,
						SHADOW_MAP_SIZE);
				if (RENDER_SKYBOX)
					cubeMap = textures.createCubeMap(gl, Settings.ENGINE_FOLDER
							+ "img/black-pixel.jpg");
			}
		});
		lightRenderer = new LightRenderer();
		// super.excludeGameObjectFromRendering(PointLight.LIGHT_OBJECT_TYPE_NAME);
	}

	private void renderDepthIntoFBO(GameObject l, float radius) {
		ShaderScript oldShader = ShaderScript.getActiveShader(gl);
		if (oldShader != null)
			oldShader.end(gl);
		{

			gl.glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			glu.gluPerspective(SHADOW_FOV, 1, zNear, radius);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			super.setupLook(l);
			setTextureMatrix();
			renderObjects(true);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPopMatrix();
			gl.glViewport(0, 0, Settings.WIDTH, Settings.HEIGHT);

		}
		if (oldShader != null)
			oldShader.execute(gl);
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

	private void createGBuffer() {
		int[] fboId = new int[1];
		int[] texId = new int[3];
		int[] depId = new int[1];
		gl.glGenFramebuffers(1, fboId, 0);
		gl.glGenTextures(texId.length, texId, 0);
		gl.glGenRenderbuffers(1, depId, 0);

		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depId[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
				Settings.WIDTH, Settings.HEIGHT);

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, depId[0]);
		initTextures(texId);
		int[] fob = new int[] { fboId[0], texId[0], texId[1], texId[2] };
		super.textures.addTexture("gbuffer", fob);
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			Log.log(this, "Frame buffer object successfully created ("
					+ Settings.WIDTH + "," + Settings.HEIGHT + ")");
		} else if (status == GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS) {
			throw new IllegalStateException(
					"Frame Buffer Oject not created.->GL_FRAMEBUFFER_INCOMPLETE_FORMATS");
		} else if (status == GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
			throw new IllegalStateException(
					"Frame Buffer Oject not created.->GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
		} else if (status == GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
			throw new IllegalStateException(
					"Frame Buffer Oject not created.->GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
		} else if (status == GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER) {
			throw new IllegalStateException(
					"Frame Buffer Oject not created.->GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
		} else if (status == GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER) {
			throw new IllegalStateException(
					"Frame Buffer Oject not created.->GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
		} else {
			throw new IllegalStateException("Frame Buffer Oject not created.");
		}
	}

	private void initTextures(int[] texId) {
		int index = 0;
		for (Integer tex : texId) {
			gl.glBindTexture(GL.GL_TEXTURE_2D, tex);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
					GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
					GL.GL_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
					GL2.GL_CLAMP);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
					GL2.GL_CLAMP);
			int internalFormat = GL2.GL_RGBA16F;
			int type = GL2.GL_FLOAT;
			int width = Settings.WIDTH;
			int height = Settings.HEIGHT;
			switch (index) {
			case 0:
				internalFormat = GL2.GL_RGBA8;
				type = GL2.GL_BYTE;
				break;
			}
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width, height,
					0, GL.GL_RGBA, type, null);
			int colorAttach = GL2.GL_COLOR_ATTACHMENT0;
			switch (index) {
			case 1:
				colorAttach = GL2.GL_COLOR_ATTACHMENT1;
			case 2:
				colorAttach = GL2.GL_COLOR_ATTACHMENT2;
			case 3:
				colorAttach = GL2.GL_COLOR_ATTACHMENT3;
			}
			gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, colorAttach,
					GL.GL_TEXTURE_2D, tex, 0);
			index++;
		}
	}

	public void renderObjects() {
		if (textures.getTextureInformation("gbuffer") == null
				|| !UberManager.areShaderInitialized(shader.Shader.values()))
			return;
		deferredRenderer = UberManager.getShader(shader.Shader.DEFERRED);
		textureShader = UberManager.getShader(shader.Shader.TEXTURE);
		deferredLighting = UberManager.getShader(shader.Shader.DEFERRED_LIGHT);
		hBlur = UberManager.getShader(shader.Shader.H_BLUR);
		vBlur = UberManager.getShader(shader.Shader.V_BLUR);
		bokeh = UberManager.getShader(shader.Shader.BOKEH);
		ssaoShader = UberManager.getShader(shader.Shader.SSAO);
		skyboxShader = UberManager.getShader(shader.Shader.SKYBOX);
		hatchShader = UberManager.getShader(shader.Shader.HATCH);

		gl.glViewport(0, 0, Settings.WIDTH, Settings.HEIGHT);
		renderIntoGBuffer();
		if (DEBUG) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			if (renderState.stereo) {
				gl.glViewport(width, 0, width, height);
				startOrthoRenderOffset();
			} else {
				gl.glViewport(0, 0, width, height);
				startOrthoRender();
			}

			textureShader.execute(gl);
			ShaderScript.setUniform(gl, "ambient", 1f);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("gbuffer")[1]);
			drawQuad(0, false, renderState.stereo ? width : 0);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("gbuffer")[2]);
			drawQuad(1, false, renderState.stereo ? width : 0);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("gbuffer")[3]);
			drawQuad(2, false, renderState.stereo ? width : 0);
			textureShader.end(gl);
			if (renderState.stereo)
				gl.glViewport(0, 0, Settings.WIDTH, Settings.HEIGHT);

			endOrthoRender();
		}
		// render into texture 1
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER,
				textures.getTextureInformation("1")[1]);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		if (RENDER_SKYBOX)
			renderSkyBox();
		startOrthoRender();
		{
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			renderAmbient();
			gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);
			renderLights();
			gl.glDisable(GL2.GL_BLEND);

			// render texture 1 into texture 2 with dof or normal
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER,
					textures.getTextureInformation("2")[1]);
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
			if (DOF && !DEBUG) {
				renderDof();
			} else {
				textureShader.execute(gl);
				ShaderScript.setUniform(gl, "ambient", 1f);
				ShaderScript.setUniformTexture(gl, "tex", 0,
						textures.getTextureInformation("1")[0]);
				drawQuad(3, true);
				textureShader.end(gl);
			}

			if (BLUR > 0) {
				// render texture 2 into texture 3 with vBlur
				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER,
						textures.getTextureInformation("3")[1]);
				vBlur.execute(gl);
				ShaderScript.setUniformTexture(gl, "tex", 0,
						textures.getTextureInformation("2")[0]);
				ShaderScript.setUniform(gl, "width", (float) width
						/ (DeferredRenderer.BLUR * 2 + 1));
				drawQuad(3, true);
				vBlur.end(gl);

				// render texture 3 into texture 1 with hBlur
				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER,
						textures.getTextureInformation("1")[1]);
				hBlur.execute(gl);
				ShaderScript.setUniformTexture(gl, "tex", 0,
						textures.getTextureInformation("3")[0]);
				ShaderScript.setUniform(gl, "height", (float) height
						/ (DeferredRenderer.BLUR * 2 + 1));
				drawQuad(3, true);
				hBlur.end(gl);
			}

			/*
			 * ShaderScript depthShader = UberManager.getShader(Shader.DEPTH);
			 * if (depthShader != null) { depthShader.execute(gl);
			 * ShaderScript.setUniformTexture("depth", 0,
			 * textures.getTextureInformation("shadowmap")[0]);
			 * ShaderScript.setUniform("zNear", ZNEAR);
			 * ShaderScript.setUniform("zFar", ZFar); drawQuad(0, false);
			 * depthShader.end(gl); }
			 */

		}
		endOrthoRender();

		if (renderState.stereo) {
			gl.glViewport(width, 0, width, height);
			startOrthoRenderOffset();
		} else {
			gl.glViewport(0, 0, width, height);
			startOrthoRender();
		}
		// render texture 2 + texture 1 (GL_ONE,GL_ONE) onto screen

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glEnable(GL2.GL_BLEND);
		if (HATCHED) {
			hatchShader.execute(gl);
			// ShaderScript.setUniform("ambient", 1f);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("2")[0]);
			drawQuad(3, !DEBUG, renderState.stereo ? width : 0);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("1")[0]);
			drawQuad(3, !DEBUG, renderState.stereo ? width : 0);
			hatchShader.end(gl);
		} else {
			textureShader.execute(gl);
			ShaderScript.setUniform(gl, "ambient", 1f);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("2")[0]);
			drawQuad(3, !DEBUG, renderState.stereo ? width : 0);
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("1")[0]);
			drawQuad(3, !DEBUG, renderState.stereo ? width : 0);
			textureShader.end(gl);
		}
		if (renderState.stereo)
			gl.glViewport(0, 0, width, height);
		gl.glDisable(GL2.GL_BLEND);
		endOrthoRender();
		gl.glEnable(GL2.GL_DEPTH_TEST);
	}

	private void renderLights() {
		List<GameObject> lights = renderObjs
				.get(PointLight.LIGHT_OBJECT_TYPE_NAME);
		if (lights == null || lights.size() == 0)
			return;
		gl.glColor4f(1, 1, 1, 1);
		if (!DEBUG)
			endOrthoRender();
		deferredLighting.execute(gl);
		ShaderScript.setUniform(gl, "width", (float) Settings.WIDTH);
		ShaderScript.setUniform(gl, "height", (float) Settings.HEIGHT);
		ShaderScript.setUniformTexture(gl, "diff", 0,
				textures.getTextureInformation("gbuffer")[1]);
		ShaderScript.setUniformTexture(gl, "normal", 1,
				textures.getTextureInformation("gbuffer")[2]);
		ShaderScript.setUniformTexture(gl, "position", 2,
				textures.getTextureInformation("gbuffer")[3]);
		ShaderScript.setUniform3fv(gl, "camPos",
				interpolatePos(Game.INSTANCE.cam));
		// ShaderScript.setUniformMatrix3("camRotation",
		// Game.INSTANCE.cam.rotationMatrixArray, true);
		ShaderScript.setUniform(gl, "isFullScreen", !DEBUG);
		if (DEBUG) {
			for (GameObject go : lights) {
				PointLight l = (PointLight) go;
				lightRenderer.setUniforms(l);
				ShaderScript.setUniform(gl, "hasShadowMap", false);
				drawQuad(0, width, height, 1f, 0);
			}
		} else {
			lightRenderer.draw(gl, lights);
		}
		deferredLighting.end(gl);
		if (!DEBUG)
			super.startOrthoRender();
	}

	private static float[] interpolatePos(GameObject go) {
		for (int i = 0; i < 3; i++)
			tmpArray[i] = INTERP * go.pos[i] + (1 - INTERP) * go.oldPos[i];
		return tmpArray;
	}

	private void renderAmbient() {
		textureShader.execute(gl);
		if (SSAO) {
			textureShader.end(gl);
			ssaoShader.execute(gl);
			ShaderScript.setUniform(gl, "ssaoStrength", SSAO_STRENGTH);
			ShaderScript.setUniformTexture(gl, "gdiffuse", 0,
					textures.getTextureInformation("gbuffer")[1]);
			ShaderScript.setUniformTexture(gl, "gnormals", 1,
					textures.getTextureInformation("gbuffer")[2]);
			ShaderScript.setUniformTexture(gl, "gpos", 2,
					textures.getTextureInformation("gbuffer")[3]);
			ShaderScript.setUniform(gl, "width", (float) Settings.WIDTH);
			ShaderScript.setUniform(gl, "height", (float) Settings.HEIGHT);
			Texture rand = UberManager.getTexture("img/random.png");
			if (rand != null)
				ShaderScript.setUniformTexture(gl, "grandom", 3,
						rand.getTextureObject(gl));
			ShaderScript.setUniform3fv(gl, "camPos", Game.INSTANCE.cam.pos);
		} else {
			ShaderScript.setUniformTexture(gl, "tex", 0,
					textures.getTextureInformation("gbuffer")[1]);
		}
		ShaderScript.setUniform(gl, "ambient", AMBIENT);
		drawQuad(3, true);
		if (SSAO) {
			ssaoShader.end(gl);
		} else {
			textureShader.end(gl);
		}
	}

	private void renderDof() {
		bokeh.execute(gl);
		ShaderScript.setUniform(gl, "zFar", zFar);
		ShaderScript.setUniform(gl, "bgl_RenderedTextureWidth",
				(float) Settings.WIDTH);
		ShaderScript.setUniform(gl, "bgl_RenderedTextureHeight",
				(float) Settings.HEIGHT);
		ShaderScript.setUniformTexture(gl, "bgl_RenderedTexture", 0,
				textures.getTextureInformation("1")[0]);
		ShaderScript.setUniformTexture(gl, "bgl_DepthTexture", 1,
				textures.getTextureInformation("gbuffer")[3]);
		ShaderScript.setUniform3fv(gl, "camPos", Game.INSTANCE.cam.pos);
		ShaderScript.setUniform(gl, "focalDepth", Game.INSTANCE.cam.focus);
		drawQuad(3, true);
		bokeh.end(gl);
	}

	private void renderIntoGBuffer() {
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER,
				textures.getTextureInformation("gbuffer")[0]);
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0,
				GL.GL_TEXTURE_2D, textures.getTextureInformation("gbuffer")[1],
				0);
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT1,
				GL.GL_TEXTURE_2D, textures.getTextureInformation("gbuffer")[2],
				0);
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT2,
				GL.GL_TEXTURE_2D, textures.getTextureInformation("gbuffer")[3],
				0);
		gl.glDrawBuffers(3, gbufferDrawBuffer, 0);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		if (DEPTH_FIRST)
			renderObjects(true);
		deferredRenderer.execute(gl);
		initShaderUniforms();
		super.renderObjects();
		endShaderUniforms();
		deferredRenderer.end(gl);
	}

	private void renderSkyBox() {
		/*
		 * gl.glDisable(GL2.GL_DEPTH_TEST); gl.glDepthMask(false);
		 * gl.glPushMatrix(); gl.glLoadIdentity(); super.setupLook(new float[] {
		 * 0, 0, 0 }, cam); gl.glScalef(SKYBOX_SCALE, SKYBOX_SCALE,
		 * SKYBOX_SCALE); super.renderExcludedObjects(); gl.glPopMatrix();
		 * gl.glDepthMask(true); gl.glEnable(GL2.GL_DEPTH_TEST);
		 */
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		super.setupLook(nullVector, cam.rotationMatrix);
		gl.glScalef(SKYBOX_SCALE, SKYBOX_SCALE, SKYBOX_SCALE);
		skyboxShader.execute(gl);
		ShaderScript.setUniformCubemap(gl, "cubeMap", 10, cubeMap);
		gl.glFrontFace(GL.GL_CW);
		glut.glutSolidCube(1);
		gl.glFrontFace(GL2.GL_CCW);
		skyboxShader.end(gl);
		gl.glPopMatrix();
		gl.glDepthMask(true);
		gl.glEnable(GL2.GL_DEPTH_TEST);
	}

	private void drawQuad(int num, boolean fullscreen) {
		drawQuad(num, fullscreen, 0);
	}

	private void drawQuad(int num, boolean fullscreen, float offset) {
		float scale = 0.5f;
		if (fullscreen) {
			num = 0;
			scale = 1;
		}
		float width = super.width;
		float height = super.height;
		drawQuad(num, width, height, scale, offset);
	}

	protected void renderObjects(boolean depthOnly) {
		renderState.depthOnly = depthOnly;
		if (depthOnly) {
			gl.glColorMask(false, false, false, false);
		}
		// render this shit
		renderObjects(renderObjs);

		if (depthOnly)
			gl.glColorMask(true, true, true, true);
		renderState.depthOnly = false;
	}

	private void drawQuad(int num, float width, float height, float scale,
			float offset) {
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glColor4f(1, 1, 1, 1);
		gl.glBegin(GL.GL_TRIANGLE_FAN);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f((num % 2) * width * scale + offset, (num / 2) * height
				* scale, 0);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f((num % 2) * width * scale + offset, (num / 2) * height
				* scale + height * scale, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f((num % 2) * width * scale + width * scale + offset,
				(num / 2) * height * scale + height * scale, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f((num % 2) * width * scale + width * scale + offset,
				(num / 2) * height * scale, 0);
		gl.glEnd();
		gl.glDisable(GL.GL_TEXTURE_2D);
	}

	@Override
	public void initShaderUniforms() {
		super.initShaderUniforms();
		ShaderScript.setUniform(gl, "time", (float) Game.INSTANCE.loop.tick);
		ShaderScript.setUniformCubemap(gl, "cubeMap", 10, cubeMap);
		ShaderScript.setUniform3fv(gl, "camPos",
				interpolatePos(Game.INSTANCE.cam));
	}

	@Override
	public void endShaderUniforms() {
		super.endShaderUniforms();
		ShaderScript.releaseCube(gl, cubeMap);
	}

	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = super.getSettings();
		settings.put("isdDebug", DEBUG);
		settings.put("isDepthFirst", DEPTH_FIRST);
		settings.put("isDof", DOF);
		settings.put("isSSAO", SSAO);
		settings.put("blur", BLUR);
		settings.put("ambient", AMBIENT);
		settings.put("ssao", SSAO_STRENGTH);
		return settings;
	}

}
