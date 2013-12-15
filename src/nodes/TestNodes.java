package nodes;

import javax.vecmath.Vector3d;

import node.ComputeNetwork;
import node.NodeVar.VarConnection;

public class TestNodes {

	public static void main(String[] args) {
		ComputeNetwork cn = new ComputeNetwork();
		Number n1 = new Number(3d);
		Number n2 = new Number(2d);
		Multiplication m1 = new Multiplication();
		Multiplication m2 = new Multiplication();
		Addition a1 = new Addition();
		Addition a2 = new Addition();
		cn.addConnection(new VarConnection<Double>(n1.outNum, m1.inNum1));
		cn.addConnection(new VarConnection<Double>(n1.outNum, m2.inNum1));
		cn.addConnection(new VarConnection<Double>(n1.outNum, a1.inNum1));
		cn.addConnection(new VarConnection<Double>(n2.outNum, m1.inNum2));
		cn.addConnection(new VarConnection<Double>(n2.outNum, a1.inNum2));
		cn.addConnection(new VarConnection<Double>(m1.outMul, m2.inNum2));
		cn.addConnection(new VarConnection<Double>(m2.outMul, a2.inNum1));
		cn.addConnection(new VarConnection<Double>(a1.outAdd, a2.inNum2));
		Vector3 v = new Vector3(2, 1, 0.4);
		Vector3 v2 = new Vector3(1, 0.1, 1);
		Dot dot = new Dot();
		cn.addConnection(new VarConnection<Double>(n1.outNum, v2.inY));
		cn.addConnection(new VarConnection<Vector3d>(v.outVec, dot.inVec1));
		cn.addConnection(new VarConnection<Vector3d>(v2.outVec, dot.inVec2));
		cn.compute();
		System.out.println(n1);
		System.out.println(n2);
		System.out.println(m1);
		System.out.println(m2);
		System.out.println(a1);
		System.out.println(a2);
		System.out.println(v);
		System.out.println(v2);
		System.out.println(dot);
	}
}
