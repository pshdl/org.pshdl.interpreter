/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *     
 *     Copyright (C) 2013 Karsten Becker (feedback (at) pshdl (dot) org)
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *     This License does not grant permission to use the trade names, trademarks,
 *     service marks, or product names of the Licensor, except as required for 
 *     reasonable and customary use in describing the origin of the Work.
 * 
 * Contributors:
 *     Karsten Becker - initial API and implementation
 ******************************************************************************/
package org.pshdl.interpreter.utils;

import java.util.*;

import org.pshdl.interpreter.*;

public class Graph<T extends Frame> {

	public static class Node<T> {
		public final T object;
		public final LinkedHashSet<Edge<T>> inEdges;
		public final LinkedHashSet<Edge<T>> outEdges;

		public Node(T object) {
			this.object = object;
			inEdges = new LinkedHashSet<Edge<T>>();
			outEdges = new LinkedHashSet<Edge<T>>();
		}

		public Node<T> addEdge(Node<T> node) {
			Edge<T> e = new Edge<T>(this, node);
			outEdges.add(e);
			node.inEdges.add(e);
			return this;
		}

		public Node<T> reverseAddEdge(Node<T> node) {
			Edge<T> e = new Edge<T>(node, this);
			node.outEdges.add(e);
			inEdges.add(e);
			return this;
		}

		@Override
		public String toString() {
			return object.toString();
		}
	}

	public static class Edge<T> {
		public final Node<T> from;
		public final Node<T> to;

		public Edge(Node<T> from, Node<T> to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + ((from == null) ? 0 : from.hashCode());
			result = (prime * result) + ((to == null) ? 0 : to.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Edge<?> other = (Edge<?>) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
	}

	public Node<T> createNode(T object) {
		return new Node<T>(object);
	}

	public ArrayList<Node<T>> sortNodes(List<Node<T>> allNodes, ExecutableModel em) {
		// L <- Empty list that will contain the sorted elements
		ArrayList<Node<T>> L = new ArrayList<Node<T>>();

		// S <- Set of all nodes with no incoming edges
		LinkedHashSet<Node<T>> S = new LinkedHashSet<Node<T>>();
		for (Node<T> n : allNodes) {
			if (n.inEdges.size() == 0) {
				S.add(n);
			}
		}

		// while S is non-empty do
		while (!S.isEmpty()) {
			// remove a node n from S
			Node<T> n = S.iterator().next();
			S.remove(n);

			// insert n into L
			L.add(n);

			// for each node m with an edge e from n to m do
			for (Iterator<Edge<T>> it = n.outEdges.iterator(); it.hasNext();) {
				// remove edge e from the graph
				Edge<T> e = it.next();
				Node<T> m = e.to;
				it.remove();// Remove edge from n
				m.inEdges.remove(e);// Remove edge from m

				// if m has no other incoming edges then insert m into S
				if (m.inEdges.isEmpty()) {
					S.add(m);
				}
			}
		}
		// Check to see if all edges are removed
		boolean cycle = false;
		for (Node<T> n : allNodes) {
			if (!n.inEdges.isEmpty()) {
				cycle = true;
				for (Edge<T> e : n.inEdges) {
					if (printEdges(e.from, new LinkedHashSet<T>(), n, em))
						throw new RuntimeException("Cycle present, topological sort not possible");
				}
			}
		}
		if (cycle)
			throw new RuntimeException("Cycle present, topological sort not possible");
		return L;
	}

	public boolean printEdges(Node<T> n, LinkedHashSet<T> visitedNodes, Node<T> target, ExecutableModel em) {
		if (visitedNodes.contains(n.object))
			return false;
		LinkedHashSet<T> newVisited = new LinkedHashSet<T>(visitedNodes);
		newVisited.add(n.object);
		for (Edge<T> e : n.inEdges) {
			// System.out.println(e.from + " -> " + e.to);
			if (e.to == target) {
				Frame lastFrame = target.object;
				for (T t : newVisited) {
					InternalInformation thisInt = em.internals[t.outputId];
					System.out.println(thisInt);
					lastFrame = t;
				}
				// System.out.println("Graph.printEdges()" + e.from + "->" +
				// e.to);
				return true;
			}
			if (printEdges(e.from, newVisited, target, em))
				return true;
		}
		return false;
	}
}
