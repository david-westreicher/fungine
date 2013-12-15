package nodes;

import node.ComputeNode;
import node.NodeVar;

public class Number extends ComputeNode {
	public NodeVar<Double> outNum;

	public Number(Double number) {
		outNum = addOutput(new NodeVar<Double>("number", this));
		outNum.value = number;
	}
}