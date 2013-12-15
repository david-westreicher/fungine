package rendering.nodes;

import javax.media.opengl.GL2;

import node.ComputeNode;

public abstract class ShaderNode extends ComputeNode {

	abstract void addVertexUniforms(StringBuilder sb);

	abstract void addVertexCode(StringBuilder sb);

	abstract void addFragmentUniforms(StringBuilder sb);

	abstract void addUniformCode(StringBuilder sb);

	public class Uniform<T> {
		public String varName;

		public Uniform(String varName) {
			this.varName = varName;
		}
	}

	public static abstract class InitableNode extends ShaderNode {
		abstract void shaderInit(GL2 gl);
	}

	public static abstract class RenderNode extends ShaderNode {

		abstract boolean init(GL2 gl);

		abstract void end();

		abstract void draw(GL2 gl);

	}

}
