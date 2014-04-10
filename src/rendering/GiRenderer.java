package rendering;

import game.Game;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import manager.UberManager;

import com.jogamp.common.nio.Buffers;

import shader.Shader;
import shader.ShaderScript;
import util.Log;
import util.MathHelper;
import util.MathHelper.Tansformation;
import util.Util;

public class GiRenderer extends RenderUpdater {

	private static final int TEXTURE_SIZE = 100;
	private float[] vertices;
	private float[] uvs;
	private float[] normals;
	private float[][][] lookup;
	private boolean[][] bitmap;
	private int[] uvBuffer;
	private int[] frameBuffer;
	private int[] renderedTexture;
	private int cubeSize;
	private FloatBuffer textureBuffer;

	public GiRenderer() {
		cubeSize = 10;
		vertices = RenderUtil.merge(
				RenderUtil.box(0, 0, 0, cubeSize, 1, cubeSize),
				RenderUtil.box(0, 1, -1, cubeSize, cubeSize, 1),
				RenderUtil.box(0, 1, cubeSize, cubeSize, cubeSize, 1),
				RenderUtil.box(-1, 1, 0, 1, cubeSize, cubeSize),
				RenderUtil.box(cubeSize, 1, 0, 1, cubeSize, cubeSize));
		uvs = new float[(vertices.length / 3) * 2];
		normals = new float[vertices.length];
		lookup = new float[TEXTURE_SIZE][TEXTURE_SIZE][6];
		bitmap = new boolean[TEXTURE_SIZE][TEXTURE_SIZE];
		for (int i = 0; i < vertices.length; i += 9) {
			float x0 = vertices[i + 0];
			float y0 = vertices[i + 1];
			float z0 = vertices[i + 2];
			float x1 = vertices[i + 3];
			float y1 = vertices[i + 4];
			float z1 = vertices[i + 5];
			float x2 = vertices[i + 6];
			float y2 = vertices[i + 7];
			float z2 = vertices[i + 8];
			Vector3f v0 = new Vector3f(x0, y0, z0);
			Vector3f v1 = new Vector3f(x1, y1, z1);
			Vector3f v2 = new Vector3f(x2, y2, z2);
			Vector3f[] triangle = new Vector3f[] { v0, v1, v2 };
			Vector3f normal = MathHelper.findNormal(triangle);
			normals[i + 0] = normal.x;
			normals[i + 1] = normal.y;
			normals[i + 2] = normal.z;
			normals[i + 3] = normal.x;
			normals[i + 4] = normal.y;
			normals[i + 5] = normal.z;
			normals[i + 6] = normal.x;
			normals[i + 7] = normal.y;
			normals[i + 8] = normal.z;
			Vector2f[] newuvs = MathHelper.getProjectedTriangle(triangle);
			// Log.log(this, newuvs);
			fit(newuvs, triangle, normal);
			// Log.log(this, newuvs);
			int count = (i / 3) * 2;
			for (int j = 0; j < 3; j++) {
				uvs[count++] = newuvs[j].x;
				uvs[count++] = newuvs[j].y;
			}
		}
		// Log.log(this, normals);
		super.executeInOpenGLContext(new GLRunnable() {
			@Override
			public void run(GL2 gl) {
				uploadTexture();
				generateFBO();
				sendToGPU(gl);
			}

		});
		// StringBuilder sb = new StringBuilder();
		// for (int x = 0; x < TEXTURE_SIZE; x++) {
		// for (int y = 0; y < TEXTURE_SIZE; y++) {
		// sb.append(bitmap[x][y] ? "x " : "  ");
		// }
		// sb.append("\n");
		// }
		// System.out.println(sb);
	}

