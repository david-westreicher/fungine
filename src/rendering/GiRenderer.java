package rendering;

import game.Game;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.GIUtil;
import util.GIUtil.ProxyBakeable;
import util.Log;
import util.MathHelper;
import util.ObjLoader;
import util.Util;

import com.jogamp.common.nio.Buffers;

public class GiRenderer extends RenderUpdater {

	private static final int TEXTURE_SIZE = 512;
	private static final int CUBEMAP_SIZE = 40;
	private static final int PIXELS_PER_RENDER_TICK = 30;
	private static final boolean SAVE_DEBUG_SCREENSHOTS = false;
	private int[] uvBuffer;
	private int[] frameBuffer;
	private int[] renderedTexture;
	private int cubeSize;
	private ByteBuffer imageBuffer = ByteBuffer.allocate(CUBEMAP_SIZE
			* CUBEMAP_SIZE * 4);
	private Vector3f normal = new Vector3f();
	private Vector3f pos = new Vector3f();
	private Vector2f uvpos = new Vector2f();
	private GIUtil giUtil;

	public GiRenderer() {
		cubeSize = CUBEMAP_SIZE;
		ObjLoader objloader = new ObjLoader("obj/daemon/daemon.obj", false,
				false);
		float[] fish = objloader.flattenToTriangle();
		float[] fish2 = objloader.flattenToTriangle();
		float[] sphere = new ObjLoader("obj/sphere.obj", false, false)
				.flattenToTriangle();
		Matrix4f rotate = new Matrix4f();
		rotate.rotY((float) (Math.PI));
		MathHelper.apply(rotate, fish);
		MathHelper.apply(rotate, fish2);
		MathHelper.mul(sphere, cubeSize / 2);
		MathHelper.mul(fish, cubeSize / 2);
		MathHelper.mul(fish2, cubeSize / 2);
		MathHelper
				.translate(fish, cubeSize * 3 / 2, cubeSize / 4, cubeSize / 2);
		MathHelper.translate(sphere, cubeSize * 3 / 2, cubeSize, cubeSize / 2);
		MathHelper.translate(fish2, cubeSize / 2, cubeSize / 4, cubeSize / 2);
		this.giUtil = new GIUtil(TEXTURE_SIZE);
		giUtil.add(new ProxyBakeable(RenderUtil.box(0, 0, 0, cubeSize * 2, 1,
				cubeSize), new float[] { 1f, 1f, 0 }));
		giUtil.add(new ProxyBakeable(RenderUtil.box(0, 0, 0, cubeSize,
				cubeSize, 1)));
		giUtil.add(new ProxyBakeable(RenderUtil.box(0, 0, cubeSize,
				cubeSize * 2, cubeSize, 1), new float[] { 1f, 0, 0 }, true));
		giUtil.add(new ProxyBakeable(RenderUtil.box(0, 0, 0, 1, cubeSize,
				cubeSize)));
		giUtil.add(new ProxyBakeable(RenderUtil.box(cubeSize * 2 - 1, 0, 0, 1,
				cubeSize, cubeSize), new float[] { 0.5f, 0f, 1f }));
		giUtil.add(new ProxyBakeable(RenderUtil.box(cubeSize, cubeSize / 2,
				-cubeSize / 2, 1, cubeSize, cubeSize)));
		giUtil.add(new ProxyBakeable(RenderUtil.box(cubeSize, cubeSize / 2,
				-cubeSize / 2, 1, cubeSize, cubeSize)));
		giUtil.add(new ProxyBakeable(RenderUtil.box(0, cubeSize,
				cubeSize * 0.0f / 4f, cubeSize, 1, cubeSize), new float[] { 1,
				1, 0 }));
		giUtil.add(new ProxyBakeable(fish, new float[] { 1, 1, 1 }));
		giUtil.add(new ProxyBakeable(fish2, new float[] { 1, 1, 1 }));
		giUtil.add(new ProxyBakeable(sphere, new float[] { 1, 1, 1 }));
		// giUtil.saveTextureAtlasToFile();

		// Log.log(this, normals);
		super.executeInOpenGLContext(new GLRunnable() {
			@Override
			public void run(GL2 gl) {
				uploadTexture();
				generateFBO();
				sendToGPU(gl);
			}
		});
	}

