package node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ComputeNode {
	private static int ID = 0;
	private List<NodeVar<?>> out = new ArrayList<NodeVar<?>>();
	private List<NodeVar<?>> in = new ArrayList<NodeVar<?>>();
	private Map<String, InternalInfo<?>> internals = new HashMap<String, InternalInfo<?>>();
	private transient List<NodeVar<?>> internalNodes = new ArrayList<NodeVar<?>>();
	private String name;
	public String id;

	public ComputeNode() {
		this.name = this.getClass().getSimpleName();
		this.id = name + ID++;
	}

	public class InternalInfo<T> {

		private String name;
		private T val;

		public InternalInfo(String name, T value) {
			this.name = name;
			this.val = value;
		}
	}

	protected <T> NodeVar<T> addInput(NodeVar<T> var) {
		in.add(var);
		return var;
	}

	protected <T> NodeVar<T> addOutput(NodeVar<T> var) {
		out.add(var);
		return var;
	}

	protected <T> NodeVar<T> addInternal(NodeVar<T> var, Class<T> classType) {
		internals.put(var.name, new InternalInfo<T>(classType.getName(),
				var.value));
		internalNodes.add(var);
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

	public void changed(String internalName, Float val) {
		for (NodeVar<?> var : internalNodes)
			if (var.name.equals(internalName)) {
				NodeVar<Float> floatVar = (NodeVar<Float>) var;
				floatVar.value = val;
				((InternalInfo<Float>) internals.get(internalName)).val = val;
			}
	}
}
