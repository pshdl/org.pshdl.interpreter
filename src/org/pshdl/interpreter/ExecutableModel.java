package org.pshdl.interpreter;

import java.io.*;
import java.util.*;

import org.pshdl.interpreter.utils.*;
import org.pshdl.interpreter.utils.Graph.*;

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
		for (int i = 0; i < frames.length; i++) {
			Frame frame = frames[i];
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
		if (frames != null)
			builder.append("frames=\n").append(Arrays.toString(frames)).append(", ");
		builder.append("]");
		return builder.toString();
	}

	public ExecutableModel sortTopological() {
		Graph<Frame> graph = new Graph<Frame>();
		ArrayList<Node<Frame>> nodes = new ArrayList<Graph.Node<Frame>>();
		Map<String, Node<Frame>> intProvider = new RandomHashMap<String, Graph.Node<Frame>>();
		for (Frame f : frames) {
			Node<Frame> node = new Node<Frame>(f);
			nodes.add(node);
			intProvider.put(internals[f.outputId], node);
		}
		for (Node<Frame> node : nodes) {
			for (int intDep : node.object.internalDependencies) {
				String string = internals[intDep];
				Node<Frame> node2 = intProvider.get(string);
				if (node2 != null)
					node.reverseAddEdge(node2);
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
			String input = internals[i];
			sb.append("node [shape = box, color=orange, label=\"").append(input).append("\"]");
			sb.append(" int").append(i & 0xFFFF);
			sb.append(";\n");
		}
		for (int i = 0; i < frames.length; i++) {
			sb.append("node [shape = circle, color=black, label=\"").append(frames[i].outputId).append("\"]");
			sb.append(" frame").append(i & 0xFFFF);
			sb.append(";\n");
		}
		for (int i = 0; i < frames.length; i++) {
			Frame input = frames[i];
			String frameId = "frame" + i;
			for (int in : input.internalDependencies) {
				sb.append("int").append(in & 0xFFFF).append(" -> ").append(frameId).append(";\n");
			}
			sb.append(frameId).append(" -> ");
			sb.append("int");
			sb.append(input.outputId).append(";\n");
		}
		sb.append("}");
		return sb.toString();
	}

	public int getWidth(String name) {
		return widths.get(stripReg(name));
	}
}