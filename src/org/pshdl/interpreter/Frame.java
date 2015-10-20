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
import java.math.BigInteger;
import java.util.Arrays;

import org.pshdl.interpreter.utils.Instruction;

public class Frame implements Serializable {

	public static class FastInstruction implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -322363811192724107L;
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
			if ((em != null) && (inst == Instruction.invokeFunction))
				return "invokeFunction[" + em.functions[arg1].signature() + "]";
			if (inst.argCount >= 2)
				return inst.name() + "[" + inst.args[0] + "=" + arg1 + "," + inst.args[1] + "=" + arg2 + "]";
			if (inst.argCount >= 1)
				return inst.name() + "[" + inst.args[0] + "=" + arg1 + "]";
			return inst.name();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + arg1;
			result = (prime * result) + arg2;
			result = (prime * result) + ((inst == null) ? 0 : inst.hashCode());
			result = (prime * result) + (popA ? 1231 : 1237);
			result = (prime * result) + (popB ? 1231 : 1237);
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
			final FastInstruction other = (FastInstruction) obj;
			if (arg1 != other.arg1)
				return false;
			if (arg2 != other.arg2)
				return false;
			if (inst != other.inst)
				return false;
			if (popA != other.popA)
				return false;
			if (popB != other.popB)
				return false;
			return true;
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
	public final String[] constantStrings;
	public final int[] outputIds;
	public int maxDataWidth;
	public final int maxStackDepth;
	public final int uniqueID;
	public final boolean constant;
	public int scheduleStage;
	public final String process;
	public final boolean isFuncStatement;
	private static final long serialVersionUID = -1690021519637432408L;

	public Frame(FastInstruction[] instructions, int[] internalDependencies, int[] predPosDepRes, int[] predNegDepRes, int edgePosDepRes, int edgeNegDepRes, int[] outputIds,
			int maxDataWidth, int maxStackDepth, BigInteger[] constants, String[] constantStrings, int uniqueID, boolean constant, int scheduleStage, String process,
			boolean isFuncStatement) {
		super();
		this.constants = constants;
		this.constantStrings = constantStrings;
		this.instructions = instructions;
		this.internalDependencies = selfOrEmpty(internalDependencies);
		this.outputIds = selfOrEmpty(outputIds);
		this.predNegDepRes = selfOrEmpty(predNegDepRes);
		this.predPosDepRes = selfOrEmpty(predPosDepRes);
		this.edgeNegDepRes = edgeNegDepRes;
		this.edgePosDepRes = edgePosDepRes;
		this.maxDataWidth = maxDataWidth;
		this.maxStackDepth = maxStackDepth;
		this.constant = constant;
		this.uniqueID = uniqueID;
		this.scheduleStage = scheduleStage;
		this.process = process;
		this.isFuncStatement = isFuncStatement;
	}

	protected int[] selfOrEmpty(int[] predPosDepRes) {
		return predPosDepRes != null ? predPosDepRes : new int[0];
	}

	public Frame aliasedFrame(ExecutableModel em) {
		final FastInstruction newInst[] = new FastInstruction[instructions.length];
		for (int i = 0; i < instructions.length; i++) {
			final FastInstruction instruction = instructions[i];
			switch (instruction.inst) {
			case loadInternal:
			case writeInternal:
			case posPredicate:
			case negPredicate:
			case isFallingEdge:
			case isRisingEdge:
				final int aliasedId = alias(instruction.arg1, em);
				newInst[i] = new FastInstruction(instruction.inst, aliasedId, instruction.arg2);
				break;
			default:
				newInst[i] = instruction;
			}
		}
		return new Frame(newInst, alias(internalDependencies, em), alias(predPosDepRes, em), alias(predNegDepRes, em), alias(edgePosDepRes, em), alias(edgeNegDepRes, em),
				new int[0], maxDataWidth, maxStackDepth, constants, constantStrings, -1, constant, scheduleStage, process, isFuncStatement);
	}

	private int[] alias(int[] internals, ExecutableModel em) {
		final int res[] = new int[internals.length];
		for (int i = 0; i < internals.length; i++) {
			final int internalId = internals[i];
			final int finalId = alias(internalId, em);
			res[i] = finalId;
		}
		return res;
	}

