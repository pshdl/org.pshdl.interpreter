package org.pshdl.interpreter.utils;

import java.math.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.*;

import org.pshdl.interpreter.*;

public class FluidFrame {
	public static class ArgumentedInstruction {
		public final String args[];
		public final Instruction instruction;

		public ArgumentedInstruction(Instruction instruction, String... args) {
			super();
			this.instruction = instruction;
			if (args != null) {
				for (String string : args) {
					if (string == null)
						throw new IllegalArgumentException("Null is not a valid argument");
				}
			}
			this.args = args;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(instruction);
			if (args.length != 0) {
				builder.append(Arrays.toString(args));
			}
			return builder.toString();
		}
	}

	private static class FrameRegister {
		public final Set<String> inputs = new RandomIterHashSet<String>();
		private int internalIdCounter = 0;
		public final Map<String, Integer> internalIds = new RandomHashMap<String, Integer>();

		public Integer getInternal(String in) {
			return internalIds.get(in);
		}

		public Integer registerInternal(String in) {
			if (internalIds.get(in) == null) {
				internalIds.put(in, internalIdCounter++);
			}
			return internalIds.get(in);
		}

		public Integer registerInput(String string) {
			inputs.add(string);
			return registerInternal(string);
		}

	}

	public static enum Instruction {
		noop(true, 0, "Does nothing"), //
		and(false, 0, "A binary & operation"), //
		arith_neg(false, 0, "Arithmetically negates"), //
		bit_neg(false, 0, "Bit inverts"), //
		bitAccessSingle(false, 1, "Access a single bit"), //
		bitAccessSingleRange(false, 2, "Access a bit range"), //
		cast_int(false, 2, "Re-interprets the operand as int and resizes it"), //
		cast_uint(false, 2, "Re-interprets the operand as uint and resizes it"), //
		concat(false, 0, "Concatenate bits"), //
		const0(true, 0, "Loads a zero to the stack"), //
		div(false, 0, "An arithmetic / operation"), //
		eq(false, 0, "An equality == operation"), //
		greater(false, 0, "An equality > operation"), //
		greater_eq(false, 0, "An equality >= operation"), //
		isFallingEdgeInternal(false, 1, "Checks for an rising edge on from an internal signal"), //
		isRisingEdgeInternal(false, 1, "Checks for an rising edge on from an internal signal"), //
		less(false, 0, "An equality < operation"), //
		less_eq(false, 0, "An equality <= operation"), //
		loadConstant(false, 1, "Loads a value from the constant storage"), //
		loadInternal(false, -1, "Loads a value from an internal. The first value is the input name, then followed by bit access indexes"), //
		logiAnd(false, 0, "A logical && operation"), //
		logic_neg(false, 0, "Logically negates"), //
		logiOr(false, 0, "A logical || operation"), //
		minus(false, 0, "An arithmetic - operation"), //
		mul(false, 0, "An arithmetic * operation"), //
		not_eq(false, 0, "An equality != operation"), //
		or(false, 0, "A binary | operation"), //
		plus(false, 0, "An arithmetic + operation"), //
		posPredicate(false, 1, "Checks if the given predicate has evaluated to true"), //
		negPredicate(false, 1, "Checks if the given predicate has evaluated to false"), //
		sll(false, 0, "A shift << operation"), //
		sra(false, 0, "A shift >> operation"), //
		srl(false, 0, "A shift >>> operation"), //
		xor(false, 0, "A binary ^ operation"), //

		;
		final int argCount;
		final String description;
		final boolean immediate;

		Instruction(boolean immediate, int argCount, String desc) {
			this.argCount = argCount;
			this.immediate = immediate;
			this.description = desc;
		}
	}

	/**
	 * A global counter for frame ids
	 */
	private static AtomicInteger gid = new AtomicInteger();

	/**
	 * The local id of this frame
	 */
	public final int id;

	/**
	 * All constants by name
	 */
	public final Map<String, BigInteger> constants = new RandomHashMap<String, BigInteger>();

	public final LinkedList<ArgumentedInstruction> instructions = new LinkedList<ArgumentedInstruction>();
	public String outputName;

	public final Set<FluidFrame> references = new LinkedHashSet<FluidFrame>();

	public final Map<String, Integer> widths = new RandomHashMap<String, Integer>();

	public FluidFrame() {
		this(null);
	}

