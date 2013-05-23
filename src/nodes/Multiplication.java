package nodes;

import node.ComputeNode;
import node.NodeVar;

public class Multiplication extends ComputeNode {

	public NodeVar<Double> inNum1;
	public NodeVar<Double> inNum2;
	public NodeVar<Double> outMul;

	public Multiplication() {
		inNum1 = addInput(new NodeVar<Double>("num1", this));
		inNum2 = addInput(new NodeVar<Double>("num2", this));
		outMul = addOutput(new NodeVar<Double>("mul", this));
	}

	@Override
	protected void compute() {
		super.compute();
		outMul.value = inNum1.value * inNum2.value;
	}
}