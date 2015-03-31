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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import org.pshdl.interpreter.Frame;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.HDLFrameInterpreter;
import org.pshdl.interpreter.access.EncapsulatedAccess;

public final class BigIntegerFrame extends ExecutableFrame {
	private final BigInteger stack[];
	private final BigInteger constants[];
	private final IDebugListener listener;

	public BigIntegerFrame(IDebugListener listener, HDLFrameInterpreter fir, Frame f, EncapsulatedAccess internals[], EncapsulatedAccess internals_prev[]) {
		super(fir, f, internals, internals_prev);
		this.stack = new BigInteger[f.maxStackDepth];
		this.constants = f.constants;
		this.listener = listener;
	}

	@Override
	public void execute(int deltaCycle, int epsCycle) {
		int stackPos = -1;
		currentPos = 0;
		int arrayPos = -1;
		int bitPos = -1;
		BigInteger b = BigInteger.ZERO, a = BigInteger.ZERO;
		if (listener != null) {
			listener.startFrame(uniqueID, deltaCycle, epsCycle, this);
		}
		for (final FastInstruction f : instructions) {
			if (f.popA) {
				a = stack[stackPos--];
			}
			if (f.popB) {
				b = stack[stackPos--];
			}
			switch (f.inst) {
			case noop:
				break;
			case and: {
				stack[++stackPos] = b.and(a);
				break;
			}
			case arith_neg: {
				stack[++stackPos] = a.negate();
				break;
			}
			case bit_neg: {
				stack[++stackPos] = a.not();
				break;
			}
			case bitAccessSingle: {
				int bit = f.arg1;
				if (bit == -1) {
					bit = bitIndex[bitPos];
					bitPos = -1;
				}
				stack[++stackPos] = a.shiftRight(bit).and(BigInteger.ONE);
				break;
			}
			case bitAccessSingleRange: {
				final int highBit = f.arg1;
				final int lowBit = f.arg2;
				final BigInteger mask = BigInteger.ONE.shiftLeft((highBit - lowBit) + 1).subtract(BigInteger.ONE);
				final BigInteger current = a.shiftRight(lowBit).and(mask);
				stack[++stackPos] = current;
				break;
			}
			case cast_int: {
				final int targetWidth = f.arg1;
				final int currWidth = f.arg2;
				if (targetWidth >= currWidth) {
					// Do nothing
					stack[++stackPos] = a;
				} else {
					final BigInteger mask = BigInteger.ONE.shiftLeft(targetWidth - 1).subtract(BigInteger.ONE);
					// System.out.println("BigIntegerFrame.execute() cast int<"
					// + currWidth + "> to int<" + targetWidth +
					// "> masking with:" + mask.toString(16));
					BigInteger t = a;
					t = t.and(mask);
					if (a.testBit(targetWidth - 1)) { // MSB is set
						if (t.signum() > 0) {
							t = t.negate();
						}
					} else {
						if (t.signum() < 0) {
							t = t.negate();
						}
					}
					stack[++stackPos] = t;
				}
				break;
			}
			case cast_uint: {
				final BigInteger mask = BigInteger.ONE.shiftLeft(f.arg1).subtract(BigInteger.ONE);
				stack[++stackPos] = a.and(mask);
				break;
			}
			case concat: {
				stack[++stackPos] = b.shiftLeft(f.arg2).or(a);
				break;
			}
			case const0:
				stack[++stackPos] = ZERO;
				break;
			case const1:
				stack[++stackPos] = ONE;
				break;
			case const2:
				stack[++stackPos] = BigInteger.valueOf(2);
				break;
			case constAll1:
				stack[++stackPos] = BigInteger.ONE.shiftLeft(f.arg1).subtract(BigInteger.ONE);
				break;
			case div: {
				stack[++stackPos] = b.divide(a);
				break;
			}
			case eq: {
				stack[++stackPos] = b.equals(a) ? ONE : ZERO;
				break;
			}
			case greater: {
				stack[++stackPos] = b.compareTo(a) > 0 ? ONE : ZERO;
				break;
			}
			case greater_eq: {
				stack[++stackPos] = b.compareTo(a) >= 0 ? ONE : ZERO;
				break;
			}
			case less: {
				stack[++stackPos] = b.compareTo(a) < 0 ? ONE : ZERO;
				break;
			}
			case less_eq: {
				stack[++stackPos] = b.compareTo(a) <= 0 ? ONE : ZERO;
				break;
			}
			case loadConstant:
				stack[++stackPos] = constants[f.arg1];
				break;
			case loadInternal:
				stack[++stackPos] = getInternal(f.arg1, arrayPos).getDataBig();
				arrayPos = -1;
				break;
			case logiAnd: {
				stack[++stackPos] = ((!ZERO.equals(b)) && (!ZERO.equals(a))) ? ONE : ZERO;
				break;
			}
			case logiOr: {
				stack[++stackPos] = (!ZERO.equals(b) || !ZERO.equals(a)) ? ONE : ZERO;
				break;
			}
			case logiNeg: {
				if (ZERO.equals(a)) {
					stack[++stackPos] = ONE;
				} else {
					stack[++stackPos] = ZERO;
				}
				break;
			}
			case minus: {
				stack[++stackPos] = b.subtract(a);
				break;
			}
			case mul: {
				stack[++stackPos] = b.multiply(a);
				break;
			}
			case mod:
				stack[++stackPos] = b.mod(a);
				break;
			case pow:
				stack[++stackPos] = b.pow(a.intValue());
				break;
			case not_eq: {
				stack[++stackPos] = !b.equals(a) ? ONE : ZERO;
				break;
			}
			case or: {
				stack[++stackPos] = b.or(a);
				break;
			}
			case plus: {
				stack[++stackPos] = b.add(a);
				break;
			}
			case sll: {
				stack[++stackPos] = b.shiftLeft(a.intValue());
				break;
			}
			case sra: {
				stack[++stackPos] = b.shiftRight(a.intValue());
				break;
			}
			case srl: {
				stack[++stackPos] = srl(b, f.arg1, a.intValue());
				break;
			}
			case xor: {
				stack[++stackPos] = b.xor(a);
				break;
			}
			case isFallingEdge: {
				final int off = f.arg1;
				final EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (listener != null) {
						listener.skippingHandledEdge(uniqueID, access.ii, false, this);
					}
					return;
				}
				final long curr = access.getDataLong();
				final EncapsulatedAccess prevAccess = internals_prev[off];
				prevAccess.offset = access.offset;
				final long prev = prevAccess.getDataLong();
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
				final int off = f.arg1;
				final EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (listener != null) {
						listener.skippingHandledEdge(uniqueID, access.ii, true, this);
					}
					return;
				}
				final long curr = access.getDataLong();
				final EncapsulatedAccess prevAccess = internals_prev[off];
				prevAccess.offset = access.offset;
				final long prev = prevAccess.getDataLong();
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
				final int off = f.arg1;
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
				if (ZERO.equals(access.getDataBig())) {
					if (listener != null) {
						listener.skippingPredicateNotMet(uniqueID, access.ii, true, access.getDataBig(), this);
					}
					return;
				}
				break;
			}
			case negPredicate: {
				final int off = f.arg1;
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
				if (!ZERO.equals(access.getDataBig())) {
					if (listener != null) {
						listener.skippingPredicateNotMet(uniqueID, access.ii, false, access.getDataBig(), this);
					}
					return;
				}
				break;
			}
			case pushAddIndex:
				if (f.arg2 == 0) {
					writeIndex[++arrayPos] = a.intValue();
				} else {
					bitIndex[++bitPos] = a.intValue();
				}
				break;
			case writeInternal:
				final int off = f.arg1;
				final EncapsulatedAccess access = getInternal(off, -1);
				access.fillDataBig(arrayPos, writeIndex, a, deltaCycle, epsCycle);
				arrayPos = -1;
				break;
			}
			if (listener != null)
				if (stackPos >= 0) {
					if (f.popB) {
						listener.twoArgOp(uniqueID, b, f, (a), (stack[stackPos]), this);
					} else if (f.popA) {
						listener.oneArgOp(uniqueID, f, (a), (stack[stackPos]), this);
					} else {
						listener.noArgOp(uniqueID, f, (stack[stackPos]), this);
					}
				} else {
					listener.emptyStack(uniqueID, f, this);
				}
		}
		for (final EncapsulatedAccess encapsulatedAccess : outputAccess) {
			if (arrayPos != -1) {
				encapsulatedAccess.setOffset(writeIndex);
			}
			if (bitPos != -1) {
				encapsulatedAccess.setBitOffset(bitIndex[bitPos]);
			}
			encapsulatedAccess.setDataBig(stack[0], deltaCycle, epsCycle);
			if (listener != null) {
				listener.writingResult(uniqueID, encapsulatedAccess.ii, stack[0], this);
			}
		}

		return;
	}

	public EncapsulatedAccess getInternal(int off, int arrayPos) {
		final EncapsulatedAccess res = internals[off];
		if (arrayPos != -1) {
			res.setOffset(writeIndex);
		}
		return res;
	}

	public static BigInteger srl(BigInteger l, int width, int shiftBy) {
		if (shiftBy == 0)
			return l;
		if (l.signum() >= 0)
			return l.shiftRight(shiftBy);
		final BigInteger opener = BigInteger.ONE.shiftLeft(width + 1);
		final BigInteger opened = l.subtract(opener);
		final BigInteger mask = opener.subtract(BigInteger.ONE).shiftRight(shiftBy + 1);
		final BigInteger res = opened.shiftRight(shiftBy).and(mask);
		return res;
	}
}
