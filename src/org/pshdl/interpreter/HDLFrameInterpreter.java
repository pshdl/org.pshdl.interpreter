package org.pshdl.interpreter;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.pshdl.interpreter.utils.*;
import org.pshdl.interpreter.utils.FluidFrame.Instruction;

public final class HDLFrameInterpreter {
	protected final ExecutableModel model;

	protected final long storage[], storage_prev[];

	public static final Pattern aiFormatName = Pattern.compile("(.*?)(?:\\{(?:(\\d+)(?:\\:(\\d+))?)\\})?(\\" + FluidFrame.REG_POSTFIX + ")?");

	private class PrintingAccess extends EncapsulatedAccess {

		public PrintingAccess(String name, int accessIndex, boolean prev) {
			super(name, accessIndex, prev);
		}

		@Override
		public void setData(long data, int deltaCycle, int epsCycle) {
			super.setData(data, deltaCycle, epsCycle);
			System.out.println("\t\tsetData()" + name + "{" + bitStart + ":" + bitEnd + "} " + storage[accessIndex] + " data:" + data);
		}

	}

	private class EncapsulatedAccess {
		public final int shift;
		public final long mask;
		public final long writeMask;
		public final String name;
		public final int accessIndex;
		public final boolean prev;
		public final int bitStart;
		public final int bitEnd;
		private boolean isPredicate;

		public EncapsulatedAccess(String name, int accessIndex, boolean prev) {
			super();
			this.accessIndex = accessIndex;
			this.prev = prev;
			Matcher matcher = aiFormatName.matcher(name);
			this.isPredicate = name.startsWith(FluidFrame.PRED_PREFIX);
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

		public void setData(long data, int deltaCycle, int epsCycle) {
			long initial;
			initial = storage[accessIndex];
			long current = initial & writeMask;
			storage[accessIndex] = current | ((data & mask) << shift);
			if (isPredicate) {
				deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
			}
		}

		public long getData() {
			if (prev)
				return (storage_prev[accessIndex] >> shift) & mask;
			return (storage[accessIndex] >> shift) & mask;
		}

		public void setLastUpdate(int deltaCycle, int epsCycle) {
			deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
		}

		public boolean skip(int deltaCycle, int epsCycle) {
			long local = deltaUpdates[accessIndex];
			long dc = local >>> 16;
			// Register was updated in previous delta cylce, that is ok
			if (dc < deltaCycle)
				return false;
			// Register was updated in this delta cycle but it is the same eps,
			// that is ok as well
			if ((dc == deltaCycle) && ((local & 0xFFFF) == epsCycle))
				return false;
			// Don't update
			return true;
		}

		public boolean isFresh(int deltaCycle) {
			return (deltaUpdates[accessIndex] >>> 16) == deltaCycle;
		}
	}

	private final EncapsulatedAccess[] internals, internals_prev;
	private final int[] regIndex, regIndexTarget;
	private final Map<String, Integer> idx = new TreeMap<String, Integer>();
	private int deltaCycle = 0;

	private boolean printing;

	private long[] deltaUpdates;

	public HDLFrameInterpreter(ExecutableModel model, boolean printing) {
		this.printing = printing;
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
			if (printing) {
				internals[i] = new PrintingAccess(in, accessIndex, false);
				internals_prev[i] = new PrintingAccess(in, accessIndex, true);
			} else {
				internals[i] = new EncapsulatedAccess(in, accessIndex, false);
				internals_prev[i] = new EncapsulatedAccess(in, accessIndex, true);
			}
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
		deltaUpdates = new long[currentIdx];
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
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		storage[integer] = value;
	}

	public long getOutput(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		return storage[integer];
	}

	private static final Instruction[] values = Instruction.values();

	private static final class InstructionCursor {
		public InstructionCursor(byte[] instr) {
			super();
			this.instr = instr;
		}

		private final byte[] instr;
		private int currentPos;

		public int next() {
			return instr[currentPos++] & 0xff;
		}

		public boolean hasMore() {
			return currentPos < instr.length;
		}

