package org.pshdl.interpreter;

import java.util.*;

public class VariableInformation {
	public static enum Direction {
		IN, INOUT, OUT, INTERNAL
	}

	public final Direction dir;
	public final String name;
	public final int width;
	public final boolean isRegister;

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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableInformation other = (VariableInformation) obj;
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

	public static enum Type {
		UINT, INT, BIT
	}

	public final Type type;
	public final int[] dimensions;

	public VariableInformation(Direction dir, String name, int width, Type type, boolean isRegister, int... dimensions) {
		super();
		this.dir = dir;
		this.name = name;
		this.width = width;
		this.type = type;
		this.isRegister = isRegister;
		this.dimensions = dimensions;
	}
}
