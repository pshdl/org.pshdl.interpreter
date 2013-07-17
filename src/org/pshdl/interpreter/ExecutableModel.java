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
package org.pshdl.interpreter;

import java.io.*;
import java.util.*;

import org.pshdl.interpreter.utils.*;
import org.pshdl.interpreter.utils.Graph.CycleException;
import org.pshdl.interpreter.utils.Graph.Node;

public class ExecutableModel implements Serializable {
	public final int maxDataWidth;
	public final int maxExecutionWidth;
	public final int maxStackDepth;
	public final Frame[] frames;
	public final InternalInformation[] internals;
	public final VariableInformation[] variables;
	private static final long serialVersionUID = 7515137334641792104L;

	public ExecutableModel(Frame[] frames, InternalInformation[] internals, VariableInformation[] variables) {
		super();
		this.frames = frames;
		this.internals = internals;
		int maxWidth = -1, maxExecWidth = -1;
		int maxStack = -1;
		for (final InternalInformation ii : internals) {
			maxWidth = Math.max(ii.info.width, maxWidth);
			maxExecWidth = Math.max(ii.actualWidth, maxExecWidth);
		}
		this.maxDataWidth = maxWidth;
		this.maxExecutionWidth = maxExecWidth;
		final List<Integer> regOuts = new ArrayList<Integer>();
		for (final Frame frame : frames) {
			if (frame.isReg()) {
				regOuts.add(frame.outputId);
			}
			maxStack = Math.max(maxStack, frame.maxStackDepth);
		}
		this.maxStackDepth = maxStack;
		this.variables = variables;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ExecutableModel [maxDataWidth=").append(maxDataWidth).append(", maxStackDepth=").append(maxStackDepth).append(", ");
		if (frames != null) {
			builder.append("frames=\n");
			for (final Frame f : frames) {
				builder.append(f.toString(this, false));
			}
			builder.append('\n').append(", ");
		}
		builder.append("]");
		return builder.toString();
	}

	public ExecutableModel sortTopological() throws CycleException {
		final Graph<String> graph = new Graph<String>();
		final ArrayList<Node<String>> nodes = new ArrayList<Graph.Node<String>>();
		final Map<String, Node<String>> nodeNames = new HashMap<String, Graph.Node<String>>();
		final Map<String, Frame> frameNames = new HashMap<String, Frame>();
		for (final Frame f : frames) {
			final String string = getFrame(f.uniqueID);
			final Node<String> node = new Node<String>(string);
			nodes.add(node);
			nodeNames.put(string, node);
			frameNames.put(string, f);
		}
		for (int i = 0; i < internals.length; i++) {
			final String string = getInternal(i);
			// System.out.println(i + " " + internals[i]);
			final Node<String> node = new Node<String>(string);
			nodes.add(node);
			nodeNames.put(string, node);
		}
		for (final Frame f : frames) {
			final Node<String> node = nodeNames.get(getFrame(f.uniqueID));
			final Node<String> outNode = nodeNames.get(getInternal(f.outputId));
			outNode.reverseAddEdge(node);
			for (final int i : f.internalDependencies) {
				final Node<String> ni = nodeNames.get(getInternal(i));
				node.reverseAddEdge(ni);
			}
			if (f.executionDep != -1) {
				final Node<String> ni = nodeNames.get(getFrame(f.executionDep));
				node.reverseAddEdge(ni);
			}
		}
		// for (Node<String> node : nodes) {
		// System.out.print(node.object + " -> ");
		// for (Edge<String> i : node.inEdges) {
		// System.out.print(i.from.object + " ");
		// }
		// System.out.println();
		// Frame f = frameNames.get(node.object);
		// if (f != null)
		// System.out.println(f.toString(this));
		// }
		final ArrayList<Node<String>> sortNodes = graph.sortNodes(nodes);
		// for (Node<String> node : sortNodes) {
		// System.out.println(node.object);
		// }
		int pos = 0;
		for (final Node<String> node : sortNodes) {
			final String obj = node.object;
			final Frame f = frameNames.get(obj);
			if (f != null) {
				frames[pos++] = f;
			}
		}
		return this;
	}

	private String getFrame(int uniqueID) {
		return "Frame" + uniqueID;
	}

	private String getInternal(int i) {
		return "Internal" + i;
	}

	public String toDotFile() {
		final StringBuilder sb = new StringBuilder();
		sb.append("digraph ExecutableModel {\n");
		for (int i = 0; i < internals.length; i++) {
			String label = internals[i].fullName;
			String style = "solid";
			String color;
			if (label.startsWith(InternalInformation.PRED_PREFIX)) {
				color = "blue";
				label = label.substring(6);
			} else if (label.endsWith(InternalInformation.REG_POSTFIX)) {
				color = "gray";
				label = label.substring(0, label.length() - 4);
				style = "bold";
			} else {
				color = "orange";
			}
			sb.append("node [shape = box, color=" + color + ", style=" + style + " label=\"").append(label).append("\"]");
			sb.append(" int").append(i);
			sb.append(";\n");
		}
		for (final Frame frame : frames) {
			String color = "black";
			if ((frame.predPosDepRes != null) && (frame.predPosDepRes.length > 0)) {
				color = "darkgreen";
			}
			if ((frame.predNegDepRes != null) && (frame.predNegDepRes.length > 0)) {
				color = "red";
			}
			String style = "solid";
			if (frame.edgeNegDepRes != -1) {
				style = "bold";
			}
			if (frame.edgePosDepRes != -1) {
				style = "bold";
			}
			sb.append("node [shape = circle, color=" + color + ", style=" + style + ", label=\"").append(frame.uniqueID).append("\"]");
			sb.append(" frame").append(frame.uniqueID);
			sb.append(";\n");
		}
		for (final Frame frame : frames) {
			final String frameId = "frame" + frame.uniqueID;
			for (final int in : frame.internalDependencies) {
				sb.append("int").append(in).append(" -> ").append(frameId);
				if (in == frame.edgeNegDepRes) {
					sb.append(" [style=dotted, color=red, arrowType=empty]");
				}
				if (in == frame.edgePosDepRes) {
					sb.append(" [style=dotted, color=darkgreen, arrowType=empty]");
				}
				if (frame.predNegDepRes != null) {
					for (final int p : frame.predNegDepRes)
						if (in == p) {
							sb.append(" [style=dotted, color=red]");
						}
				}
				if (frame.predPosDepRes != null) {
					for (final int p : frame.predPosDepRes)
						if (in == p) {
							sb.append(" [style=dotted, color=darkgreen]");
						}
				}
				sb.append(";\n");
			}
			if (frame.executionDep != -1) {
				sb.append("frame" + frame.executionDep).append(" -> ");
				sb.append(frameId);
				sb.append(" [style=dashed, color=blue]");
				sb.append(";\n");
			}
			sb.append(frameId).append(" -> ");
			sb.append("int");
			sb.append(frame.outputId);
			sb.append(" [color=black]");
			sb.append(";\n");
		}
		sb.append("}");
		return sb.toString();
	}

	public InternalInformation getInternal(String string) {
		for (final InternalInformation ii : internals) {
			if (ii.fullName.equals(string))
				return ii;
		}
		return null;
	}

}
