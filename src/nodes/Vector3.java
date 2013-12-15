package nodes;

import javax.vecmath.Vector3d;

import node.ComputeNode;
import node.NodeVar;

public class Vector3 extends ComputeNode {

	private Vector3d vec;
	public NodeVar<Vector3d> outVec;
	public NodeVar<Double> inX;
	public NodeVar<Double> inY;
	public NodeVar<Double> inZ;
	public NodeVar<Double> outX;
	public NodeVar<Double> outY;
	public NodeVar<Double> outZ;

	public Vector3(double x, double y, double z) {
		vec = new Vector3d(x, y, z);
		inX = addInput(new NodeVar<Double>("x", this));
		inY = addInput(new NodeVar<Double>("y", this));
		inZ = addInput(new NodeVar<Double>("z", this));
		outVec = addOutput(new NodeVar<Vector3d>("vec", this));
		outVec.value = vec;
		outX = addOutput(new NodeVar<Double>("x", this));
		outY = addOutput(new NodeVar<Double>("y", this));
		outZ = addOutput(new NodeVar<Double>("z", this));
	}

	@Override
	public void compute() {
		super.compute();
		vec.x = (inX.value == null) ? vec.x : inX.value;
		vec.y = (inY.value == null) ? vec.y : inY.value;
		vec.z = (inZ.value == null) ? vec.z : inZ.value;
		outX.value = vec.x;
		outY.value = vec.y;
		outZ.value = vec.z;
	}

}
