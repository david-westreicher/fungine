package rendering.nodes;

import javax.media.opengl.GL2;

import node.NodeVar;

import rendering.nodes.ShaderNode.RenderNode;

public class DrawQuad extends RenderNode {

	public NodeVar<Float> inWidth;
	public NodeVar<Float> inHeight;

	public DrawQuad() {
		inWidth = addInput(new NodeVar<Float>("width", this));
		inHeight = addInput(new NodeVar<Float>("height", this));
	}

	@Override
	boolean init(GL2 gl) {
		return true;
	}

	@Override
	void end() {
	}

	@Override
	void draw(GL2 gl) {
		float width = inWidth.value;
		float height = inHeight.value;
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(width, 0, 0);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(width, height, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(0, height, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(0, 0, 0);
		gl.glEnd();
	}

	@Override
	void addVertexUniforms(StringBuilder sb) {
	}

	@Override
	void addVertexCode(StringBuilder sb) {
	}

	@Override
	void addFragmentUniforms(StringBuilder sb) {
	}

	@Override
	void addUniformCode(StringBuilder sb) {
	}

}
