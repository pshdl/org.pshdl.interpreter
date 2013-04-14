package org.pshdl.interpreter.frames;

import java.math.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.utils.FluidFrame.Instruction;

public final class LongFrame extends ExecutableFrame {
	private final long stack[];
	private final long constants[];

	public LongFrame(Frame f, boolean printing, EncapsulatedAccess internals[], EncapsulatedAccess internals_prev[]) {
		super(f, printing, internals, internals_prev);
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
		currentPos = 0;
		regUpdated = false;
		do {
			Instruction instruction = values[next()];
			switch (instruction) {
			case noop:
				break;
			case and: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a & b;
				break;
			}
			case arith_neg: {
				stack[stackPos] = -stack[stackPos];
				break;
			}
			case bit_neg: {
				stack[stackPos] = ~stack[stackPos];
				break;
			}
			case bitAccessSingle: {
				int bit = readVarInt();
				long current = stack[stackPos];
				current >>= bit;
				current &= 1;
				stack[stackPos] = current;
				break;
			}
			case bitAccessSingleRange: {
				int lowBit = readVarInt();
				int highBit = readVarInt();
				long current = stack[stackPos];
				current >>= lowBit;
				current &= (1 << ((highBit - lowBit) + 1)) - 1;
				stack[stackPos] = current;
				break;
			}
			case cast_int:
				// Corner cases:
				// value is 0xF (-1 int<4>)
				// cast to int<8> result should be 0xFF
				// value is 0xA (-6 int<4>)
				// cast to int<3> result should be 0xE (-2)
				// Resize sign correctly to correct size
				int targetSize = readVarInt();
				int currentSize = readVarInt();
				// Move the highest bit to the MSB
				long temp = stack[stackPos] << (64 - currentSize);
				// And move it back. As in Java everything is signed,
				// the sign extension is done correctly. We now have a
				// fully signed value
				temp = (temp >> (64 - currentSize));
				// Throw away unnecessary bits (only needed when
				// targetsize>currentSize)
				temp = stack[stackPos] << (64 - targetSize);
				stack[stackPos] = (temp >> (64 - targetSize));
				break;
			case cast_uint:
				// There is nothing special about uints, so we just mask
				// them
				long mask = (1 << (readVarInt())) - 1;
				readVarInt();
				stack[stackPos] &= mask;
				break;
			case concat:
				// Implement somewhen...
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
				int width = readVarInt();
				stack[++stackPos] = (1 << width) - 1;
				break;
			case div: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a / b;
				break;
			}
			case eq: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a == b ? 1 : 0;
				break;
			}
			case greater: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a > b ? 1 : 0;
				break;
			}
			case greater_eq: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a >= b ? 1 : 0;
				break;
			}
			case less: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a < b ? 1 : 0;
				break;
			}
			case less_eq: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a <= b ? 1 : 0;
				break;
			}
			case loadConstant:
				stack[++stackPos] = constants[readVarInt()];
				break;
			case loadInternal:
				stack[++stackPos] = internals[readVarInt()].getDataLong();
				break;
			case logiAnd: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = ((a != 0) && (b != 0)) ? 1 : 0;
				break;
			}
			case logiOr: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = ((a != 0) || (b != 0)) ? 1 : 0;
				break;
			}
			case logiNeg: {
				long a = stack[stackPos];
				if (a == 0) {
					stack[stackPos] = 1;
				} else {
					stack[stackPos] = 0;
				}
				break;
			}
			case minus: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a - b;
				break;
			}
			case mul: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a * b;
				break;
			}
			case not_eq: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a != b ? 1 : 0;
				break;
			}
			case or: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a | b;
				break;
			}
			case plus: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a + b;
				break;
			}
			case sll: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a << b;
				break;
			}
			case sra: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a >> b;
				break;
			}
			case srl: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a >>> b;
				break;
			}
			case xor: {
				long b = stack[stackPos--];
				long a = stack[stackPos];
				stack[stackPos] = a ^ b;
				break;
			}
			case isFallingEdge: {
				int off = readVarInt();
				EncapsulatedAccess access = internals[off];
				if (access.skip(deltaCycle, epsCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: falling edge already handled");
					}
					return;
				}
				long curr = internals[off].getDataLong();
				long prev = internals_prev[off].getDataLong();
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
				int off = readVarInt();
				EncapsulatedAccess access = internals[off];
				if (access.skip(deltaCycle, epsCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: rising edge already handled");
					}
					return;
				}
				long curr = internals[off].getDataLong();
				long prev = internals_prev[off].getDataLong();
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
			case posPredicate: {
				int off = readVarInt();
				EncapsulatedAccess access = internals[off];
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not fresh enough");
					}
					return;
				}
				if (access.getDataLong() == 0) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not positive");
					}
					return;
				}
				break;
			}
			case negPredicate: {
				int off = readVarInt();
				EncapsulatedAccess access = internals[off];
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle)) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not fresh enough");
					}
					return;
				}
				if (access.getDataLong() != 0) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not negative");
					}
					return;
				}
				break;
			}
			}
		} while (hasMore());
		EncapsulatedAccess ea = internals[outputID];
		ea.setData(stack[0], deltaCycle, epsCycle);
		// System.out.println("LongFrame.execute()" + ea + " Value:" + stack[0]
		// + " read:" + ea.getDataLong());
		return;
	}
}
