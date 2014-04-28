/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *
 *     Copyright (C) 2013 Karsten Becker (feedback (at) pshdl (dot) org)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     This License does not grant permission to use the trade names, trademarks,
 *     service marks, or product names of the Licensor, except as required for
 *     reasonable and customary use in describing the origin of the Work.
 *
 * Contributors:
 *     Karsten Becker - initial API and implementation
 ******************************************************************************/
package org.pshdl.interpreter.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.pshdl.interpreter.ExecutableModel;
import org.pshdl.interpreter.Frame;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.InternalInformation;
import org.pshdl.interpreter.VariableInformation;
import org.pshdl.interpreter.VariableInformation.Direction;
import org.pshdl.interpreter.VariableInformation.Type;
import org.pshdl.interpreter.utils.IOUtil.FrameTypes;
import org.pshdl.interpreter.utils.IOUtil.IDType;
import org.pshdl.interpreter.utils.IOUtil.InternalTypes;
import org.pshdl.interpreter.utils.IOUtil.ModelTypes;
import org.pshdl.interpreter.utils.IOUtil.VariableTypes;

public class ExecutableInputStream extends DataInputStream {

	protected ExecutableInputStream(InputStream in) {
		super(in);
	}

	private class TLV {
		public final Enum<?> type;
		public final byte[] value;

		private TLV(Enum<?> type, byte[] value) {
			super();
			this.type = type;
			this.value = value;
		}

		public String asString() throws UnsupportedEncodingException {
			return new String(value, "UTF-8");
		}

	}

	public TLV readTLV(IDType<?> e) throws IOException {
		int len = 0;
		int type = -1;
		type = read();
		if (type == -1)
			return null;
		final Enum<?> ne = e.getFromID(type);
		len = readVarInt();
		final byte data[] = new byte[len];
		readFully(data);
		return new TLV(ne, data);
	}

	public ExecutableModel readExecutableModel(boolean verbose) throws IOException {
		TLV tlv = null;
		final byte[] header = new byte[4];
		readFully(header);
		if (!"PSEX".equals(new String(header)))
			throw new IllegalArgumentException("Not a PS Executable: Missing or wrong header!");
		final List<InternalInformation> internals = new LinkedList<>();
		final List<Frame> frameList = new LinkedList<>();
		final List<VariableInformation> vars = new LinkedList<>();
		String moduleName = null;
		String src = null;
		String[] annotations = null;
		while ((tlv = readTLV(ModelTypes.date)) != null) {
			final ModelTypes type = (ModelTypes) tlv.type;
			final ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			switch (type) {
			case date:
				if (verbose) {
					final long readDate = ex.readLong();
					System.out.printf("Created on: %tF %<tR\n", new Date(readDate));
				}
				break;
			case frame:
				frameList.add(ex.readFrame(verbose));
				break;
			case variable:
				vars.add(ex.readVariable());
				break;
			case internal:
				internals.add(ex.readInternal(vars));
				break;
			case maxDataWidth:
				if (verbose) {
					final int maxDataWidth = ex.readVarInt();
					System.out.println("Max data width:" + maxDataWidth);
				}
				break;
			case maxStackDepth:
				if (verbose) {
					final int maxStackDepth = ex.readVarInt();
					System.out.println("Max Stack depth:" + maxStackDepth);
				}
				break;
			case src:
				src = tlv.asString();
				if (verbose) {
					System.out.println("Generated from resource:" + src);
				}
				break;
			case version:
				if (verbose) {
					final byte[] version = new byte[3];
					ex.readFully(version);
					System.out.printf("Compiled with version: %d.%d.%d\n", version[0], version[1], version[2]);
				}
				break;
			case moduleName:
				moduleName = tlv.asString();
				break;
			case annotations:
				annotations = ex.readStringArray();
				break;
			default:
				ex.close();
				throw new IllegalArgumentException("The type:" + type + " is not handled");
			}
			ex.close();
		}
		final Frame[] frames = frameList.toArray(new Frame[frameList.size()]);
		final InternalInformation[] iis = internals.toArray(new InternalInformation[internals.size()]);
		final VariableInformation[] fvars = vars.toArray(new VariableInformation[vars.size()]);
		final ExecutableModel executableModel = new ExecutableModel(frames, iis, fvars, moduleName, src, annotations);
		return executableModel;
	}

