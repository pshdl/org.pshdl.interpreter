package org.pshdl.interpreter.frames;

import static java.math.BigInteger.*;

import java.math.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.utils.FluidFrame.Instruction;

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
		do {
			Instruction instruction = values[next()];
			switch (instruction) {
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
				int bit = readVarInt();
				BigInteger current = stack[stackPos].shiftRight(bit).and(BigInteger.ONE);
				stack[stackPos] = current;
				break;
			}
			case bitAccessSingleRange: {
				int lowBit = readVarInt();
				int highBit = readVarInt();
				BigInteger mask = BigInteger.ONE.shiftLeft((highBit - lowBit) + 1).subtract(BigInteger.ONE);
				BigInteger current = stack[stackPos].shiftRight(lowBit).and(mask);
				stack[stackPos] = current;
				break;
			}
			case cast_int: {
				int targetWidth = readVarInt();
				int currWidth = readVarInt();
				if (targetWidth >= currWidth) {
					// Do nothing
				} else {
					BigInteger mask = BigInteger.ONE.shiftLeft(targetWidth).subtract(BigInteger.ONE);
					System.out.println("BigIntegerFrame.execute() cast int<" + currWidth + "> to int<" + targetWidth + "> masking with:" + mask.toString(16));
					BigInteger t = stack[stackPos];
					t = t.and(mask);
					if (t.testBit(targetWidth - 1)) { // MSB is set
						if (t.signum() > 0) // Sign is +
							t = t.negate();
					} else {
						if (t.signum() < 0) // Sign is -
							t = t.negate();
					}
					stack[stackPos] = t;
				}
				break;
			}
			case cast_uint: {
				BigInteger mask = getWidthMask();
				readVarInt();// Ignore currentSize
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
				stack[++stackPos] = getWidthMask();
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
				stack[++stackPos] = constants[readVarInt()];
				break;
			case loadInternal:
				stack[++stackPos] = internals[readVarInt()].getDataBig();
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
				if (ZERO.equals(access.getDataBig())) {
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
				if (!ZERO.equals(access.getDataBig())) {
					if (printing) {
						System.out.println("\t\tSkipped: predicate not negative");
					}
					return;
				}
				break;
			}
			}
		} while (hasMore());
		internals[outputID].setDataBig(stack[0], deltaCycle, epsCycle);
		return;
	}

	private BigInteger getWidthMask() {
		return BigInteger.ONE.shiftLeft(readVarInt()).subtract(BigInteger.ONE);
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
