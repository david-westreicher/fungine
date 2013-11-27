package rendering;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class FPSRenderer {
	public static final int WIDTH = 200;
	public static final int HEIGHT = 80;
	public int currentIndex = 0;
	private ByteBuffer buf;

	public FPSRenderer(TextureHelper textures, GL2 gl) {
		textures.createTex("debugTexture", WIDTH, HEIGHT, false, GL2.GL_REPEAT,
				false);
		buf = ByteBuffer.allocate(HEIGHT * 4);
	}

	public void render(GL2 gl, TextureHelper textures, int width,
			long timePerRender, long timePerTick) {
		gl.glBindTexture(GL2.GL_TEXTURE_2D,
				textures.getTextureInformation("debugTexture")[0]);
		gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, currentIndex, 0, 1, HEIGHT,
				GL.GL_BGRA, GL.GL_UNSIGNED_BYTE,
				getByteBuffer(timePerRender, timePerTick));
		float translateTexX = ((float) currentIndex) / WIDTH;
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glColor4f(1f, 1f, 1f, 1f);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(translateTexX, 0);
		gl.glVertex3f(width - WIDTH, 0, 0);
		gl.glTexCoord2f(translateTexX, 1);
		gl.glVertex3f(width - WIDTH, HEIGHT, 0);
		gl.glTexCoord2f(translateTexX + 1, 1);
		gl.glVertex3f(width, HEIGHT, 0);
		gl.glTexCoord2f(translateTexX + 1, 0);
		gl.glVertex3f(width, 0, 0);
		gl.glEnd();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_BLEND);
		currentIndex = (currentIndex + 1) % WIDTH;
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
