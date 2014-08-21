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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.pshdl.interpreter.utils.Graph;
import org.pshdl.interpreter.utils.Graph.Cycle;
import org.pshdl.interpreter.utils.Graph.CycleException;
import org.pshdl.interpreter.utils.Graph.Node;

public class ExecutableModel implements Serializable {
	public final int maxDataWidth;
	public final int maxExecutionWidth;
	public final int maxStackDepth;
	public final String moduleName;
	public Frame[] frames;
	public final InternalInformation[] internals;
	public final VariableInformation[] variables;
	public final String source;
	public final String[] annotations;
	private static final long serialVersionUID = 7515137334641792104L;

	public ExecutableModel(Frame[] frames, InternalInformation[] internals, VariableInformation[] variables, String moduleName, String source, String[] annotations) {
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
		for (final VariableInformation variableInformation : variables) {
			variableInformation.writeCount = 0;
		}
		for (final Frame frame : frames) {
			maxStack = Math.max(maxStack, frame.maxStackDepth);
			final VariableInformation varInfo = internals[frame.outputId].info;
			varInfo.writeCount++;
		}
		final LinkedHashMap<Integer, Integer> tempAliasMap = new LinkedHashMap<>();
		for (final Frame frame : frames) {
			final InternalInformation outputId = internals[frame.outputId];
			if ((outputId.info.writeCount == 1) && frame.isRename()) {
				tempAliasMap.put(frame.outputId, frame.internalDependencies[0]);
			}
		}
		for (final Entry<Integer, Integer> x : tempAliasMap.entrySet()) {
			Integer currentAlias = x.getValue();
			final Integer deepAlias = getDeepAlias(tempAliasMap, currentAlias);
			if (deepAlias != currentAlias) {
				currentAlias = deepAlias;
			}
			final InternalInformation internal = internals[x.getKey()];
			final InternalInformation aliasInternal = internals[currentAlias];
			internal.info.aliasVar = aliasInternal.info;
			internal.aliasID = currentAlias;
		}
		this.maxStackDepth = maxStack;
		this.variables = variables;
		this.moduleName = moduleName;
		this.source = source;
		this.annotations = annotations;
	}

