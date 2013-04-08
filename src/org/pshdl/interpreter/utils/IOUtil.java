package org.pshdl.interpreter.utils;

import java.io.*;
import java.math.*;
import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.HDLFrameInterpreter.InstructionCursor;

public class IOUtil {
	private static enum ModelTypes {
		version, src, date, maxDataWidth, maxStackDepth, internals, widths, registers, frame
	}

	private static enum FrameTypes {
		uniqueID, outputID, internalDep, edgePosDep, edgeNegDep, predPosDep, predNegDep, executionDep, constants, instructions, maxDataWidth, maxStackDepth
	}

	private static class TLV {
		public final Enum<?> type;
		public final byte[] value;

		private TLV(Enum<?> type, byte[] value) {
			super();
			this.type = type;
			this.value = value;
		}

		public static TLV read(DataInputStream is, Enum<?> e[]) throws IOException {
			int len = 0;
			int type = -1;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			do {
				type = is.read();
				if (type == -1)
					return null;
				if ((type & 0x80) != 0) {
					type &= 0x7F;
				}
				len = is.readShort() & 0xFFFF;
				// System.out.println("IOUtil.TLV.read()Reading type:" + e[type]
				// + " with len:" + len);
				byte data[] = new byte[len];
				is.readFully(data);
				baos.write(data);
			} while (len == 0xFFFF);
			Enum<?> ne = e[type];
			return new TLV(ne, baos.toByteArray());
		}

	}

	public static ExecutableModel readExecutableModel(File source, boolean verbose) throws IOException {
		FileInputStream fis = new FileInputStream(source);
		ExecutableModel res = readExecutableModel(fis, verbose);
		fis.close();
		return res;
	}

	private static ExecutableModel readExecutableModel(InputStream fis, boolean verbose) throws IOException {
		TLV tlv = null;
		DataInputStream dis = new DataInputStream(fis);
		byte[] header = new byte[4];
		dis.readFully(header);
		if (!"PSEX".equals(new String(header)))
			throw new IllegalArgumentException("Not an PS Executable: Missing header!");
		int[] widths = new int[0];
		String[] internals = new String[0];
		List<Frame> frameList = new LinkedList<Frame>();
		while ((tlv = TLV.read(dis, ModelTypes.values())) != null) {
			ModelTypes type = (ModelTypes) tlv.type;
			switch (type) {
			case date:
				if (verbose) {
					System.out.printf("Created on: %1$F %1$T\n" + new Date(asLong(tlv.value)));
				}
				break;
			case frame:
				frameList.add(asFrame(tlv.value, verbose));
				break;
			case internals:
				internals = asStringArray(tlv.value);
				break;
			case maxDataWidth:
				if (verbose) {
					System.out.println("Max data width:" + asInt(tlv.value));
				}
				break;
			case maxStackDepth:
				if (verbose) {
					System.out.println("Max Stack depth:" + asInt(tlv.value));
				}
				break;
			case registers:
				if (verbose) {
					int[] asIntArray = asIntArray(tlv.value);
					StringBuilder sb = new StringBuilder();
					for (int i : asIntArray) {
						sb.append(' ').append(internals[i]);
					}
					System.out.println("The following internals are registers:" + sb);
				}
				break;
			case src:
				if (verbose) {
					// Don't use the terminating 0 in Java
					System.out.println("Generated from resource:" + new String(tlv.value, 0, tlv.value.length - 1));
				}
				break;
			case version:
				if (verbose) {
					System.out.printf("Compiled with version: %d.%d.%d\n", tlv.value[0], tlv.value[1], tlv.value[2]);
				}
				break;
			case widths:
				widths = asIntArray(tlv.value);
				break;
			default:
				throw new IllegalArgumentException("The type:" + type + " is not handled");
			}
		}
		Frame[] frames = frameList.toArray(new Frame[frameList.size()]);
		Map<String, Integer> widthMap = new HashMap<String, Integer>();
		for (int i = 0; i < internals.length; i++) {
			String s = internals[i];
			widthMap.put(s, widths[i]);
		}
		return new ExecutableModel(frames, internals, widthMap);
	}

