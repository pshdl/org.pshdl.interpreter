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

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.access.*;

public final class LongFrame extends ExecutableFrame {

	private final long stack[];
	private final long constants[];
	private final IDebugListener listener;

	public LongFrame(IDebugListener listener, HDLFrameInterpreter fir, Frame f, EncapsulatedAccess internals[], EncapsulatedAccess internals_prev[]) {
		super(fir, f, internals, internals_prev);
		this.listener = listener;
		this.stack = new long[f.maxStackDepth];
		this.constants = new long[f.constants.length];

		for (int i = 0; i < f.constants.length; i++) {
			final BigInteger bi = f.constants[i];
			constants[i] = bi.longValue();
		}
	}

	@Override
	public void execute(int deltaCycle, int epsCycle) {
		int stackPos = -1;
		int arrayPos = -1;
		currentPos = 0;
		long a = 0;
		long b = 0;
		if (listener != null) {
			listener.startFrame(uniqueID, deltaCycle, epsCycle, this);
		}
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
			// Throw away unnecessary bits (only needed when
			// targetsize>currentSize)
			stack[++stackPos] = (a << shift) >> shift;
			break;
			case cast_uint:
				// There is nothing special about uints, so we just mask
				// them
				if (fi.arg1 != 64) {
					final long mask = (1l << (fi.arg1)) - 1;
					final long res = a & mask;
					stack[++stackPos] = res;
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
			case not_eq:
				stack[++stackPos] = b != a ? 1 : 0;
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
				final EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (listener != null) {
						listener.skippingHandledEdge(uniqueID, access.ii, false, this);
					}
					return;
				}
				final long curr = access.getDataLong();
				final EncapsulatedAccess prevAcc = internals_prev[off];
				prevAcc.offset = access.offset;
				final long prev = prevAcc.getDataLong();
				if ((prev != 1) || (curr != 0)) {
					if (listener != null) {
						listener.skippingNotAnEdge(uniqueID, access.ii, false, this);
					}
					return;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				break;
			}
			case isRisingEdge: {
				final int off = fi.arg1;
				final EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (listener != null) {
						listener.skippingHandledEdge(uniqueID, access.ii, true, this);
					}
					return;
				}
				final long curr = access.getDataLong();
				final EncapsulatedAccess prevAcc = internals_prev[off];
				prevAcc.offset = access.offset;
				final long prev = prevAcc.getDataLong();
				if ((prev != 0) || (curr != 1)) {
					if (listener != null) {
						listener.skippingNotAnEdge(uniqueID, access.ii, true, this);
					}
					return;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				break;
			}
			case posPredicate: {
				final int off = fi.arg1;
				final EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle, epsCycle)) {
					if (listener != null) {
						listener.skippingPredicateNotFresh(uniqueID, access.ii, true, this);
					}
					return;
				}
				if (access.getDataLong() == 0) {
					if (listener != null) {
						listener.skippingPredicateNotMet(uniqueID, access.ii, true, access.getDataBig(), this);
					}
					return;
				}
				break;
			}
			case negPredicate: {
				final int off = fi.arg1;
				final EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle, epsCycle)) {
					if (listener != null) {
						listener.skippingPredicateNotFresh(uniqueID, access.ii, false, this);
					}
					return;
				}
				if (access.getDataLong() != 0) {
					if (listener != null) {
						listener.skippingPredicateNotMet(uniqueID, access.ii, false, access.getDataBig(), this);
					}
					return;
				}
				break;
			}
			case pushAddIndex:
				writeIndex[++arrayPos] = (int) a;
				break;
			case writeInternal:
				final int off = fi.arg1;
				final EncapsulatedAccess access = getInternal(off, -1);
				access.fillDataLong(arrayPos, writeIndex, a, deltaCycle, epsCycle);
				if (listener != null) {
					listener.writeInternal(uniqueID, arrayPos, writeIndex, BigInteger.valueOf(a), access.ii, this);
				}
				arrayPos = -1;
				break;
			}
			if (listener != null)
				if (stackPos >= 0) {
					if (fi.popB) {
						listener.twoArgOp(uniqueID, BigInteger.valueOf(b), fi, BigInteger.valueOf(a), BigInteger.valueOf(stack[stackPos]), this);
					} else if (fi.popA) {
						listener.oneArgOp(uniqueID, fi, BigInteger.valueOf(a), BigInteger.valueOf(stack[stackPos]), this);
					} else {
						listener.noArgOp(uniqueID, fi, BigInteger.valueOf(stack[stackPos]), this);
					}
				} else {
					listener.emptyStack(uniqueID, fi, this);
				}
		}
		if (arrayPos != -1) {
			outputAccess.setOffset(writeIndex);
		}
		outputAccess.setDataLong(stack[0], deltaCycle, epsCycle);
		if (listener != null) {
			listener.writingResult(uniqueID, outputAccess.ii, BigInteger.valueOf(stack[0]), this);
		}
		return;
	}

	private long fixOp(long l, int arg1) {
		final int val = arg1 >> 1;
					if ((arg1 & 1) == 1)
						return ((l << val) >> val);
					return l & ((1l << val) - 1);
	}

	public EncapsulatedAccess getInternal(int off, int arrayPos) {
		final EncapsulatedAccess ea = internals[off];
		if (arrayPos != -1) {
			ea.setOffset(writeIndex);
		}
		if (listener != null) {
			listener.loadingInternal(uniqueID, ea.ii, ea.getDataBig(), this);
		}
		return ea;
	}

}
