package org.pshdl.interpreter.utils;

import java.io.*;
import org.pshdl.interpreter.*;

public class IOUtil {
	public static enum ModelTypes {
		version, src, date, maxDataWidth, maxStackDepth, internals, widths, registers, frame
	}

	public static enum FrameTypes {
		uniqueID, outputID, internalDep, edgePosDep, edgeNegDep, predPosDep, predNegDep, executionDep, constants, instructions, maxDataWidth, maxStackDepth
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
