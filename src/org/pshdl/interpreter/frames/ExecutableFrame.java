package org.pshdl.interpreter.frames;

import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.utils.FluidFrame.Instruction;

public abstract class ExecutableFrame {

	protected static class FastInstruction {
		public final Instruction inst;
		public final int arg1, arg2;

		public FastInstruction(Instruction inst, int arg1, int arg2) {
			super();
			this.inst = inst;
			this.arg1 = arg1;
			this.arg2 = arg2;
		}
	}

	protected final FastInstruction[] instructions;

	protected int currentPos = 0;
	protected final EncapsulatedAccess internals[], internals_prev[];

	protected final int outputID;

	protected final boolean printing;
	public boolean regUpdated;
	public final int uniqueID;
	private final byte[] instr;

	public ExecutableFrame(Frame f, boolean printing, EncapsulatedAccess[] internals, EncapsulatedAccess[] internals_prev) {
		this.printing = printing;
		this.internals = internals;
		this.internals_prev = internals_prev;
		this.outputID = f.outputId;
		this.uniqueID = f.uniqueID;
		List<FastInstruction> instr = new LinkedList<>();
		this.instr = f.instructions;
		Instruction[] values = Instruction.values();
		do {
			Instruction instruction = values[next()];
			int arg1 = 0;
			int arg2 = 0;
			if (instruction.argCount > 0) {
				arg1 = readVarInt();
			}
			if (instruction.argCount > 1) {
				arg2 = readVarInt();
			}
			instr.add(new FastInstruction(instruction, arg1, arg2));
		} while (hasMore());
		instructions = instr.toArray(new FastInstruction[instr.size()]);
	}

	private boolean hasMore() {
		return currentPos < instr.length;
	}

	private int next() {
		return instr[currentPos++] & 0xff;
	}

	private int readVarInt() {
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

	public abstract void execute(int deltaCycle, int epsCycle);

	public static void main(String[] args) {
		for (Instruction i : Instruction.values()) {
			System.out.println("protected static final int " + i.name() + "=" + i.ordinal() + ";");
		}
	}
}
