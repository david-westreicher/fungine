package nodes;

import node.ComputeNode;
import node.NodeVar;

public class Addition extends ComputeNode {

	public NodeVar<Double> inNum1;
	public NodeVar<Double> inNum2;
	public NodeVar<Double> outAdd;

	public Addition() {
		inNum1 = addInput(new NodeVar<Double>("num1", this));
		inNum2 = addInput(new NodeVar<Double>("num2", this));
		outAdd = addOutput(new NodeVar<Double>("add", this));
	}

	@Override
	protected void compute() {
		super.compute();
		outAdd.value = inNum1.value + inNum2.value;
	}
}
