package rendering.nodes;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import node.NodeVar;
import rendering.nodes.ShaderNode.InitableNode;
import shader.ShaderScript;

public class UniformVec extends InitableNode {

	private Vector3f vec;
	private String varName;
	public NodeVar<Uniform<Vector3f>> outConstVec;

	public UniformVec(String varName, float x, float y, float z) {
		this.varName = varName;
		this.vec = new Vector3f(x, y, z);
		outConstVec = addOutput(new NodeVar<Uniform<Vector3f>>("variable name",
				this));
		outConstVec.value = new Uniform<Vector3f>(varName);
	}

	public void compute() {
	}

	@Override
	void addVertexUniforms(StringBuilder sb) {
		sb.append("uniform vec3 " + varName + ";\n");
	}

	@Override
	void addVertexCode(StringBuilder sb) {
	}

	@Override
	void addFragmentUniforms(StringBuilder sb) {
		sb.append("uniform vec3 " + varName + ";\n");
	}

	@Override
	void addUniformCode(StringBuilder sb) {
	}

	@Override
	void shaderInit(GL2 gl) {
		ShaderScript.setUniform(gl, varName, vec);
	}

}
