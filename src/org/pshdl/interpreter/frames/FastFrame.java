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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.pshdl.interpreter.FastSimpleInterpreter;
import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess;
import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess.RegUpdater;
import org.pshdl.interpreter.Frame;
import org.pshdl.interpreter.Frame.FastInstruction;

public class FastFrame {

	private final long stack[];
	private final long constants[];
	public List<RegUpdater> regUpdates = new ArrayList<>();
	private int arrayPos = -1;
	private final int[] writeIndex = new int[8];
	private final FastInstruction[] instructions;
	private final LongAccess[] internals;
	private final LongAccess[] internals_prev;
	public final LongAccess outputAccess;
	public boolean disableEdge;

	public FastFrame(FastSimpleInterpreter fir, Frame f, boolean disableEdge) {
		this.stack = new long[f.maxStackDepth];
		this.constants = new long[f.constants.length];
		this.instructions = f.instructions;
		for (int i = 0; i < f.constants.length; i++) {
			final BigInteger bi = f.constants[i];
			constants[i] = bi.longValue();
		}
		this.internals = fir.internals;
		this.internals_prev = fir.internals_prev;
		this.outputAccess = internals[f.outputId];
		this.disableEdge = disableEdge;
	}

	public boolean execute(int deltaCycle, int epsCycle) {
		int stackPos = -1;
		arrayPos = -1;
		long a = 0;
		long b = 0;
		regUpdates.clear();
		for (final FastInstruction fi : instructions) {
			if (fi.popA) {
				a = stack[stackPos--];
			}
			if (fi.popB) {
				b = stack[stackPos--];
			}
			switch (fi.inst) {
			case noop:
				break;
			case and:
				stack[++stackPos] = fixOp(b & a, fi.arg1);
				break;
			case arith_neg:
				stack[++stackPos] = fixOp(-a, fi.arg1);
				break;
			case bit_neg:
				stack[++stackPos] = fixOp(~a, fi.arg1);
				break;
			case bitAccessSingle:
				final int bit = fi.arg1;
				long t = a >> bit;
				t &= 1;
				stack[++stackPos] = t;
				break;
			case bitAccessSingleRange:
				final int highBit = fi.arg1;
				final int lowBit = fi.arg2;
				long t2 = a >> lowBit;
				t2 &= (1l << ((highBit - lowBit) + 1)) - 1;
				stack[++stackPos] = t2;
				break;
			case cast_int:
				// Corner cases:
				// value is 0xF (-1 int<4>)
				// cast to int<8> result should be 0xFF
				// value is 0xA (-6 int<4>)
				// cast to int<3> result should be 0xE (-2)
				// Resize sign correctly to correct size
				final int shift = 64 - Math.min(fi.arg1, fi.arg2);
				stack[++stackPos] = ((a << shift) >> shift);
				break;
			case cast_uint:
				// There is nothing special about uints, so we just mask
				// them
				if (fi.arg1 != 64) {
					final long mask = (1l << (fi.arg1)) - 1;
					stack[++stackPos] = a & mask;
				} else {
					stack[++stackPos] = a;
				}
				break;
			case concat:
				stack[++stackPos] = (b << fi.arg2) | a;
				break;
			case const0:
				stack[++stackPos] = 0;
				break;
			case const1:
				stack[++stackPos] = 1;
				break;
			case const2:
				stack[++stackPos] = 2;
				break;
			case constAll1:
				final int width = fi.arg1;
				stack[++stackPos] = (1 << width) - 1;
				break;
			case div:
				stack[++stackPos] = fixOp(b / a, fi.arg1);
				break;
			case eq:
				stack[++stackPos] = b == a ? 1 : 0;
				break;
			case greater:
				stack[++stackPos] = b > a ? 1 : 0;
				break;
			case greater_eq:
				stack[++stackPos] = b >= a ? 1 : 0;
				break;
			case less:
				stack[++stackPos] = b < a ? 1 : 0;
				break;
			case less_eq:
				stack[++stackPos] = b <= a ? 1 : 0;
				break;
			case loadConstant:
				stack[++stackPos] = constants[fi.arg1];
				break;
			case loadInternal:
				stack[++stackPos] = getInternal(fi.arg1, arrayPos).getDataLong();
				arrayPos = -1;
				break;
			case logiAnd:
				stack[++stackPos] = ((a != 0) && (b != 0)) ? 1 : 0;
				break;
			case logiOr:
				stack[++stackPos] = ((a != 0) || (b != 0)) ? 1 : 0;
				break;
			case logiNeg:
				stack[++stackPos] = a == 0 ? 1 : 0;
				break;
			case minus:
				stack[++stackPos] = fixOp(b - a, fi.arg1);
				break;
			case mul:
				stack[++stackPos] = fixOp(b * a, fi.arg1);
				break;
			case mod:
				stack[++stackPos] = fixOp(b % a, fi.arg1);
				break;
			case pow:
				stack[++stackPos] = fixOp(pow(b, a), fi.arg1);
				break;
			case not_eq:
				stack[++stackPos] = b != a ? 1 : 0;
				break;
			case or:
				stack[++stackPos] = fixOp(b | a, fi.arg1);
				break;
			case plus:
				stack[++stackPos] = fixOp(b + a, fi.arg1);
				break;
			case sll:
				stack[++stackPos] = fixOp(b << a, fi.arg1);
				break;
			case sra:
				stack[++stackPos] = fixOp(b >> a, fi.arg1);
				break;
			case srl:
				stack[++stackPos] = fixOp(b >>> a, fi.arg1);
				break;
			case xor:
				stack[++stackPos] = fixOp(b ^ a, fi.arg1);
				break;
			case isFallingEdge: {
				final int off = fi.arg1;
				final LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle))
					return false;
				final long curr = access.getDataLong();
				if (!disableEdge) {
					final LongAccess prevAcc = internals_prev[off];
					prevAcc.offset = access.offset;
					final long prev = prevAcc.getDataLong();
					if ((prev != 1) || (curr != 0))
						return false;
				} else {
					if (curr != 0)
						return false;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				break;
			}
			case isRisingEdge: {
				final int off = fi.arg1;
				final LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle))
					return false;
				final long curr = access.getDataLong();
				if (!disableEdge) {
					final LongAccess prevAcc = internals_prev[off];
					prevAcc.offset = access.offset;
					final long prev = prevAcc.getDataLong();
					if ((prev != 0) || (curr != 1))
						return false;
				} else {
					if (curr != 1)
						return false;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				break;
			}
			case posPredicate: {
				final int off = fi.arg1;
				final LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle, epsCycle))
					return false;
				if (access.getDataLong() == 0)
					return false;
				break;
			}
			case negPredicate: {
				final int off = fi.arg1;
				final LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle, epsCycle))
					return false;
				if (access.getDataLong() != 0)
					return false;
				break;
			}
			case pushAddIndex:
				writeIndex[++arrayPos] = (int) a;
				break;
			case writeInternal:
				final int off = fi.arg1;
				final LongAccess access = getInternal(off, -1);
				access.fillDataLong(arrayPos, writeIndex, a, deltaCycle, epsCycle);
				if (access.ii.isShadowReg) {
					regUpdates.add(access.getRegUpdater());
				}
				arrayPos = -1;
				break;
			}

		}
		if (arrayPos != -1) {
			outputAccess.setOffset(writeIndex);
		}
		outputAccess.setDataLong(stack[0], deltaCycle, epsCycle);
		if (outputAccess.ii.isShadowReg) {
			regUpdates.add(outputAccess.getRegUpdater());
		}
		return true;
	}

	private long pow(long a, long n) {
		long x = 1;
		long nValue = n;
		while (nValue > 0) {
			if ((nValue % 2) == 0) {
				x = x * x;
			} else {
				x = a * x * x;
			}
			nValue /= 2;
		}
		return x;
	}

	private long fixOp(long value, int witdhWithType) {
		final int width = witdhWithType >> 1;
		if ((witdhWithType & 1) == 1)
			return ((value << width) >> width);
		return value & ((1l << width) - 1);
	}

	public LongAccess getInternal(int off, int arrayPos) {
		final LongAccess ea = internals[off];
		if (arrayPos != -1) {
			ea.setOffset(writeIndex);
		}
		return ea;
	}
}
