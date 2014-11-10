package rendering.material;

import java.util.Arrays;

import javax.media.opengl.GL3;

import manager.UberManager;
import shader.ShaderScript;

import com.jogamp.opengl.util.texture.Texture;

public class Material {

	public enum Map {
		NORMAL_MAP, COLOR_MAP, MASK_MAP, SPEC_MAP
	}

	public static final Map mapValues[] = Map.values();

	public String name;
	private String[] mapFiles;
	// TODO do we need this?
	public float[] color;

	public Material(String name) {
		this.name = name;
		this.mapFiles = new String[mapValues.length];
	}

	public void activate(GL3 gl) {
		int index = 0;
		for (int i = 0; i < mapValues.length; i++) {
			String mapFile = mapFiles[i];
			if (mapFile == null)
				continue;
			Texture tex = UberManager.getTexture(mapFile);
			if (tex != null) {
				ShaderScript.setUniformTexture(gl, mapValues[i].toString(),
						index++, tex.getTextureObject(gl));
			}
		}
	}

	@Override
	public String toString() {
		return "Material [name=" + name + ", maps=" + Arrays.toString(mapFiles)
				+ "]";
	}

	public boolean has(Map map) {
		return mapFiles[map.ordinal()] != null;
	}

	public void set(Map map, String mapFile) {
		mapFiles[map.ordinal()] = mapFile;
	}
}