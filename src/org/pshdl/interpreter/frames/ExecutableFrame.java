package org.pshdl.interpreter.frames;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.utils.FluidFrame.Instruction;

public abstract class ExecutableFrame {
	protected static final Instruction[] values = Instruction.values();
	protected int currentPos = 0;
	private byte[] instr;
	protected final EncapsulatedAccess internals[], internals_prev[];

	protected int outputID;

	protected final boolean printing;
	public boolean regUpdated;
	public final int uniqueID;

	public ExecutableFrame(Frame f, boolean printing, EncapsulatedAccess[] internals, EncapsulatedAccess[] internals_prev) {
		this.instr = f.instructions;
		this.printing = printing;
		this.internals = internals;
		this.internals_prev = internals_prev;
		this.outputID = f.outputId;
		this.uniqueID = f.uniqueID;
	}

	public boolean hasMore() {
		return currentPos < instr.length;
	}

	public int next() {
		return instr[currentPos++] & 0xff;
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

	public abstract void execute(int deltaCycle, int epsCycle);
}
