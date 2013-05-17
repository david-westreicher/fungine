package manager;

import io.IO;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import rendering.RenderUpdater;
import shader.ShaderScript;
import util.Log;

import com.jogamp.common.nio.Buffers;

public class ShaderManager extends Manager<ShaderScript> {

	public ShaderManager() {
		super("shader");
	}

	@Override
	protected void updateObject(ShaderScript t, String file) {
		Log.log(this, "updating " + file);
		String shader = IO.readToString(file);
		String[] shaders = shader.split("//fragment");
		if (shaders.length != 2)
			throw new RuntimeException("could'nt parse shader " + file);
		else
			compileShader(shaders[0], shaders[1], file, t);
	}

	private void compileShader(final String vertexShader,
			final String fragmentShader, final String file, final ShaderScript t) {
		RenderUpdater.executeInOpenGLContext(new Runnable() {
			@Override
			public void run() {
				GL2 gl = RenderUpdater.gl;
				int v = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
				int f = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
				int shaderprogram = 0;
				gl.glShaderSource(v, 1, new String[] { vertexShader },
						(int[]) null, 0);
				gl.glCompileShader(v);
				checkCompileError(gl, v, file + " vertex shader");
				gl.glShaderSource(f, 1, new String[] { fragmentShader },
						(int[]) null, 0);
				gl.glCompileShader(f);
				checkCompileError(gl, f, file + " fragment shader");
				shaderprogram = gl.glCreateProgram();

				gl.glAttachShader(shaderprogram, v);
				gl.glAttachShader(shaderprogram, f);
				gl.glLinkProgram(shaderprogram);
				gl.glValidateProgram(shaderprogram);
				if (shaderprogram != 0)
					t.shaderNum = shaderprogram;
			}

		});
	}

	private void checkCompileError(GL2 gl, int id, String effect) {
		IntBuffer status = Buffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(id, GL2.GL_COMPILE_STATUS, status);
		if (status.get() == GL.GL_FALSE) {
			getInfoLog(gl, id, effect);
		} else {
			Log.log("Shader Successfully compiled asdasdasd" + effect);
		}
	}

	private static void getInfoLog(GL2 gl, int id, String effect) {
		IntBuffer infoLogLength = Buffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(id, GL2.GL_INFO_LOG_LENGTH, infoLogLength);

		ByteBuffer infoLog = Buffers.newDirectByteBuffer(infoLogLength.get(0));
		gl.glGetShaderInfoLog(id, infoLogLength.get(0), null, infoLog);

		String infoLogString = Charset.forName("US-ASCII").decode(infoLog)
				.toString();
		Log.err("Shader compile error: in " + effect + "\n" + infoLogString);
	}

}
