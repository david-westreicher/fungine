package shader;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import rendering.RenderUpdater;
import util.Log;

import com.jogamp.opengl.util.texture.Texture;

public class ShaderScript {

	private static ShaderScript activatedShader = null;
	public int shaderNum;
	private String file;

	public ShaderScript(int shaderprogram, String file) {
		this.shaderNum = shaderprogram;
		this.file = file;
	}

	@Override
	public String toString() {
		return "ShaderScript [shaderNum=" + shaderNum + ", file=" + file + "]";
	}

	public void execute(GL2 gl) {
		if (shaderNum != 0) {
			if (activatedShader != null) {
				Log.log(this, activatedShader + " and " + this);
				throw new RuntimeException("activating shader in shader");
			}
			activatedShader = this;
			gl.glUseProgram(shaderNum);
		}
	}

	public void end(GL2 gl) {
		activatedShader = null;
		gl.glUseProgram(0);
	}

	public static void setUniform(String str, float[] pos) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		switch (pos.length) {
		case 2:
			RenderUpdater.gl.glUniform2fv(location, 1, pos, 0);
			break;
		case 3:
			RenderUpdater.gl.glUniform3fv(location, 1, pos, 0);
			break;
		case 4:
			RenderUpdater.gl.glUniform4fv(location, 1, pos, 0);
			break;
		}
	}

	public static void setUniform(String str, float time) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		RenderUpdater.gl.glUniform1f(location, time);
	}

	public static void setUniform(String str, int time) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		RenderUpdater.gl.glUniform1i(location, time);
	}

	public static void setUniform3fv(String str, float[] scales) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		RenderUpdater.gl.glUniform3fv(location, scales.length / 3, scales, 0);
	}

	public static void setUniformMatrix3(String str, FloatBuffer matrix,
			boolean transpose) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		RenderUpdater.gl.glUniformMatrix3fv(location, matrix.capacity() / 9,
				transpose, matrix);
	}

	public static void setUniformMatrix4(String str, FloatBuffer matrix,
			boolean transpose) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		RenderUpdater.gl.glUniformMatrix4fv(location, matrix.capacity() / 16,
				transpose, matrix);
	}

	public static void setUniform3fv(String str, FloatBuffer scales) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		RenderUpdater.gl.glUniform3fv(location, scales.limit() / 3, scales);
	}

	public static void setUniformTexture(String string, int num, int texId) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				string);
		RenderUpdater.gl.glUniform1i(location, num);
		switch (num) {
		case 0:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE0);
			break;
		case 1:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE1);
			break;
		case 2:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE2);
			break;
		case 3:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE3);
			break;
		case 4:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE4);
			break;
		}
		RenderUpdater.gl.glBindTexture(GL2.GL_TEXTURE_2D, texId);
		if (num != 0)
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE0);
	}

	public static void setUniformCubemap(String string, int num, Texture cubeMap) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				string);
		RenderUpdater.gl.glUniform1i(location, num);
		switch (num) {
		case 0:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE0);
			break;
		case 1:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE1);
			break;
		case 2:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE2);
			break;
		case 3:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE3);
			break;
		case 4:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE4);
			break;
		case 5:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE5);
			break;
		case 6:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE6);
			break;
		case 7:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE7);
			break;
		case 8:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE8);
			break;
		case 9:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE9);
			break;
		case 10:
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE10);
			break;
		}
		bindCube(RenderUpdater.gl, cubeMap);
		if (num != 0)
			RenderUpdater.gl.glActiveTexture(GL2.GL_TEXTURE0);
	}

	public static void bindCube(GL2 gl, Texture cubeMap) {
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
				GL2.GL_MODULATE);
		gl.glEnable(GL2.GL_TEXTURE_CUBE_MAP);
		gl.glEnable(GL2.GL_TEXTURE_GEN_S);
		gl.glEnable(GL2.GL_TEXTURE_GEN_T);
		gl.glEnable(GL2.GL_TEXTURE_GEN_R);

		gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_REFLECTION_MAP);
		gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_REFLECTION_MAP);
		gl.glTexGeni(GL2.GL_R, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_REFLECTION_MAP);

		cubeMap.bind(gl);
		cubeMap.enable(gl);
	}

	public static void releaseCube(GL gl, Texture cubeMap) {
		cubeMap.disable(gl);
		gl.glDisable(GL2.GL_TEXTURE_GEN_S);
		gl.glDisable(GL2.GL_TEXTURE_GEN_T);
		gl.glDisable(GL2.GL_TEXTURE_GEN_R);
		gl.glDisable(GL2.GL_TEXTURE_CUBE_MAP);
	}

	public static int getActiveShader() {
		return activatedShader.shaderNum;
	}

	public void deleteShader(GL2 gl) {
		gl.glDeleteShader(shaderNum);
	}

	public static boolean isShaderActivated(ShaderScript transformShader) {
		return activatedShader == transformShader;
	}

	public static boolean isShaderActivated() {
		return activatedShader != null;
	}

	public static void setUniform(String str, boolean b) {
		setUniform(str, b ? 1 : 0);
	}

	public static void setUniformMatrix3(String str,
			float[] rotationMatrixArray, boolean transpose) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		int capacity = rotationMatrixArray.length;
		RenderUpdater.gl.glUniformMatrix3fv(location, capacity / 9, transpose,
				rotationMatrixArray, 0);
	}

	public static void setUniformMatrix4(String str,
			float[] rotationMatrixArray, boolean transpose) {
		int location = RenderUpdater.gl.glGetUniformLocation(getActiveShader(),
				str);
		int capacity = rotationMatrixArray.length;
		RenderUpdater.gl.glUniformMatrix4fv(location, capacity / 16, transpose,
				rotationMatrixArray, 0);
	}

	public static ShaderScript getActiveShader(GL2 gl) {
		return activatedShader;
	}

}
