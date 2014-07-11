package org.pshdl.interpreter;

public class RegUpdate {
	public final int internalID;
	public final int offset;
	public int fillValue;

	public RegUpdate(int internalID, int offset) {
		super();
		this.internalID = internalID;
		this.offset = offset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + internalID;
		result = (prime * result) + offset;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final RegUpdate other = (RegUpdate) obj;
		if (internalID != other.internalID)
			return false;
		if (offset != other.offset)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return internalID + ":" + offset;
	}

}