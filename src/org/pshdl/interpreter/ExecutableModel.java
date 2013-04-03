package org.pshdl.interpreter;

import java.io.*;
import java.util.*;

import org.pshdl.interpreter.utils.*;
import org.pshdl.interpreter.utils.Graph.Node;

public class ExecutableModel implements Serializable {
	public final int maxDataWidth;
	public final int maxStackDepth;
	public final Frame[] frames;
	public final String[] internals;
	public final int[] registerOutputs;
	public final Map<String, Integer> widths;
	private static final long serialVersionUID = 7515137334641792104L;

	public ExecutableModel(Frame[] frames, String[] internals, Map<String, Integer> widths, int maxStackDepth) {
		super();
		this.frames = frames;
		this.internals = internals;
		this.maxStackDepth = maxStackDepth;
		this.widths = widths;
		int maxWidth = -1;
		for (Integer width : widths.values()) {
			maxWidth = Math.max(width, maxWidth);
		}
		this.maxDataWidth = maxWidth;
		List<Integer> regOuts = new ArrayList<Integer>();
		for (Frame frame : frames) {
			if (frame.isReg) {
				regOuts.add(frame.outputId);
			}
		}
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
		Map<String, Node<Frame>> intProvider = new RandomHashMap<String, Graph.Node<Frame>>();
		Map<Integer, Node<Frame>> outputIDMap = new RandomHashMap<Integer, Graph.Node<Frame>>();
		for (Frame f : frames) {
			Node<Frame> node = new Node<Frame>(f);
			nodes.add(node);
			outputIDMap.put(f.uniqueID, node);
			intProvider.put(internals[f.outputId], node);
		}
		for (Node<Frame> node : nodes) {
			if (node.object.executionDep != -1) {
				Node<Frame> preNode = outputIDMap.get(node.object.executionDep);
				node.reverseAddEdge(preNode);
			}
			for (int intDep : node.object.internalDependencies) {
				String string = internals[intDep];
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

	public static String stripReg(String string) {
		if (string.endsWith("$reg"))
			return string.substring(0, string.length() - 4);
		return string;
	}

	public String toDotFile() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph ExecutableModel {\n");
		for (int i = 0; i < internals.length; i++) {
			String label = internals[i];
			String color;
			if (label.startsWith("$Pred")) {
				color = "blue";
				label = label.substring(6);
			} else {
				color = "orange";
			}
			sb.append("node [shape = box, color=" + color + ", label=\"").append(label).append("\"]");
			sb.append(" int").append(i);
			sb.append(";\n");
		}
		for (int i = 0; i < frames.length; i++) {
			String color = "black";
			Frame frame = frames[i];
			if (frame.instructions[0] == FluidFrame.Instruction.posPredicate.ordinal()) {
				color = "green";
			}
			if (frame.instructions[0] == FluidFrame.Instruction.negPredicate.ordinal()) {
				color = "red";
			}
			sb.append("node [shape = circle, color=" + color + ", label=\"").append(i).append("\"]");
			sb.append(" frame").append(frame.uniqueID);
			sb.append(";\n");
		}
		for (Frame frame : frames) {
			String frameId = "frame" + frame.uniqueID;
			for (int in : frame.internalDependencies) {
				sb.append("int").append(in).append(" -> ").append(frameId);
				for (int clk : frame.edgeDepRes) {
					if (in == clk) {
						sb.append(" [style=dotted]");
					}
				}
				for (int pred : frame.predicateDepRes) {
					if (in == pred) {
						sb.append(" [color=blue]");
					}
				}
				sb.append(";\n");
			}
			if (frame.executionDep != -1) {
				sb.append("frame" + frame.executionDep).append(" -> ");
				sb.append(frameId);
				sb.append(" [style=dashed, color=red]");
				sb.append(";\n");
			}
			sb.append(frameId).append(" -> ");
			sb.append("int");
			sb.append(frame.outputId);
			sb.append(" [style=normal, color=black]");
			sb.append(";\n");
		}
		sb.append("}");
		return sb.toString();
	}

	public int getWidth(String name) {
		Integer integer = widths.get(stripReg(name));
		if (integer == null) {
			if (name.startsWith("$Pred_"))
				return 1;
			throw new IllegalArgumentException("Unknown width of signal:" + name);
		}
		return integer;
	}
}
