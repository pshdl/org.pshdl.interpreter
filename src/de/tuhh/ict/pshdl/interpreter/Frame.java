package de.tuhh.ict.pshdl.interpreter;

import java.io.*;
import java.math.*;
import java.util.*;

public class Frame implements Serializable {
	public final byte[] instructions;
	public final int[] internalDependencies;
	public final BigInteger[] constants;
	public final int outputId;
	public final int maxDataWidth;
	public final int maxStackDepth;
	public final String id;
	public final boolean isReg;
	transient public int lastUpdate;
	private static final long serialVersionUID = -1690021519637432408L;

	public Frame(byte[] instructions, int[] internalDependencies, int outputId, int maxDataWidth, int maxStackDepth, BigInteger[] constants, String id, boolean isReg) {
		super();
		this.constants = constants;
		this.instructions = instructions;
		this.internalDependencies = internalDependencies;
		this.outputId = outputId;
		this.maxDataWidth = maxDataWidth;
		this.maxStackDepth = maxStackDepth;
		this.id = id;
		this.isReg = isReg;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Frame [");
		if (instructions != null)
			builder.append("instructions=").append(Arrays.toString(instructions)).append(", ");
		if (internalDependencies != null)
			builder.append("internalDependencies=").append(Arrays.toString(internalDependencies)).append(", ");
		if (constants != null)
			builder.append("constants=").append(Arrays.toString(constants)).append(", ");
		builder.append("outputId=").append(outputId).append(", maxDataWidth=").append(maxDataWidth).append(", maxStackDepth=").append(maxStackDepth).append(", ");
		if (id != null)
			builder.append("id=").append(id);
		builder.append("]\n");
		return builder.toString();
	}
}