	public VariableInformation readVariable() throws IOException {
		TLV tlv = null;
		Direction dir = Direction.INTERNAL;
		boolean isRegister = false;
		Type type = Type.BIT;
		String name = null;
		int width = -1;
		int dimensions[] = new int[0];
		boolean isClock = false, isReset = false;
		String[] annotations = null;
		while ((tlv = readTLV(VariableTypes.name)) != null) {
			final ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			final VariableTypes it = (VariableTypes) tlv.type;
			switch (it) {
			case dimensions:
				dimensions = ex.readIntArray();
				break;
			case flags:
				final int flags = ex.readVarInt();
				isRegister = (flags & IOUtil.REG_FLAG) == IOUtil.REG_FLAG;
				if ((flags & IOUtil.IO_FLAG) == IOUtil.IO_FLAG) {
					dir = Direction.INOUT;
				} else {
					if ((flags & IOUtil.IN_FLAG) == IOUtil.IN_FLAG) {
						dir = Direction.IN;
					}
					if ((flags & IOUtil.OUT_FLAG) == IOUtil.OUT_FLAG) {
						dir = Direction.OUT;
					}
				}
				if ((flags & IOUtil.INT_FLAG) == IOUtil.INT_FLAG) {
					type = Type.INT;
				}
				if ((flags & IOUtil.UINT_FLAG) == IOUtil.UINT_FLAG) {
					type = Type.UINT;
				}
				if ((flags & IOUtil.CLOCK_FLAG) == IOUtil.CLOCK_FLAG) {
					isClock = true;
				}
				if ((flags & IOUtil.RESET_FLAG) == IOUtil.RESET_FLAG) {
					isReset = true;
				}
				break;
			case name:
				name = tlv.asString();
				break;
			case width:
				width = ex.readVarInt();
				break;
			case annotations:
				annotations = ex.readStringArray();
				break;
			}
			ex.close();
		}
		final VariableInformation res = new VariableInformation(dir, name, width, type, isRegister, isClock, isReset, annotations, dimensions);
		return res;
	}

	public InternalInformation readInternal(List<VariableInformation> varInfos) throws IOException {
		TLV tlv = null;
		int bitStart = -1, bitEnd = -1;
		int flags = 0;
		int[] arrayIdx = new int[0];
		int varIdx = -1;
		while ((tlv = readTLV(InternalTypes.flags)) != null) {
			final ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			final InternalTypes it = (InternalTypes) tlv.type;
			switch (it) {
			case varIdx:
				varIdx = ex.readVarInt();
				break;
			case bitEnd:
				bitEnd = ex.readVarInt();
				break;
			case bitStart:
				bitStart = ex.readVarInt();
				break;
			case flags:
				flags = ex.readVarInt();
				break;
			case arrayIdx:
				arrayIdx = ex.readIntArray();
				break;
			}
			ex.close();
		}
		final boolean isPred = (flags & IOUtil.PRED_FLAG) == IOUtil.PRED_FLAG;
		final boolean isReg = (flags & IOUtil.REG_FLAG) == IOUtil.REG_FLAG;
		final InternalInformation ii = new InternalInformation(isReg, isPred, bitStart, bitEnd, arrayIdx, varInfos.get(varIdx));
		return ii;
	}

