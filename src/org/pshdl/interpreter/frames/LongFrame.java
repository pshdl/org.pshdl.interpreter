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

	public LongFrame(HDLFrameInterpreter fir, Frame f, EncapsulatedAccess internals[], EncapsulatedAccess internals_prev[]) {
		super(fir, f, internals, internals_prev);
		this.stack = new long[f.maxStackDepth];
		this.constants = new long[f.constants.length];

		for (int i = 0; i < f.constants.length; i++) {
			BigInteger bi = f.constants[i];
			constants[i] = bi.longValue();
		}
	}

	@Override
	public void execute(int deltaCycle, int epsCycle) {
		int stackPos = -1;
		int arrayPos = -1;
		currentPos = 0;
		regUpdated = false;
		long a = 0;
		long b = 0;
		for (FastInstruction fi : instructions) {
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
				stack[++stackPos] = b & a;
				break;
			case arith_neg:
				stack[++stackPos] = -a;
				break;
			case bit_neg:
				stack[++stackPos] = ~a;
				break;
			case bitAccessSingle:
				int bit = fi.arg1;
				long t = a >> bit;
				t &= 1;
				stack[++stackPos] = t;
				break;
			case bitAccessSingleRange:
				int highBit = fi.arg1;
				int lowBit = fi.arg2;
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
				int targetSize = fi.arg1;
				// Throw away unnecessary bits (only needed when
				// targetsize>currentSize)
				long temp = a << (64 - targetSize);
				stack[++stackPos] = (temp >> (64 - targetSize));
				break;
			case cast_uint:
				// There is nothing special about uints, so we just mask
				// them
				if (fi.arg1 != 64) {
					long mask = (1l << (fi.arg1)) - 1;
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
				int width = fi.arg1;
				stack[++stackPos] = (1 << width) - 1;
				break;
			case div:
				stack[++stackPos] = b / a;
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
				stack[++stackPos] = b - a;
				break;
			case mul:
				stack[++stackPos] = b * a;
				break;
			case not_eq:
				stack[++stackPos] = b != a ? 1 : 0;
				break;
			case or:
				stack[++stackPos] = b | a;
				break;
			case plus:
				stack[++stackPos] = b + a;
				break;
			case sll:
				stack[++stackPos] = b << a;
				break;
			case sra:
				stack[++stackPos] = b >> a;
				break;
			case srl:
				stack[++stackPos] = b >>> a;
				break;
			case xor:
				stack[++stackPos] = b ^ a;
				break;
			case isFallingEdge: {
				int off = fi.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: falling edge already handled");
					}
					return;
				}
				long curr = access.getDataLong();
				EncapsulatedAccess prevAcc = internals_prev[off];
				prevAcc.offset = access.offset;
				long prev = prevAcc.getDataLong();
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
				int off = fi.arg1;
				EncapsulatedAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle)) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: rising edge already handled");
					}
					return;
				}
				long curr = access.getDataLong();
				EncapsulatedAccess prevAcc = internals_prev[off];
				prevAcc.offset = access.offset;
				long prev = prevAcc.getDataLong();
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
				int off = fi.arg1;
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
				if (access.getDataLong() == 0) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: predicate not positive");
					}
					return;
				}
				break;
			}
			case negPredicate: {
				int off = fi.arg1;
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
				if (access.getDataLong() != 0) {
					if (isPrinting()) {
						System.out.println("\t\tSkipped: predicate not negative");
					}
					return;
				}
				break;
			}
			case pushAddIndex:
				writeIndex[++arrayPos] = (int) a;
				break;
			}
			if (isPrinting()) {
				if (stackPos >= 0) {
					if (fi.popB) {
						System.out.printf("\t\t0x%x %s 0x%x = 0x%x\n", b, fi, a, stack[stackPos]);
					} else if (fi.popA) {
						System.out.printf("\t\t%s 0x%x = 0x%x\n", fi, a, stack[stackPos]);
					} else {
						System.out.printf("\t\t%s = 0x%x\n", fi, stack[stackPos]);
					}
				} else {
					System.out.printf("\t\t%s = emptyStack\n", fi);
				}
			}
		}
		if (arrayPos != -1) {
			outputAccess.setOffset(writeIndex);
		}
		outputAccess.setDataLong(stack[0], deltaCycle, epsCycle);
		if (isPrinting()) {
			System.out.println("\t\tWriting result:" + outputAccess + " Value:" + stack[0] + " read:" + outputAccess.getDataLong());
		}
		return;
	}

	public EncapsulatedAccess getInternal(int off, int arrayPos) {
		EncapsulatedAccess ea = internals[off];
		if (arrayPos != -1) {
			ea.setOffset(writeIndex);
		}
		if (isPrinting()) {
			// System.out.println("\t\t\tLoading:" + ea);
		}
		return ea;
	}

}
