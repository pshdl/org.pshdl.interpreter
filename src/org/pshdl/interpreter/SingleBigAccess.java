package org.pshdl.interpreter;

import java.math.*;

public class SingleBigAccess extends EncapsulatedAccess {

	/**
	 * 
	 */
	private final HDLFrameInterpreter hdlFrameInterpreter;
	private int bit;

	public SingleBigAccess(HDLFrameInterpreter hdlFrameInterpreter, String name, int accessIndex, boolean prev) {
		super(hdlFrameInterpreter, name, accessIndex, prev);
		this.hdlFrameInterpreter = hdlFrameInterpreter;
		this.bit = bitEnd == -1 ? 0 : bitEnd;
	}

	@Override
	public void setData(BigInteger data, int deltaCycle, int epsCycle) {
		BigInteger initial = this.hdlFrameInterpreter.big_storage[accessIndex];
		if (BigInteger.ZERO.equals(data.and(BigInteger.ONE))) {
			this.hdlFrameInterpreter.big_storage[accessIndex] = initial.clearBit(bit);
		} else {
			this.hdlFrameInterpreter.big_storage[accessIndex] = initial.setBit(bit);
		}
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
			return this.hdlFrameInterpreter.big_storage_prev[accessIndex].testBit(bit) ? BigInteger.ONE : BigInteger.ZERO;
		return this.hdlFrameInterpreter.big_storage[accessIndex].testBit(bit) ? BigInteger.ONE : BigInteger.ZERO;
	}

	@Override
	public long getDataLong() {
		return getDataBig().longValue();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(name).append(" SingleBigAccess [bit=").append(bit).append("]");
		return builder.toString();
	}

}