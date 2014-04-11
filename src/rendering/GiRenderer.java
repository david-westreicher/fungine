package rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.GIUtil;
import util.Log;
import util.MathHelper;
import util.MathHelper.Tansformation;
import util.ObjLoader;

import com.jogamp.common.nio.Buffers;

public class GiRenderer extends RenderUpdater {

	private static final int TEXTURE_SIZE = 150;
	private static final int CUBEMAP_SIZE = 50;
	private int[] uvBuffer;
	private int[] frameBuffer;
	private int[] renderedTexture;
	private int cubeSize;
	private ByteBuffer imageBuffer = ByteBuffer.allocate(CUBEMAP_SIZE
			* CUBEMAP_SIZE * 6);
	private Vector3f normal = new Vector3f();
	private Vector3f pos = new Vector3f();
	private Vector2f uvpos = new Vector2f();
	private GIUtil giUtil;

	public GiRenderer() {
		cubeSize = 10;
		// ObjLoader objloader = new ObjLoader("obj/daemon/daemon.obj", false,
		// false);
		// float[] fish = objloader.flattenToTriangle();
		// MathHelper.mul(fish, cubeSize);
		// MathHelper
		// .translate(fish, cubeSize * 3 / 2, cubeSize / 4, cubeSize / 2);
		float[] vertices = RenderUtil.merge(RenderUtil.box(0, 0, 0,
				cubeSize * 2, 1, cubeSize), RenderUtil.box(0, 0, 0, cubeSize,
				cubeSize, 1), RenderUtil.box(0, 0, cubeSize, cubeSize * 2,
				cubeSize, 1), RenderUtil.box(0, 0, 0, 1, cubeSize, cubeSize),
				RenderUtil.box(cubeSize * 2 - 1, 0, 0, 1, cubeSize, cubeSize),
				RenderUtil.box(cubeSize, cubeSize / 2, -cubeSize / 2, 1,
						cubeSize, cubeSize), RenderUtil.box(0, cubeSize,
						cubeSize * 0.0f / 4f, cubeSize, 1, cubeSize));
		this.giUtil = new GIUtil(TEXTURE_SIZE, vertices);

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
		// textureBuffer = FloatBuffer.allocate(TEXTURE_SIZE * TEXTURE_SIZE *
		// 4);
		// for (int i = 0; i < TEXTURE_SIZE; i++) {
		// for (int j = 0; j < TEXTURE_SIZE; j++) {
		// textureBuffer.put(0);
		// textureBuffer.put(0);
		// textureBuffer.put(0);
		// // textureBuffer.put(lookup[j][i][0]);
		// // textureBuffer.put(lookup[j][i][1]);
		// // textureBuffer.put(lookup[j][i][2]);
		// textureBuffer.put(1);
		// }
		// }
		// textureBuffer.rewind();
		gl.glBindTexture(GL2.GL_TEXTURE_2D,
				textures.getTextureInformation("gitexture")[0]);
		gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, TEXTURE_SIZE,
				TEXTURE_SIZE, GL.GL_RGBA, GL.GL_FLOAT, null);
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
				1024, 768);
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
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[0]);
		gl.glViewport(0, 0, CUBEMAP_SIZE, CUBEMAP_SIZE);
		for (int i = 0; i < 30; i++) {
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
			renderFromLookup();
			// long start = System.currentTimeMillis();
			copyTextureToCPU(renderedTexture[0]);
			// Log.log(this, System.currentTimeMillis() - start);
		}

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);
		glutil.glPushMatrix();
		glutil.glTranslatef(0, -cubeSize - 3, 0);
		// RenderUtil.drawTexture(gl, glutil, pos.x, pos.y, pos.z, 0.25f, 0.25f,
		// textures.getTextureInformation("debugTexture")[0], 0, 1);
		drawGI();
		glutil.glPopMatrix();
		RenderUtil.drawTexture(gl, glutil, 10, 10, -1, 20, 20,
				renderedTexture[0], 0, 1);
		RenderUtil.drawTexture(gl, glutil, -10, 10, -1, 20, 20,
				textures.getTextureInformation("gitexture")[0], 0, 1);
	}

	private void renderFromLookup() {
		giUtil.getNewPosNormal(pos, normal, uvpos);
		// pos.set(0, 0, 0);
		// normal.set(0, 1, 0);
		// Log.log(this, x, y);
		// 7Log.log(this, pos);
		// 7Log.log(this, normal);
		// }
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		glutil.gluPerspective(90, 1, 0.01f, 100);
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		glutil.gluLookAt(pos.x, pos.y, pos.z, pos.x + normal.x, pos.y
				+ normal.y, pos.z + normal.z, normal.y, -normal.z, normal.x);
		drawGI();
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPopMatrix();
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		glutil.glPopMatrix();
	}

	private void copyTextureToCPU(int tex) {
		gl.glBindTexture(GL2.GL_TEXTURE_2D, tex);
		gl.glGetTexImage(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB,
				GL2.GL_UNSIGNED_BYTE, imageBuffer);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		double color[] = new double[4];
		double size = CUBEMAP_SIZE * CUBEMAP_SIZE;
		while (imageBuffer.hasRemaining()) {
			color[0] += (imageBuffer.get() & 0xFF) / size;
			color[1] += (imageBuffer.get() & 0xFF) / size;
			color[2] += (imageBuffer.get() & 0xFF) / size;
		}
		imageBuffer.rewind();
		MathHelper.mul(color, 0.5 / 255.0);
		float[] colorf = MathHelper.toFloat(color);
		colorf[0] += 0.5f;
		colorf[2] = 0f;
		colorf[3] = 1f;
		FloatBuffer newColorBuffer = FloatBuffer.wrap(colorf);
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
			gl.glDrawArrays(GL2.GL_TRIANGLES, 0, giUtil.triangleNum());
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
