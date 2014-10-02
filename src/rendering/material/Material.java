package rendering.material;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import manager.UberManager;
import shader.ShaderScript;

import com.jogamp.opengl.util.texture.Texture;

public class Material {

	public String name;
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
			ShaderScript.setUniformTexture(gl, "tex", 0,
					colorTexture.getTextureObject(gl));
			ShaderScript.setUniform(gl, "hasTexture", true);
		} else
			ShaderScript.setUniform(gl, "hasTexture", false);
		if (normalTexture != null) {
			ShaderScript.setUniformTexture(gl, "normalMap", 1,
					normalTexture.getTextureObject(gl));
			ShaderScript.setUniform(gl, "hasNormalMap", true);
		} else
			ShaderScript.setUniform(gl, "hasNormalMap", false);
		if (specTexture != null) {
			ShaderScript.setUniformTexture(gl, "specMap", 2,
					specTexture.getTextureObject(gl));
			ShaderScript.setUniform(gl, "hasSpecMap", true);
		} else
			ShaderScript.setUniform(gl, "hasSpecMap", false);
		if (displacementTexture != null) {
			ShaderScript.setUniformTexture(gl, "displacementMap", 3,
					displacementTexture.getTextureObject(gl));
			ShaderScript.setUniform(gl, "hasDisplacement", true);
		} else
			ShaderScript.setUniform(gl, "hasDisplacement", false);
		activateMaskMap(gl);
	}

	public void activate(GL3 gl) {
		Texture colorTexture = UberManager.getTexture(texture);
		if (colorTexture != null)
			ShaderScript.setUniformTexture(gl, "tex", 0,
					colorTexture.getTextureObject(gl));
		else
			gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
	}

	@Override
	public String toString() {
		return "Material [name=" + name + ", ns=" + ns + ", texture=" + texture
				+ ", normalMap=" + normalMap + ", maskMap=" + maskMap + "]";
	}

	public static void deactivate(GL2 gl) {
		if (ShaderScript.getActiveShader(gl) != null) {
			ShaderScript.setUniform(gl, "hasTexture", false);
			ShaderScript.setUniform(gl, "hasNormalMap", false);
			ShaderScript.setUniform(gl, "hasSpecMap", false);
			ShaderScript.setUniform(gl, "hasDisplacement", false);
			ShaderScript.setUniform(gl, "hasMask", false);
		}
	}

	public void activateMaskMap(GL2 gl) {
		Texture maskTexture = UberManager.getTexture(maskMap);
		if (maskTexture != null) {
			ShaderScript.setUniformTexture(gl, "maskMap", 4,
					maskTexture.getTextureObject(gl));
			ShaderScript.setUniform(gl, "hasMask", true);
		} else
			ShaderScript.setUniform(gl, "hasMask", false);
	}

}