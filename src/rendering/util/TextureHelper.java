package rendering.util;

import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;

import settings.Settings;
import util.Log;

import com.jogamp.common.nio.Buffers;

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
					GL3.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
					GL3.GL_CLAMP_TO_EDGE);
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
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat,
					Settings.WIDTH, Settings.HEIGHT, 0, GL.GL_RGBA, type, null);
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

	public static class FrameBuffer {
		public int id = -1;
		public int texID = -1;
		public int depthTexID = -1;
		public int width;
		public int height;
		private boolean hasDepth;

		public FrameBuffer(GL3 gl, int width, int height) {
			this(gl, width, height, false, false);
		}

		public FrameBuffer(GL3 gl, int width, int height, boolean hasDepth) {
			this(gl, width, height, hasDepth, false);
		}

		public FrameBuffer(GL3 gl, int width, int height, boolean hasDepth,
				boolean hasAlpha) {
			this.width = width;
			this.height = height;
			this.hasDepth = hasDepth;
			int[] frameBuffer = new int[1];
			gl.glGenFramebuffers(1, frameBuffer, 0);
			this.id = frameBuffer[0];
			gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, frameBuffer[0]);
			{
				// Texture
				int[] renderedTexture = new int[1];
				gl.glGenTextures(1, renderedTexture, 0);
				this.texID = renderedTexture[0];
				gl.glBindTexture(GL3.GL_TEXTURE_2D, renderedTexture[0]);
				gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, hasAlpha ? GL3.GL_RGBA16F
						: GL3.GL_RGB16F, width, height, 0,
						hasAlpha ? GL3.GL_RGB : GL3.GL_RGBA, GL3.GL_FLOAT, null);
				gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S,
						GL3.GL_MIRRORED_REPEAT);
				gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T,
						GL3.GL_MIRRORED_REPEAT);
				enableMipMap(gl, false);
				gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
						GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D,
						renderedTexture[0], 0);
			}
			if (hasDepth) {
				int[] depthrenderbuffer = new int[1];
				gl.glGenRenderbuffers(1, depthrenderbuffer, 0);
				this.depthTexID = depthrenderbuffer[0];
				gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, depthrenderbuffer[0]);
				gl.glRenderbufferStorage(GL3.GL_RENDERBUFFER,
						GL3.GL_DEPTH_COMPONENT, width, height);
				gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER,
						GL3.GL_DEPTH_ATTACHMENT, GL3.GL_RENDERBUFFER,
						depthrenderbuffer[0]);
			}
			gl.glDrawBuffers(1, new int[] { GL3.GL_COLOR_ATTACHMENT0 }, 0);
			if (gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE)
				Log.err(this, "framebuffer not completed");
			gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
		}

		public void clear(GL3 gl) {
			if (hasDepth)
				gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
			else
				gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
		}

		private void enableMipMap(GL3 gl, boolean useMipMap) {
			gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER,
					useMipMap ? GL3.GL_NEAREST : GL3.GL_LINEAR);
			gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER,
					useMipMap ? GL3.GL_LINEAR_MIPMAP_NEAREST : GL3.GL_LINEAR);
		}

		public void bind(GL3 gl) {
			gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, id);
			gl.glViewport(0, 0, width, height);
		}

		public void generateMipMaps(GL3 gl) {
			gl.glBindTexture(GL3.GL_TEXTURE_2D, texID);
			gl.glGenerateMipmap(GL3.GL_TEXTURE_2D);
			enableMipMap(gl, true);
			gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
		}

		public void dispose(GL2GL3 gl) {
			Log.log(this, "disposing");
			gl.glDeleteFramebuffers(1, new int[] { id }, 0);
			gl.glDeleteTextures(1, new int[] { texID }, 0);
			gl.glDeleteRenderbuffers(1, new int[] { depthTexID }, 0);
		}
	}

	public static class GBuffer {
		private static final int[] gbufferDrawBuffer = new int[] {
				GL2.GL_COLOR_ATTACHMENT0, GL2.GL_COLOR_ATTACHMENT1 };
		private static final int[] normalDrawBuffer = new int[] { GL2.GL_COLOR_ATTACHMENT0 };

		private int width;
		private int height;
		private int fbo;
		private int depth;
		public int[] textures;

		public GBuffer(GL3 gl, int width, int height) {
			this.width = width;
			this.height = height;
			int[] fboId = new int[1];
			textures = new int[2];
			int[] depId = new int[1];
			gl.glGenFramebuffers(1, fboId, 0);
			gl.glGenTextures(textures.length, textures, 0);
			gl.glGenRenderbuffers(1, depId, 0);
			this.fbo = fboId[0];
			this.depth = depId[0];

			gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, depId[0]);
			gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER,
					GL2.GL_DEPTH_COMPONENT, width, height);
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[0]);
			gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
					GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, depId[0]);
			initTextures(gl, textures);
			int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
			if (status == GL2.GL_FRAMEBUFFER_COMPLETE) {
				gl.glDrawBuffers(gbufferDrawBuffer.length, gbufferDrawBuffer, 0);
				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
				Log.log(this, "Frame buffer object successfully created ("
						+ width + "," + height + ")");
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
				throw new IllegalStateException(
						"Frame Buffer Oject not created.");
			}
		}

		private void initTextures(GL3 gl, int[] texId) {
			int index = 0;
			for (Integer tex : texId) {
				gl.glBindTexture(GL.GL_TEXTURE_2D, tex);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
						GL.GL_LINEAR);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
						GL.GL_LINEAR);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
						GL3.GL_CLAMP_TO_EDGE);
				gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
						GL3.GL_CLAMP_TO_EDGE);
				gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL3.GL_RGB16F,
						Settings.WIDTH, Settings.HEIGHT, 0, GL3.GL_RGB,
						GL3.GL_FLOAT, null);
				gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER,
						GL3.GL_COLOR_ATTACHMENT0 + index, GL.GL_TEXTURE_2D,
						tex, 0);
				index++;
			}
		}

		public void bind(GL3 gl) {
			gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fbo);
			gl.glViewport(0, 0, width, height);
		}

		public void clear(GL3 gl) {
			gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		}

		public void dispose(GL2GL3 gl) {
			gl.glDeleteFramebuffers(1, new int[] { fbo }, 0);
			gl.glDeleteTextures(textures.length, textures, 0);
			gl.glDeleteRenderbuffers(1, new int[] { depth }, 0);
		}
	}
}
