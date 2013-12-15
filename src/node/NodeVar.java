package node;

import java.util.ArrayList;
import java.util.List;

public class NodeVar<T> {
	protected String name;
	private ComputeNode node;
	public T value;
	public List<NodeVar<T>> dependencies = new ArrayList<NodeVar<T>>();

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

	public static class VarConnection<T> {
		protected NodeVar<T> out;
		protected NodeVar<T> in;

		public VarConnection(NodeVar<T> out, NodeVar<T> in) {
			this.out = out;
			this.in = in;
			out.addDependency(in);
		}

		@Override
		public String toString() {
			return "VarConnection [out=" + out.name + ", in=" + in.name + "]";
		}

	}
}