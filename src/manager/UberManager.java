package manager;

import io.IO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

import rendering.RenderUpdater;
import settings.Settings;
import shader.Shader;
import shader.ShaderScript;
import shader.ShaderUtil;
import util.Log;
import util.RepeatedRunnable;
import browser.Browser;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class UberManager {

	private static List<String> loadingTextures = new ArrayList<String>();
	private static List<Shader> loadingShaders = new ArrayList<Shader>();
	private static Map<String, Texture> textures = new HashMap<String, Texture>();
	private static Worker worker;
	private static Map<Shader, ShaderScript> shaders = new HashMap<Shader, ShaderScript>();

	static {
		worker = new Worker();
		worker.start();
	}

	public static Texture getTexture(final String name) {
		return getTexture(name, false);
	}

	public static Texture getTexture(final String name,
			final boolean engineFolder) {
		if (name == null || loadingTextures.contains(name))
			return null;
		if (name.equals(Browser.TEXTURE_NAME))
			return RenderUpdater.getBrowser().getTexture();
		Texture t = textures.get(name);
		if (t != null)
			return t;
		else {
			loadingTextures.add(name);
			IO.queue(new Runnable() {
				@Override
				public void run() {
					try {
						Log.log(this, "loading: " + name);
						final TextureData textData = TextureIO.newTextureData(
								RenderUpdater.gl.getGLProfile(), new File(
										(engineFolder ? Settings.ENGINE_FOLDER
												: Settings.RESSOURCE_FOLDER)
												+ name), false, null);
						RenderUpdater.queue(new Runnable() {

							@Override
							public void run() {
								Texture text;
								try {
									text = TextureIO.newTexture(textData);

									text.bind(RenderUpdater.gl);
									text.setTexParameteri(RenderUpdater.gl,
											GL2.GL_TEXTURE_WRAP_S,
											GL2.GL_REPEAT);
									text.setTexParameteri(RenderUpdater.gl,
											GL2.GL_TEXTURE_WRAP_T,
											GL2.GL_REPEAT);
									text.setTexParameteri(RenderUpdater.gl,
											GL2.GL_TEXTURE_MAG_FILTER,
											GL2.GL_LINEAR);
									text.setTexParameteri(RenderUpdater.gl,
											GL2.GL_TEXTURE_MIN_FILTER,
											GL2.GL_LINEAR);
									Log.log(UberManager.class, name
											+ " successfully loaded!");
									textures.put(name, text);
									loadingTextures.remove(name);
								} catch (GLException e) {
									e.printStackTrace();
								} catch (RuntimeException e) {
									Log.err("still not finished to write to "
											+ name);
								}
							}
						});
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			return null;
		}
	}

	public static void textureChanged(final String s) {
		Log.log(UberManager.class, "trying to update " + s);
		final Texture t = textures.get(s);
		if (t != null) {
			textures.remove(s);
			RenderUpdater.executeInOpenGLContext(new Runnable() {
				@Override
				public void run() {
					Log.log(UberManager.class, "destroying " + s);
					t.destroy(RenderUpdater.gl);
				}
			});
		}

	}

	public static void clear() {

		RenderUpdater.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				for (String s : textures.keySet()) {
					Log.log(UberManager.class, "destroying " + s);
					textures.get(s).destroy(RenderUpdater.gl);
				}
				textures.clear();
			}
		});
	}

	public static final class Worker extends RepeatedRunnable {
		private List<Runnable> jobs = new LinkedList<Runnable>();

		public Worker() {
			super("IO Loader");
		}

		public void addJob(Runnable job) {
			synchronized (jobs) {
				jobs.add(job);
			}
		}

		@Override
		protected void executeRepeatedly() {
			Runnable job = null;
			synchronized (jobs) {
				job = jobs.size() == 0 ? null : jobs.remove(0);
			}
			if (job != null)
				job.run();
		}
	}

	public static ShaderScript getShader(final Shader shader) {
		if (shader == null || loadingShaders.contains(shader)) {
			return null;
		}
		ShaderScript s = shaders.get(shader);
		if (s != null)
			return s;
		else {
			loadingShaders.add(shader);
			RenderUpdater.executeInOpenGLContext(new Runnable() {
				@Override
				public void run() {
					compileShader(shader);

				}
			});
			return null;
		}
	}

	protected static void compileShader(final Shader shader) {
		ShaderUtil.compile(shader.file,
				new ShaderUtil.ShaderCompiledListener() {
					@Override
					public void shaderCompiled(int shaderprogram) {
						shaders.put(shader, new ShaderScript(shaderprogram,
								shader.file));
						loadingShaders.remove(shader);
					}
				});
	}

	public static void shaderChanged(String s) {
		Log.log(UberManager.class, "shader file " + s + " changed");
		Shader changedShader = null;
		for (Shader shader : Shader.values()) {
			if (s.equals(shader.file)) {
				changedShader = shader;
				break;
			}
		}
		if (changedShader != null) {
			final ShaderScript ss = shaders.remove(changedShader);
			if (ss != null)
				RenderUpdater.executeInOpenGLContext(new Runnable() {
					@Override
					public void run() {
						ss.deleteShader(RenderUpdater.gl);
					}
				});
			else
				Log.err(UberManager.class, "couldn't remove: " + changedShader);
		} else {
			Log.err(UberManager.class, "couldn't update: " + s);
		}
	}

	public static void initializeShaders() {
		for (Shader s : Shader.values())
			compileShader(s);
	}

	public static boolean areShaderInitialized(Shader[] values) {
		for (Shader s : Shader.values())
			if (getShader(s) == null) {
				return false;
			}
		return true;
	}

	public static int getTexturesToLoad() {
		return loadingTextures.size();
	}
}
