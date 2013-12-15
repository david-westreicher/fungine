package shader;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import util.Log;

import com.jogamp.opengl.util.texture.Texture;

public class ShaderScript {

	private static ShaderScript activatedShader = null;
	private static Map<String, Integer> locationCache = new HashMap<String, Integer>();
	private static final float[] tmpFloatArr = new float[4];
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

	public static void setUniform(GL2 gl, String str, float[] pos) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		switch (pos.length) {
		case 2:
			gl.glUniform2fv(location, 1, pos, 0);
			break;
		case 3:
			gl.glUniform3fv(location, 1, pos, 0);
			break;
		case 4:
			gl.glUniform4fv(location, 1, pos, 0);
			break;
		}
	}

	public static void setUniform(GL2 gl, String str, Vector3f value) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniform3fv(location, 1, tmpVecToFloatArr(value), 0);
	}

	private static float[] tmpVecToFloatArr(Vector3f vec) {
		tmpFloatArr[0] = vec.x;
		tmpFloatArr[1] = vec.y;
		tmpFloatArr[2] = vec.z;
		return tmpFloatArr;
	}

	public static void setUniform(GL2 gl, String str, float time) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniform1f(location, time);
	}

	public static void setUniform(GL2 gl, String str, int time) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniform1i(location, time);
	}

	public static void setUniform3fv(GL2 gl, String str, float[] scales) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniform3fv(location, scales.length / 3, scales, 0);
	}

	public static void setUniformMatrix3(GL2 gl, String str,
			FloatBuffer matrix, boolean transpose) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniformMatrix3fv(location, matrix.capacity() / 9, transpose,
				matrix);
	}

	public static void setUniformMatrix4(GL2 gl, String str,
			FloatBuffer matrix, boolean transpose) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniformMatrix4fv(location, matrix.capacity() / 16, transpose,
				matrix);
	}

	public static void setUniform3fv(GL2 gl, String str, FloatBuffer scales) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		gl.glUniform3fv(location, scales.limit() / 3, scales);
	}

	public static void setUniformTexture(GL2 gl, String string, int num,
			int texId) {
		int location = glGetUniformLocation(gl, getActiveShader(), string);
		gl.glUniform1i(location, num);
		switch (num) {
		case 0:
			gl.glActiveTexture(GL2.GL_TEXTURE0);
			break;
		case 1:
			gl.glActiveTexture(GL2.GL_TEXTURE1);
			break;
		case 2:
			gl.glActiveTexture(GL2.GL_TEXTURE2);
			break;
		case 3:
			gl.glActiveTexture(GL2.GL_TEXTURE3);
			break;
		case 4:
			gl.glActiveTexture(GL2.GL_TEXTURE4);
			break;
		}
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texId);
		if (num != 0)
			gl.glActiveTexture(GL2.GL_TEXTURE0);
	}

	public static void setUniformCubemap(GL2 gl, String string, int num,
			Texture cubeMap) {
		int location = glGetUniformLocation(gl, getActiveShader(), string);
		gl.glUniform1i(location, num);
		switch (num) {
		case 0:
			gl.glActiveTexture(GL2.GL_TEXTURE0);
			break;
		case 1:
			gl.glActiveTexture(GL2.GL_TEXTURE1);
			break;
		case 2:
			gl.glActiveTexture(GL2.GL_TEXTURE2);
			break;
		case 3:
			gl.glActiveTexture(GL2.GL_TEXTURE3);
			break;
		case 4:
			gl.glActiveTexture(GL2.GL_TEXTURE4);
			break;
		case 5:
			gl.glActiveTexture(GL2.GL_TEXTURE5);
			break;
		case 6:
			gl.glActiveTexture(GL2.GL_TEXTURE6);
			break;
		case 7:
			gl.glActiveTexture(GL2.GL_TEXTURE7);
			break;
		case 8:
			gl.glActiveTexture(GL2.GL_TEXTURE8);
			break;
		case 9:
			gl.glActiveTexture(GL2.GL_TEXTURE9);
			break;
		case 10:
			gl.glActiveTexture(GL2.GL_TEXTURE10);
			break;
		}
		bindCube(gl, cubeMap);
		if (num != 0)
			gl.glActiveTexture(GL2.GL_TEXTURE0);
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
		List<String> toDelete = new ArrayList<String>();
		for (String shaderLocation : locationCache.keySet()) {
			if (shaderLocation.startsWith(shaderNum + "")) {
				toDelete.add(shaderLocation);
			}
		}
		for (String delete : toDelete)
			locationCache.remove(delete);
	}

	public static boolean isShaderActivated(ShaderScript transformShader) {
		return activatedShader == transformShader;
	}

	public static boolean isShaderActivated() {
		return activatedShader != null;
	}

	public static void setUniform(GL2 gl, String str, boolean b) {
		setUniform(gl, str, b ? 1 : 0);
	}

	public static void setUniformMatrix3(GL2 gl, String str,
			float[] rotationMatrixArray, boolean transpose) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		int capacity = rotationMatrixArray.length;
		gl.glUniformMatrix3fv(location, capacity / 9, transpose,
				rotationMatrixArray, 0);
	}

	public static void setUniformMatrix4(GL2 gl, String str,
			float[] rotationMatrixArray, boolean transpose) {
		int location = glGetUniformLocation(gl, getActiveShader(), str);
		int capacity = rotationMatrixArray.length;
		gl.glUniformMatrix4fv(location, capacity / 16, transpose,
				rotationMatrixArray, 0);
	}

	private static int glGetUniformLocation(GL2 gl, int shadernum, String str) {
		Integer location = locationCache.get(shadernum + str);
		if (location == null) {
			location = gl.glGetUniformLocation(shadernum, str);
			if (location == -1) {
				Log.err(ShaderScript.class, "couldn't find location for "
						+ ShaderScript.activatedShader + ": " + str
						+ ", because location=" + location
						+ ", activatedShader=" + shadernum);
			}
			locationCache.put(shadernum + str, location);
		}
		return location;
	}

	public static ShaderScript getActiveShader(GL2 gl) {
		return activatedShader;
	}

}