	protected Integer getDeepAlias(LinkedHashMap<Integer, Integer> tempAliasMap, int outputId) {
		final Integer alias = tempAliasMap.get(outputId);
		if (alias == null)
			return outputId;
		if (alias == outputId)
			return outputId;
		return getDeepAlias(tempAliasMap, alias);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ExecutableModel [maxDataWidth=").append(maxDataWidth).append(", maxStackDepth=").append(maxStackDepth).append(", ");
		if (frames != null) {
			builder.append("frames=\n");
			for (final Frame f : frames) {
				builder.append(f.toString(this, false)).append('\n');
			}
			builder.append(", ");
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Sorts the ExecutableModel in such a way, that all Frames can be executed
	 * sequentially. This is happening in-place.
	 *
	 * @return this
	 * @throws CycleException
	 *             if a combinatorial loop has been detected
	 */
	public ExecutableModel sortTopological(boolean useAlias) throws CycleException {
		final Graph<String> graph = new Graph<>();
		final ArrayList<Node<String>> nodes = new ArrayList<>();
		final Map<String, Node<String>> nodeNames = new LinkedHashMap<>();
		final Map<String, Frame> frameNames = new LinkedHashMap<>();
		for (final Frame f : frames) {
			final String frameName = getFrame(f.uniqueID);
			final Node<String> node = new Node<>(frameName);
			nodes.add(node);
			nodeNames.put(frameName, node);
			frameNames.put(frameName, f);
		}
		for (int i = 0; i < internals.length; i++) {
			final String string = getInternal(i, useAlias);
			final Node<String> node = new Node<>(string);
			nodes.add(node);
			nodeNames.put(string, node);
		}
		for (final Frame f : frames) {
			if (f.process != null) {
				continue;
			}
			final Node<String> node = nodeNames.get(getFrame(f.uniqueID));
			final Node<String> outNode = nodeNames.get(getInternal(f.outputId, useAlias));
			outNode.reverseAddEdge(node);
			for (final int i : f.internalDependencies) {
				final Node<String> ni = nodeNames.get(getInternal(i, useAlias));
				node.reverseAddEdge(ni);
			}
			if (f.executionDep != -1) {
				final Node<String> ni = nodeNames.get(getFrame(f.executionDep));
				node.reverseAddEdge(ni);
			}
		}
		final ArrayList<Node<String>> sortNodes = graph.sortNodes(nodes);
		int pos = 0;
		// if (useAlias) {
		// frames = new Frame[frameNames.size()];
		// }
		for (final Node<String> node : sortNodes) {
			final String obj = node.object;
			final Frame f = frameNames.get(obj);
			if (f != null) {
				f.scheduleStage = node.stage;
				frames[pos++] = f;
			}
		}
		return this;
	}

	private String getFrame(int uniqueID) {
		return "F" + uniqueID;
	}

	private String getInternal(int i, boolean useAlias) {
		int id = i;
		final InternalInformation ii = internals[i];
		if ((ii.aliasID != -1) && useAlias) {
			id = ii.aliasID;
		}
		return "I" + i;
	}

	public String humanReadableExplaination(Cycle<String, ?> ce) {
		if (ce == null)
			return "";
		final String nodeName = ce.node.object;
		final int id = Integer.parseInt(nodeName.substring(1));
		switch (nodeName.charAt(0)) {
		case 'F':
			final Frame frame = findFrame(id);
			if (ce.prior != null) {
				final String priorNode = ce.prior.node.object;
				final int priorId = Integer.parseInt(priorNode.substring(1));
				final String priorExplaination = humanReadableExplaination(ce.prior) + "\nExecution of frame " + frame.uniqueID;
				if (priorNode.charAt(0) == 'I') {
					final InternalInformation ii = internals[priorId];
					if (frame.edgeNegDepRes == priorId)
						return priorExplaination + " is waiting for a negative edge on: " + ii.fullName;
					if (frame.edgePosDepRes == priorId)
						return priorExplaination + " is waiting for a negative edge on: " + ii.fullName;
					for (final int intDep : frame.internalDependencies) {
						if (intDep == priorId)
							return priorExplaination + " is waiting for the computation of: " + ii.fullName;
					}
					if (priorId == frame.outputId)
						return priorExplaination + " produces " + ii.fullName;
					for (final int predDep : frame.predPosDepRes) {
						if (predDep == priorId)
							return priorExplaination + " requires that this predicate (if/ternary/switch-case) is true: " + ii.fullName;
					}
					for (final int predDep : frame.predNegDepRes) {
						if (predDep == priorId)
							return priorExplaination + " requires that this predicate (if/ternary/switch-case) is false: " + ii.fullName;
					}
				}
				if (priorNode.charAt(0) == 'F') {
					final Frame priorFrame = findFrame(priorId);
					return priorExplaination + " requires the prior computation of: " + internals[priorFrame.outputId].fullName;
				}
				return priorExplaination;
			}
			return "Execution of frame " + frame.uniqueID + " produces " + internals[frame.outputId].fullName;
		case 'I':
			if (ce.prior != null) {
				final String priorNode = ce.prior.node.object;
				final String priorExplaination = humanReadableExplaination(ce.prior);
				if (priorNode.charAt(0) == 'F')
					return priorExplaination + "\nThe variable " + internals[id].fullName + " is used as connection between frames";
			}
			return "The variable " + internals[id].fullName + " is used";
		default:
			throw new IllegalArgumentException("Can not reparse ID:" + nodeName);
		}
	}

	public Frame findFrame(final int id) {
		for (final Frame f : frames) {
			if (f.uniqueID == id)
				return f;
		}
		return null;
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

	public String toRegisterDot() {
		final Map<String, Set<String>> connections = new LinkedHashMap<>();
		final Map<String, VariableInformation> varNames = new LinkedHashMap<>();
		for (final VariableInformation var : variables) {
			final HashSet<String> connSet = new LinkedHashSet<>();
			connections.put(var.name, connSet);
			varNames.put(var.name, var);
		}
		for (final Frame f : frames) {
			final String output = getVarName(f.outputId);
			for (final int intDep : f.internalDependencies) {
				final Set<String> set = connections.get(getVarName(intDep));
				set.add(output);
			}
			for (final int intDep : f.predNegDepRes) {
				final Set<String> set = connections.get(getVarName(intDep));
				set.add(output);
			}
			for (final int intDep : f.predPosDepRes) {
				final Set<String> set = connections.get(getVarName(intDep));
				set.add(output);
			}
			if (f.edgeNegDepRes != -1) {
				final Set<String> set = connections.get(getVarName(f.edgeNegDepRes));
				set.add(output);
			}
			if (f.edgePosDepRes != -1) {
				final Set<String> set = connections.get(getVarName(f.edgePosDepRes));
				set.add(output);
			}
		}
		final Map<String, Set<String>> regConnections = new LinkedHashMap<>();
		for (final VariableInformation var : variables) {
			if (var.isRegister) {
				final HashSet<String> connSet = new LinkedHashSet<>();
				regConnections.put(var.name, connSet);
				follow(true, var.name, connections, connSet, varNames);
			}
		}
		// System.out.println("ExecutableModel.toRegisterDot()" +
		// regConnections);
		final int skipLen = moduleName.length();
		final StringBuilder sb = new StringBuilder();
		sb.append("digraph ExecutableModelRegister {\n");
		for (final String regName : regConnections.keySet()) {
			final VariableInformation key = varNames.get(regName);
			final long keyID = hash(key);
			sb.append("\tnode [shape=\"box\" label=\"" + key.name.substring(skipLen + 1) + "\"] " + keyID + ";\n");
		}
		for (final Entry<String, Set<String>> e : regConnections.entrySet()) {
			final Set<String> value = e.getValue();
			final VariableInformation key = varNames.get(e.getKey());
			final long keyID = hash(key);
			for (final String val : value) {
				final VariableInformation remoteVar = varNames.get(val);
				if (remoteVar.isRegister) {
					sb.append('\t').append(keyID).append(" -> ").append(hash(remoteVar)).append(";\n");
				}
			}
		}
		sb.append('}');
		return sb.toString();
	}

	protected long hash(final VariableInformation key) {
		final long sysID = System.identityHashCode(key) & 0xFFFFFFFFl;
		final long objID = key.hashCode() & 0xFFFFFFFFl;
		return (sysID << 32) | objID;
	}

	private void follow(boolean first, String varName, Map<String, Set<String>> connections, HashSet<String> connSet, Map<String, VariableInformation> varNames) {
		if (connSet.contains(varName))
			return;
		if (!first) {
			connSet.add(varName);
		}
		final Set<String> set = connections.get(varName);
		for (final String connVarName : set) {
			final VariableInformation var = varNames.get(connVarName);
			if (!var.isRegister) {
				follow(false, connVarName, connections, connSet, varNames);
			} else {
				connSet.add(var.name);
			}
		}
	}

	private String getVarName(int intDep) {
		final InternalInformation ii = internals[intDep];
		return ii.info.name;
	}

	public InternalInformation getInternal(String string) {
		for (final InternalInformation ii : internals) {
			if (ii.fullName.equals(string))
				return ii;
		}
		return null;
	}

}
