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

import java.math.*;
import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess;
import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess.RegUpdater;
import org.pshdl.interpreter.Frame.FastInstruction;

public class FastFrame {

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
	protected static final int writeInternal = 38;

	private final long stack[];
	private final long constants[];
	public List<RegUpdater> regUpdates = new ArrayList<>();
	private int arrayPos = -1;
	private final int[] writeIndex = new int[8];
	private final FastInstruction[] instructions;
	private final LongAccess[] internals;
	private final LongAccess[] internals_prev;
	public final LongAccess outputAccess;
	private final boolean disableEdge;

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
			switch (fi.inst.ordinal()) {
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
				final int targetSize = fi.arg1;
				// Throw away unnecessary bits (only needed when
				// targetsize>currentSize)
				final long temp = a << (64 - targetSize);
				stack[++stackPos] = (temp >> (64 - targetSize));
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

	private long fixOp(long l, int arg1) {
		final int val = arg1 >> 1;
		if ((arg1 & 1) == 1)
			return ((l << val) >> val);
		return l & ((1l << val) - 1);
	}

	public LongAccess getInternal(int off, int arrayPos) {
		final LongAccess ea = internals[off];
		if (arrayPos != -1) {
			ea.setOffset(writeIndex);
		}
		return ea;
	}
}
