package org.pshdl.interpreter.access;

import java.math.*;

import org.pshdl.interpreter.*;

public class BigAccesses {
	private static final class DirectBigAccess extends EncapsulatedAccess {

		private final HDLFrameInterpreter hdlFrameInterpreter;

		public DirectBigAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.hdlFrameInterpreter = hdlFrameInterpreter;
		}

		@Override
		public void setData(BigInteger data, int deltaCycle, int epsCycle) {
			this.hdlFrameInterpreter.big_storage[accessIndex] = data;
			if (name.isPred) {
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
			builder.append("DirectBigAccess [hdlFrameInterpreter=").append(hdlFrameInterpreter).append(", name=").append(name).append(", accessIndex=").append(accessIndex)
					.append(", prev=").append(prev).append("]");
			return builder.toString();
		}

	}

	private static final class SingleBigAccess extends EncapsulatedAccess {

		private final HDLFrameInterpreter hdlFrameInterpreter;
		private final int bit;

		public SingleBigAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.hdlFrameInterpreter = hdlFrameInterpreter;
			this.bit = name.bitEnd == -1 ? 0 : name.bitEnd;
		}

		@Override
		public void setData(BigInteger data, int deltaCycle, int epsCycle) {
			BigInteger initial = this.hdlFrameInterpreter.big_storage[accessIndex];
			if (BigInteger.ZERO.equals(data.and(BigInteger.ONE))) {
				this.hdlFrameInterpreter.big_storage[accessIndex] = initial.clearBit(bit);
			} else {
				this.hdlFrameInterpreter.big_storage[accessIndex] = initial.setBit(bit);
			}
			if (name.isPred) {
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

	private static final class RangeBigAccess extends EncapsulatedAccess {
		private final HDLFrameInterpreter hdlFrameInterpreter;
		private BigInteger writeMask;
		private BigInteger mask;
		private int shift;

		public RangeBigAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.hdlFrameInterpreter = hdlFrameInterpreter;
			this.mask = BigInteger.ONE.shiftLeft(name.actualWidth).subtract(BigInteger.ONE);
			this.shift = name.bitEnd;
			this.writeMask = BigInteger.ZERO.setBit(name.actualWidth);
		}

		@Override
		public void setData(BigInteger data, int deltaCycle, int epsCycle) {
			BigInteger initial = this.hdlFrameInterpreter.big_storage[accessIndex];
			BigInteger current = initial.and(writeMask);
			this.hdlFrameInterpreter.big_storage[accessIndex] = current.or(data.and(mask).shiftLeft(shift));
			if (name.isPred) {
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
				return (this.hdlFrameInterpreter.big_storage_prev[accessIndex].shiftRight(shift)).and(mask);
			return (this.hdlFrameInterpreter.big_storage[accessIndex].shiftRight(shift)).and(mask);
		}

		@Override
		public long getDataLong() {
			return getDataBig().longValue();
		}

	}

	public static EncapsulatedAccess getInternal(InternalInformation ii, int accessIndex, boolean prev, HDLFrameInterpreter interpreter) {
		if (ii.actualWidth == ii.baseWidth)
			return new DirectBigAccess(interpreter, ii, accessIndex, prev);
		if (ii.actualWidth == 1)
			return new SingleBigAccess(interpreter, ii, accessIndex, prev);
		return new RangeBigAccess(interpreter, ii, accessIndex, prev);
	}
}
