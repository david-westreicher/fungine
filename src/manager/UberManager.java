package manager;

import io.IO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.media.opengl.GLException;

import rendering.RenderUpdater;
import rendering.util.GLRunnable;
import settings.Settings;
import shader.Shader;
import shader.ShaderScript;
import shader.ShaderUtil;
import util.Log;
import browser.AwesomiumWrapper;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class UberManager {

	private static List<String> loadingTextures = new ArrayList<String>();
	private static List<Shader> loadingShaders = new ArrayList<Shader>();
	private static Map<String, Texture> textures = new HashMap<String, Texture>();
	private static Map<Shader, ShaderScript> shaders = new HashMap<Shader, ShaderScript>();

	public static Texture getTexture(final String name) {
		return getTexture(name, false);
	}

	public static Texture getTexture(final String name,
			final boolean engineFolder) {
		if (name == null || loadingTextures.contains(name)
				|| RenderUpdater.getGLProfile() == null)
			return null;
		if (name.equals(AwesomiumWrapper.BROWSER_TEXTURE))
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
								RenderUpdater.getGLProfile(), new File(
										(engineFolder ? Settings.ENGINE_FOLDER
												: Settings.RESSOURCE_FOLDER)
												+ name), false, null);
						RenderUpdater.queue(new GLRunnable() {

							@Override
							public void run(GL2 gl) {
								Texture text;
								try {
									text = TextureIO.newTexture(textData);
									text.bind(gl);
									text.setTexParameteri(gl,
											GL2.GL_TEXTURE_WRAP_S,
											GL2.GL_REPEAT);
									text.setTexParameteri(gl,
											GL2.GL_TEXTURE_WRAP_T,
											GL2.GL_REPEAT);
									text.setTexParameteri(gl,
											GL2.GL_TEXTURE_MAG_FILTER,
											GL2.GL_LINEAR);
									text.setTexParameteri(gl,
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
			RenderUpdater.executeInOpenGLContext(new GLRunnable() {
				@Override
				public void run(GL2 gl) {
					Log.log(UberManager.class, "destroying " + s);
					t.destroy(gl);
				}
			});
		}

	}

	public static void clear() {

		RenderUpdater.executeInOpenGLContext(new GLRunnable() {
			@Override
			public void run(GL2 gl) {
				clearNow(gl);
			}
		});
	}

	public static void clearNow(GL2 gl) {
		for (String s : textures.keySet()) {
			Log.log(UberManager.class, "destroying " + s);
			textures.get(s).destroy(gl);
		}
		textures.clear();
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
			RenderUpdater.executeInOpenGLContext(new GLRunnable() {
				@Override
				public void run(GL2 gl) {
					compileShader(gl, shader);
				}
			});
			return null;
		}
	}

	protected static void compileShader(GL2 gl, final Shader shader) {
		ShaderUtil.compile(gl, shader.file,
				new ShaderUtil.ShaderCompiledListener() {
					@Override
					public void shaderCompiled(ShaderScript shaderprogram) {
						Log.log(UberManager.class, shaderprogram, " compiled",
								shader);
						shaders.put(shader, shaderprogram);
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
				RenderUpdater.executeInOpenGLContext(new GLRunnable() {
					@Override
					public void run(GL2 gl) {
						ss.deleteShader(gl);
					}
				});
			else
				Log.err(UberManager.class, "couldn't remove: " + changedShader);
		} else {
			Log.err(UberManager.class, "couldn't update: " + s);
		}
	}

	public static void initializeShaders(GL2 gl) {
		for (Shader s : Shader.values())
			compileShader(gl, s);
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
