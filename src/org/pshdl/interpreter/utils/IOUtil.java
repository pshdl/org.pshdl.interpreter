package org.pshdl.interpreter.utils;

import java.io.*;

import org.pshdl.interpreter.*;

public class IOUtil {
	public static int PRED_FLAG = 0x01;
	public static int REG_FLAG = 0x02;

	public static interface IDType<T extends Enum<T>> {
		public int getID();

		public T getFromID(int id);
	}

	public static enum ModelTypes implements IDType<ModelTypes> {
		version, src, date, maxDataWidth, maxStackDepth, internal, registers, frame;

		@Override
		public int getID() {
			return ordinal();
		}

		@Override
		public ModelTypes getFromID(int id) {
			return values()[id];
		}
	}

	public static enum FrameTypes implements IDType<FrameTypes> {
		uniqueID, outputID, internalDep, edgePosDep, edgeNegDep, predPosDep, predNegDep, executionDep, constants, instructions, maxDataWidth, maxStackDepth;

		@Override
		public int getID() {
			return ordinal() | 0x80;
		}

		@Override
		public FrameTypes getFromID(int id) {
			return values()[id & 0x7F];
		}
	}

	public static enum InternalTypes implements IDType<InternalTypes> {
		baseName, baseWidth, bitStart, bitEnd, arrayDims, flags;

		@Override
		public int getID() {
			return ordinal() | 0x40;
		}

		@Override
		public InternalTypes getFromID(int id) {
			return values()[id & 0x3F];
		}
	}

	public static ExecutableModel readExecutableModel(File source, boolean verbose) throws IOException {
		ExecutableInputStream fis = new ExecutableInputStream(new FileInputStream(source));
		ExecutableModel res = fis.readExecutableModel(verbose);
		fis.close();
		return res;
	}

	public static void writeExecutableModel(String source, long date, ExecutableModel model, File target) throws IOException {
		ExecutableOutputStream fos = new ExecutableOutputStream(new FileOutputStream(target));
		fos.writeExecutableModel(source, date, model);
		fos.close();
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
