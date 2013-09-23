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

import java.io.*;
import java.util.*;

import org.pshdl.interpreter.*;

public class Graph<T> {

	public static class Node<T> {
		public final T object;
		public final LinkedHashSet<Edge<T>> inEdges;
		public final LinkedHashSet<Edge<T>> outEdges;
		public int stage = -1;

		public Node(T object) {
			this.object = object;
			inEdges = new LinkedHashSet<Edge<T>>();
			outEdges = new LinkedHashSet<Edge<T>>();
		}

		public Node<T> addEdge(Node<T> node) {
			final Edge<T> e = new Edge<T>(this, node);
			outEdges.add(e);
			node.inEdges.add(e);
			return this;
		}

		public Node<T> reverseAddEdge(Node<T> node) {
			final Edge<T> e = new Edge<T>(node, this);
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
			final Edge<?> other = (Edge<?>) obj;
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

	public ArrayList<Node<T>> sortNodes(List<Node<T>> allNodes) throws CycleException {
		// L <- Empty list that will contain the sorted elements
		final ArrayList<Node<T>> L = new ArrayList<Node<T>>();

		// S <- Set of all nodes with no incoming edges
		LinkedHashSet<Node<T>> S = new LinkedHashSet<Node<T>>();
		LinkedHashSet<Node<T>> nextS = new LinkedHashSet<Node<T>>();
		for (final Node<T> n : allNodes) {
			if (n.inEdges.size() == 0) {
				S.add(n);
			}
		}
		int stage = 0;
		do {
			// while S is non-empty do
			while (!S.isEmpty()) {
				// remove a node n from S
				final Node<T> n = S.iterator().next();
				n.stage = stage;
				S.remove(n);

				// insert n into L
				L.add(n);

				// for each node m with an edge e from n to m do
				for (final Iterator<Edge<T>> it = n.outEdges.iterator(); it.hasNext();) {
					// remove edge e from the graph
					final Edge<T> e = it.next();
					final Node<T> m = e.to;
					it.remove();// Remove edge from n
					m.inEdges.remove(e);// Remove edge from m

					// if m has no other incoming edges then insert m into S
					if (m.inEdges.isEmpty()) {
						nextS.add(m);
					}
				}
			}
			stage++;
			S = nextS;
			nextS = new LinkedHashSet<Graph.Node<T>>();
		} while (!S.isEmpty());
		// Check to see if all edges are removed
		boolean cycle = false;
		for (final Node<T> n : allNodes) {
			if (!n.inEdges.isEmpty()) {
				cycle = true;
				// for (Edge<T> e : n.inEdges) {
				// Cycle findCycle = findCycle(e.from, new LinkedHashSet<T>(),
				// n);
				// if (findCycle != null)
				// throw new CycleException(findCycle);
				// }
				break;
			}
		}
		if (cycle)
			throw new RuntimeException("Cycle present, topological sort not possible");
		return L;
	}

	public static class CycleException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6522657621203932715L;

		public final Cycle cycle;

		public ExecutableModel model;

		public CycleException(Cycle cycle) {
			super();
			this.cycle = cycle;
		}

		public void explain(PrintStream out) {
			if (model == null)
				throw new IllegalArgumentException("You need to set the model first");
			out.printf("In order to compute %s (Frame %d) the following other variables need to be computed:\n", model.internals[cycle.frame.outputId], cycle.frame.uniqueID);
			explain(out, cycle);
		}

		public void explain(PrintStream out, Cycle current) {
			if (current.type == null)
				return;
			if (current.prior != null) {
				explain(out, current.prior);
			}

			if (model == null)
				throw new IllegalArgumentException("You need to set the model first");
			out.printf("\t%s (Frame %d) depency type %s prior frame: %s\n", model.internals[current.frame.outputId], current.frame.uniqueID, current.type,
					current.prior.frame.uniqueID);

		}
	}

	public static class Cycle {
		public static enum DependencyType {
			unknown, internal, negEdge, posEdge, posPred, negPred, order
		};

		public final Cycle prior;
		public final Frame frame;
		public final DependencyType type;

		public Cycle(Cycle prior, Frame frame, DependencyType type) {
			super();
			this.prior = prior;
			this.frame = frame;
			this.type = type;
		}

	}

	// public Cycle findCycle(Node<T> n, LinkedHashSet<T> visitedNodes, Node<T>
	// target) throws CycleException {
	// if (visitedNodes.contains(n.object))
	// return null;
	// LinkedHashSet<T> newVisited = new LinkedHashSet<T>(visitedNodes);
	// newVisited.add(n.object);
	// for (Edge<T> e : n.inEdges) {
	// // System.out.println(e.from + " -> " + e.to);
	// if (e.to == target) {
	// Frame lastFrame = target.object;
	// Cycle lastCycle = new Cycle(null, lastFrame, null);
	// for (Iterator<T> iterator = newVisited.iterator(); iterator.hasNext();) {
	// T t = iterator.next();
	// DependencyType type = DependencyType.unknown;
	// if (lastFrame.edgeNegDepRes == t.outputId) {
	// type = DependencyType.negEdge;
	// }
	// if (lastFrame.edgePosDepRes == t.outputId) {
	// type = DependencyType.posEdge;
	// }
	// for (int pnd : lastFrame.predNegDepRes) {
	// if (pnd == t.outputId) {
	// type = DependencyType.negPred;
	// }
	// }
	// for (int pnd : lastFrame.predPosDepRes) {
	// if (pnd == t.outputId) {
	// type = DependencyType.posPred;
	// }
	// }
	// if (lastFrame.executionDep == t.uniqueID)
	// type = DependencyType.order;
	// if (type == DependencyType.unknown) {
	// for (int t2 : lastFrame.internalDependencies) {
	// if (t2 == t.outputId)
	// type = DependencyType.internal;
	// }
	// }
	// lastCycle = new Cycle(lastCycle, t, type);
	// lastFrame = t;
	// }
	// return lastCycle;
	// }
	// Cycle findCycle = findCycle(e.from, newVisited, target);
	// if (findCycle != null)
	// return findCycle;
	// }
	// return null;
	// }
}
