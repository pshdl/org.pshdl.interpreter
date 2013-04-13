package org.pshdl.interpreter;

import java.math.*;

public class DirectBigAccess extends EncapsulatedAccess {

	/**
	 * 
	 */
	private final HDLFrameInterpreter hdlFrameInterpreter;

	public DirectBigAccess(HDLFrameInterpreter hdlFrameInterpreter, String name, int accessIndex, boolean prev, int width) {
		super(hdlFrameInterpreter, name, accessIndex, prev);
		this.hdlFrameInterpreter = hdlFrameInterpreter;
	}

	@Override
	public void setData(BigInteger data, int deltaCycle, int epsCycle) {
		this.hdlFrameInterpreter.big_storage[accessIndex] = data;
		if (isPredicate) {
			this.hdlFrameInterpreter.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
		}
	}

	@Override
	public void setData(long dataIn, int deltaCycle, int epsCycle) {
		BigInteger data = BigInteger.valueOf(dataIn);
		setData(data, deltaCycle, epsCycle);
	}

	@Override
	public BigInteger getDataBig() {
		if (prev)
			return this.hdlFrameInterpreter.big_storage_prev[accessIndex];
		return this.hdlFrameInterpreter.big_storage[accessIndex];
	}

	@Override
	public long getDataLong() {
		return getDataBig().longValue();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DirectBigAccess");
		return builder.toString();
	}

}