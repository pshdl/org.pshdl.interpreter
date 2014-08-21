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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.pshdl.interpreter.ExecutableModel;
import org.pshdl.interpreter.Frame;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.InternalInformation;
import org.pshdl.interpreter.VariableInformation;
import org.pshdl.interpreter.utils.IOUtil.FrameTypes;
import org.pshdl.interpreter.utils.IOUtil.IDType;
import org.pshdl.interpreter.utils.IOUtil.InternalTypes;
import org.pshdl.interpreter.utils.IOUtil.ModelTypes;
import org.pshdl.interpreter.utils.IOUtil.VariableTypes;

public class ExecutableOutputStream extends DataOutputStream {

	public ExecutableOutputStream(OutputStream out) {
		super(out);
	}

	public void writeExecutableModel(long date, ExecutableModel model) throws IOException {
		// System.out.println("ExecutableOutputStream.writeExecutableModel()" +
		// model);
		write("PSEX".getBytes(StandardCharsets.UTF_8));
		writeByteArray(ModelTypes.version, new byte[] { 0, 3, 0 });
		writeString(ModelTypes.src, model.source);
		if (model.moduleName != null) {
			writeString(ModelTypes.moduleName, model.moduleName);
		}
		if (date != -1) {
			writeLong(ModelTypes.date, date);
		}
		writeInt(ModelTypes.maxDataWidth, model.maxDataWidth);
		writeInt(ModelTypes.maxStackDepth, model.maxStackDepth);
		if ((model.annotations != null) && (model.annotations.length != 0)) {
			writeStringArray(ModelTypes.annotations, model.annotations);
		}
		final Map<String, Integer> varIdx = new LinkedHashMap<>();
		final VariableInformation[] variables = model.variables;
		for (int i = 0; i < variables.length; i++) {
			final VariableInformation vi = variables[i];
			varIdx.put(vi.name, i);
			writeVariable(vi);
		}
		for (final InternalInformation ii : model.internals) {
			writeInternal(ii, varIdx.get(ii.info.name));
		}
		for (final Frame f : model.frames) {
			writeFrame(f);
		}
	}

	public void writeVariable(VariableInformation vi) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ExecutableOutputStream obj = new ExecutableOutputStream(baos);
		final String name = vi.name;
		obj.writeString(VariableTypes.name, name);
		obj.writeInt(VariableTypes.width, vi.width);
		int flags = 0;
		switch (vi.dir) {
		case IN:
			flags |= IOUtil.IN_FLAG;
			break;
		case INOUT:
			flags |= IOUtil.IO_FLAG;
			break;
		case OUT:
			flags |= IOUtil.OUT_FLAG;
			break;
		default:
		}
		switch (vi.type) {
		case INT:
			flags |= IOUtil.INT_FLAG;
			break;
		case UINT:
			flags |= IOUtil.UINT_FLAG;
			break;
		default:
			break;
		}
		if (vi.isRegister) {
			flags |= IOUtil.REG_FLAG;
		}
		if (vi.isClock) {
			flags |= IOUtil.CLOCK_FLAG;
		}
		if (vi.isReset) {
			flags |= IOUtil.RESET_FLAG;
		}
		obj.writeInt(VariableTypes.flags, flags);
		if (vi.dimensions.length != 0) {
			obj.writeIntArray(VariableTypes.dimensions, vi.dimensions);
		}
		if ((vi.annotations != null) && (vi.annotations.length != 0)) {
			obj.writeStringArray(VariableTypes.annotations, vi.annotations);
		}
		writeByteArray(ModelTypes.variable, baos.toByteArray());
		obj.close();
	}

	public void writeInternal(InternalInformation ii, int varIdx) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ExecutableOutputStream obj = new ExecutableOutputStream(baos);
		obj.writeInt(InternalTypes.varIdx, varIdx);
		if (ii.bitStart != -1) {
			obj.writeInt(InternalTypes.bitStart, ii.bitStart);
		}
		if (ii.bitEnd != -1) {
			obj.writeInt(InternalTypes.bitEnd, ii.bitEnd);
		}
		if (ii.arrayIdx.length > 0) {
			obj.writeIntArray(InternalTypes.arrayIdx, ii.arrayIdx);
		}
		obj.writeInt(InternalTypes.flags, (ii.isPred ? IOUtil.PRED_FLAG : 0) | (ii.isShadowReg ? IOUtil.REG_FLAG : 0));
		writeByteArray(ModelTypes.internal, baos.toByteArray());
		obj.close();
	}

	public void writeFrame(Frame f) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ExecutableOutputStream obj = new ExecutableOutputStream(baos);
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
		final String[] consts = new String[f.constants.length];
		for (int i = 0; i < consts.length; i++) {
			consts[i] = f.constants[i].toString(16); // Represent as hex String
		}
		obj.writeStringArray(FrameTypes.constants, consts);
		obj.writeByteArray(FrameTypes.instructions, getInstructions(f.instructions));
		obj.writeInt(FrameTypes.maxDataWidth, f.maxDataWidth);
		obj.writeInt(FrameTypes.maxStackDepth, f.maxStackDepth);
		int flags = 0;
		if (f.constant) {
			flags |= IOUtil.CONST_FLAG;
		}
		if (flags != 0) {
			obj.writeInt(FrameTypes.flags, flags);
		}
		if (f.scheduleStage != -1) {
			obj.writeInt(FrameTypes.scheduleStage, f.scheduleStage);
		}
		if (f.process != null) {
			obj.writeString(FrameTypes.process, f.process);
		}
		writeByteArray(ModelTypes.frame, baos.toByteArray());
		obj.close();
	}

	private byte[] getInstructions(FastInstruction[] instructions) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (final FastInstruction fi : instructions) {
			baos.write(fi.inst.toByte());
			if (fi.inst.argCount > 0) {
				baos.write(getVarInt(fi.arg1));
			}
			if (fi.inst.argCount > 1) {
				baos.write(getVarInt(fi.arg2));
			}
		}
		return baos.toByteArray();
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
		final ByteArrayOutputStream os = new ByteArrayOutputStream(5);
		while ((num > 127) || (num < 0)) {
			t = 0x80 | (num & 0x7F);
			os.write(t);
			num >>>= 7;
		}
		t = num & 0x7F;
		os.write(t);

		return os.toByteArray();
	}

	public static void main(String[] args) throws IOException {
		final byte[] varInt = getVarInt(-1);
		for (final byte b : varInt) {
			System.out.printf("%02X", b & 0xFF);
		}
	}

	public void writeIntArray(IDType<?> e, int... data) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (final int i : data) {
			baos.write(getVarInt(i));
		}
		final byte[] lenHeader = getVarInt(data.length);
		final byte[] varIntArray = baos.toByteArray();
		writeHeader(e, varIntArray.length + lenHeader.length);
		write(lenHeader); // Write the number of elements
		write(varIntArray); // Write all VarInts
	}

	public void writeString(IDType<?> e, String data) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] bytes = data.getBytes("UTF-8");
		baos.write(bytes);
		writeByteArray(e, data.getBytes("UTF-8"));
	}

	public void writeStringArray(IDType<?> e, String... data) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(getVarInt(data.length));
		for (final String string : data) {
			final byte[] bytes = string.getBytes("UTF-8");
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
		final int len = bytes.length;
		writeHeader(e, len);
		write(bytes, 0, len); // Write data
	}

	private void writeHeader(IDType<?> e, int len) throws IOException {
		writeByte(e.getID()); // Write 1 byte type field
		write(getVarInt(len)); // Write 2 byte length field
	}

}
