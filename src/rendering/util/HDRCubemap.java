package rendering.util;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import util.HDRParser;
import util.HDRParser.HDRData;
import util.Log;

public class HDRCubemap {

	public int cubemapTex = -1;
	private int width;
	private int faceSize;

	public HDRCubemap(String file, GL3 gl) {
		HDRData hdrData = HDRParser.readHDR(file);
		int height = hdrData.height;
		width = hdrData.width;
		int[] cubemapTexArr = new int[1];
		gl.glGenTextures(1, cubemapTexArr, 0);
		cubemapTex = cubemapTexArr[0];
		gl.glBindTexture(GL3.GL_TEXTURE_CUBE_MAP, cubemapTex);
		gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_S,
				GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_T,
				GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_WRAP_R,
				GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_MAG_FILTER,
				GL3.GL_LINEAR);
		gl.glTexParameteri(GL3.GL_TEXTURE_CUBE_MAP, GL3.GL_TEXTURE_MIN_FILTER,
				GL3.GL_LINEAR);
		if (height * 3 / 4 != width)
			throw new RuntimeException("width!=height*3/4");
		faceSize = width / 3;

		// up,left,middle,right,down,behind
		// ___|u|___
		// _l_.m._r_
		// ...|d|
		// ...|b|
		for (int i = 0; i < 6; i++) {
			int target = 0;
			float data[] = new float[faceSize * faceSize * 3];
			switch (i) {
			case 0:
				fillData(data, hdrData.rgb, faceSize * 2, faceSize);
				target = GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
				break;
			case 1:
				fillData(data, hdrData.rgb, 0, faceSize);
				target = GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
				break;
			case 2:
				fillData(data, hdrData.rgb, faceSize, 0);
				target = GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
				break;
			case 3:
				fillData(data, hdrData.rgb, faceSize, faceSize * 2);
				target = GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
				break;
			case 4:
				fillData(data, hdrData.rgb, faceSize, faceSize);
				target = GL3.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
				break;
			case 5:
				fillData(data, hdrData.rgb, faceSize, faceSize * 3, true);
				target = GL3.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
				break;
			}
			gl.glTexImage2D(target, 0, GL3.GL_RGB16F, faceSize, faceSize, 0,
					GL3.GL_RGB, GL3.GL_FLOAT, FloatBuffer.wrap(data));
		}
		gl.glBindTexture(GL3.GL_TEXTURE_CUBE_MAP, 0);
	}

	private void fillData(float[] data, float[] rgb, int x0, int y0,
			boolean flip) {
		Log.log(this, x0, y0);
		Log.log(this, flip);
		int index = 0;
		if (flip)
			for (int y = y0 + faceSize - 1; y >= y0; y--) {
				for (int x = x0 + faceSize - 1; x >= x0; x--) {
					data[index++] = rgb[(x + y * width) * 3 + 0];
					data[index++] = rgb[(x + y * width) * 3 + 1];
					data[index++] = rgb[(x + y * width) * 3 + 2];
				}
			}
		else
			for (int y = y0; y < y0 + faceSize; y++) {
				for (int x = x0; x < x0 + faceSize; x++) {
					data[index++] = rgb[(x + y * width) * 3 + 0];
					data[index++] = rgb[(x + y * width) * 3 + 1];
					data[index++] = rgb[(x + y * width) * 3 + 2];
				}
			}
	}

	private void fillData(float[] data, float[] rgb, int x0, int y0) {
		fillData(data, rgb, x0, y0, false);
	}

	public void dispose(GL2GL3 gl) {
		if (cubemapTex > -1)
			gl.glDeleteTextures(1, new int[] { cubemapTex }, 0);
	}
}