	public Frame readFrame(boolean verbose) throws IOException {
		TLV tlv = null;
		boolean constant = false;
		BigInteger consts[] = new BigInteger[0];
		int edgeNegDep = -1, edgePosDep = -1;
		int[] predNegDep = null;
		int[] predPosDep = null;
		int executionDep = -1;
		int maxDataWidth = -1, maxStackDepth = -1;
		int outputID = -1, uniqueID = -1;
		int scheduleStage = -1;
		FastInstruction[] instructions = new FastInstruction[0];
		int[] intDeps = new int[0];
		while ((tlv = readTLV(FrameTypes.constants)) != null) {
			final ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			final FrameTypes type = (FrameTypes) tlv.type;
			switch (type) {
			case constants:
				final String[] strings = ex.readStringArray();
				consts = new BigInteger[strings.length];
				for (int i = 0; i < strings.length; i++) {
					final String string = strings[i];
					consts[i] = new BigInteger(string, 16);
				}
				break;
			case edgeNegDep:
				edgeNegDep = ex.readVarInt();
				break;
			case edgePosDep:
				edgePosDep = ex.readVarInt();
				break;
			case executionDep:
				executionDep = ex.readVarInt();
				break;
			case instructions:
				instructions = readInstructions(tlv);
				break;
			case internalDep:
				intDeps = ex.readIntArray();
				break;
			case maxDataWidth:
				maxDataWidth = ex.readVarInt();
				break;
			case maxStackDepth:
				maxStackDepth = ex.readVarInt();
				break;
			case outputID:
				outputID = ex.readVarInt();
				break;
			case predNegDep:
				predNegDep = ex.readIntArray();
				break;
			case predPosDep:
				predPosDep = ex.readIntArray();
				break;
			case uniqueID:
				uniqueID = ex.readVarInt();
				break;
			case flags:
				final int flags = ex.readVarInt();
				if ((flags & IOUtil.CONST_FLAG) == IOUtil.CONST_FLAG) {
					constant = true;
				}
				break;
			case scheduleStage:
				scheduleStage = ex.readVarInt();
				break;
			default:
				ex.close();
				throw new IllegalArgumentException("The type:" + type + " is not handled");
			}
			ex.close();
		}
		final Frame frame = new Frame(instructions, intDeps, predPosDep, predNegDep, edgePosDep, edgeNegDep, outputID, maxDataWidth, maxStackDepth, consts, uniqueID, constant,
				scheduleStage);
		frame.executionDep = executionDep;
		return frame;
	}

	public FastInstruction[] readInstructions(TLV tlv) throws IOException {
		final ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
		FastInstruction[] instructions;
		final Instruction[] values = Instruction.values();
		final List<FastInstruction> instr = new LinkedList<>();
		int read = -1;
		while ((read = ex.read()) != -1) {
			final Instruction instruction = values[read & 0x3F];
			int arg1 = 0;
			int arg2 = 0;
			if (instruction.argCount > 0) {
				arg1 = ex.readVarInt();
			}
			if (instruction.argCount > 1) {
				arg2 = ex.readVarInt();
			}
			instr.add(new FastInstruction(instruction, arg1, arg2));
		}
		ex.close();
		instructions = instr.toArray(new FastInstruction[instr.size()]);
		return instructions;
	}

	public String[] readStringArray() throws IOException {
		final int amount = readVarInt();
		final String[] res = new String[amount];
		for (int i = 0; i < amount; i++) {
			res[i] = readSubString();
		}
		return res;
	}

	public String readSubString() throws IOException {
		final int len = readVarInt();
		final byte[] buf = new byte[len];
		readFully(buf);
		return new String(buf, "UTF-8");
	}

	public int[] readIntArray() throws IOException {
		final int amount = readVarInt();
		final int res[] = new int[amount];
		for (int i = 0; i < amount; i++) {
			res[i] = readVarInt();
		}
		return res;
	}

	public int readVarInt() throws IOException {
		int tmp = 0;
		if (((tmp = read()) & 0x80) == 0)
			return tmp;
		int result = tmp & 0x7f;
		if (((tmp = read()) & 0x80) == 0) {
			result |= tmp << 7;
		} else {
			result |= (tmp & 0x7f) << 7;
			if (((tmp = read()) & 0x80) == 0) {
				result |= tmp << 14;
			} else {
				result |= (tmp & 0x7f) << 14;
				if (((tmp = read()) & 0x80) == 0) {
					result |= tmp << 21;
				} else {
					result |= (tmp & 0x7f) << 21;
					result |= (tmp = read()) << 28;
					if (tmp < 0)
						throw new IllegalArgumentException("Too many bits");
				}
			}
		}
		return result;
	}

}
