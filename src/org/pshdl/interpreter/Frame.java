package org.pshdl.interpreter;

import java.io.*;
import java.math.*;
import java.util.*;

public class Frame implements Serializable {
	public final byte[] instructions;
	public final int[] internalDependencies;
	public final int predPosDepRes, predNegDepRes;
	public final int edgePosDepRes, edgeNegDepRes;
	public final BigInteger[] constants;
	public final int outputId;
	public final int maxDataWidth;
	public final int maxStackDepth;
	transient public int lastUpdate;
	public int executionDep = -1;
	public final int uniqueID;
	private static final long serialVersionUID = -1690021519637432408L;

	public Frame(byte[] instructions, int[] internalDependencies, int predPosDepRes, int predNegDepRes, int edgePosDepRes, int edgeNegDepRes, int outputId, int maxDataWidth,
			int maxStackDepth, BigInteger[] constants, int uniqueID) {
		super();
		this.constants = constants;
		this.instructions = instructions;
		this.internalDependencies = internalDependencies;
		this.outputId = outputId;
		this.predNegDepRes = predNegDepRes;
		this.predPosDepRes = predPosDepRes;
		this.edgeNegDepRes = edgeNegDepRes;
		this.edgePosDepRes = edgePosDepRes;
		this.maxDataWidth = maxDataWidth;
		this.maxStackDepth = maxStackDepth;
		this.uniqueID = uniqueID;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Frame [");
		if (instructions != null) {
			builder.append("instructions=").append(Arrays.toString(instructions)).append(", ");
		}
		if (internalDependencies != null) {
			builder.append("internalDependencies=").append(Arrays.toString(internalDependencies)).append(", ");
		}
		if (constants != null) {
			builder.append("constants=").append(Arrays.toString(constants)).append(", ");
		}
		builder.append("outputId=").append(outputId).append(", maxDataWidth=").append(maxDataWidth).append(", maxStackDepth=").append(maxStackDepth).append(", ");
		builder.append("executionDep=").append(executionDep);
		builder.append("]\n");
		return builder.toString();
	}

	public boolean isReg() {
		return (edgeNegDepRes != -1) || (edgePosDepRes != -1);
	}
}