	protected void uploadTexture() {
		textures.createTex(gl, "gitexture", TEXTURE_SIZE, TEXTURE_SIZE, true,
				GL2.GL_CLAMP_TO_EDGE, false, true);
		FloatBuffer textureBuffer = giUtil.getFloatTexture();
		gl.glBindTexture(GL2.GL_TEXTURE_2D,
				textures.getTextureInformation("gitexture")[0]);
		gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, TEXTURE_SIZE,
				TEXTURE_SIZE, GL.GL_RGBA, GL.GL_FLOAT, textureBuffer);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	protected void generateFBO() {
		frameBuffer = new int[1];
		renderedTexture = new int[1];
		int[] depthrenderbuffer = new int[1];
		gl.glGenFramebuffers(1, frameBuffer, 0);
		gl.glGenTextures(1, renderedTexture, 0);
		gl.glGenRenderbuffers(1, depthrenderbuffer, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[0]);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, renderedTexture[0]);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, CUBEMAP_SIZE,
				CUBEMAP_SIZE, 0, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_NEAREST);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depthrenderbuffer[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
				CUBEMAP_SIZE, CUBEMAP_SIZE);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER,
				depthrenderbuffer[0]);
		gl.glFramebufferTextureARB(GL2.GL_FRAMEBUFFER,
				GL2.GL_COLOR_ATTACHMENT0, renderedTexture[0], 0);
		gl.glDrawBuffers(1, new int[] { GL2.GL_COLOR_ATTACHMENT0 }, 0);
		if (gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE)
			Log.err(this, "framebuffer not completed");
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
	}

	protected void sendToGPU(GL2 gl) {
		FloatBuffer verticeUVs = giUtil.getGPUData();
		uvBuffer = new int[1];
		int[] vertexArrayID = new int[1];
		gl.glGenVertexArrays(1, vertexArrayID, 0);
		gl.glBindVertexArray(vertexArrayID[0]);
		gl.glGenBuffers(1, uvBuffer, 0);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, uvBuffer[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, verticeUVs.capacity()
				* Buffers.SIZEOF_FLOAT, verticeUVs, GL2.GL_STATIC_DRAW);
	}

	@Override
	protected void renderObjects() {

		updateRadiosity();

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);
		glutil.glPushMatrix();
		glutil.scale(10.0f / cubeSize, 10.0f / cubeSize, 10.0f / cubeSize);
		glutil.glTranslatef(-cubeSize, -cubeSize * 2, -cubeSize);
		RenderUtil.drawTexture(gl, glutil, pos.x, pos.y, pos.z, 0.25f, 0.25f,
				textures.getTextureInformation("debugTexture")[0], 0, 1);
		if (SAVE_DEBUG_SCREENSHOTS) {
			takeCubemapScreenshots();
			takeFrameBufferScreenshot();
		}
		drawGI();
		glutil.glPopMatrix();
		RenderUtil.drawTexture(gl, glutil, 10, 10, -1, 20, 20,
				renderedTexture[0], 0, 1);
		RenderUtil.drawTexture(gl, glutil, -10, 10, -1, 20, 20,
				textures.getTextureInformation("gitexture")[0], 0, 1);
	}

	private void takeCubemapScreenshots() {
		long renderTick = Game.INSTANCE.loop.renderTick;
		if (renderTick > 15000 && renderTick % 200 == 0) {
			RenderUtil.textureToFile(renderedTexture[0], CUBEMAP_SIZE,
					CUBEMAP_SIZE, Util.generateScreenshotFile());
		}
	}

	private void takeFrameBufferScreenshot() {
		long renderTick = Game.INSTANCE.loop.renderTick;
		if (renderTick == 12000 || renderTick == 12001
				|| (renderTick > 15000 && renderTick % 1000 == 0)) {
			// gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
			// gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
			Log.log(this, "saving framebuffer screenshot");
			super.takeScreen = true;
		}
	}

	private void updateRadiosity() {
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[0]);
		gl.glViewport(0, 0, CUBEMAP_SIZE, CUBEMAP_SIZE);
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		glutil.gluPerspective(90, 1, 0.5f, 100);
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glClearColor(1, 1, 1, 1);
		for (int i = 0; i < PIXELS_PER_RENDER_TICK; i++) {
			// if (Game.INSTANCE.loop.tick % 100 == 0)
			giUtil.getNext();
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
			renderFromLookup();
			// long start = System.currentTimeMillis();
			copyTextureToCPU(renderedTexture[0]);
			// Log.log(this, System.currentTimeMillis() - start);
		}
		// Log.log(this, cpuToGPUTime);
		gl.glClearColor(1, 1, 1, 1);
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPopMatrix();
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
	}

	private void renderFromLookup() {
		giUtil.getPosNormal(pos, normal, uvpos);
		// pos.set(0, 0, 0);
		// normal.set(0, 1, 0);
		// Log.log(this, x, y);
		// 7Log.log(this, pos);
		// 7Log.log(this, normal);
		// }
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		glutil.gluLookAt(pos.x, pos.y, pos.z, pos.x + normal.x, pos.y
				+ normal.y, pos.z + normal.z, normal.y, -normal.z, normal.x);
		drawGI();
		glutil.glPopMatrix();
	}

	private void copyTextureToCPU(int tex) {
		gl.glBindTexture(GL2.GL_TEXTURE_2D, tex);
		gl.glGetTexImage(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA,
				GL2.GL_UNSIGNED_BYTE, imageBuffer);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		double color[] = new double[4];
		int skipNumber = 1;
		double size = CUBEMAP_SIZE * CUBEMAP_SIZE / skipNumber;
		for (int i = 0; i < imageBuffer.capacity(); i += 4) {
			color[0] += (imageBuffer.get(i + 0) & 0xFF) / size;
			color[1] += (imageBuffer.get(i + 1) & 0xFF) / size;
			color[2] += (imageBuffer.get(i + 2) & 0xFF) / size;
		}
		imageBuffer.rewind();
		FloatBuffer newColorBuffer = giUtil.radiosity(color);
		gl.glBindTexture(GL2.GL_TEXTURE_2D,
				textures.getTextureInformation("gitexture")[0]);
		gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, (int) uvpos.x, (int) uvpos.y,
				1, 1, GL.GL_RGBA, GL.GL_FLOAT, newColorBuffer);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	private void drawGI() {
		ShaderScript giShader = UberManager.getShader(Shader.GI);
		if (giShader == null || uvBuffer == null)
			return;
		giShader.execute(gl);
		{
			ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
					glutil.getModelViewProjection(), true);
			ShaderScript.setUniformTexture(gl, "giMap", 0,
					textures.getTextureInformation("gitexture")[0]);
			ShaderScript.setUniform(gl, "textureSize", (float) TEXTURE_SIZE);
			// ShaderScript.setUniform(gl, "camPos", Game.INSTANCE.cam.pos);
			gl.glEnableVertexAttribArray(0);
			gl.glEnableVertexAttribArray(1);
			gl.glEnableVertexAttribArray(2);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, uvBuffer[0]);
			gl.glVertexAttribPointer(0, 2, GL2.GL_FLOAT, false,
					8 * Buffers.SIZEOF_FLOAT, 0);
			gl.glVertexAttribPointer(1, 3, GL2.GL_FLOAT, false,
					8 * Buffers.SIZEOF_FLOAT, 2 * Buffers.SIZEOF_FLOAT);
			gl.glVertexAttribPointer(2, 3, GL2.GL_FLOAT, false,
					8 * Buffers.SIZEOF_FLOAT, 5 * Buffers.SIZEOF_FLOAT);
			for (int i = 0; i < 2; i++) {
				gl.glCullFace(i == 0 ? GL2.GL_BACK : GL2.GL_FRONT);
				ShaderScript.setUniform(gl, "colorscale", (float) (1 - i));
				gl.glDrawArrays(GL2.GL_TRIANGLES, 0, giUtil.triangleNum());
			}
			gl.glCullFace(GL2.GL_BACK);
		}
		giShader.end(gl);
	}

	@Override
	public void initShaderUniforms() {
	}

	@Override
	public void endShaderUniforms() {
	}

}
