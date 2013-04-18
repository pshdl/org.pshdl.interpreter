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

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.utils.IOUtil.FrameTypes;
import org.pshdl.interpreter.utils.IOUtil.IDType;
import org.pshdl.interpreter.utils.IOUtil.InternalTypes;
import org.pshdl.interpreter.utils.IOUtil.ModelTypes;

public class ExecutableOutputStream extends DataOutputStream {

	public ExecutableOutputStream(OutputStream out) {
		super(out);
	}

	public void writeExecutableModel(String source, long date, ExecutableModel model) throws IOException {
		write("PSEX".getBytes());
		writeByteArray(ModelTypes.version, new byte[] { 0, 2, 0 });
		writeString(ModelTypes.src, source);
		if (date != -1) {
			writeLong(ModelTypes.date, date);
		}
		writeInt(ModelTypes.maxDataWidth, model.maxDataWidth);
		writeInt(ModelTypes.maxStackDepth, model.maxStackDepth);
		for (InternalInformation ii : model.internals) {
			writeInternal(ii);
		}
		writeIntArray(ModelTypes.registers, model.registerOutputs);
		for (Frame f : model.frames) {
			writeFrame(f);
		}
	}

	private void writeInternal(InternalInformation ii) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ExecutableOutputStream obj = new ExecutableOutputStream(baos);
		obj.writeString(InternalTypes.baseName, ii.baseName);
		if (ii.arrayIdx.length > 0) {
			obj.writeIntArray(InternalTypes.arrayIdx, ii.arrayIdx);
		}
		obj.writeInt(InternalTypes.baseWidth, ii.baseWidth);
		if (ii.bitStart != -1) {
			obj.writeInt(InternalTypes.bitStart, ii.bitStart);
		}
		if (ii.bitEnd != -1) {
			obj.writeInt(InternalTypes.bitEnd, ii.bitEnd);
		}
		if (ii.arrayStart.length > 0)
			obj.writeIntArray(InternalTypes.arrayStart, ii.arrayStart);
		if (ii.arrayEnd.length > 0)
			obj.writeIntArray(InternalTypes.arrayEnd, ii.arrayEnd);

		obj.writeInt(InternalTypes.flags, (ii.isPred ? IOUtil.PRED_FLAG : 0) | (ii.isReg ? IOUtil.REG_FLAG : 0));
		writeByteArray(ModelTypes.internal, baos.toByteArray());
		obj.close();
	}

	public void writeFrame(Frame f) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ExecutableOutputStream obj = new ExecutableOutputStream(baos);
		obj.writeInt(FrameTypes.uniqueID, f.uniqueID);
		obj.writeInt(FrameTypes.outputID, f.outputId);
		obj.writeIntArray(FrameTypes.internalDep, f.internalDependencies);
		// Only write if they are set
		if (f.edgeNegDepRes != -1) {
			obj.writeInt(FrameTypes.edgeNegDep, f.edgeNegDepRes);
		}
		if (f.edgePosDepRes != -1) {
			obj.writeInt(FrameTypes.edgePosDep, f.edgePosDepRes);
		}
		if ((f.predNegDepRes != null) && (f.predNegDepRes.length > 0)) {
			obj.writeIntArray(FrameTypes.predNegDep, f.predNegDepRes);
		}
		if ((f.predPosDepRes != null) && (f.predPosDepRes.length > 0)) {
			obj.writeIntArray(FrameTypes.predPosDep, f.predPosDepRes);
		}
		if (f.executionDep != -1) {
			obj.writeInt(FrameTypes.executionDep, f.executionDep);
		}
		String[] consts = new String[f.constants.length];
		for (int i = 0; i < consts.length; i++) {
			consts[i] = f.constants[i].toString(16); // Represent as hex String
		}
		obj.writeStringArray(FrameTypes.constants, consts);
		obj.writeByteArray(FrameTypes.instructions, f.instructions);
		obj.writeInt(FrameTypes.maxDataWidth, f.maxDataWidth);
		obj.writeInt(FrameTypes.maxStackDepth, f.maxStackDepth);

		writeByteArray(ModelTypes.frame, baos.toByteArray());
		obj.close();
	}

	/**
	 * Simple TLV for single integers. No preceeding items count
	 */
	public void writeInt(IDType<?> e, int data) throws IOException {
		writeByteArray(e, getVarInt(data));
	}

	public static byte[] getVarInt(int val) throws IOException {
		int num = val;
		int t = 0;
		ByteArrayOutputStream os = new ByteArrayOutputStream(5);
		while (num > 127) {
			t = 0x80 | (num & 0x7F);
			os.write(t);
			num >>= 7;
		}
		t = num & 0x7F;
		os.write(t);
		return os.toByteArray();
	}

	public static void main(String[] args) throws IOException {
		byte[] varInt = getVarInt(123456);
		for (byte b : varInt) {
			System.out.printf("%02X", b & 0xFF);
		}
	}

	public void writeIntArray(IDType<?> e, int... data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i : data) {
			baos.write(getVarInt(i));
		}
		byte[] lenHeader = getVarInt(data.length);
		byte[] varIntArray = baos.toByteArray();
		writeHeader(e, varIntArray.length + lenHeader.length);
		write(lenHeader); // Write the number of elements
		write(varIntArray); // Write all VarInts
	}

	public void writeString(IDType<?> e, String data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytes = data.getBytes("UTF-8");
		baos.write(bytes);
		writeByteArray(e, data.getBytes("UTF-8"));
	}

	public void writeStringArray(IDType<?> e, String... data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(getVarInt(data.length));
		for (String string : data) {
			byte[] bytes = string.getBytes("UTF-8");
			baos.write(getVarInt(bytes.length));
			baos.write(bytes);
		}
		writeByteArray(e, baos.toByteArray());
	}

	public void writeLong(IDType<?> e, long data) throws IOException {
		writeHeader(e, 8);
		writeLong(data);
	}

	public void writeByteArray(IDType<?> e, byte[] bytes) throws IOException {
		int len = bytes.length;
		writeHeader(e, len);
		write(bytes, 0, len); // Write data
	}

	private void writeHeader(IDType<?> e, int len) throws IOException {
		writeByte(e.getID()); // Write 1 byte type field
		write(getVarInt(len)); // Write 2 byte length field
	}

}
