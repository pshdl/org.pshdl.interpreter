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

import java.io.*;
import java.math.*;
import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.utils.IOUtil.FrameTypes;
import org.pshdl.interpreter.utils.IOUtil.IDType;
import org.pshdl.interpreter.utils.IOUtil.InternalTypes;
import org.pshdl.interpreter.utils.IOUtil.ModelTypes;

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

	}

	public TLV readTLV(IDType<?> e) throws IOException {
		int len = 0;
		int type = -1;
		type = read();
		if (type == -1)
			return null;
		Enum<?> ne = e.getFromID(type);
		len = readVarInt();
		byte data[] = new byte[len];
		readFully(data);
		return new TLV(ne, data);
	}

	public ExecutableModel readExecutableModel(boolean verbose) throws IOException {
		TLV tlv = null;
		byte[] header = new byte[4];
		readFully(header);
		if (!"PSEX".equals(new String(header)))
			throw new IllegalArgumentException("Not a PS Executable: Missing or wrong header!");
		List<InternalInformation> internals = new LinkedList<InternalInformation>();
		List<Frame> frameList = new LinkedList<Frame>();
		while ((tlv = readTLV(ModelTypes.date)) != null) {
			ModelTypes type = (ModelTypes) tlv.type;
			ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			switch (type) {
			case date:
				if (verbose) {
					long readDate = ex.readLong();
					System.out.printf("Created on: %1$F %1$T\n" + new Date(readDate));
				}
				break;
			case frame:
				frameList.add(ex.readFrame(verbose));
				break;
			case internal:
				internals.add(ex.readInternal());
				break;
			case maxDataWidth:
				if (verbose) {
					int maxDataWidth = ex.readVarInt();
					System.out.println("Max data width:" + maxDataWidth);
				}
				break;
			case maxStackDepth:
				if (verbose) {
					int maxStackDepth = ex.readVarInt();
					System.out.println("Max Stack depth:" + maxStackDepth);
				}
				break;
			case registers:
				if (verbose) {
					int[] asIntArray = ex.readIntArray();
					StringBuilder sb = new StringBuilder();
					for (int i : asIntArray) {
						sb.append(' ').append(internals.get(i).fullName);
					}
					System.out.println("The following internals are registers:" + sb);
				}
				break;
			case src:
				if (verbose) {
					String src = new String(tlv.value, "UTF-8");
					System.out.println("Generated from resource:" + src);
				}
				break;
			case version:
				if (verbose) {
					byte[] version = new byte[3];
					ex.readFully(version);
					System.out.printf("Compiled with version: %d.%d.%d\n", version[0], version[1], version[2]);
				}
				break;
			default:
				ex.close();
				throw new IllegalArgumentException("The type:" + type + " is not handled");
			}
			ex.close();
		}
		Frame[] frames = frameList.toArray(new Frame[frameList.size()]);
		InternalInformation[] iis = internals.toArray(new InternalInformation[internals.size()]);
		return new ExecutableModel(frames, iis);
	}

	private void printHex(byte[] value) {
		for (byte b : value) {
			System.out.printf("%02X ", b);
		}
		System.out.println();
	}

	private InternalInformation readInternal() throws IOException {
		TLV tlv = null;
		int[] dims = new int[0];
		String baseName = null;
		int baseWidth = -1;
		int bitStart = -1, bitEnd = -1;
		int flags = 0;
		int[] arrayStart = new int[0], arrayEnd = new int[0];
		while ((tlv = readTLV(InternalTypes.baseName)) != null) {
			ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			InternalTypes it = (InternalTypes) tlv.type;
			switch (it) {
			case arrayIdx:
				dims = ex.readIntArray();
				break;
			case baseName:
				baseName = new String(tlv.value, "UTF-8");
				break;
			case baseWidth:
				baseWidth = ex.readVarInt();
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
			case arrayStart:
				arrayStart = ex.readIntArray();
				break;
			case arrayEnd:
				arrayEnd = ex.readIntArray();
				break;
			}
			ex.close();
		}
		boolean isPred = (flags & IOUtil.PRED_FLAG) == IOUtil.PRED_FLAG;
		boolean isReg = (flags & IOUtil.REG_FLAG) == IOUtil.REG_FLAG;
		InternalInformation ii = new InternalInformation(baseName, isReg, isPred, bitStart, bitEnd, baseWidth, dims, arrayStart, arrayEnd);
		return ii;
	}

	public Frame readFrame(boolean verbose) throws IOException {
		TLV tlv = null;
		BigInteger consts[] = new BigInteger[0];
		int edgeNegDep = -1, edgePosDep = -1;
		int[] predNegDep = null;
		int[] predPosDep = null;
		int executionDep = -1;
		int maxDataWidth = -1, maxStackDepth = -1;
		int outputID = -1, uniqueID = -1;
		byte[] instructions = new byte[0];
		int[] intDeps = new int[0];
		while ((tlv = readTLV(FrameTypes.constants)) != null) {
			ExecutableInputStream ex = new ExecutableInputStream(new ByteArrayInputStream(tlv.value));
			FrameTypes type = (FrameTypes) tlv.type;
			switch (type) {
			case constants:
				String[] strings = ex.readStringArray();
				consts = new BigInteger[strings.length];
				for (int i = 0; i < strings.length; i++) {
					String string = strings[i];
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
				instructions = tlv.value;
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
			default:
				ex.close();
				throw new IllegalArgumentException("The type:" + type + " is not handled");
			}
			ex.close();
		}
		Frame frame = new Frame(instructions, intDeps, predPosDep, predNegDep, edgePosDep, edgeNegDep, outputID, maxDataWidth, maxStackDepth, consts, uniqueID);
		frame.executionDep = executionDep;
		return frame;
	}

	public String[] readStringArray() throws IOException {
		int amount = readVarInt();
		String[] res = new String[amount];
		for (int i = 0; i < amount; i++) {
			res[i] = readSubString();
		}
		return res;
	}

	public String readSubString() throws IOException {
		int len = readVarInt();
		byte[] buf = new byte[len];
		readFully(buf);
		return new String(buf, "UTF-8");
	}

	public int[] readIntArray() throws IOException {
		int amount = readVarInt();
		int res[] = new int[amount];
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
