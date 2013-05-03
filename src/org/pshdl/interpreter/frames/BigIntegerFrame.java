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
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.access.*;

public final class BigIntegerFrame extends ExecutableFrame {
	private final BigInteger stack[];
	private final BigInteger constants[];

	public BigIntegerFrame(HDLFrameInterpreter fir, Frame f, EncapsulatedAccess internals[], EncapsulatedAccess internals_prev[]) {
		super(fir, f, internals, internals_prev);
		this.stack = new BigInteger[f.maxStackDepth];
		this.constants = f.constants;
	}

	@Override
	public void execute(int deltaCycle, int epsCycle) {
		int stackPos = -1;
		currentPos = 0;
		regUpdated = false;
		int arrayPos = -1;
		BigInteger b = null, a = null;
		for (FastInstruction f : instructions) {
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
				stack[++stackPos] = a.shiftRight(bit).and(BigInteger.ONE);
				break;
			}
			case bitAccessSingleRange: {
				int highBit = f.arg1;
				int lowBit = f.arg2;
				BigInteger mask = BigInteger.ONE.shiftLeft((highBit - lowBit) + 1).subtract(BigInteger.ONE);
				BigInteger current = a.shiftRight(lowBit).and(mask);
				stack[++stackPos] = current;
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
					BigInteger t = a;
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
					stack[++stackPos] = t;
				}
				break;
			}
			case cast_uint: {
				BigInteger mask = BigInteger.ONE.shiftLeft(f.arg1).subtract(BigInteger.ONE);
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
				stack[++stackPos] = srl(b, 1024, a.intValue());
				break;
			}
			case xor: {
				stack[++stackPos] = b.xor(a);
				break;
			}
			case isFallingEdge: {
				int off = f.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: falling edge already handled");
					}
					return;
				}
				long curr = access.getDataLong();
				EncapsulatedAccess prevAccess = internals_prev[off];
				prevAccess.offset = access.offset;
				long prev = prevAccess.getDataLong();
				if ((prev != 1) || (curr != 0)) {
					if (isPrinting()) {
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
					if (isPrinting()) {
						System.out.println("\t\tSkipped: rising edge already handled");
					}
					return;
				}
				long curr = access.getDataLong();
				EncapsulatedAccess prevAccess = internals_prev[off];
				prevAccess.offset = access.offset;
				long prev = prevAccess.getDataLong();
				if ((prev != 0) || (curr != 1)) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: Not a rising edge");
					}
					return;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				regUpdated = true;
				break;
			}
			case posPredicate: {
				int off = f.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle)) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: predicate not fresh enough");
					}
					return;
				}
				if (ZERO.equals(access.getDataBig())) {
					if (isPrinting()) {
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
					if (isPrinting()) {
						System.out.println("\t\tSkipped: predicate not fresh enough");
					}
					return;
				}
				if (!ZERO.equals(access.getDataBig())) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: predicate not negative");
					}
					return;
				}
				break;
			}
			case pushAddIndex:
				writeIndex[++arrayPos] = a.intValue();
				break;
			}
			if (isPrinting()) {
				if (stackPos >= 0) {
					if (f.popB) {
						System.out.printf("\t\t0x%x %s 0x%x = 0x%x\n", b, f, a, stack[stackPos]);
					} else if (f.popA) {
						System.out.printf("\t\t%s 0x%x = 0x%x\n", f, a, stack[stackPos]);
					} else {
						System.out.printf("\t\t%s = 0x%x\n", f, stack[stackPos]);
					}
				} else {
					System.out.printf("\t\t%s = emptyStack\n", f);
				}
			}
		}
		if (arrayPos != -1) {
			outputAccess.setOffset(writeIndex);
		}
		outputAccess.setDataBig(stack[0], deltaCycle, epsCycle);
		if (isPrinting()) {
			System.out.println("\t\tWriting result:" + outputAccess + " Value:" + stack[0] + " read:" + outputAccess.getDataBig());
		}
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
