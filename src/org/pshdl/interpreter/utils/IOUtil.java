package org.pshdl.interpreter.utils;

import java.io.*;
import java.util.*;

import org.pshdl.interpreter.*;

public class IOUtil {
	public static void writeExecutableModel(String source, ExecutableModel model, File target) throws IOException {
		FileOutputStream fos = new FileOutputStream(target);
		writeExecutableModel(source, model, fos);
		fos.close();
	}

	private static enum ModelTypes {
		src, date, maxDataWidth, maxStackDepth, internals, widths, registers, frame
	}

	private static enum FrameTypes {
		uniqueID, id, internalDep, edgePosDep, edgeNegDep, predPosDep, predNegDep, constants, instructions
	}

	public static void writeExecutableModel(String source, ExecutableModel model, OutputStream oos) throws IOException {
		DataOutputStream obj = new DataOutputStream(oos);
		oos.write("PSEX".getBytes());
		writeTLV(obj, ModelTypes.src, source);
		writeTLV(obj, ModelTypes.date, new Date().getTime());
		writeTLV(obj, ModelTypes.maxDataWidth, model.maxDataWidth);
		writeTLV(obj, ModelTypes.maxStackDepth, model.maxStackDepth);
		writeTLV(obj, ModelTypes.internals, model.internals);
		int[] widths = new int[model.internals.length];
		for (int i = 0; i < widths.length; i++) {
			// Transform map into sequence of width with same ordering as
			// internals
			widths[i] = model.getWidth(model.internals[i]);
		}
		writeTLV(obj, ModelTypes.widths, widths);
		writeTLV(obj, ModelTypes.registers, model.registerOutputs);
		for (Frame f : model.frames) {
			writeFrame(obj, f);
		}
		obj.flush();
	}

	private static void writeFrame(DataOutputStream modelOO, Frame f) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream obj = new DataOutputStream(baos);
		writeTLV(obj, FrameTypes.uniqueID, f.uniqueID);
		writeTLV(obj, FrameTypes.id, f.id);
		writeTLV(obj, FrameTypes.internalDep, f.internalDependencies);
		// Only write if they are set
		if (f.edgeNegDepRes != -1)
			writeTLV(obj, FrameTypes.edgeNegDep, f.edgeNegDepRes);
		if (f.edgePosDepRes != -1)
			writeTLV(obj, FrameTypes.edgePosDep, f.edgePosDepRes);
		if (f.predNegDepRes != -1)
			writeTLV(obj, FrameTypes.predNegDep, f.predNegDepRes);
		if (f.predPosDepRes != -1)
			writeTLV(obj, FrameTypes.predPosDep, f.predPosDepRes);
		String[] consts = new String[f.constants.length];
		for (int i = 0; i < consts.length; i++) {
			consts[i] = f.constants[i].toString(16); // Represent as hex String
		}
		writeTLV(obj, FrameTypes.constants, consts);
		writeTLV(obj, FrameTypes.instructions, f.instructions);
		writeTLV(modelOO, ModelTypes.frame, baos.toByteArray());
		obj.close();
	}

	private static void writeTLV(DataOutputStream obj, Enum<?> e, int... data) throws IOException {
		writeHeader(obj, e, 4 * data.length);
		for (int i : data) {
			obj.writeInt(i);
		}
	}

	private static void writeTLV(DataOutputStream obj, Enum<?> e, String... data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (String string : data) {
			byte[] bytes = string.getBytes();
			baos.write(bytes);
			baos.write(0); // Make null terminated strings
		}
		writeTLV(obj, e, baos.toByteArray());
	}

	private static void writeTLV(DataOutputStream obj, Enum<?> e, long... data) throws IOException {
		writeHeader(obj, e, 8 * data.length);
		for (long l : data) {
			obj.writeLong(l);
		}
	}

	private static void writeTLV(DataOutputStream obj, Enum<?> e, byte[] bytes) throws IOException {
		writeHeader(obj, e, bytes.length);
		obj.write(bytes); // Write data
	}

	public static void writeHeader(DataOutputStream obj, Enum<?> e, int len) throws IOException {
		obj.writeByte(e.ordinal()); // Write 1 byte type field
		obj.writeShort(len); // Write 2 byte length field
	}
}
