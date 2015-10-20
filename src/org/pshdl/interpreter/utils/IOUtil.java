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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.pshdl.interpreter.ExecutableModel;

public class IOUtil {

	// Variable Flags
	public static final int PRED_FLAG = 0x01;
	public static final int REG_FLAG = 0x02;
	public static final int IN_FLAG = 0x4;
	public static final int OUT_FLAG = 0x8;
	public static final int IO_FLAG = IN_FLAG | OUT_FLAG;
	public static final int INT_FLAG = 0x10;
	public static final int UINT_FLAG = 0x20;
	public static final int BOOL_FLAG = 0x40;
	public static final int STRING_FLAG = 0x80;
	public static final int ENUM_FLAG = 0x100;

	public static final int CLOCK_FLAG = 0x200;
	public static final int RESET_FLAG = 0x400;

	// Frame flags
	public static final int CONST_FLAG = 0x01;

	public static interface IDType<T extends Enum<T>> {
		public int getID();

		public T getFromID(int id);
	}

	public static enum ModelTypes implements IDType<ModelTypes> {
		version, src, date, maxDataWidth, maxStackDepth, internal, frame, variable, moduleName, annotation, function;

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
		uniqueID, outputID, internalDep, edgePosDep, edgeNegDep, predPosDep, predNegDep, executionDep, constants, constantStrings, instructions, maxDataWidth, maxStackDepth, flags, scheduleStage, process, enumInfo, isFuncStatement;

		@Override
		public int getID() {
			return ordinal() | 0x80;
		}

		@Override
		public FrameTypes getFromID(int id) {
			return values()[id & 0x7F];
		}
	}

	public static enum FunctionTypes implements IDType<FunctionTypes> {
		name, returnType, parameter, annotations, statement;

		@Override
		public int getID() {
			return ordinal() | 0x100;
		}

		@Override
		public FunctionTypes getFromID(int id) {
			return values()[id & 0xFF];
		}

	}

	public static enum ParameterTypes implements IDType<ParameterTypes> {
		rwType, type, enumSpec, ifSpec, funcSpec, funcReturnSpec, name, width, dims, constant;

		@Override
		public int getID() {
			return ordinal() | 0x200;
		}

		@Override
		public ParameterTypes getFromID(int id) {
			return values()[id & 0x1FF];
		}

	}

	public static enum VariableTypes implements IDType<VariableTypes> {
		name, width, flags, dimensions, annotations;

		@Override
		public int getID() {
			return ordinal() | 0x20;
		}

		@Override
		public VariableTypes getFromID(int id) {
			return values()[id & 0x1F];
		}
	}

	public static enum InternalTypes implements IDType<InternalTypes> {
		bitStart, bitEnd, arrayIdx, flags, varIdx;

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
		final ExecutableInputStream fis = new ExecutableInputStream(new FileInputStream(source));
		final ExecutableModel res = fis.readExecutableModel(verbose);
		fis.close();
		return res;
	}

	public static void writeExecutableModel(long date, ExecutableModel model, File target) throws IOException {
		final ExecutableOutputStream fos = new ExecutableOutputStream(new FileOutputStream(target));
		fos.writeExecutableModel(date, model);
		fos.close();
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		System.out.println("Model types:");
		for (final ModelTypes type : ModelTypes.values()) {
			System.out.printf("0x%02x | %-16s|%n", type.getID(), type.name());
		}
		System.out.println("Frame types:");
		for (final FrameTypes type : FrameTypes.values()) {
			System.out.printf("0x%02x | %-16s|%n", type.getID(), type.name());
		}
		System.out.println("Internal types:");
		for (final InternalTypes type : InternalTypes.values()) {
			System.out.printf("0x%02x | %-16s|%n", type.getID(), type.name());
		}
		System.out.println("Variable types:");
		for (final VariableTypes type : VariableTypes.values()) {
			System.out.printf("0x%02x | %-16s|%n", type.getID(), type.name());
		}
		System.out.println("Function types:");
		for (final FunctionTypes type : FunctionTypes.values()) {
			System.out.printf("0x%02x | %-16s|%n", type.getID(), type.name());
		}
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ExecutableOutputStream os = new ExecutableOutputStream(baos);
		os.writeInt(ModelTypes.date, 512);
		// os.writeIntArray(ModelTypes.date, new int[] { 128 });
		final byte[] res = baos.toByteArray();
		for (final byte b : res) {
			System.out.printf("%02X", b);
		}
		System.out.println();
		final ExecutableInputStream is = new ExecutableInputStream(new ByteArrayInputStream(res));
		System.out.println(is.readVarInt());
		System.out.println(is.readVarInt());
		System.out.println(is.readVarInt());
		// System.out.println(Arrays.toString(is.readIntArray()));
	}
}
