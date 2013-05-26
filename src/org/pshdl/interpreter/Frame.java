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
			return toString(null);
		}

		public String toString(ExecutableModel em) {
			if ((em != null) && (inst == Instruction.loadInternal))
				return "loadInternal[" + em.internals[arg1] + "]";
			if ((em != null) && (inst == Instruction.posPredicate))
				return "posPredicate[" + em.internals[arg1] + "]";
			if ((em != null) && (inst == Instruction.negPredicate))
				return "negPredicate[" + em.internals[arg1] + "]";
			if (inst.argCount >= 2)
				return inst.name() + "[" + inst.args[0] + "=" + arg1 + "," + inst.args[1] + "=" + arg2 + "]";
			if (inst.argCount >= 1)
				return inst.name() + "[" + inst.args[0] + "=" + arg1 + "]";
			return inst.name();
		}
	}

	public final FastInstruction[] instructions;
	/**
	 * An array of internal IDs that need to be computed before this frame can
	 * be executed
	 */
	public final int[] internalDependencies;
	/**
	 * An array of internal IDs that need to be evaluate to true to execute this
	 * frame
	 */
	public final int[] predPosDepRes, predNegDepRes;
	/**
	 * An internal ID that needs to have a edge in order to execute this frame
	 */
	public final int edgePosDepRes, edgeNegDepRes;
	/**
	 * A uniqueID of a frame that needs to be executed before this frame can be
	 * executed
	 */
	public int executionDep = -1;
	public final BigInteger[] constants;
	public final int outputId;
	public int maxDataWidth;
	public final int maxStackDepth;
	transient public int lastUpdate;
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
		return toString(null);
	}

	public String toString(ExecutableModel em) {
		final StringBuilder builder = new StringBuilder();
		builder.append("Frame [").append(uniqueID).append(' ');
		if (instructions != null) {
			builder.append("instructions=");
			for (final FastInstruction fi : instructions) {
				builder.append(fi.toString(em)).append(',');
			}
		}
		if (internalDependencies != null) {
			builder.append("internalDependencies=").append(Arrays.toString(internalDependencies)).append(", ");
		}
		if (constants != null) {
			builder.append("constants=").append(Arrays.toString(constants)).append(", ");
		}
		String outputName = Integer.toString(outputId);
		if (em != null) {
			outputName = em.internals[outputId].toString();
		}
		builder.append("outputId=").append(outputName).append(", maxDataWidth=").append(maxDataWidth).append(", maxStackDepth=").append(maxStackDepth).append(", ");
		builder.append("executionDep=").append(executionDep);
		builder.append("]");
		return builder.toString();
	}

	public boolean isReg() {
		return (edgeNegDepRes != -1) || (edgePosDepRes != -1);
	}
}
