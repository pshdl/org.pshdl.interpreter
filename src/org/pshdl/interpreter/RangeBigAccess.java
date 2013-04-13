package org.pshdl.interpreter;

import java.math.*;

public class RangeBigAccess extends EncapsulatedAccess {
	/**
	 * 
	 */
	private final HDLFrameInterpreter hdlFrameInterpreter;
	private final int width;
	private BigInteger writeMask;
	private BigInteger mask;
	private int shift;

	public RangeBigAccess(HDLFrameInterpreter hdlFrameInterpreter, String name, int accessIndex, boolean prev, int width) {
		super(hdlFrameInterpreter, name, accessIndex, prev);
		this.hdlFrameInterpreter = hdlFrameInterpreter;
		this.width = width;
		this.mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
		this.shift = bitEnd;
		this.writeMask = BigInteger.ZERO.setBit(width);
	}

	@Override
	public void setData(BigInteger data, int deltaCycle, int epsCycle) {
		BigInteger initial = this.hdlFrameInterpreter.big_storage[accessIndex];
		BigInteger current = initial.and(writeMask);
		this.hdlFrameInterpreter.big_storage[accessIndex] = current.or(data.and(mask).shiftLeft(shift)).setBit(width);
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
			return (this.hdlFrameInterpreter.big_storage_prev[accessIndex].shiftLeft(shift)).and(mask);
		return (this.hdlFrameInterpreter.big_storage[accessIndex].shiftLeft(shift)).and(mask);
	}

	@Override
	public long getDataLong() {
		return getDataBig().longValue();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RangeBigAccess [width=").append(width).append(", writeMask=").append(writeMask.toString(16)).append(", mask=").append(mask.toString(16)).append(", shift=")
				.append(shift).append("]");
		return builder.toString();
	}

}