package rendering.util;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.GLUtil;
import util.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class RenderUtil {
	private static final float[] rectangle = new float[] { -0.5f, 0.5f, 0.0f,
			0.5f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, };
	private static int[] textureBuffer;

	public static void init(GL2 gl) {
		FloatBuffer verticeUVs = FloatBuffer.wrap(rectangle);
		textureBuffer = new int[1];
		int[] vertexArrayID = new int[1];
		gl.glGenVertexArrays(1, vertexArrayID, 0);
		gl.glBindVertexArray(vertexArrayID[0]);
		gl.glGenBuffers(1, textureBuffer, 0);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, textureBuffer[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, verticeUVs.capacity()
				* Buffers.SIZEOF_FLOAT, verticeUVs, GL2.GL_STATIC_DRAW);
	}

	public static void drawTexture(GL2GL3 gl, GLUtil glutil, float x, float y,
			float z, float width, float height, int texID, float translateX,
			float colorScale, ShaderScript shader) {
		if (shader == null)
			return;
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

		gl.glDisable(GL2.GL_CULL_FACE);
		glutil.glPushMatrix();
		{
			glutil.glTranslatef(x, y, z);
			glutil.scale(width, height, 1);
			shader.execute(gl);
			{
				ShaderScript.setUniformTexture(gl, "fpsTex", 0, texID);
				ShaderScript.setUniformMatrix4(gl, "modelviewprojection",
						glutil.getModelViewProjection(), true);
				ShaderScript.setUniform(gl, "translateX", translateX);

				gl.glEnableVertexAttribArray(0);
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, textureBuffer[0]);
				gl.glVertexAttribPointer(0, 3, GL2.GL_FLOAT, false, 0, 0);
				gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, rectangle.length / 3);
				gl.glDisableVertexAttribArray(0);
			}
			shader.end(gl);
		}
		gl.glEnable(GL2.GL_CULL_FACE);
		glutil.glPopMatrix();

		gl.glDisable(GL2.GL_BLEND);
	}

	public static void drawTexture(GL2GL3 gl, GLUtil glutil, float x, float y,
			float z, float width, float height, int texID, float translateX,
			float colorScale) {
		drawTexture(gl, glutil, x, y, z, width, height, texID, translateX,
				colorScale, UberManager.getShader(Shader.FPS));
	}

	public static float[] merge(float[]... boxs) {
		int length = 0;
		for (float[] box : boxs)
			length += box.length;
		float verts[] = new float[length];
		int count = 0;
		for (float[] box : boxs) {
			for (int i = 0; i < box.length; i++)
				verts[count++] = box[i];
		}
		return verts;
	}

	public static float[] box(float x, float y, float z, float width,
			float height, float depth) {
		float[] verts = new float[6 * 2 * 3 * 3];
		int count = 0;
		// back t1
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z;
		// back t2
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z;
		// front t1
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z + depth;
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z + depth;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		// front t2
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z + depth;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		// bottom t1
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z + depth;
		// bottom t2
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z + depth;
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z + depth;
		// top t1
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z;
		// top t2
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z;
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		// left t1
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z;
		// left t2
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x;
		verts[count++] = y;
		verts[count++] = z + depth;
		verts[count++] = x;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		// right t1
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		// right t2
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z;
		verts[count++] = x + width;
		verts[count++] = y + height;
		verts[count++] = z + depth;
		verts[count++] = x + width;
		verts[count++] = y;
		verts[count++] = z + depth;
		return verts;
	}

	public static void textureToFile(int textureID, int width, int height,
			File file) {
		Texture text = TextureIO.newTexture(textureID, GL2.GL_TEXTURE_2D,
				width, height, width, height, false);
		try {
			TextureIO.write(text, file);
			Log.log(RenderUtil.class, "saving texture screenshot");
		} catch (GLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
