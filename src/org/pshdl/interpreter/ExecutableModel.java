package org.pshdl.interpreter;

import java.io.*;
import java.util.*;

import org.pshdl.interpreter.utils.*;
import org.pshdl.interpreter.utils.Graph.Node;

public class ExecutableModel implements Serializable {
	public final int maxDataWidth;
	public final int maxExecutionWidth;
	public final int maxStackDepth;
	public final Frame[] frames;
	public final InternalInformation[] internals;
	public final int[] registerOutputs;
	private static final long serialVersionUID = 7515137334641792104L;

	public ExecutableModel(Frame[] frames, InternalInformation[] internals) {
		super();
		this.frames = frames;
		this.internals = internals;
		int maxWidth = -1, maxExecWidth = -1;
		int maxStack = -1;
		for (InternalInformation ii : internals) {
			maxWidth = Math.max(ii.baseWidth, maxWidth);
			maxExecWidth = Math.max(ii.actualWidth, maxExecWidth);
		}
		this.maxDataWidth = maxWidth;
		this.maxExecutionWidth = maxExecWidth;
		List<Integer> regOuts = new ArrayList<Integer>();
		for (Frame frame : frames) {
			if (frame.isReg()) {
				regOuts.add(frame.outputId);
			}
			maxStack = Math.max(maxStack, frame.maxStackDepth);
		}
		this.maxStackDepth = maxStack;
		int pos = 0;
		this.registerOutputs = new int[regOuts.size()];
		for (Integer integer : regOuts) {
			registerOutputs[pos++] = integer;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExecutableModel [maxDataWidth=").append(maxDataWidth).append(", maxStackDepth=").append(maxStackDepth).append(", ");
		if (frames != null) {
			builder.append("frames=\n").append(Arrays.toString(frames)).append(", ");
		}
		builder.append("]");
		return builder.toString();
	}

	public ExecutableModel sortTopological() {
		Graph<Frame> graph = new Graph<Frame>();
		ArrayList<Node<Frame>> nodes = new ArrayList<Graph.Node<Frame>>();
		Map<String, Node<Frame>> intProvider = new LinkedHashMap<String, Graph.Node<Frame>>();
		Map<Integer, Node<Frame>> outputIDMap = new LinkedHashMap<Integer, Graph.Node<Frame>>();
		for (Frame f : frames) {
			Node<Frame> node = new Node<Frame>(f);
			nodes.add(node);
			outputIDMap.put(f.uniqueID, node);
			intProvider.put(internals[f.outputId].fullName, node);
		}
		for (Node<Frame> node : nodes) {
			if (node.object.executionDep != -1) {
				Node<Frame> preNode = outputIDMap.get(node.object.executionDep);
				node.reverseAddEdge(preNode);
			}
			for (int intDep : node.object.internalDependencies) {
				String string = internals[intDep].fullName;
				Node<Frame> node2 = intProvider.get(string);
				if (node2 != null) {
					node.reverseAddEdge(node2);
				}
			}
		}
		ArrayList<Node<Frame>> sortNodes = graph.sortNodes(nodes);
		int pos = 0;
		for (Node<Frame> node : sortNodes) {
			frames[pos++] = node.object;
		}
		return this;
	}

	public static String getBasicName(String inName, boolean stripReg) {
		String name = inName;
		int openBrace = name.indexOf('{');
		if (openBrace != -1) {
			name = name.substring(0, openBrace);
			if (inName.endsWith(FluidFrame.REG_POSTFIX) && !stripReg)
				return name + FluidFrame.REG_POSTFIX;
		}
		if (stripReg)
			return stripReg(name);
		return name;
	}

	public static String stripReg(String string) {
		if (string.endsWith(FluidFrame.REG_POSTFIX))
			return string.substring(0, string.length() - 4);
		return string;
	}

	public String toDotFile() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph ExecutableModel {\n");
		for (int i = 0; i < internals.length; i++) {
			String label = internals[i].fullName;
			String style = "solid";
			String color;
			if (label.startsWith(FluidFrame.PRED_PREFIX)) {
				color = "blue";
				label = label.substring(6);
			} else if (label.endsWith(FluidFrame.REG_POSTFIX)) {
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
		for (Frame frame : frames) {
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
		for (Frame frame : frames) {
			String frameId = "frame" + frame.uniqueID;
			for (int in : frame.internalDependencies) {
				sb.append("int").append(in).append(" -> ").append(frameId);
				if (in == frame.edgeNegDepRes) {
					sb.append(" [style=dotted, color=red, arrowType=empty]");
				}
				if (in == frame.edgePosDepRes) {
					sb.append(" [style=dotted, color=darkgreen, arrowType=empty]");
				}
				if (frame.predNegDepRes != null) {
					for (int p : frame.predNegDepRes)
						if (in == p) {
							sb.append(" [style=dotted, color=red]");
						}
				}
				if (frame.predPosDepRes != null) {
					for (int p : frame.predPosDepRes)
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

}
