package shader;

public enum Shader {
	DEFERRED("shader/deferred.glsl"), TEXTURE("shader/textureShader.glsl"), DEFERRED_LIGHT(
			"shader/deferredLighting.glsl"), SSAO("shader/ssao.glsl"), H_BLUR(
			"shader/hBlur.glsl"), V_BLUR("shader/vBlur.glsl"), DOF(
			"shader/bokeh.glsl"), TRANSFORM_TEXTURE(
			"shader/transformTexture.glsl"), TRANSFORM_SIMPLE(
			"shader/transformVertices.glsl"), BOKEH("shader/bokeh.glsl"), DEPTH(
			"shader/renderDepth.glsl"), SKINNING("shader/skinning.glsl"), TRANSFORM_SKINNING(
			"shader/transformTextureSkinning.glsl"), SKYBOX(
			"shader/skybox.glsl"), HATCH("shader/hatch.glsl"), VOXEL(
			"shader/voxel.glsl"), VOXEL_DEPTH("shader/voxelDepth.glsl");
	public String file;

	Shader(String file) {
		this.file = file;
	}
}