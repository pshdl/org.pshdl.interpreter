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
package org.pshdl.interpreter;

import java.io.*;
import java.util.*;

public class VariableInformation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7590745375562877673L;

	public static enum Direction {
		IN, INOUT, OUT, INTERNAL
	}

	public static enum Type {
		UINT, INT, BIT
	}

	public final Direction dir;
	public final String name;
	public final int width;
	public final boolean isRegister;
	public final Type type;
	public final int[] dimensions;
	public final boolean isClock;
	public final boolean isReset;
	public final String[] annotations;

	public int readCount = 0, writeCount = 0;

	public VariableInformation(Direction dir, String name, int width, Type type, boolean isRegister, boolean isClock, boolean isReset, String[] annotations, int... dimensions) {
		super();
		this.isClock = isClock;
		this.isReset = isReset;
		this.dir = dir;
		this.name = name;
		this.width = width;
		this.type = type;
		this.isRegister = isRegister;
		this.dimensions = dimensions;
		this.annotations = annotations;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final VariableInformation other = (VariableInformation) obj;
		if (!Arrays.equals(dimensions, other.dimensions))
			return false;
		if (dir != other.dir)
			return false;
		if (isRegister != other.isRegister)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		if (width != other.width)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + Arrays.hashCode(dimensions);
		result = (prime * result) + ((dir == null) ? 0 : dir.hashCode());
		result = (prime * result) + (isRegister ? 1231 : 1237);
		result = (prime * result) + ((name == null) ? 0 : name.hashCode());
		result = (prime * result) + ((type == null) ? 0 : type.hashCode());
		result = (prime * result) + width;
		return result;
	}

	@Override
	public String toString() {
		return "VariableInformation [dir=" + dir + ", name=" + name + ", width=" + width + ", isRegister=" + isRegister + ", type=" + type + ", dimensions="
				+ Arrays.toString(dimensions) + "]";
	}
}
