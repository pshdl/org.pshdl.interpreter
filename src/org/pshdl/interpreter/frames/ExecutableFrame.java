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
package org.pshdl.interpreter.frames;

import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.utils.*;

public abstract class ExecutableFrame {

	protected static final int noop = 0;
	protected static final int bitAccessSingle = 1;
	protected static final int bitAccessSingleRange = 2;
	protected static final int cast_int = 3;
	protected static final int cast_uint = 4;
	protected static final int loadConstant = 5;
	protected static final int loadInternal = 6;
	protected static final int concat = 7;
	protected static final int const0 = 8;
	protected static final int const1 = 9;
	protected static final int const2 = 10;
	protected static final int constAll1 = 11;
	protected static final int isFallingEdge = 12;
	protected static final int isRisingEdge = 13;
	protected static final int posPredicate = 14;
	protected static final int negPredicate = 15;
	protected static final int and = 16;
	protected static final int or = 17;
	protected static final int xor = 18;
	protected static final int div = 19;
	protected static final int minus = 20;
	protected static final int mul = 21;
	protected static final int plus = 22;
	protected static final int eq = 23;
	protected static final int greater = 24;
	protected static final int greater_eq = 25;
	protected static final int less = 26;
	protected static final int less_eq = 27;
	protected static final int not_eq = 28;
	protected static final int logiOr = 29;
	protected static final int logiAnd = 30;
	protected static final int logiNeg = 31;
	protected static final int arith_neg = 32;
	protected static final int bit_neg = 33;
	protected static final int sll = 34;
	protected static final int sra = 35;
	protected static final int srl = 36;
	protected static final int pushAddIndex = 37;
	protected static final int loadMultiplex = 38;

	protected static class FastInstruction {
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

	protected final FastInstruction[] instructions;

	protected int currentPos = 0;
	protected final EncapsulatedAccess internals[], internals_prev[];

	protected final int outputID;

	protected final boolean printing;
	public boolean regUpdated;
	public final int uniqueID;
	private final byte[] instr;
	public final EncapsulatedAccess outputAccess;
	protected final int[] writeIndex = new int[10];

	public ExecutableFrame(Frame f, boolean printing, EncapsulatedAccess[] internals, EncapsulatedAccess[] internals_prev) {
		this.printing = printing;
		this.internals = internals;
		this.internals_prev = internals_prev;
		this.outputID = f.outputId;
		this.uniqueID = f.uniqueID;
		this.instr = f.instructions;
		Instruction[] values = Instruction.values();
		List<FastInstruction> instr = new LinkedList<>();
		do {
			Instruction instruction = values[next()];
			int arg1 = 0;
			int arg2 = 0;
			if (instruction.argCount > 0) {
				arg1 = readVarInt();
			}
			if (instruction.argCount > 1) {
				arg2 = readVarInt();
			}
			instr.add(new FastInstruction(instruction, arg1, arg2));
		} while (hasMore());
		this.outputAccess = internals[outputID];
		this.instructions = instr.toArray(new FastInstruction[instr.size()]);
	}

	private boolean hasMore() {
		return currentPos < instr.length;
	}

	private int next() {
		return instr[currentPos++] & 0xff;
	}

	private int readVarInt() {
		int tmp = 0;
		if (((tmp = next()) & 0x80) == 0)
			return tmp;
		int result = tmp & 0x7f;
		if (((tmp = next()) & 0x80) == 0) {
			result |= tmp << 7;
		} else {
			result |= (tmp & 0x7f) << 7;
			if (((tmp = next()) & 0x80) == 0) {
				result |= tmp << 14;
			} else {
				result |= (tmp & 0x7f) << 14;
				if (((tmp = next()) & 0x80) == 0) {
					result |= tmp << 21;
				} else {
					result |= (tmp & 0x7f) << 21;
					result |= (tmp = next()) << 28;
					if (tmp < 0)
						throw new IllegalArgumentException("Too many bits");
				}
			}
		}
		return result;
	}

	public abstract void execute(int deltaCycle, int epsCycle);

	public static void main(String[] args) {
		for (Instruction i : Instruction.values()) {
			System.out.println("protected static final int " + i.name() + "=" + i.ordinal() + ";");
		}
	}
}