	protected int alias(final int internalId, ExecutableModel em) {
		if (internalId == -1)
			return internalId;
		final InternalInformation ii = em.internals[internalId];
		int finalId;
		if (ii.aliasID != -1) {
			finalId = ii.aliasID;
		} else {
			finalId = internalId;
		}
		return finalId;
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	public String toString(ExecutableModel em, boolean formatted) {
		final StringBuilder builder = new StringBuilder();
		builder.append("Frame ").append(uniqueID);
		if (formatted) {
			builder.append("\n\t");
		} else {
			builder.append(' ');
		}
		if (instructions != null) {
			builder.append("Instructions");
			if (formatted) {
				builder.append(":\n");
			} else {
				builder.append('{');
			}
			for (final FastInstruction fi : instructions) {
				if (formatted) {
					builder.append("\t\t");
				}
				builder.append(fi.toString(em));
				if (formatted) {
					builder.append('\n');
				} else {
					builder.append(',');
				}
			}
			if (!formatted) {
				builder.append('}');
			}
		}
		if (internalDependencies != null) {
			if (formatted) {
				builder.append('\t');
			}
			builder.append("internalDependencies=").append(Arrays.toString(internalDependencies));
			if (formatted) {
				builder.append("\n");
			} else {
				builder.append(", ");
			}
		}
		if (constants != null) {
			if (formatted) {
				builder.append('\t');
			}
			builder.append("constants=").append(Arrays.toString(constants));
			if (formatted) {
				builder.append("\n");
			} else {
				builder.append(", ");
			}
		}
		if (constantStrings != null) {
			if (formatted) {
				builder.append('\t');
			}
			builder.append("constantStrings=").append(Arrays.toString(constantStrings));
			if (formatted) {
				builder.append("\n");
			} else {
				builder.append(", ");
			}
		}
		if (formatted) {
			builder.append('\t');
		}
		String outputName = Arrays.toString(outputIds);
		if (em != null) {
			final StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (final int id : outputIds) {
				if (!first) {
					sb.append(",");
				}
				first = false;
				sb.append(em.internals[id].toString());
			}
			outputName = sb.toString();
		}
		builder.append("outputId=").append(outputName);
		if (formatted) {
			builder.append("\n\t");
		} else {
			builder.append(", ");
		}
		builder.append("maxDataWidth=").append(maxDataWidth);
		if (formatted) {
			builder.append("\n\t");
		} else {
			builder.append(", ");
		}
		builder.append("maxStackDepth=").append(maxStackDepth);
		if (formatted) {
			builder.append("\n\t");
		} else {
			builder.append(", ");
		}
		builder.append("executionDep=").append(executionDep);
		if (formatted) {
			builder.append("\n");
		}
		return builder.toString();
	}

	public boolean isRename(ExecutableModel model) {
		if ((edgeNegDepRes != -1) || (edgePosDepRes != -1))
			return false;
		if ((predNegDepRes != null) && (predNegDepRes.length != 0))
			return false;
		if ((predPosDepRes != null) && (predPosDepRes.length != 0))
			return false;
		if (process != null)
			return false;
		if (instructions[0].inst != Instruction.loadInternal)
			return false;
		if ((outputIds.length != 1) || (model.internals[outputIds[0]].info.writeCount != 1))
			return false;
		if (instructions.length == 1)
			return true;
		switch (instructions[1].inst) {
		case cast_int:
		case cast_uint:
			if (instructions[1].arg1 != instructions[1].arg2)
				return false;
			break;
		default:
			return false;
		}
		if (instructions.length == 2)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (constant ? 1231 : 1237);
		result = (prime * result) + Arrays.hashCode(constants);
		result = (prime * result) + Arrays.hashCode(constantStrings);
		result = (prime * result) + edgeNegDepRes;
		result = (prime * result) + edgePosDepRes;
		result = (prime * result) + Arrays.hashCode(instructions);
		result = (prime * result) + Arrays.hashCode(internalDependencies);
		result = (prime * result) + Arrays.hashCode(outputIds);
		result = (prime * result) + Arrays.hashCode(predNegDepRes);
		result = (prime * result) + Arrays.hashCode(predPosDepRes);
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
		final Frame other = (Frame) obj;
		if (constant != other.constant)
			return false;
		if (!Arrays.equals(constants, other.constants))
			return false;
		if (!Arrays.equals(constantStrings, other.constantStrings))
			return false;
		if (edgeNegDepRes != other.edgeNegDepRes)
			return false;
		if (edgePosDepRes != other.edgePosDepRes)
			return false;
		if (!Arrays.equals(instructions, other.instructions))
			return false;
		if (!Arrays.equals(internalDependencies, other.internalDependencies))
			return false;
		if (!Arrays.equals(outputIds, other.outputIds))
			return false;
		if (!Arrays.equals(predNegDepRes, other.predNegDepRes))
			return false;
		if (!Arrays.equals(predPosDepRes, other.predPosDepRes))
			return false;
		return true;
	}
}
