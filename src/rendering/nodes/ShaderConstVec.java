package rendering.nodes;

import javax.vecmath.Vector3f;

import node.NodeVar;

public class ShaderConstVec extends ShaderNode {

	private Vector3f vec;
	private String name;
	public NodeVar<Uniform<Vector3f>> outConstVec;

	public ShaderConstVec(String name, float x, float y, float z) {
		this.name = name;
		this.vec = new Vector3f(x, y, z);
		outConstVec = addOutput(new NodeVar<Uniform<Vector3f>>("variable name",
				this));
	}

	@Override
	void addVertexUniforms(StringBuilder sb) {
		sb.append("const vec3 " + name + " = vec3(" + vec.x + "," + vec.y + ","
				+ vec.z + ");\n");
	}

	public void compute() {
		outConstVec.value = new Uniform<Vector3f>(name);
	}

	@Override
	void addVertexCode(StringBuilder sb) {
	}

	@Override
	void addFragmentUniforms(StringBuilder sb) {
		sb.append("const vec3 " + name + " = vec3(" + vec.x + "," + vec.y + ","
				+ vec.z + ");\n");
	}

	@Override
	void addUniformCode(StringBuilder sb) {
	}

}