	public FluidFrame(String outputName) {
		this.id = gid.incrementAndGet();
		if (outputName != null) {
			this.outputName = outputName;
		} else {
			this.outputName = Integer.toString(id);
		}
	}

	public void add(ArgumentedInstruction argumentedInstruction) {
		instructions.add(argumentedInstruction);
	}

	public void add(Instruction inst) {
		instructions.add(new ArgumentedInstruction(inst));
	}

	public void addReferencedFrame(FluidFrame frame) {
		widths.putAll(frame.widths);
		if (frame.hasInstructions()) {
			references.add(frame);
		}
	}

	public void addWith(String var, Integer width) {
		widths.put(var, width);
	}

	public FluidFrame append(FluidFrame frame) {
		// Don't copy references
		widths.putAll(frame.widths);
		constants.putAll(frame.constants);
		instructions.addAll(frame.instructions);
		return this;
	}

	public ExecutableModel getExecutable() {
		FrameRegister register = new FrameRegister();
		for (FluidFrame entry : references) {
			entry.registerFrame(register);
		}
		List<Frame> res = toFrame(register);
		int maxStack = -1;
		Map<String, Integer> lastID = new HashMap<>();
		for (Frame frame : res) {
			String name = frame.id;
			int brace = name.indexOf('{');
			if (brace != -1) {
				name = name.substring(0, brace);
			}
			name = ExecutableModel.stripReg(name);
			Integer lID = lastID.get(name);
			if (lID != null) {
				frame.executionDep = lID;
			}
			lastID.put(name, frame.uniqueID);
			maxStack = Math.max(maxStack, frame.maxStackDepth);
		}
		String[] internals = new String[register.internalIds.size()];
		for (Entry<String, Integer> e : register.internalIds.entrySet()) {
			internals[e.getValue()] = e.getKey();
		}
		return new ExecutableModel(res.toArray(new Frame[res.size()]), internals, widths, maxStack);
	}

	private void registerFrame(FrameRegister register) {
		if (outputName.endsWith("$reg")) {
			register.registerInternal(ExecutableModel.stripReg(outputName));
		}
		register.registerInternal(outputName);
		for (FluidFrame entry : references) {
			entry.registerFrame(register);
		}
	}

	private byte[] toByteArray(Collection<Byte> instr) {
		byte[] instrRes = new byte[instr.size()];
		int pos = 0;
		for (Byte i : instr) {
			instrRes[pos++] = i;
		}
		return instrRes;
	}

	private int[] toIntArray(Collection<Integer> instr) {
		int[] instrRes = new int[instr.size()];
		int pos = 0;
		for (int i : instr) {
			instrRes[pos++] = i;
		}
		return instrRes;
	}

