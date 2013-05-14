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
import java.math.*;
import java.util.*;

import org.pshdl.interpreter.utils.*;

public class Frame implements Serializable {

	public static class FastInstruction {
		public final Instruction inst;
		public final int arg1, arg2;
		public final boolean popA;
		public final boolean popB;

		public FastInstruction(Instruction inst, int arg1, int arg2) {
			super();
			this.inst = inst;
			this.arg1 = arg1;
			this.arg2 = arg2;
			popA = inst.pop > 0;
			popB = inst.pop > 1;
		}

		@Override
		public String toString() {
			if (inst.argCount >= 2)
				return inst.name() + "[" + inst.args[0] + "=" + arg1 + "," + inst.args[1] + "=" + arg2 + "]";
			if (inst.argCount >= 1)
				return inst.name() + "[" + inst.args[0] + "=" + arg1 + "]";
			return inst.name();
		}
	}

	public final FastInstruction[] instructions;
	public final int[] internalDependencies;
	public final int[] predPosDepRes;
	public final int[] predNegDepRes;
	public final int edgePosDepRes, edgeNegDepRes;
	public final BigInteger[] constants;
	public final int outputId;
	public int maxDataWidth;
	public final int maxStackDepth;
	transient public int lastUpdate;
	public int executionDep = -1;
	public final int uniqueID;
	public final boolean constant;
	private static final long serialVersionUID = -1690021519637432408L;

	public Frame(FastInstruction[] instructions, int[] internalDependencies, int[] predPosDepRes, int[] predNegDepRes, int edgePosDepRes, int edgeNegDepRes, int outputId,
			int maxDataWidth, int maxStackDepth, BigInteger[] constants, int uniqueID, boolean constant) {
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
		this.constant = constant;
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
