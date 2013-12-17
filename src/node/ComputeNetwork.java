package node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import node.Network.Edge;
import node.Network.Node;
import node.NodeVar.VarConnection;

public class ComputeNetwork {
	private Network network = new Network();
	private Map<ComputeNode, Node> computeToNodes = new HashMap<ComputeNode, Node>();
	private Map<Node, ComputeNode> nodesToCompute = new HashMap<Node, ComputeNode>();

	public ComputeNode createNode(String name) {
		try {
			Class<?> cl = Class.forName("node." + name);
			return (ComputeNode) cl.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public <T> void addConnection(VarConnection var) {
		network.addConnection(new Edge(createProxyNode(var.out.getNode()),
				createProxyNode(var.in.getNode())));
	}

	private Node createProxyNode(ComputeNode node) {
		Node proxy = computeToNodes.get(node);
		if (proxy == null) {
			proxy = new Node();
			computeToNodes.put(node, proxy);
			nodesToCompute.put(proxy, node);
		}
		return proxy;
	}

	public void compute() {
		List<Node> topologicalOrder = network.getTopologicalOrder();
		for (Node n : topologicalOrder) {
			ComputeNode cnode = nodesToCompute.get(n);
			cnode.compute();
			cnode.updateOutputs();
		}
	}

	public void reset() {
		network.reset();
		computeToNodes.clear();
		nodesToCompute.clear();
	}

}
