package node;

import java.util.ArrayList;
import java.util.List;

public abstract class ComputeNode {
	private static int ID = 0;
	private List<NodeVar<?>> out = new ArrayList<NodeVar<?>>();
	private List<NodeVar<?>> in = new ArrayList<NodeVar<?>>();
	private String name;
	private String id;

	public ComputeNode() {
		this.name = this.getClass().getSimpleName();
		this.id = name + ID++;
	}

	protected <T> NodeVar<T> addInput(NodeVar<T> var) {
		in.add(var);
		return var;
	}

	protected <T> NodeVar<T> addOutput(NodeVar<T> var) {
		out.add(var);
		return var;
	}

	public void compute() {
		// Log.log(this, "compute");
	}

	public void updateOutputs() {
		for (NodeVar<?> var : out)
			updateOutput(var);
	}

	private <T> void updateOutput(NodeVar<T> var) {
		T val = var.value;
		for (NodeVar<T> in : var.dependencies) {
			in.value = val;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append("\n\tIN:\n");
		for (NodeVar<?> var : in) {
			sb.append("\t\t");
			sb.append(var.name);
			sb.append(": ");
			sb.append(var.value);
			sb.append("\n");
		}
		sb.append("\n\tOUT:");
		for (NodeVar<?> var : out) {
			sb.append("\t");
			sb.append(var.name);
			sb.append(": ");
			sb.append(var.value);
			sb.append("\n");
		}
		return sb.toString();
	}

}
