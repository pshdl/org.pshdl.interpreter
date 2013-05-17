package org.pshdl.interpreter.frames;

import java.math.*;
import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.utils.*;

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
	public boolean regUpdated;
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
		List<FastInstruction> fi = new LinkedList<FastInstruction>();
		for (FastInstruction fast : f.instructions) {
			if ((fast.inst == Instruction.isRisingEdge) || (fast.inst == Instruction.isFallingEdge)) {
				if (!disableEdge) {
					fi.add(fast);
				} else {
					regUpdated = true;
				}
			} else {
				fi.add(fast);
			}
		}
		this.instructions = fi.toArray(new FastInstruction[fi.size()]);
		for (int i = 0; i < f.constants.length; i++) {
			BigInteger bi = f.constants[i];
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
		if (!disableEdge) {
			regUpdated = false;
		}
		long a = 0;
		long b = 0;
		for (FastInstruction fi : instructions) {
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
				LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle))
					return false;
				long curr = access.getDataLong();
				if (!disableEdge) {
					LongAccess prevAcc = internals_prev[off];
					prevAcc.offset = access.offset;
					long prev = prevAcc.getDataLong();
					if ((prev != 1) || (curr != 0))
						return false;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				regUpdated = true;
				break;
			}
			case isRisingEdge: {
				int off = fi.arg1;
				LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				if (access.skip(deltaCycle, epsCycle))
					return false;
				if (!disableEdge) {
					long curr = access.getDataLong();
					LongAccess prevAcc = internals_prev[off];
					prevAcc.offset = access.offset;
					long prev = prevAcc.getDataLong();
					if ((prev != 0) || (curr != 1))
						return false;
				}
				access.setLastUpdate(deltaCycle, epsCycle);
				regUpdated = true;
				break;
			}
			case posPredicate: {
				int off = fi.arg1;
				LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle))
					return false;
				if (access.getDataLong() == 0)
					return false;
				break;
			}
			case negPredicate: {
				int off = fi.arg1;
				LongAccess access = getInternal(off, arrayPos);
				arrayPos = -1;
				// If data is not from this deltaCycle it was not
				// updated that means prior predicates failed
				if (!access.isFresh(deltaCycle))
					return false;
				if (access.getDataLong() != 0)
					return false;
				break;
			}
			case pushAddIndex:
				writeIndex[++arrayPos] = (int) a;
				break;
			case writeInternal:
				int off = fi.arg1;
				LongAccess access = getInternal(off, -1);
				access.fillDataLong(arrayPos, writeIndex, a, deltaCycle, epsCycle);
				arrayPos = -1;
				break;
			}

		}
		if (arrayPos != -1) {
			outputAccess.setOffset(writeIndex);
		}
		outputAccess.setDataLong(stack[0], deltaCycle, epsCycle);
		return true;
	}

	public LongAccess getInternal(int off, int arrayPos) {
		LongAccess ea = internals[off];
		if (arrayPos != -1) {
			ea.setOffset(writeIndex);
		}
		return ea;
	}
}
