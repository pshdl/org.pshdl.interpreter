package org.pshdl.interpreter.utils;

import java.io.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.utils.IOUtil.*;

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
		writeStringArray(ModelTypes.internals, model.internals);
		int[] widths = new int[model.internals.length];
		for (int i = 0; i < widths.length; i++) {
			// Transform map into sequence of width with same ordering as
			// internals
			widths[i] = model.getWidth(model.internals[i]);
		}
		writeIntArray(ModelTypes.widths, widths);
		writeIntArray(ModelTypes.registers, model.registerOutputs);
		for (Frame f : model.frames) {
			writeFrame(f);
		}
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
	public void writeInt(Enum<?> e, int data) throws IOException {
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

	public void writeIntArray(Enum<?> e, int... data) throws IOException {
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

	public void writeString(Enum<?> e, String data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytes = data.getBytes("UTF-8");
		baos.write(bytes);
		writeByteArray(e, data.getBytes("UTF-8"));
	}

	public void writeStringArray(Enum<?> e, String... data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(getVarInt(data.length));
		for (String string : data) {
			byte[] bytes = string.getBytes("UTF-8");
			baos.write(getVarInt(bytes.length));
			baos.write(bytes);
		}
		writeByteArray(e, baos.toByteArray());
	}

	public void writeLong(Enum<?> e, long data) throws IOException {
		writeHeader(e, 8);
		writeLong(data);
	}

	public void writeByteArray(Enum<?> e, byte[] bytes) throws IOException {
		int len = bytes.length;
		writeHeader(e, len);
		write(bytes, 0, len); // Write data
	}

	private void writeHeader(Enum<?> e, int len) throws IOException {
		if (e instanceof FrameTypes) {
			writeByte(e.ordinal() | 0x80); // Write 1 byte type field
		} else {
			writeByte(e.ordinal()); // Write 1 byte type field
		}
		write(getVarInt(len)); // Write 2 byte length field
	}

}