	private static Frame asFrame(byte[] value, boolean verbose) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(value));
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
		while ((tlv = TLV.read(dis, FrameTypes.values())) != null) {
			FrameTypes type = (FrameTypes) tlv.type;
			switch (type) {
			case constants:
				String[] strings = asStringArray(tlv.value);
				consts = new BigInteger[strings.length];
				for (int i = 0; i < strings.length; i++) {
					String string = strings[i];
					consts[i] = new BigInteger(string, 16);
				}
				break;
			case edgeNegDep:
				edgeNegDep = asInt(tlv.value);
				break;
			case edgePosDep:
				edgePosDep = asInt(tlv.value);
				break;
			case executionDep:
				executionDep = asInt(tlv.value);
				break;
			case instructions:
				instructions = tlv.value;
				break;
			case internalDep:
				intDeps = asIntArray(tlv.value);
				break;
			case maxDataWidth:
				maxDataWidth = asInt(tlv.value);
				break;
			case maxStackDepth:
				maxStackDepth = asInt(tlv.value);
				break;
			case outputID:
				outputID = asInt(tlv.value);
				break;
			case predNegDep:
				predNegDep = asIntArray(tlv.value);
				break;
			case predPosDep:
				predPosDep = asIntArray(tlv.value);
				break;
			case uniqueID:
				uniqueID = asInt(tlv.value);
				break;
			default:
				throw new IllegalArgumentException("The type:" + type + " is not handled");
			}
		}
		Frame frame = new Frame(instructions, intDeps, predPosDep, predNegDep, edgePosDep, edgeNegDep, outputID, maxDataWidth, maxStackDepth, consts, uniqueID);
		frame.executionDep = executionDep;
		return frame;
	}

	private static String[] asStringArray(byte[] value) {
		if (value.length == 0)
			return new String[0];
		String totalString = new String(value);
		return totalString.split("\0");
	}

	private static int[] asIntArray(byte[] value) {
		InstructionCursor cursor = new HDLFrameInterpreter.InstructionCursor(value);
		int amount = cursor.readVarInt();
		int res[] = new int[amount];
		for (int i = 0; i < amount; i++) {
			res[i] = cursor.readVarInt();
		}
		return res;
	}

	private static int asInt(byte[] value) {
		InstructionCursor cursor = new HDLFrameInterpreter.InstructionCursor(value);
		return cursor.readVarInt();
	}

	private static long asLong(byte[] readBuffer) {
		return (((long) readBuffer[0] << 56) + ((long) (readBuffer[1] & 255) << 48) + ((long) (readBuffer[2] & 255) << 40) + ((long) (readBuffer[3] & 255) << 32)
				+ ((long) (readBuffer[4] & 255) << 24) + ((readBuffer[5] & 255) << 16) + ((readBuffer[6] & 255) << 8) + ((readBuffer[7] & 255) << 0));
	}

	public static void writeExecutableModel(String source, long date, ExecutableModel model, File target) throws IOException {
		FileOutputStream fos = new FileOutputStream(target);
		writeExecutableModel(source, date, model, fos);
		fos.close();
	}

	public static void writeExecutableModel(String source, long date, ExecutableModel model, OutputStream oos) throws IOException {
		DataOutputStream obj = new DataOutputStream(oos);
		oos.write("PSEX".getBytes());
		writeTLVByteArray(obj, ModelTypes.version, new byte[] { 0, 2, 0 });
		writeTLVString(obj, ModelTypes.src, source);
		if (date != -1) {
			writeTLVSingleLong(obj, ModelTypes.date, date);
		}
		writeTLVSingleInt(obj, ModelTypes.maxDataWidth, model.maxDataWidth);
		writeTLVSingleInt(obj, ModelTypes.maxStackDepth, model.maxStackDepth);
		writeTLVString(obj, ModelTypes.internals, model.internals);
		int[] widths = new int[model.internals.length];
		for (int i = 0; i < widths.length; i++) {
			// Transform map into sequence of width with same ordering as
			// internals
			widths[i] = model.getWidth(model.internals[i]);
		}
		writeTLVIntArray(obj, ModelTypes.widths, widths);
		writeTLVIntArray(obj, ModelTypes.registers, model.registerOutputs);
		for (Frame f : model.frames) {
			writeFrame(obj, f);
		}
		obj.flush();
	}

	private static void writeFrame(DataOutputStream modelOO, Frame f) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream obj = new DataOutputStream(baos);
		writeTLVSingleInt(obj, FrameTypes.uniqueID, f.uniqueID);
		writeTLVSingleInt(obj, FrameTypes.outputID, f.outputId);
		writeTLVIntArray(obj, FrameTypes.internalDep, f.internalDependencies);
		// Only write if they are set
		if (f.edgeNegDepRes != -1) {
			writeTLVSingleInt(obj, FrameTypes.edgeNegDep, f.edgeNegDepRes);
		}
		if (f.edgePosDepRes != -1) {
			writeTLVSingleInt(obj, FrameTypes.edgePosDep, f.edgePosDepRes);
		}
		if ((f.predNegDepRes != null) && (f.predNegDepRes.length > 0)) {
			writeTLVIntArray(obj, FrameTypes.predNegDep, f.predNegDepRes);
		}
		if ((f.predPosDepRes != null) && (f.predPosDepRes.length > 0)) {
			writeTLVIntArray(obj, FrameTypes.predPosDep, f.predPosDepRes);
		}
		if (f.executionDep != -1) {
			writeTLVSingleInt(obj, FrameTypes.executionDep, f.executionDep);
		}
		String[] consts = new String[f.constants.length];
		for (int i = 0; i < consts.length; i++) {
			consts[i] = f.constants[i].toString(16); // Represent as hex String
		}
		writeTLVString(obj, FrameTypes.constants, consts);
		writeTLVByteArray(obj, FrameTypes.instructions, f.instructions);
		writeTLVSingleInt(obj, FrameTypes.maxDataWidth, f.maxDataWidth);
		writeTLVSingleInt(obj, FrameTypes.maxStackDepth, f.maxStackDepth);

		writeTLVByteArray(modelOO, ModelTypes.frame, baos.toByteArray());
		obj.close();
	}

	/**
	 * Simple TLV for single integers. No preceeding items count
	 */
	private static void writeTLVSingleInt(DataOutputStream obj, Enum<?> e, int data) throws IOException {
		writeTLVByteArray(obj, e, getVarInt(data));
	}

	private static byte[] getVarInt(int val) throws IOException {
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

	private static void writeTLVIntArray(DataOutputStream obj, Enum<?> e, int... data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i : data) {
			baos.write(getVarInt(i));
		}
		byte[] lenHeader = getVarInt(data.length);
		byte[] varIntArray = baos.toByteArray();
		writeHeader(obj, e, varIntArray.length + lenHeader.length);
		obj.write(lenHeader); // Write the number of elements
		obj.write(varIntArray); // Write all VarInts
	}

	private static void writeTLVString(DataOutputStream obj, Enum<?> e, String... data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (String string : data) {
			byte[] bytes = string.getBytes();
			baos.write(bytes);
			baos.write(0); // Make null terminated strings
		}
		writeTLVByteArray(obj, e, baos.toByteArray());
	}

	private static void writeTLVSingleLong(DataOutputStream obj, Enum<?> e, long... data) throws IOException {
		writeHeader(obj, e, 8 * data.length);
		for (long l : data) {
			obj.writeLong(l);
		}
	}

	private static void writeTLVByteArray(DataOutputStream obj, Enum<?> e, byte[] bytes) throws IOException {
		int len = bytes.length;
		int off = 0;
		while (len >= 0xFFFF) {
			writeHeader(obj, e, 0xFFFF);
			obj.write(bytes, off, 0xFFFF); // Write data
			len -= 0xFFFF;
			off += 0xFFFF;
		}
		writeHeader(obj, e, len);
		obj.write(bytes, off, len); // Write data
	}

	public static void writeHeader(DataOutputStream obj, Enum<?> e, int len) throws IOException {
		if (e instanceof FrameTypes) {
			obj.writeByte(e.ordinal() | 0x80); // Write 1 byte type field
		} else {
			obj.writeByte(e.ordinal()); // Write 1 byte type field
		}
		obj.writeShort(len); // Write 2 byte length field
	}

	public static void main(String[] args) {
		System.out.println("Model types:");
		for (ModelTypes type : ModelTypes.values()) {
			System.out.printf("%15s=%02x\n", type.name(), type.ordinal());
		}
		System.out.println("Frame types:");
		for (FrameTypes type : FrameTypes.values()) {
			System.out.printf("0x%02x | %-15s\n", 0x80 | type.ordinal(), type.name());
		}
	}
}
