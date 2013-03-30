package org.pshdl.interpreter;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.pshdl.interpreter.utils.FluidFrame.Instruction;

public final class HDLFrameInterpreter {
	protected final ExecutableModel model;

	protected final long storage[], storage_prev[];

	protected static final Pattern aiFormatName = Pattern.compile("(.*?)(?:\\{(?:(\\d+)(?:\\:(\\d+))?)\\})?(\\$reg)?");

	private final class EncapsulatedAccess {
		public final int shift;
		public final long mask;
		public final long writeMask;
		public final String name;
		public final int accessIndex;
		public final boolean prev;
		public final int bitStart;
		public final int bitEnd;

		public EncapsulatedAccess(String name, int accessIndex, boolean prev) {
			super();
			this.accessIndex = accessIndex;
			this.prev = prev;
			Matcher matcher = aiFormatName.matcher(name);
			if (matcher.matches()) {
				this.name = matcher.group(1);
				if (matcher.group(2) == null) {
					int width = model.getWidth(name);
					bitStart = 0;
					bitEnd = 64;
					this.shift = 0;
					if (width == 64) {
						this.mask = 0xFFFFFFFFFFFFFFFFL;
					} else {
						this.mask = (1l << width) - 1;
					}
					this.writeMask = 0;
				} else if (matcher.group(3) != null) {
					bitStart = Integer.parseInt(matcher.group(2));
					bitEnd = Integer.parseInt(matcher.group(3));
					int actualWidth = (bitStart - bitEnd) + 1;
					this.shift = bitEnd;
					this.mask = (1l << actualWidth) - 1;
					this.writeMask = ~(mask << shift);
				} else {
					this.shift = Integer.parseInt(matcher.group(2));
					bitStart = shift;
					bitEnd = shift;
					this.mask = 1;
					this.writeMask = ~(mask << shift);
				}
			} else
				throw new IllegalArgumentException("Name:" + name + " is not valid!");
			// System.out.println("HDLFrameInterpreter.EncapsulatedAccess.EncapsulatedAccess()"
			// + this);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("EncapsulatedAccess [shift=").append(shift).append(", mask=").append(mask).append(", writeMask=").append(Long.toHexString(writeMask)).append(", name=")
					.append(name).append(", accessIndex=").append(accessIndex).append("]");
			return builder.toString();
		}

		public void setData(long data) {
			long initial;
			initial = storage[accessIndex];
			long current = initial & writeMask;
			storage[accessIndex] = current | ((data & mask) << shift);
			// System.out.println("setData()" + name + "{" + bitStart + ":" +
			// bitEnd + "} " + storage[accessIndex] + " data:" + data);
		}

		public long getData() {
			if (prev)
				return (storage_prev[accessIndex] >> shift) & mask;
			return (storage[accessIndex] >> shift) & mask;
		}
	}

	private final EncapsulatedAccess[] internals, internals_prev;
	private final int[] regIndex, regIndexTarget;
	private final Map<String, Integer> idx = new TreeMap<String, Integer>();
	private int deltaCycle = 0;

	public HDLFrameInterpreter(ExecutableModel model) {
		this.model = model;
		int currentIdx = 0;
		this.internals = new EncapsulatedAccess[model.internals.length];
		this.internals_prev = new EncapsulatedAccess[model.internals.length];
		for (int i = 0; i < model.internals.length; i++) {
			String in = model.internals[i];
			String basicName = getBasicName(in);
			Integer accessIndex = idx.get(basicName);
			if (accessIndex == null) {
				accessIndex = currentIdx++;
				idx.put(basicName, accessIndex);
			}
			internals[i] = new EncapsulatedAccess(in, accessIndex, false);
			internals_prev[i] = new EncapsulatedAccess(in, accessIndex, true);
		}
		regIndex = new int[model.registerOutputs.length];
		regIndexTarget = new int[model.registerOutputs.length];
		for (int i = 0; i < model.registerOutputs.length; i++) {
			int ridx = model.registerOutputs[i];
			String name = model.internals[ridx];
			Integer idxSrc = idx.get(name);
			regIndex[i] = idxSrc;
			regIndexTarget[i] = idx.get(ExecutableModel.stripReg(name));
		}
		storage = new long[currentIdx];
		storage_prev = new long[currentIdx];
	}

	private String getBasicName(String name) {
		int openBrace = name.indexOf('{');
		if (openBrace != -1) {
			name = name.substring(0, openBrace);
		}
		return name;
	}

