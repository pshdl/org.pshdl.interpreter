package de.tuhh.ict.pshdl.interpreter.utils;

import java.math.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.*;

import de.tuhh.ict.pshdl.interpreter.*;

public class FluidFrame {
	public static class ArgumentedInstruction {
		public final String args[];
		public final Instruction instruction;

		public ArgumentedInstruction(Instruction instruction, String... args) {
			super();
			this.instruction = instruction;
			this.args = args;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(instruction);
			if (args.length != 0)
				builder.append(Arrays.toString(args));
			return builder.toString();
		}
	}

	private static class FrameRegister {
		private int frameIdCounter = 0;
		public final Map<String, Byte> frameIds = new LinkedHashMap<String, Byte>();
		public final List<Frame> frames = new ArrayList<Frame>();
		private int inIdCounter = 0;
		public final Map<String, Byte> inputIds = new LinkedHashMap<String, Byte>();
		private int internalIdCounter = 0;
		public final Map<String, Byte> internalIds = new LinkedHashMap<String, Byte>();
		private int outIdCounter = 0;
		public final Map<String, Byte> outputIds = new LinkedHashMap<String, Byte>();

		public void addFrame(Frame frame) {
			frames.add(frame);
		}

		public Byte getFrame(String string) {
			return frameIds.get(string);
		}

		public Byte getInternal(String in) {
			return internalIds.get(in);
		}

		public Byte registerFrame(String in) {
			if (frameIds.get(in) == null) {
				frameIds.put(in, (byte) frameIdCounter++);
			}
			return frameIds.get(in);
		}

		public Byte registerInput(String in) {
			if (inputIds.get(in) == null) {
				inputIds.put(in, (byte) inIdCounter++);
			}
			return inputIds.get(in);
		}

		public Byte registerInternal(String in) {
			if (internalIds.get(in) == null) {
				internalIds.put(in, (byte) internalIdCounter++);
			}
			return internalIds.get(in);
		}

		public Byte registerOutput(String in) {
			if (outputIds.get(in) == null) {
				outputIds.put(in, (byte) outIdCounter++);
			}
			return outputIds.get(in);
		}
	}

	public static enum Instruction {
		noop(true, 0, "Does nothing"), //
		and(false, 0, "A binary & operation"), //
		arith_neg(false, 0, "Arithmetically negates"), //
		bit_neg(false, 0, "Bit inverts"), //
		bitAccess(false, -1, "Use the bits given as operands"), //
		callFrame(false, 1, "Calls a frame"), //
		cast_int(false, 2, "Re-interprets the operand as int and resizes it"), //
		cast_uint(false, 2, "Re-interprets the operand as uint and resizes it"), //
		concat(false, 0, "Concatenate bits"), //
		const0(true, 0, "Loads a zero to the stack"), //
		div(false, 0, "An arithmetic / operation"), //
		eq(false, 0, "An equality == operation"), //
		greater(false, 0, "An equality > operation"), //
		greater_eq(false, 0, "An equality >= operation"), //
		isFallingEdgeInput(false, 1, "Checks for an rising edge on from an input signal"), //
		isFallingEdgeInternal(false, 1, "Checks for an rising edge on from an internal signal"), //
		isRisingEdgeInput(false, 1, "Checks for an rising edge on from an input signal"), //
		isRisingEdgeInternal(false, 1, "Checks for an rising edge on from an internal signal"), //
		less(false, 0, "An equality < operation"), //
		less_eq(false, 0, "An equality <= operation"), //
		loadConstant(false, 1, "Loads a value from the internal storage"), //
		loadInput(false, -1, "Loads a value from an input. The first value is the input name, then followed by bit access indexes"), //
		loadInternal(false, -1, "Loads a value from an internal. The first value is the input name, then followed by bit access indexes"), //
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
	public final Map<String, BigInteger> constants = new TreeMap<String, BigInteger>();

	public final Set<String> inputs = new LinkedHashSet<String>();
	public final LinkedList<ArgumentedInstruction> instructions = new LinkedList<ArgumentedInstruction>();
	private boolean isInternal;
	public boolean isPredicate;
	public final String outputName;

	public Set<Predicate> predicates = new HashSet<Predicate>();
	public final Map<String, FluidFrame> references = new TreeMap<String, FluidFrame>();

	public final Map<String, Integer> widths = new TreeMap<String, Integer>();

	public FluidFrame() {
		this(null);
	}

