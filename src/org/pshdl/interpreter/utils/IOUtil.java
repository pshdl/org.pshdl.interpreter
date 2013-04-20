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

public class IOUtil {
	public static final int PRED_FLAG = 0x01;
	public static final int REG_FLAG = 0x02;
	public static final int IN_FLAG = 0x4;
	public static final int OUT_FLAG = 0x8;
	public static final int IO_FLAG = IN_FLAG | OUT_FLAG;
	public static final int INT_FLAG = 0x10;
	public static final int UINT_FLAG = 0x20;

	public static interface IDType<T extends Enum<T>> {
		public int getID();

		public T getFromID(int id);
	}

	public static enum ModelTypes implements IDType<ModelTypes> {
		version, src, date, maxDataWidth, maxStackDepth, internal, frame, variable;

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

	public static enum VariableTypes implements IDType<VariableTypes> {
		name, width, flags, dimensions;

		@Override
		public int getID() {
			return ordinal() | 0x40;
		}

		@Override
		public VariableTypes getFromID(int id) {
			return values()[id & 0x3F];
		}
	}

	public static enum InternalTypes implements IDType<InternalTypes> {
		bitStart, bitEnd, flags, arrayStart, arrayEnd, varIdx;

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
			System.out.printf("0x%02x | %-15s\n", type.getID(), type.name());
		}
		System.out.println("Frame types:");
		for (FrameTypes type : FrameTypes.values()) {
			System.out.printf("0x%02x | %-15s\n", type.getID(), type.name());
		}
		System.out.println("Internal types:");
		for (InternalTypes type : InternalTypes.values()) {
			System.out.printf("0x%02x | %-15s\n", type.getID(), type.name());
		}
	}
}
