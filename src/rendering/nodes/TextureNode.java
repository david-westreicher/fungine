package rendering.nodes;

import javax.vecmath.Vector3f;

import node.NodeVar;

import com.jogamp.opengl.util.texture.Texture;

public class TextureNode extends ShaderNode {
	private Texture texture;
	public NodeVar<Uniform<Vector3f>> color;

	public TextureNode() {
		color = addInput(new NodeVar<Uniform<Vector3f>>("color", this));
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
