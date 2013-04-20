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

import static java.math.BigInteger.*;

import java.math.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.access.*;

public final class BigIntegerFrame extends ExecutableFrame {
	private final BigInteger stack[];
	private final BigInteger constants[];

	public BigIntegerFrame(Frame f, boolean printing, EncapsulatedAccess internals[], EncapsulatedAccess internals_prev[]) {
		super(f, printing, internals, internals_prev);
		this.stack = new BigInteger[f.maxStackDepth];
		this.constants = f.constants;
	}

	@Override
	public void execute(int deltaCycle, int epsCycle) {
		int stackPos = -1;
		currentPos = 0;
		regUpdated = false;
		int arrayPos = -1;
		for (FastInstruction f : instructions) {
			switch (f.inst) {
			case noop:
				break;
			case and: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.and(b);
				break;
			}
			case arith_neg: {
				stack[stackPos] = stack[stackPos].negate();
				break;
			}
			case bit_neg: {
				stack[stackPos] = stack[stackPos].not();
				break;
			}
			case bitAccessSingle: {
				int bit = f.arg1;
				BigInteger current = stack[stackPos].shiftRight(bit).and(BigInteger.ONE);
				stack[stackPos] = current;
				break;
			}
			case bitAccessSingleRange: {
				int lowBit = f.arg1;
				int highBit = f.arg2;
				BigInteger mask = BigInteger.ONE.shiftLeft((highBit - lowBit) + 1).subtract(BigInteger.ONE);
				BigInteger current = stack[stackPos].shiftRight(lowBit).and(mask);
				stack[stackPos] = current;
				break;
			}
			case cast_int: {
				int targetWidth = f.arg1;
				int currWidth = f.arg2;
				if (targetWidth >= currWidth) {
					// Do nothing
				} else {
					BigInteger mask = BigInteger.ONE.shiftLeft(targetWidth).subtract(BigInteger.ONE);
					System.out.println("BigIntegerFrame.execute() cast int<" + currWidth + "> to int<" + targetWidth + "> masking with:" + mask.toString(16));
					BigInteger t = stack[stackPos];
					t = t.and(mask);
					if (t.testBit(targetWidth - 1)) { // MSB is set
						if (t.signum() > 0) {
							t = t.negate();
						}
					} else {
						if (t.signum() < 0) {
							t = t.negate();
						}
					}
					stack[stackPos] = t;
				}
				break;
			}
			case cast_uint: {
				BigInteger mask = BigInteger.ONE.shiftLeft(f.arg1).subtract(BigInteger.ONE);
				stack[stackPos] = stack[stackPos].and(mask);
				break;
			}
			case concat:
				// Implement somewhen...
				break;
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
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.divide(b);
				break;
			}
			case eq: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.equals(b) ? ONE : ZERO;
				break;
			}
			case greater: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.compareTo(b) > 0 ? ONE : ZERO;
				break;
			}
			case greater_eq: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.compareTo(b) >= 0 ? ONE : ZERO;
				break;
			}
			case less: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.compareTo(b) < 0 ? ONE : ZERO;
				break;
			}
			case less_eq: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.compareTo(b) <= 0 ? ONE : ZERO;
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
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = ((!ZERO.equals(a)) && (!ZERO.equals(b))) ? ONE : ZERO;
				break;
			}
			case logiOr: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = (!ZERO.equals(a) || !ZERO.equals(b)) ? ONE : ZERO;
				break;
			}
			case logiNeg: {
				BigInteger a = stack[stackPos];
				if (ZERO.equals(a)) {
					stack[stackPos] = ONE;
				} else {
					stack[stackPos] = ZERO;
				}
				break;
			}
			case minus: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.subtract(b);
				break;
			}
			case mul: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.multiply(b);
				break;
			}
			case not_eq: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = !a.equals(b) ? ONE : ZERO;
				break;
			}
			case or: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.or(b);
				break;
			}
			case plus: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.add(b);
				break;
			}
			case sll: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.shiftLeft(b.intValue());
				break;
			}
			case sra: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.shiftRight(b.intValue());
				break;
			}
			case srl: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = srl(a, 1024, b.intValue());
				break;
			}
			case xor: {
				BigInteger b = stack[stackPos--];
				BigInteger a = stack[stackPos];
				stack[stackPos] = a.xor(b);
				break;
			}
			case isFallingEdge: {
				int off = f.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: falling edge already handled");
					}
					return;
				}
				long curr = access.getDataLong();
				EncapsulatedAccess prevAccess = internals_prev[off];
				prevAccess.offset = access.offset;
				long prev = prevAccess.getDataLong();
				if ((prev != 1) || (curr != 0)) {
					if (printing) {
						System.out.println("\t\tSkipped: not a falling edge");
					}
					return;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				regUpdated = true;
				break;
			}
			case isRisingEdge: {
				int off = f.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: rising edge already handled");
					}
					return;
				}
				long curr = access.getDataLong();
				EncapsulatedAccess prevAccess = internals_prev[off];
				prevAccess.offset = access.offset;
				long prev = prevAccess.getDataLong();
				if ((prev != 0) || (curr != 1)) {
					if (printing) {
						System.out.println("\t\tSkipped: Not a rising edge");
					}
					return;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				regUpdated = true;
				break;
			}
			case pushAddIndex:
				writeIndex[++arrayPos] = stack[stackPos--].intValue();
				break;
			case posPredicate: {
				int off = f.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not fresh enough");
					}
					return;
				}
				if (ZERO.equals(access.getDataBig())) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not positive");
					}
					return;
				}
				break;
			}
			case negPredicate: {
				int off = f.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not fresh enough");
					}
					return;
				}
				if (!ZERO.equals(access.getDataBig())) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not negative");
					}
					return;
				}
				break;
			}
			}
		}
		if (arrayPos != -1) {
			outputAccess.setOffset(writeIndex);
		}
		outputAccess.setDataBig(stack[0], deltaCycle, epsCycle);
		return;
	}

	public EncapsulatedAccess getInternal(int off, int arrayPos) {
		EncapsulatedAccess res = internals[off];
		if (arrayPos != -1) {
			res.setOffset(writeIndex);
		}
		return res;
	}

	public static BigInteger srl(BigInteger l, int width, int shiftBy) {
		if (l.signum() >= 0)
			return l.shiftRight(shiftBy);
		BigInteger opener = BigInteger.ONE.shiftLeft(width + 1);
		BigInteger opened = l.subtract(opener);
		BigInteger mask = opener.subtract(BigInteger.ONE).shiftRight(shiftBy + 1);
		BigInteger res = opened.shiftRight(shiftBy).and(mask);
		return res;
	}
}
