package shader;

import io.IO;
import io.IO.LineComparer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

import settings.Settings;
import util.Log;

import com.jogamp.common.nio.Buffers;

public class ShaderUtil {
	public static final String FRAGMENT_SPLITTER = "//fragment";

	public interface ShaderCompiledListener {
		public void shaderCompiled(ShaderScript shaderprogram);
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

	public static void compile(GL2 gl, String file, ShaderCompiledListener r) {
		Log.log(ShaderUtil.class, "starting to compile " + file);
		String shader = IO.readToString(Settings.ENGINE_FOLDER, file,
				new Import());
		// Log.log(ShaderUtil.class, shader);
		compileFromString(gl, shader, file, r);
	}

	public static void compileFromString(GL2GL3 gl, String shader, String name,
			ShaderCompiledListener r) {
		String[] shaders = shader.split(FRAGMENT_SPLITTER);
		Log.log(ShaderUtil.class, "trying to compile: " + name + "\n" + shader);
		if (shaders.length != 2)
			throw new RuntimeException("could'nt parse shader " + name);
		else
			compileShader(gl, shaders[0], shaders[1], name, r);
	}

	public static void compileShader(GL2GL3 gl, final String vertexShader,
			final String fragmentShader, final String name,
			final ShaderCompiledListener r) {
		int v = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		int f = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
		int shaderprogram = 0;
		gl.glShaderSource(v, 1, new String[] { vertexShader }, (int[]) null, 0);
		gl.glCompileShader(v);
		if (checkCompileError(gl, v, name + " vertex shader", true))
			return;
		gl.glShaderSource(f, 1, new String[] { fragmentShader }, (int[]) null,
				0);
		gl.glCompileShader(f);
		if (checkCompileError(gl, f, name + " fragment shader", true))
			return;
		shaderprogram = gl.glCreateProgram();

		gl.glAttachShader(shaderprogram, v);
		gl.glAttachShader(shaderprogram, f);
		gl.glLinkProgram(shaderprogram);
		if (checkCompileError(gl, shaderprogram, name + " linked shader", false))
			return;
		gl.glValidateProgram(shaderprogram);
		if (shaderprogram != 0) {
			r.shaderCompiled(new ShaderScript(shaderprogram, name));
		}
	}

	public static boolean checkCompileError(GL2GL3 gl, int id, String effect,
			boolean isCompile) {
		IntBuffer status = Buffers.newDirectIntBuffer(1);
		if (isCompile)
			gl.glGetShaderiv(id, GL2.GL_COMPILE_STATUS, status);
		else
			gl.glGetProgramiv(id, GL2.GL_LINK_STATUS, status);
		if (status.get() == GL.GL_FALSE) {
			getInfoLog(gl, id, effect);
			return true;
		} else {
			Log.log("Shader Successfully compiled " + effect);
			return false;
		}
	}

	public static void getInfoLog(GL2GL3 gl, int id, String effect) {
		IntBuffer infoLogLength = Buffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(id, GL2.GL_INFO_LOG_LENGTH, infoLogLength);

		ByteBuffer infoLog = Buffers.newDirectByteBuffer(infoLogLength.get(0));
		gl.glGetShaderInfoLog(id, infoLogLength.get(0), null, infoLog);

		String infoLogString = Charset.forName("US-ASCII").decode(infoLog)
				.toString();
		Log.err("Shader compile error: in " + effect + "\n" + infoLogString);
	}

}