	private List<Frame> toFrame(FrameRegister register) {
		List<Byte> instr = new LinkedList<>();
		Set<Integer> internalDependencies = new LinkedHashSet<>();
		Set<Integer> predicateDependencies = new LinkedHashSet<>();
		Set<Integer> edgeDependencies = new LinkedHashSet<>();
		List<BigInteger> constants = new LinkedList<>();
		int stackCount = 0;
		int maxStackCount = -1;
		int constantIdCount = 0;
		for (ArgumentedInstruction ai : instructions) {
			int ordinal = ai.instruction.ordinal();
			instr.add((byte) (ordinal & 0xff));
			switch (ai.instruction) {
			case negPredicate:
			case posPredicate: {
				Integer internalId = register.registerInternal("$Pred_" + toFullRef(ai));
				if (internalId == null)
					throw new IllegalArgumentException(ai.toString());
				internalDependencies.add(internalId);
				predicateDependencies.add(internalId);
				writeVarInt32(instr, internalId);
				break;
			}
			case isFallingEdgeInternal:
			case isRisingEdgeInternal: {
				Integer internalId = register.registerInternal(toFullRef(ai));
				if (internalId == null)
					throw new IllegalArgumentException(ai.toString());
				internalDependencies.add(internalId);
				edgeDependencies.add(internalId);
				writeVarInt32(instr, internalId);
				break;
			}
			case loadInternal:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				Integer internalId = register.getInternal(toFullRef(ai));
				if (internalId != null) {
					internalDependencies.add(internalId);
					writeVarInt32(instr, internalId);
				} else {
					internalId = register.getInternal(ai.args[0]);
					if (internalId == null) {
						internalId = register.registerInput(ai.args[0]);
						System.out.println("FluidFrame.toFrame() Registering supected input:" + ai);
					}
					internalDependencies.add(internalId);
					writeVarInt32(instr, internalId);
					if (ai.args.length > 1) {
						if (ai.args[1].indexOf(':') != -1) {
							String[] split = ai.args[1].split(":");
							instr.add((byte) (Instruction.bitAccessSingleRange.ordinal() & 0xFF));
							instr.add(Byte.parseByte(split[0]));
							instr.add(Byte.parseByte(split[1]));
						} else {
							instr.add((byte) (Instruction.bitAccessSingle.ordinal() & 0xFF));
							instr.add(Byte.parseByte(ai.args[1]));
						}
					}
				}
				break;
			case const0:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				break;
			case loadConstant:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				constants.add(this.constants.get(ai.args[0]));
				instr.add((byte) constantIdCount++);
				break;
			case cast_uint:
			case cast_int:
				instr.add(Byte.parseByte(ai.args[0]));
				instr.add(Byte.parseByte(ai.args[1]));
				break;
			default:
				stackCount--;
			}
		}
		List<Frame> res = new LinkedList<>();
		if (hasInstructions()) {
			Integer outputId;
			boolean isReg = outputName.endsWith("$reg");
			outputId = register.registerInternal(outputName);
			byte[] instrRes = toByteArray(instr);
			int[] internalDepRes = toIntArray(internalDependencies);
			int[] edgeDepRes = toIntArray(edgeDependencies);
			int[] predicateDepRes = toIntArray(predicateDependencies);
			// XXX determine maxBitWidth
			Frame frame = new Frame(instrRes, internalDepRes, predicateDepRes, edgeDepRes, outputId & 0xFFFF, 32, maxStackCount,
					constants.toArray(new BigInteger[constants.size()]), outputName, isReg, id);
			for (FluidFrame ff : references) {
				ff.toFrame(register);
			}
			res.add(frame);
		}
		for (FluidFrame sub : references) {
			res.addAll(sub.toFrame(register));
		}
		return res;
	}

	public static void writeVarInt32(List<Byte> instr, Integer internalId) {
		int num = internalId;
		int t = 0;
		while (num > 127) {
			t = 0x80 | (num & 0x7F);
			instr.add((byte) t);
			num >>= 7;
		}
		t = num & 0x7F;
		instr.add((byte) t);
	}

	/**
	 * Read a raw Varint from the stream. If larger than 32 bits, discard the
	 * upper bits.
	 */
	public static int readRawVarint32(List<Byte> inst) {
		Iterator<Byte> iterator = inst.iterator();
		byte tmp = iterator.next();
		if (tmp >= 0)
			return tmp;
		int result = tmp & 0x7f;
		if ((tmp = iterator.next()) >= 0) {
			result |= tmp << 7;
		} else {
			result |= (tmp & 0x7f) << 7;
			if ((tmp = iterator.next()) >= 0) {
				result |= tmp << 14;
			} else {
				result |= (tmp & 0x7f) << 14;
				if ((tmp = iterator.next()) >= 0) {
					result |= tmp << 21;
				} else {
					result |= (tmp & 0x7f) << 21;
					result |= (tmp = iterator.next()) << 28;
					if (tmp < 0)
						throw new IllegalArgumentException("Too many bits");
				}
			}
		}
		return result;
	}

	private String toFullRef(ArgumentedInstruction ai) {
		StringBuilder sb = new StringBuilder();
		sb.append(ai.args[0]);
		for (int i = 1; i < ai.args.length; i++) {
			sb.append('{').append(ai.args[i]).append('}');
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nFrame: ").append(outputName).append('[').append(id).append("]\n");
		if (constants.size() != 0) {
			sb.append("Constants:\n");
			for (Entry<String, BigInteger> entry : constants.entrySet()) {
				sb.append("\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
			}
		}

		for (ArgumentedInstruction ai : instructions) {
			sb.append(ai).append('\n');
		}
		for (FluidFrame refs : references) {
			sb.append(refs);
		}
		return sb.toString();
	}

	public void setName(String string) {
		this.outputName = string;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FluidFrame other = (FluidFrame) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public boolean hasInstructions() {
		return !instructions.isEmpty();
	}
}
