package nodes;

import javax.vecmath.Vector3d;

import node.ComputeNode;
import node.NodeVar;

public class Dot extends ComputeNode {

	public NodeVar<Vector3d> inVec1;
	public NodeVar<Vector3d> inVec2;
	public NodeVar<Double> outDot;

	public Dot() {
		inVec1 = addInput(new NodeVar<Vector3d>("vec1", this));
		inVec2 = addInput(new NodeVar<Vector3d>("vec2", this));
		outDot = addOutput(new NodeVar<Double>("dot", this));
	}

	@Override
	protected void compute() {
		super.compute();
		outDot.value = inVec1.value.dot(inVec2.value);
	}
}
