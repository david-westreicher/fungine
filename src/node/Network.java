package node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class Network {

	private HashSet<Edge> allEdges = new HashSet<Edge>();

	public static class Node {
		public String name;
		public HashSet<Edge> parents = new HashSet<Edge>();
		public HashSet<Edge> children = new HashSet<Edge>();

		public void reset() {
			parents.clear();
			children.clear();
		}
	}

	public static class Edge {

		private Node parent;
		private Node child;

		public Edge(Node parent, Node child) {
			this.parent = parent;
			this.child = child;
		}

		@Override
		public boolean equals(Object obj) {
			Edge e = (Edge) obj;
			return e.parent == parent && e.child == child;
		}
	}

	public void reset() {
		allEdges.clear();
	}

	public void addConnection(Edge e) {
		allEdges.add(e);
	}

	public List<Node> getTopologicalOrder() {
		HashSet<Node> allNodes = resetParentChildren();
		for (Edge e : allEdges) {
			e.parent.children.add(e);
			e.child.parents.add(e);
		}
		List<Node> l = new ArrayList<Node>();
		Stack<Node> s = new Stack<Node>();
		for (Node n : allNodes) {
			if (n.parents.size() == 0)
				s.push(n);
		}
		while (s.size() > 0) {
			Node n = s.pop();
			l.add(n);
			for (Iterator<Edge> it = n.children.iterator(); it.hasNext();) {
				Edge e = it.next();
				Node m = e.child;
				it.remove();
				m.parents.remove(e);
				if (m.parents.isEmpty())
					s.push(m);
			}
		}
		return l;
	}

	private HashSet<Node> resetParentChildren() {
		HashSet<Node> allNodes = new HashSet<Node>();
		for (Edge e : allEdges) {
			Node parent = e.parent;
			Node child = e.child;
			allNodes.add(parent);
			allNodes.add(child);
		}
		for (Node n : allNodes) {
			n.reset();
		}
		return allNodes;
	}

}