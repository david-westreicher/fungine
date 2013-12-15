package rendering.nodes;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import node.NodeVar;
import rendering.nodes.ShaderNode.RenderNode;

import com.jogamp.opengl.util.texture.Texture;

public class TextureNode extends RenderNode {
	public static final int WIDTH = 5;
	public static final int HEIGHT = 5;
	private Texture texture;
	public NodeVar<Uniform<Vector3f>> color;

	public TextureNode() {
		color = addInput(new NodeVar<Uniform<Vector3f>>("color", this));
	}

	public void draw(GL2 gl) {
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(WIDTH, 0, 0);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(WIDTH, HEIGHT, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(0, HEIGHT, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(0, 0, 0);
		gl.glEnd();
	}

	public void end() {
		// gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		// gl.glDisable(GL2.GL_TEXTURE_2D);
		// gl.glDisable(GL2.GL_BLEND);
	}

	public boolean init(GL2 gl) {
		// texture = UberManager.getTexture("img/daemon/color.jpg");
		// if (texture == null)
		// return false;
		// texture.bind(gl);
		// gl.glEnable(GL2.GL_BLEND);
		// gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		// gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glColor4f(1f, 1f, 1f, 1f);
		return true;
	}

	@Override
	public void addVertexUniforms(StringBuilder sb) {
	}

	@Override
	public void addVertexCode(StringBuilder sb) {
		sb.append("	gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix *gl_Vertex;\n");
		sb.append("	gl_TexCoord[0]= gl_MultiTexCoord0;\n");
		sb.append("	gl_FrontColor = gl_Color;\n");
	}

	@Override
	public void addFragmentUniforms(StringBuilder sb) {
		// sb.append("uniform sampler2D tex;\n");
		// sb.append("uniform float ambient;\n");
		// sb.append("uniform vec3 color;\n");
	}

	@Override
	public void addUniformCode(StringBuilder sb) {
		// sb.append("	gl_FragColor = texture2D(tex,gl_TexCoord[0].xy);\n");
		// sb.append("	gl_FragColor.rgb *=ambient;}\n");
		sb.append("	gl_FragColor = vec4(" + color.value.varName + ",1);\n");
	}

}
