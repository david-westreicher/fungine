package rendering;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;

import settings.Settings;
import util.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class TextureHelper {
	private Map<String, int[]> textures = new HashMap<String, int[]>();

	public void createTex(GL2GL3 gl, String name, int width, int height,
			boolean linear, int texparam, boolean withFrameBuffer,
			boolean floatTexture) {
		int[] fboId = new int[1];
		int[] texId = new int[1];
		if (withFrameBuffer)
			gl.glGenFramebuffers(1, fboId, 0);
		gl.glGenTextures(1, texId, 0);

		gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				linear ? GL.GL_LINEAR : GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				linear ? GL.GL_LINEAR : GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, texparam);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, texparam);
		if (floatTexture)
			gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA16F, width,
					height, 0, GL2.GL_RGBA, GL.GL_FLOAT, null);
		else
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, width, height, 0,
					GL.GL_BGRA, GL.GL_UNSIGNED_BYTE, null);

		if (withFrameBuffer) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);
			gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER,
					GL2.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, texId[0], 0);
		}

		addTexture(name, new int[] { texId[0], withFrameBuffer ? fboId[0] : 0 });
		if (withFrameBuffer) {
			int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
			if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
				Log.log(this, "Frame buffer object successfully created");
			} else {
				throw new IllegalStateException(
						"Frame Buffer Oject not created.");
			}
		}
	}

	public void createTex(GL2GL3 gl, String name) {
		createTex(gl, name, Settings.WIDTH, Settings.HEIGHT, true,
				GL2.GL_CLAMP_TO_EDGE, true, false);
	}

	public void createShadowFob(GL2 gl, String name, int width, int height) {
		int[] fboId = new int[1];
		int[] texId = new int[1];
		gl.glGenFramebuffers(1, fboId, 0);
		gl.glGenTextures(1, texId, 0);

		gl.glBindTexture(GL.GL_TEXTURE_2D, texId[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR);// before:GL_NEAREST
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_MODE,
				GL2.GL_COMPARE_R_TO_TEXTURE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_FUNC,
				GL.GL_LEQUAL);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_DEPTH_TEXTURE_MODE,
				GL2.GL_INTENSITY);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);

		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT32F, width,
				height, 0, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_BYTE,
				Buffers.newDirectByteBuffer(width * height * 4));

		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);

		gl.glDrawBuffer(GL2.GL_NONE);
		gl.glReadBuffer(GL2.GL_NONE);

		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT,
				GL.GL_TEXTURE_2D, texId[0], 0);

		addTexture(name, new int[] { texId[0], fboId[0] });

		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			Log.log(this, "Frame buffer object successfully created");
		} else {
			throw new IllegalStateException("Frame Buffer Oject not created.");
		}
	}

	protected Texture createCubeMap(GL2 gl, String img) {
		gl.glEnable(GL2.GL_TEXTURE_CUBE_MAP_SEAMLESS);
		Texture cubeMapTex = TextureIO.newTexture(GL.GL_TEXTURE_CUBE_MAP);
		// String[] shortCuts = new String[] { "east.bmp", "west.bmp", "up.bmp",
		// "down.bmp", "north.bmp", "south.bmp" };
		// String[] shortCuts = new String[] { "r.jpg", "l.jpg", "u.jpg",
		// "d.jpg",
		// "f.jpg", "b.jpg" };
		// String[] shortCuts = new String[] { "+Z.tga", "-Z.tga", "+Y.tga",
		// "-Y.tga", "+X.tga", "-X.tga" };
		String[] shortCuts = new String[] { "", "", "", "", "", "" };
		Log.log(this, "create cubemap: " + img);
		try {
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[0]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[1]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[2]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[3]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[4]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
			cubeMapTex.updateImage(
					gl,
					TextureIO.newTextureData(gl.getGLProfile(), new File(img
							+ shortCuts[5]), false, null),
					GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
			return cubeMapTex;
		} catch (GLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int[] getTextureInformation(String name) {
		int[] ret = textures.get(name);
		if (ret == null)
			Log.err(this, "Can't find texture: " + name);
		return ret;
	}

	public void addTexture(String name, int[] fob) {
		if (textures.containsKey(name))
			throw new RuntimeException("Texture " + name + " already exists");
		else
			textures.put(name, fob);
	}

	public void dispose(GL2 gl) {
		for (int[] inf : textures.values()) {
			if (inf.length == 2 && inf[0] != 0) {
				gl.glDeleteTextures(1, new int[] { inf[0] }, 0);
				if (inf[1] != 0) {
					gl.glDeleteFramebuffers(1, new int[] { inf[1] }, 0);
				}
			}
		}
		textures.clear();
	}

	public void createGBuffer(GL2 gl, String name) {
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
		initTextures(gl, texId);
		int[] fob = new int[] { fboId[0], texId[0], texId[1], texId[2] };
		addTexture(name, fob);
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

	private void initTextures(GL2 gl, int[] texId) {
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
			int formatSize = Buffers.SIZEOF_FLOAT;
			int type = GL2.GL_FLOAT;
			switch (index) {
			case 0:
				internalFormat = GL2.GL_RGBA8;
				formatSize = Buffers.SIZEOF_BYTE;
				type = GL2.GL_UNSIGNED_BYTE;
				break;
			}
			gl.glTexImage2D(
					GL.GL_TEXTURE_2D,
					0,
					internalFormat,
					Settings.WIDTH,
					Settings.HEIGHT,
					0,
					GL.GL_RGBA,
					type,
					Buffers.newDirectByteBuffer(Settings.WIDTH
							* Settings.HEIGHT * 4 * formatSize));
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
}