	public void setInput(String name, long value) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find an input named:" + name);
		storage[integer] = value;
	}

	public long getOutput(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find an input named:" + name);
		return storage[integer];
	}

	private static final Instruction[] values = Instruction.values();

	/*
	 * SignedCastTest.main() cast (int<8>) -2 to (uint<16>) : FFFE
	 * SignedCastTest.main() cast (int<8>) -2 to (int<16>) : FFFE
	 * SignedCastTest.main() cast (int<16>) -255 to (int<8>) : 01
	 * SignedCastTest.main() cast (int<16>) -255 to (int<32>) : FFFFFF01
	 * SignedCastTest.main() cast (uint<16>) 65535 to (int<32>) : 0000FFFF
	 */

	public void run() {
		boolean regUpdated = false;
		deltaCycle++;
		do {
			regUpdated = false;
			long stack[] = new long[model.maxStackDepth];
			nextFrame: for (Frame f : model.frames) {
				int stackPos = -1;
				int execPos = 0;
				byte[] inst = f.instructions;
				do {
					Instruction instruction = values[inst[execPos] & 0xff];
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
						int bit = inst[++execPos] & 0xff;
						long current = stack[stackPos];
						current >>= bit;
						current &= 1;
						stack[stackPos] = current;
						break;
					}
					case bitAccessSingleRange: {
						int lowBit = inst[++execPos] & 0xff;
						int highBit = inst[++execPos] & 0xff;
						long current = stack[stackPos];
						current >>= lowBit;
						current &= (1 << ((highBit - lowBit) + 1)) - 1;
						stack[stackPos] = current;
						break;
					}
					case callFrame:
						// Not implemented, not sure if I should keep it...
						break;
					case cast_int:
						// Corner cases:
						// value is 0xF (-1 int<4>)
						// cast to int<8> result should be 0xFF
						// value is 0xA (-6 int<4>)
						// cast to int<3> result should be 0xE (-2)
						// Resize sign correctly to correct size
						int targetSize = inst[++execPos] & 0xff;
						int currentSize = inst[++execPos] & 0xff;
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
						long mask = (1 << (inst[++execPos] & 0xff)) - 1;
						++execPos; // We don't need current size
						stack[stackPos] &= mask;
						break;
					case concat:
						// Implement somewhen...
						break;
					case const0:
						stack[++stackPos] = 0;
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
						stack[++stackPos] = f.constants[inst[++execPos] & 0xff].longValue();
						break;
					case loadInternal2:
						int internal = (inst[++execPos] & 0xff) << 8;
						internal |= (inst[++execPos] & 0xff) << 0;
						stack[++stackPos] = internals[internal].getData();
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
					case logic_neg: {
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
					case isFallingEdgeInternal2: {
						int off = (inst[++execPos] & 0xFF) << 8;
						off |= inst[++execPos] & 0xFF;
						long curr = internals[off].getData() & 1;
						long prev = internals_prev[off].getData() & 1;
						if (f.lastUpdate == deltaCycle) {
							continue nextFrame;
						} else if ((prev == 1) && (curr == 0)) {
							f.lastUpdate = deltaCycle;
							regUpdated = true;
						} else {
							continue nextFrame;
						}
						break;
					}
					case isRisingEdgeInternal2: {
						int off = (inst[++execPos] & 0xFF) << 8;
						off |= inst[++execPos] & 0xFF;
						long curr = internals[off].getData() & 1;
						long prev = internals_prev[off].getData() & 1;
						if (f.lastUpdate == deltaCycle) {
							continue nextFrame;
						} else if ((prev == 0) && (curr == 1)) {
							f.lastUpdate = deltaCycle;
							regUpdated = true;
						} else {
							continue nextFrame;
						}
						break;
					}
					}
					execPos++;
				} while (execPos < inst.length);
				internals[f.outputId & 0xff].setData(stack[0]);
			}
			if (regUpdated) {
				for (int i = 0; i < regIndex.length; i++) {
					long oldValue = storage[regIndex[i]];
					storage[regIndexTarget[i]] = oldValue;
				}
			}
		} while (regUpdated);
		System.arraycopy(storage, 0, storage_prev, 0, storage.length);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Current Cycle:" + deltaCycle + "\n");
		for (Entry<String, Integer> e : idx.entrySet()) {
			sb.append('\t').append(e.getKey()).append("=").append(storage[e.getValue()]).append('\n');
		}
		return sb.toString();
	}
}
