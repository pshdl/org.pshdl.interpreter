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
		private int frameIdCounter = 0;
		public final Set<String> inputs = new RandomIterHashSet<String>();
		public final Map<String, Byte> frameIds = new RandomHashMap<String, Byte>();
		public final Set<Frame> frames = new RandomIterHashSet<Frame>();
		private int internalIdCounter = 0;
		public final Map<String, Integer> internalIds = new RandomHashMap<String, Integer>();

		public void addFrame(Frame frame) {
			frames.add(frame);
		}

		public Byte getFrame(String string) {
			return frameIds.get(string);
		}

		public Byte registerFrame(String in) {
			if (frameIds.get(in) == null) {
				frameIds.put(in, (byte) frameIdCounter++);
			}
			return frameIds.get(in);
		}

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
		callFrame(false, 1, "Calls a frame"), //
		cast_int(false, 2, "Re-interprets the operand as int and resizes it"), //
		cast_uint(false, 2, "Re-interprets the operand as uint and resizes it"), //
		concat(false, 0, "Concatenate bits"), //
		const0(true, 0, "Loads a zero to the stack"), //
		div(false, 0, "An arithmetic / operation"), //
		eq(false, 0, "An equality == operation"), //
		greater(false, 0, "An equality > operation"), //
		greater_eq(false, 0, "An equality >= operation"), //
		isFallingEdgeInternal1(false, 1, "Checks for an rising edge on from an internal signal"), //
		isRisingEdgeInternal1(false, 1, "Checks for an rising edge on from an internal signal"), //
		isFallingEdgeInternal2(false, 1, "Checks for an rising edge on from an internal signal"), //
		isRisingEdgeInternal2(false, 1, "Checks for an rising edge on from an internal signal"), //
		isFallingEdgeInternal3(false, 1, "Checks for an rising edge on from an internal signal"), //
		isRisingEdgeInternal3(false, 1, "Checks for an rising edge on from an internal signal"), //
		isFallingEdgeInternal4(false, 1, "Checks for an rising edge on from an internal signal"), //
		isRisingEdgeInternal4(false, 1, "Checks for an rising edge on from an internal signal"), //
		less(false, 0, "An equality < operation"), //
		less_eq(false, 0, "An equality <= operation"), //
		loadConstant(false, 1, "Loads a value from the internal storage"), //
		loadInternal1(false, -1, "Loads a value from an internal. The first value is the input name, then followed by bit access indexes"), //
		loadInternal2(false, -1, "Loads a value from an internal. The first value is the input name, then followed by bit access indexes"), //
		loadInternal3(false, -1, "Loads a value from an internal. The first value is the input name, then followed by bit access indexes"), //
		loadInternal4(false, -1, "Loads a value from an internal. The first value is the input name, then followed by bit access indexes"), //
		logiAnd(false, 0, "A logical && operation"), //
		logic_neg(false, 0, "Logically negates"), //
		logiOr(false, 0, "A logical || operation"), //
		minus(false, 0, "An arithmetic - operation"), //
		mul(false, 0, "An arithmetic * operation"), //
		not_eq(false, 0, "An equality != operation"), //
		or(false, 0, "A binary | operation"), //
		plus(false, 0, "An arithmetic + operation"), //
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

	public static class Predicate {
		public final int predcateID;
		public final boolean predicateCondition;

		public Predicate(boolean predicateCondition, int predcateID) {
			super();
			this.predicateCondition = predicateCondition;
			this.predcateID = predcateID;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Predicate other = (Predicate) obj;
			if (predcateID != other.predcateID)
				return false;
			if (predicateCondition != other.predicateCondition)
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + predcateID;
			result = (prime * result) + (predicateCondition ? 1231 : 1237);
			return result;
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
	public boolean isPredicate;
	public final String outputName;

	public Set<Predicate> predicates = new RandomIterHashSet<Predicate>();
	public final Map<String, FluidFrame> references = new RandomHashMap<String, FluidFrame>();

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

	public void addPredicate(int predicateID, boolean condition) {
		predicates.add(new Predicate(condition, predicateID));
	}

	public void addReferencedFrame(FluidFrame frame) {
		references.put(frame.outputName, frame);
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
		for (FluidFrame entry : references.values()) {
			entry.registerFrame(register);
		}
		List<Frame> res = new LinkedList<Frame>();
		int maxStack = -1;
		for (FluidFrame entry : references.values()) {
			Frame frame = entry.toFrame(register);
			maxStack = Math.max(maxStack, frame.maxStackDepth);
			res.add(frame);
		}
		String[] internals = new String[register.internalIds.size()];
		for (Entry<String, Integer> e : register.internalIds.entrySet()) {
			internals[e.getValue()] = e.getKey();
		}
		return new ExecutableModel(res.toArray(new Frame[res.size()]), internals, widths, maxStack);
	}

	private void registerFrame(FrameRegister register) {
		register.registerFrame(outputName);
		if (outputName.endsWith("$reg")) {
			register.registerInternal(ExecutableModel.stripReg(outputName));
		}
		register.registerInternal(outputName);
		for (FluidFrame entry : references.values()) {
			entry.registerFrame(register);
		}
	}

	public void setPredicate(boolean predicate) {
		this.isPredicate = predicate;
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

	private Frame toFrame(FrameRegister register) {
		List<Byte> instr = new LinkedList<Byte>();
		Set<Integer> internalDependencies = new LinkedHashSet<Integer>();
		List<BigInteger> constants = new LinkedList<BigInteger>();
		int stackCount = 0;
		int maxStackCount = -1;
		int constantIdCount = 0;
		for (ArgumentedInstruction ai : instructions) {
			int ordinal = ai.instruction.ordinal();
			instr.add((byte) (ordinal & 0xff));
			switch (ai.instruction) {
			case isFallingEdgeInternal2:
			case isRisingEdgeInternal2: {
				Integer internalId = register.registerInternal(toFullRef(ai));
				if (internalId == null)
					throw new IllegalArgumentException(ai.toString());
				internalDependencies.add(internalId);
				addID2(instr, internalId);
				break;
			}
			case loadInternal2:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				Integer internalId = register.getInternal(toFullRef(ai));
				if (internalId != null) {
					internalDependencies.add(internalId);
					addID2(instr, internalId);
				} else {
					internalId = register.getInternal(ai.args[0]);
					if (internalId == null) {
						internalId = register.registerInput(ai.args[0]);
						System.out.println("FluidFrame.toFrame() Registering supected input:" + ai);
					}
					internalDependencies.add(internalId);
					addID2(instr, internalId);
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
			case loadConstant:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				constants.add(this.constants.get(ai.args[0]));
				instr.add((byte) constantIdCount++);
				break;
			case callFrame:
				instr.add(register.getFrame(ai.args[0]));
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
		Integer outputId;
		boolean isReg = outputName.endsWith("$reg");
		outputId = register.registerInternal(outputName);
		byte[] instrRes = toByteArray(instr);
		int[] internalDepRes = toIntArray(internalDependencies);
		// XXX determine maxBitWidth
		Frame frame = new Frame(instrRes, internalDepRes, outputId & 0xFFFF, 32, maxStackCount, constants.toArray(new BigInteger[constants.size()]), outputName, isReg);
		register.addFrame(frame);
		for (FluidFrame ff : references.values()) {
			ff.toFrame(register);
		}
		return frame;
	}

	private void addID2(List<Byte> instr, Integer internalId) {
		instr.add((byte) (internalId >> 8));
		instr.add((byte) (internalId >> 0));
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
		for (FluidFrame refs : references.values()) {
			sb.append(refs);
		}
		return sb.toString();
	}
}
