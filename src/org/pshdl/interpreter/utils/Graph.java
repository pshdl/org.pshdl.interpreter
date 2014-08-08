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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.pshdl.interpreter.ExecutableModel;

public class Graph<T> {

	public static class Node<T> {
		public final T object;
		public final LinkedHashSet<Edge<T>> inEdges;
		public final LinkedHashSet<Edge<T>> outEdges;
		public int stage = -1;

		public Node(T object) {
			this.object = object;
			inEdges = new LinkedHashSet<>();
			outEdges = new LinkedHashSet<>();
		}

		public Node<T> addEdge(Node<T> node) {
			final Edge<T> e = new Edge<>(this, node);
			outEdges.add(e);
			node.inEdges.add(e);
			return this;
		}

		public Node<T> reverseAddEdge(Node<T> node) {
			final Edge<T> e = new Edge<>(node, this);
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

		@Override
		public String toString() {
			return from + " -> " + to;
		}
	}

	public Node<T> createNode(T object) {
		return new Node<>(object);
	}

	public ArrayList<Node<T>> sortNodes(List<Node<T>> allNodes) throws CycleException {
		// L <- Empty list that will contain the sorted elements
		final ArrayList<Node<T>> L = new ArrayList<>();

		// S <- Set of all nodes with no incoming edges
		LinkedHashSet<Node<T>> S = new LinkedHashSet<>();
		LinkedHashSet<Node<T>> nextS = new LinkedHashSet<>();
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
			nextS = new LinkedHashSet<>();
		} while (!S.isEmpty());
		// Check to see if all edges are removed
		boolean cycle = false;
		final long startTime = System.currentTimeMillis();
		for (final Node<T> n : allNodes) {
			if (!n.inEdges.isEmpty()) {
				cycle = true;
				for (final Edge<T> e : n.inEdges) {
					final Cycle<T, ?> findCycle = findCycle(e.from, new LinkedHashSet<Node<T>>(), n, startTime);
					if (findCycle != null)
						throw new CycleException(findCycle);
				}
			}
		}
		if (cycle)
			throw new CycleException(null);
		return L;
	}

	public static class CycleException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = -6522657621203932715L;

		public final Cycle<?, ?> cycle;

		public ExecutableModel model;

		public CycleException(Cycle<?, ?> cycle) {
			super("Cycle present, topological sort not possible");
			this.cycle = cycle;
		}

	}

	public static class Cycle<T, X extends Enum<X>> {

		public final Cycle<T, X> prior;
		public final Node<T> node;
		public X dependency;

		public Cycle(Cycle<T, X> prior, Node<T> node) {
			super();
			this.prior = prior;
			this.node = node;
		}

	}

	public <X extends Enum<X>> Cycle<T, X> findCycle(Node<T> n, LinkedHashSet<Node<T>> visitedNodes, Node<T> target, long startTime) throws CycleException {
		if (visitedNodes.contains(n) || ((System.currentTimeMillis() - startTime) > 500))
			return null;
		final LinkedHashSet<Node<T>> newVisited = new LinkedHashSet<>(visitedNodes);
		newVisited.add(n);
		for (final Edge<T> e : n.inEdges) {
			System.out.println(e);
			if (e.to == target)
				return createCycleFromVisited(target, newVisited);
			final Cycle<T, X> findCycle = findCycle(e.from, newVisited, target, startTime);
			if (findCycle != null)
				return findCycle;
		}
		return null;
	}

	public <X extends Enum<X>> Cycle<T, X> createCycleFromVisited(Node<T> target, final LinkedHashSet<Node<T>> newVisited) {
		Cycle<T, X> lastCycle = new Cycle<>(null, target);
		for (final Node<T> t : newVisited) {
			lastCycle = new Cycle<>(lastCycle, t);
		}
		return lastCycle;
	}
}
