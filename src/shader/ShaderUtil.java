package shader;

import io.IO;
import io.IO.LineComparer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import rendering.RenderUpdater;
import settings.Settings;
import util.Log;

import com.jogamp.common.nio.Buffers;

public class ShaderUtil {

	public interface ShaderCompiledListener {
		public void shaderCompiled(int shaderprogram);
	}

	private static class Import implements LineComparer {
		@Override
		public void compareLine(String line, StringBuilder sb) {
			if (line.startsWith("//import")) {
				String[] split = line.split("\\s+");
				sb.append("//import: shader/" + split[1] + " START\n");
				sb.append(IO.readToString(Settings.ENGINE_FOLDER, "shader/"
						+ split[1], this));
				sb.append("\n//import: shader/" + split[1] + " END\n");
			}
		}
	}

	public static void compile(String file, ShaderCompiledListener r) {
		Log.log(ShaderUtil.class, "starting to compile " + file);
		String shader = IO.readToString(Settings.ENGINE_FOLDER, file,
				new Import());
		// Log.log(ShaderUtil.class, shader);
		String[] shaders = shader.split("//fragment");
		if (shaders.length != 2)
			throw new RuntimeException("could'nt parse shader " + file);
		else
			compileShader(shaders[0], shaders[1], file, r);
	}

	public static void compileShader(final String vertexShader,
			final String fragmentShader, final String file,
			final ShaderCompiledListener r) {
		GL2 gl = RenderUpdater.gl;
		int v = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		int f = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
		int shaderprogram = 0;
		gl.glShaderSource(v, 1, new String[] { vertexShader }, (int[]) null, 0);
		gl.glCompileShader(v);
		checkCompileError(gl, v, file + " vertex shader");
		gl.glShaderSource(f, 1, new String[] { fragmentShader }, (int[]) null,
				0);
		gl.glCompileShader(f);
		checkCompileError(gl, f, file + " fragment shader");
		shaderprogram = gl.glCreateProgram();

		gl.glAttachShader(shaderprogram, v);
		gl.glAttachShader(shaderprogram, f);
		gl.glLinkProgram(shaderprogram);
		gl.glValidateProgram(shaderprogram);
		if (shaderprogram != 0)
			r.shaderCompiled(shaderprogram);
	}

	public static void checkCompileError(GL2 gl, int id, String effect) {
		IntBuffer status = Buffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(id, GL2.GL_COMPILE_STATUS, status);
		if (status.get() == GL.GL_FALSE) {
			getInfoLog(gl, id, effect);
		} else {
			Log.log("Shader Successfully compiled " + effect);
		}
	}

	public static void getInfoLog(GL2 gl, int id, String effect) {
		IntBuffer infoLogLength = Buffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(id, GL2.GL_INFO_LOG_LENGTH, infoLogLength);

		ByteBuffer infoLog = Buffers.newDirectByteBuffer(infoLogLength.get(0));
		gl.glGetShaderInfoLog(id, infoLogLength.get(0), null, infoLog);

		String infoLogString = Charset.forName("US-ASCII").decode(infoLog)
				.toString();
		Log.err("Shader compile error: in " + effect + "\n" + infoLogString);
	}

}