	protected void uploadTexture() {
		textures.createTex(gl, "gitexture", TEXTURE_SIZE, TEXTURE_SIZE, true,
				GL2.GL_CLAMP_TO_EDGE, false, true);
		textureBuffer = FloatBuffer.allocate(TEXTURE_SIZE * TEXTURE_SIZE * 4);
		for (int i = 0; i < TEXTURE_SIZE; i++) {
			for (int j = 0; j < TEXTURE_SIZE; j++) {
				textureBuffer.put(lookup[j][i][0]);
				textureBuffer.put(lookup[j][i][1]);
				textureBuffer.put(lookup[j][i][2]);
				textureBuffer.put(1);
			}
		}
		textureBuffer.rewind();
		gl.glBindTexture(GL2.GL_TEXTURE_2D,
				textures.getTextureInformation("gitexture")[0]);
		gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, TEXTURE_SIZE,
				TEXTURE_SIZE, GL.GL_RGBA, GL.GL_FLOAT, textureBuffer);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	protected void generateFBO() {
		frameBuffer = new int[1];
		renderedTexture = new int[1];
		gl.glGenFramebuffers(1, frameBuffer, 0);
		gl.glGenTextures(1, renderedTexture, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[0]);

		// "Bind" the newly created texture : all future texture functions will
		// modify this texture
		gl.glBindTexture(GL2.GL_TEXTURE_2D, renderedTexture[0]);

		// Give an empty image to OpenGL ( the last "0" )
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, TEXTURE_SIZE,
				TEXTURE_SIZE, 0, GL2.GL_RGB, GL2.GL_FLOAT, null);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_NEAREST);
		// Set "renderedTexture" as our colour attachement #0
		gl.glFramebufferTextureARB(GL2.GL_FRAMEBUFFER,
				GL2.GL_COLOR_ATTACHMENT0, renderedTexture[0], 0);
		// Set the list of draw buffers.
		gl.glDrawBuffers(1, new int[] { GL2.GL_COLOR_ATTACHMENT0 }, 0);
		if (gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) != GL2.GL_FRAMEBUFFER_COMPLETE)
			Log.err(this, "framebuffer not completed");
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
	}

	protected void sendToGPU(GL2 gl) {
		uvBuffer = new int[1];
		FloatBuffer verticeUVs = FloatBuffer.allocate(vertices.length
				+ uvs.length + normals.length);
		for (int i = 0; i < vertices.length / 3; i++) {
			verticeUVs.put(uvs[i * 2 + 0]);
			verticeUVs.put(uvs[i * 2 + 1]);
			verticeUVs.put(vertices[i * 3 + 0]);
			verticeUVs.put(vertices[i * 3 + 1]);
			verticeUVs.put(vertices[i * 3 + 2]);
			verticeUVs.put(normals[i * 3 + 0]);
			verticeUVs.put(normals[i * 3 + 1]);
			verticeUVs.put(normals[i * 3 + 2]);
		}
		verticeUVs.rewind();
		int[] vertexArrayID = new int[1];
		gl.glGenVertexArrays(1, vertexArrayID, 0);
		gl.glBindVertexArray(vertexArrayID[0]);
		gl.glGenBuffers(1, uvBuffer, 0);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, uvBuffer[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, verticeUVs.capacity()
				* Buffers.SIZEOF_FLOAT, verticeUVs, GL2.GL_STATIC_DRAW);
	}

	private void fit(Vector2f[] newuvs, Vector3f[] triangle, Vector3f normal) {
		Vector2f translation = new Vector2f();
		Vector2f size = new Vector2f();
		translation.x = -Math.min(0, newuvs[2].x);
		size.x = (int) (Math.max(newuvs[1].x, newuvs[2].x) + translation.x) + 1;
		size.y = (int) (Math.max(0, newuvs[2].y)) + 1;
		for (int x = 0; x < TEXTURE_SIZE - size.x; x++)
			for (int y = 0; y < TEXTURE_SIZE - size.y; y++) {
				boolean found = true;
				float maxx = Math.min(x + size.x + 2, TEXTURE_SIZE - 1);
				float maxy = Math.min(y + size.y + 2, TEXTURE_SIZE - 1);
				loop: for (int x2 = x; x2 <= maxx; x2++)
					for (int y2 = y; y2 <= maxy; y2++) {
						if (bitmap[x2][y2]) {
							found = false;
							break loop;
						}
					}
				if (found) {
					translation.x += x + 1;
					translation.y += y + 1;
					for (Vector2f uv : newuvs) {
						uv.add(translation);
					}
					rasterize(newuvs, x, y, maxx, maxy, triangle, normal);
					return;
				}
			}
		Log.err(this, "no free space found for triangle");
	}

	private void rasterize(Vector2f[] newuvs, int x, int y, float maxx,
			float maxy, Vector3f[] position, Vector3f normal) {
		float[][] currPoints = new float[3][3];
		float[][] realPoints = new float[3][3];
		for (int i = 0; i < 3; i++) {
			realPoints[i][0] = newuvs[i].x;
			realPoints[i][1] = newuvs[i].y;
			realPoints[i][2] = 0;
			currPoints[i][0] = position[i].x;
			currPoints[i][1] = position[i].y;
			currPoints[i][2] = position[i].z;
		}
		Tansformation t = MathHelper.getTransformation(realPoints, currPoints);
		Vector3f test = new Vector3f(newuvs[0].x, newuvs[0].y, 0);
		t.transform(test);
		// Log.log(this, newuvs);
		// Log.log(this, position);
		// Log.log(this, position[0], test, t);
		Vector3f pos = new Vector3f();
		for (int x2 = x; x2 <= maxx; x2++) {
			for (int y2 = y; y2 <= maxy; y2++) {
				bitmap[x2][y2] = true;
				pos.x = x2;
				pos.y = y2;
				pos.z = 0;
				t.transform(pos);
				lookup[x2][y2][0] = pos.x;
				lookup[x2][y2][1] = pos.y;
				lookup[x2][y2][2] = pos.z;
				lookup[x2][y2][3] = normal.x;
				lookup[x2][y2][4] = normal.y;
				lookup[x2][y2][5] = normal.z;
			}
		}
	}

	private boolean inside(Vector2f[] newuvs, float x, float y) {
		Vector2f p1 = newuvs[0];
		Vector2f p2 = newuvs[1];
		Vector2f p3 = newuvs[2];
		Vector2f p = new Vector2f(x, y);
		float alpha = ((p2.y - p3.y) * (p.x - p3.x) + (p3.x - p2.x)
				* (p.y - p3.y))
				/ ((p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x)
						* (p1.y - p3.y));
		float beta = ((p3.y - p1.y) * (p.x - p3.x) + (p1.x - p3.x)
				* (p.y - p3.y))
				/ ((p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x)
						* (p1.y - p3.y));
		float gamma = 1.0f - alpha - beta;
		return alpha > 0 && beta > 0 && gamma > 0;
	}

	@Override
	protected void renderObjects() {
		/*
		 * for (float i = 0; i < 200; i++) RenderUtil.drawTexture(gl, glutil, (i
		 * % 20) / 5, (float) ((int) i / 20) / 5, 0, 0.2f, 0.2f,
		 * textures.getTextureInformation("debugTexture")[0], 0);
		 */
		// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBuffer[0]);
		gl.glViewport(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		glutil.gluOrtho2D(0, TEXTURE_SIZE, 0, TEXTURE_SIZE);
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		glutil.glPushMatrix();
		glutil.glLoadIdentity();
		drawUVMap(false);
		glutil.glMatrixMode(GL2.GL_PROJECTION);
		glutil.glPopMatrix();
		glutil.glMatrixMode(GL2.GL_MODELVIEW);
		glutil.glPopMatrix();

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glViewport(0, 0, width, height);
		drawUVMap(true);
		glutil.glPushMatrix();
		glutil.glTranslatef(0, -cubeSize - 3, 0);
		drawGI();
		glutil.glPopMatrix();
		RenderUtil.drawTexture(gl, glutil, TEXTURE_SIZE / 2, TEXTURE_SIZE / 2,
				-5, TEXTURE_SIZE, TEXTURE_SIZE, renderedTexture[0], 0);
		RenderUtil.drawTexture(gl, glutil, TEXTURE_SIZE / 2, TEXTURE_SIZE / 2,
				-2, TEXTURE_SIZE, TEXTURE_SIZE,
				textures.getTextureInformation("gitexture")[0], 0);
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
			gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertices.length / 3);
		}
		giShader.end(gl);
	}

	private void drawUVMap(boolean animYes) {
		ShaderScript uvmapShader = UberManager.getShader(Shader.UVMAP);
		if (uvmapShader == null || uvBuffer == null)
			return;

		// gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		uvmapShader.execute(gl);
		{
			ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
					glutil.getModelViewProjection(), true);
			float anim = 0.0f;
			int animTime = 200;
			if (animYes)
				switch ((Game.INSTANCE.loop.tick / animTime) % 4) {
				case 0:
					anim = 1.0f;
					break;
				case 1:
					anim = 1.0f - (float) (Game.INSTANCE.loop.tick % animTime)
							/ animTime;
					break;
				case 2:
					anim = 0.0f;
					break;
				case 3:
					anim = (float) (Game.INSTANCE.loop.tick % animTime)
							/ animTime;
					break;
				}
			ShaderScript.setUniform(gl, "anim", anim);
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
			gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertices.length / 3);
		}
		uvmapShader.end(gl);
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
	}

	@Override
	public void initShaderUniforms() {

	}

	@Override
	public void endShaderUniforms() {

	}

}
