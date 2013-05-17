package util;

import javax.media.opengl.GL2;

import manager.UberManager;
import shader.ShaderScript;

import com.jogamp.opengl.util.texture.Texture;

public class Material {

	String name;
	public float ns;
	public String texture;
	public String normalMap;
	public String specMap;
	public String displacementMap;
	public String maskMap;
	public float[] color = null;

	public Material(String name) {
		this.name = name;
	}

	public void activate(GL2 gl) {
		if (color != null)
			gl.glColor3fv(color, 0);
		Texture colorTexture = UberManager.getTexture(texture);
		Texture normalTexture = UberManager.getTexture(normalMap);
		Texture specTexture = UberManager.getTexture(specMap);
		Texture displacementTexture = UberManager.getTexture(displacementMap);
		if (colorTexture != null) {
			ShaderScript.setUniformTexture("tex", 0,
					colorTexture.getTextureObject(gl));
			ShaderScript.setUniform("hasTexture", true);
		} else
			ShaderScript.setUniform("hasTexture", false);
		if (normalTexture != null) {
			ShaderScript.setUniformTexture("normalMap", 1,
					normalTexture.getTextureObject(gl));
			ShaderScript.setUniform("hasNormalMap", true);
		} else
			ShaderScript.setUniform("hasNormalMap", false);
		if (specTexture != null) {
			ShaderScript.setUniformTexture("specMap", 2,
					specTexture.getTextureObject(gl));
			ShaderScript.setUniform("hasSpecMap", true);
		} else
			ShaderScript.setUniform("hasSpecMap", false);
		if (displacementTexture != null) {
			ShaderScript.setUniformTexture("displacementMap", 3,
					displacementTexture.getTextureObject(gl));
			ShaderScript.setUniform("hasDisplacement", true);
		} else
			ShaderScript.setUniform("hasDisplacement", false);
		activateMaskMap(gl);
	}

	@Override
	public String toString() {
		return "Material [name=" + name + ", ns=" + ns + ", texture=" + texture
				+ ", normalMap=" + normalMap + ", maskMap=" + maskMap + "]";
	}

	public static void deactivate(GL2 gl) {
		if (ShaderScript.getActiveShader(gl) != null) {
			ShaderScript.setUniform("hasTexture", false);
			ShaderScript.setUniform("hasNormalMap", false);
			ShaderScript.setUniform("hasSpecMap", false);
			ShaderScript.setUniform("hasDisplacement", false);
			ShaderScript.setUniform("hasMask", false);
		}
	}

	public void activateMaskMap(GL2 gl) {
		Texture maskTexture = UberManager.getTexture(maskMap);
		if (maskTexture != null) {
			ShaderScript.setUniformTexture("maskMap", 4,
					maskTexture.getTextureObject(gl));
			ShaderScript.setUniform("hasMask", true);
		} else
			ShaderScript.setUniform("hasMask", false);
	}

}