	public FluidFrame(String outputName) {
		this.id = gid.incrementAndGet();
		if (outputName != null)
			this.outputName = outputName;
		else
			this.outputName = Integer.toString(id);
	}

	public void add(ArgumentedInstruction argumentedInstruction) {
		instructions.add(argumentedInstruction);
	}

	public void add(Instruction inst) {
		instructions.add(new ArgumentedInstruction(inst));
	}

	public void addInput(String string) {
		inputs.add(string);
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
		inputs.addAll(frame.inputs);
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
		String[] inputs = register.inputIds.keySet().toArray(new String[0]);
		String[] outputs = register.outputIds.keySet().toArray(new String[0]);
		String[] internals = register.internalIds.keySet().toArray(new String[0]);
		return new ExecutableModel(res.toArray(new Frame[res.size()]), inputs, outputs, internals, widths, maxStack);
	}

	private void registerFrame(FrameRegister register) {
		register.registerFrame(outputName);
		if (isInternal) {
			if (outputName.endsWith("$reg")) {
				register.registerInternal(ExecutableModel.stripReg(outputName));
			}
			register.registerInternal(outputName);
		} else {
			register.registerOutput(outputName);
		}
		for (FluidFrame entry : references.values()) {
			entry.registerFrame(register);
		}
	}

	public void setInternal(boolean isInternal) {
		this.isInternal = isInternal;
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

	private Frame toFrame(FrameRegister register) {
		List<Byte> instr = new LinkedList<Byte>();
		Set<Byte> inputDependencies = new LinkedHashSet<Byte>();
		Set<Byte> internalDependencies = new LinkedHashSet<Byte>();
		List<BigInteger> constants = new LinkedList<BigInteger>();
		int stackCount = 0;
		int maxStackCount = -1;
		int constantIdCount = 0;
		for (ArgumentedInstruction ai : instructions) {
			int ordinal = ai.instruction.ordinal();
			instr.add((byte) (ordinal & 0xff));
			switch (ai.instruction) {
			case isFallingEdgeInput:
			case isRisingEdgeInput: {
				Byte inputId = register.registerInput(toFullRef(ai));
				if (inputId == null)
					throw new IllegalArgumentException(ai.toString());
				inputDependencies.add(inputId);
				instr.add(inputId);
				break;
			}
			case isFallingEdgeInternal:
			case isRisingEdgeInternal: {
				Byte internalId = register.registerInternal(toFullRef(ai));
				if (internalId == null)
					throw new IllegalArgumentException(ai.toString());
				internalDependencies.add(internalId);
				instr.add(internalId);
				break;
			}
			case loadInput:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				Byte inputId = register.registerInput(toFullRef(ai));
				if (inputId == null)
					throw new IllegalArgumentException(ai.toString());
				inputDependencies.add(inputId);
				instr.add(inputId);
				break;
			case loadInternal:
				stackCount++;
				maxStackCount = Math.max(maxStackCount, stackCount);
				Byte internalId = register.getInternal(toFullRef(ai));
				if (internalId != null) {
					internalDependencies.add(internalId);
					instr.add(internalId);
				} else {
					internalId = register.getInternal(ai.args[0]);
					if (internalId == null)
						throw new IllegalArgumentException(ai.toString());
					internalDependencies.add(internalId);
					instr.add(internalId);
					instr.add((byte) (Instruction.bitAccess.ordinal() & 0xFF));
					instr.add(Byte.parseByte(ai.args[1]));
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
		Byte outputId;
		boolean isReg = outputName.endsWith("$reg");
		if (isInternal || isReg)
			outputId = register.registerInternal(outputName);
		else
			outputId = register.registerOutput(outputName);
		byte[] instrRes = toByteArray(instr);
		byte[] inputDepRes = toByteArray(inputDependencies);
		byte[] internalDepRes = toByteArray(internalDependencies);
		// XXX determine maxBitWidth
		Frame frame = new Frame(instrRes, inputDepRes, internalDepRes, outputId & 0xFF, 32, maxStackCount, constants.toArray(new BigInteger[constants.size()]), outputName,
				isInternal, isReg);
		register.addFrame(frame);
		for (FluidFrame ff : references.values()) {
			ff.toFrame(register);
		}
		return frame;
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
		if (inputs.size() != 0) {
			sb.append("Inputs:\t");
			boolean first = true;
			for (String input : inputs) {
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(input);
			}
			sb.append('\n');
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
