package rendering.nodes;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import node.ComputeNode;
import node.NodeVar.VarConnection;
import rendering.nodes.ShaderNode.InitableNode;
import rendering.nodes.ShaderNode.RenderNode;
import shader.ShaderScript;
import shader.ShaderUtil;
import util.Log;

public class RenderGraph {
	List<ShaderNode> shaderNodes = new ArrayList<ShaderNode>();
	List<InitableNode> initableNodes = new ArrayList<InitableNode>();
	List<RenderNode> renderNodes = new ArrayList<RenderNode>();
	private List<ComputeNode> nodes = new ArrayList<ComputeNode>();
	List<RenderNode> finishedInits = new ArrayList<RenderNode>();
	List<VarConnection> connections = new ArrayList<VarConnection>();
	private ShaderScript shader;
	private String name;

	public void draw(GL2 gl) {
		if (shader == null) {
			generateShader(gl);
			return;
		}
		for (ComputeNode n : nodes) {
			n.compute();
			n.updateOutputs();
		}
		finishedInits.clear();
		for (RenderNode n : renderNodes) {
			boolean couldInit = n.init(gl);
			if (!couldInit) {
				break;
			} else {
				finishedInits.add(n);
			}
		}
		if (finishedInits.size() != renderNodes.size()) {
			for (RenderNode n : finishedInits) {
				n.end();
			}
			return;
		}
		shader.execute(gl);
		for (InitableNode n : initableNodes) {
			n.shaderInit(gl);
		}
		for (RenderNode n : renderNodes) {
			n.draw(gl);
		}
		shader.end(gl);
		for (RenderNode n : renderNodes) {
			n.end();
		}
	}

	private void generateShader(GL2 gl) {
		for (ShaderNode n : shaderNodes) {
			n.compute();
			n.updateOutputs();
		}
		StringBuilder sb = new StringBuilder();
		for (ShaderNode n : shaderNodes)
			n.addVertexUniforms(sb);
		sb.append("void main(void){\n");
		for (ShaderNode n : shaderNodes)
			n.addVertexCode(sb);
		sb.append("}\n");
		sb.append("//fragment\n");
		for (ShaderNode n : shaderNodes)
			n.addFragmentUniforms(sb);
		sb.append("void main(void){\n");
		for (ShaderNode n : shaderNodes)
			n.addUniformCode(sb);
		sb.append("}\n");
		ShaderUtil.compileFromString(gl, sb.toString(), name,
				new ShaderUtil.ShaderCompiledListener() {
					@Override
					public void shaderCompiled(int shaderprogram) {
						RenderGraph.this.shader = new ShaderScript(
								shaderprogram, name);
					}
				});
		Log.log(this, "shader:\n", sb.toString());
	}

	public void addNode(RenderNode n) {
		renderNodes.add(n);
		shaderNodes.add(n);
	}

	public void addNode(ShaderNode n) {
		shaderNodes.add(n);
	}

	public void addNode(ComputeNode n) {
		nodes.add(n);
	}

	public void addNode(InitableNode n) {
		initableNodes.add(n);
		nodes.add(n);
		shaderNodes.add(n);
	}

	public void addConnection(VarConnection var) {
		// var.out.addDependency(var.in);
		connections.add(var);
	}

	public void init() {
		// calculate topological order
	}

	public void changed(String nodeID, String internalName, float val) {
		for (ComputeNode n : nodes) {
			if (n.id.equals(nodeID))
				n.changed(internalName, val);
		}
	}

}
