package org.pshdl.interpreter;

import java.math.*;

public class LongAccess extends EncapsulatedAccess {
	/**
	 * 
	 */
	private final HDLFrameInterpreter intr;
	public final int shift;
	public final long mask;
	public final long writeMask;

	public LongAccess(HDLFrameInterpreter hdlFrameInterpreter, String name, int accessIndex, boolean prev) {
		super(hdlFrameInterpreter, name, accessIndex, prev);
		this.intr = hdlFrameInterpreter;
		if ((bitStart == -1) && (bitEnd == -1)) {
			int width = this.intr.model.getWidth(name);
			this.shift = 0;
			if (width == 64) {
				this.mask = 0xFFFFFFFFFFFFFFFFL;
			} else {
				this.mask = (1l << width) - 1;
			}
			this.writeMask = 0;
		} else if (bitEnd != bitStart) {
			int actualWidth = (bitStart - bitEnd) + 1;
			this.shift = bitEnd;
			this.mask = (1l << actualWidth) - 1;
			this.writeMask = ~(mask << shift);
		} else {
			this.shift = bitStart;
			this.mask = 1;
			this.writeMask = ~(mask << shift);
		}
	}

	@Override
	public void setData(BigInteger data, int deltaCycle, int epsCycle) {
		long initial;
		initial = this.intr.storage[accessIndex];
		long current = initial & writeMask;
		this.intr.storage[accessIndex] = current | ((data.longValue() & mask) << shift);
		if (isPredicate) {
			this.intr.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
		}
	}

	@Override
	public void setData(long data, int deltaCycle, int epsCycle) {
		long initial;
		initial = this.intr.storage[accessIndex];
		long current = initial & writeMask;
		this.intr.storage[accessIndex] = current | ((data & mask) << shift);
		if (isPredicate) {
			this.intr.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
		}
	}

	@Override
	public BigInteger getDataBig() {
		if (prev)
			return BigInteger.valueOf((this.intr.storage_prev[accessIndex] >> shift) & mask);
		return BigInteger.valueOf((this.intr.storage[accessIndex] >> shift) & mask);
	}

	@Override
	public long getDataLong() {
		if (prev)
			return (this.intr.storage_prev[accessIndex] >> shift) & mask;
		return (this.intr.storage[accessIndex] >> shift) & mask;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EncapsulatedAccess [shift=").append(shift).append(", mask=").append(mask).append(", writeMask=").append(Long.toHexString(writeMask)).append(", name=")
				.append(name).append(", accessIndex=").append(accessIndex).append("]");
		return builder.toString();
	}

}