		public int readVarInt() {
			int tmp = 0;
			if (((tmp = next()) & 0x80) == 0)
				return tmp;
			int result = tmp & 0x7f;
			if (((tmp = next()) & 0x80) == 0) {
				result |= tmp << 7;
			} else {
				result |= (tmp & 0x7f) << 7;
				if (((tmp = next()) & 0x80) == 0) {
					result |= tmp << 14;
				} else {
					result |= (tmp & 0x7f) << 14;
					if (((tmp = next()) & 0x80) == 0) {
						result |= tmp << 21;
					} else {
						result |= (tmp & 0x7f) << 21;
						result |= (tmp = next()) << 28;
						if (tmp < 0)
							throw new IllegalArgumentException("Too many bits");
					}
				}
			}
			return result;
		}
	}

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
		int epsCycle = 0;
		do {
			epsCycle++;
			if (printing) {
				System.out.println("Starting cylce:" + deltaCycle + "." + epsCycle);
			}
			regUpdated = false;
			long stack[] = new long[model.maxStackDepth];
			nextFrame: for (Frame f : model.frames) {
				if (printing) {
					System.out.println("\tExecuting frame:" + f.uniqueID);
				}
				int stackPos = -1;
				InstructionCursor inst = new InstructionCursor(f.instructions);
				do {
					Instruction instruction = values[inst.next()];
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
						int bit = inst.readVarInt();
						long current = stack[stackPos];
						current >>= bit;
						current &= 1;
						stack[stackPos] = current;
						break;
					}
					case bitAccessSingleRange: {
						int lowBit = inst.readVarInt();
						int highBit = inst.readVarInt();
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
						int targetSize = inst.readVarInt();
						int currentSize = inst.readVarInt();
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
						long mask = (1 << (inst.readVarInt())) - 1;
						inst.readVarInt();
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
						int width = inst.readVarInt();
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
						stack[++stackPos] = f.constants[inst.readVarInt()].longValue();
						break;
					case loadInternal:
						stack[++stackPos] = internals[inst.readVarInt()].getData();
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
						int off = inst.readVarInt();
						EncapsulatedAccess access = internals[off];
						if (access.skip(deltaCycle, epsCycle)) {
							if (printing) {
								System.out.println("\t\tSkipped: falling edge already handled");
							}
							continue nextFrame;
						}
						long curr = internals[off].getData();
						long prev = internals_prev[off].getData();
						if ((prev != 1) || (curr != 0)) {
							if (printing) {
								System.out.println("\t\tSkipped: not a falling edge");
							}
							continue nextFrame;
						}
						access.setLastUpdate(deltaCycle, epsCycle);
						regUpdated = true;
						break;
					}
					case isRisingEdge: {
						int off = inst.readVarInt();
						EncapsulatedAccess access = internals[off];
						if (access.skip(deltaCycle, epsCycle)) {
							if (printing) {
								System.out.println("\t\tSkipped: rising edge already handled");
							}
							continue nextFrame;
						}
						long curr = internals[off].getData();
						long prev = internals_prev[off].getData();
						if ((prev != 0) || (curr != 1)) {
							if (printing) {
								System.out.println("\t\tSkipped: Not a rising edge");
							}
							continue nextFrame;
						}
						access.setLastUpdate(deltaCycle, epsCycle);
						regUpdated = true;
						break;
					}
					case posPredicate: {
						int off = inst.readVarInt();
						EncapsulatedAccess access = internals[off];
						// If data is not from this deltaCycle it was not
						// updated that means prior predicates failed
						if (!access.isFresh(deltaCycle)) {
							if (printing) {
								System.out.println("\t\tSkipped: predicate not fresh enough");
							}
							continue nextFrame;
						}
						if (access.getData() == 0) {
							if (printing) {
								System.out.println("\t\tSkipped: predicate not positive");
							}
							continue nextFrame;
						}
						break;
					}
					case negPredicate: {
						int off = inst.readVarInt();
						EncapsulatedAccess access = internals[off];
						// If data is not from this deltaCycle it was not
						// updated that means prior predicates failed
						if (!access.isFresh(deltaCycle)) {
							if (printing) {
								System.out.println("\t\tSkipped: predicate not fresh enough");
							}
							continue nextFrame;
						}
						if (access.getData() != 0) {
							if (printing) {
								System.out.println("\t\tSkipped: predicate not negative");
							}
							continue nextFrame;
						}
						break;
					}
					}
				} while (inst.hasMore());
				internals[f.outputId & 0xff].setData(stack[0], deltaCycle, epsCycle);
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
