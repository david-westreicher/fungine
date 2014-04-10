package rendering;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import manager.UberManager;
import shader.Shader;
import shader.ShaderScript;
import util.GLUtil;
import util.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.glsl.ShaderUtil;

public class FPSRenderer {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 80;
	public int currentIndex = 0;
	private ByteBuffer buf;
	private ShaderScript fpsShader;

	public FPSRenderer(TextureHelper textures, GL2 gl) {
		textures.createTex(gl, "debugTexture", WIDTH, HEIGHT, false,
				GL2.GL_REPEAT, false, false);
		buf = ByteBuffer.allocate(HEIGHT * 4);

		RenderUtil.init(gl);
	}

	public void render(GL2 gl, GLUtil glutil, TextureHelper textures,
			int width, long timePerRender, long timePerTick) {
		// update texture
		{
			gl.glBindTexture(GL2.GL_TEXTURE_2D,
					textures.getTextureInformation("debugTexture")[0]);
			gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, currentIndex, 0, 1,
					HEIGHT, GL.GL_BGRA, GL.GL_UNSIGNED_BYTE,
					getByteBuffer(timePerRender, timePerTick));
			gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		}
		// draw texture
		RenderUtil.drawTexture(gl, glutil, width - WIDTH / 2 - 2,
				HEIGHT / 2 + 2, 0, WIDTH, HEIGHT,
				textures.getTextureInformation("debugTexture")[0],
				((float) currentIndex + 1) / WIDTH, 1);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		currentIndex = (currentIndex + 1) % WIDTH;
		return;
	}

	private Buffer getByteBuffer(long timePerRender, long timePerTick) {
		boolean redFirst = timePerRender < timePerTick;
		double min = Math.min(timePerRender, timePerTick);
		double max = Math.max(timePerRender, timePerTick);
		for (int i = HEIGHT - 1; i >= 0; i--) {
			if (i % 15 == 0) {
				buf.put((byte) 255);
				buf.put((byte) 255);
				buf.put((byte) 255);
				buf.put((byte) 255);
				continue;
			} else if (i < min) {
				buf.put((byte) 0);
				buf.put((byte) (redFirst ? 0 : 255));
				buf.put((byte) (redFirst ? 255 : 0));
				buf.put((byte) 255);
			} else if (i < max) {
				buf.put((byte) 0);
				buf.put((byte) (redFirst ? 255 : 0));
				buf.put((byte) (redFirst ? 0 : 255));
				buf.put((byte) 255);
			} else {
				buf.put((byte) 0);
				buf.put((byte) 0);
				buf.put((byte) 0);
				buf.put((byte) 0);
			}
		}
		buf.rewind();
		return buf;
	}
}
