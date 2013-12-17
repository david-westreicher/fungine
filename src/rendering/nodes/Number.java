package rendering.nodes;

import node.ComputeNode;
import node.NodeVar;

public class Number extends ComputeNode {
	public NodeVar<Float> outNum;

	public Number(Float number) {
		outNum = addOutput(new NodeVar<Float>("number", this));
		outNum.value = number;
		addInternal(outNum, Float.class);
	}
}