package node;

import java.util.ArrayList;
import java.util.List;

import rendering.nodes.ShaderNode.Uniform;

public class NodeVar<T> {
	protected String name;
	private transient ComputeNode node;
	public T value;
	public transient List<NodeVar<T>> dependencies = new ArrayList<NodeVar<T>>();

	public NodeVar(String string, ComputeNode n) {
		this.name = string;
		this.node = n;
	}

	public ComputeNode getNode() {
		return node;
	}

	public void addDependency(NodeVar<T> in) {
		dependencies.add(in);
	}

	public static class VarConnection {
		protected NodeVar<?> out;
		protected NodeVar<?> in;
		private String outID;
		private String inID;
		private String ctype;

		public <T> VarConnection(NodeVar<T> out, NodeVar<T> in, Class<T> ctype) {
			this.out = out;
			this.in = in;
			this.outID = out.node.id;
			this.inID = in.node.id;
			if (ctype != null)
				this.ctype = ctype.getName();
			out.addDependency(in);
		}

		public <T> VarConnection(NodeVar<Uniform<T>> out, NodeVar<Uniform<T>> in) {
			this(out, in, null);
			this.ctype = "Uniform";
		}

		@Override
		public String toString() {
			return "VarConnection [out=" + out.name + ", in=" + in.name + "]";
		}

	}
}