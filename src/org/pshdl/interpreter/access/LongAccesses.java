package org.pshdl.interpreter.access;

import java.math.*;

import org.pshdl.interpreter.*;

public class LongAccesses {
	private static final class LongAccess extends EncapsulatedAccess {
		/**
		 * 
		 */
		private final HDLFrameInterpreter intr;
		public final int shift;
		public final long mask;
		public final long writeMask;

		public LongAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.intr = hdlFrameInterpreter;
			if ((name.bitStart == -1) && (name.bitEnd == -1)) {
				int width = name.baseWidth;
				if (width > 64)
					throw new IllegalArgumentException("Unsupported bitWidth:" + width);
				this.shift = 0;
				if (width == 64) {
					this.mask = 0xFFFFFFFFFFFFFFFFL;
				} else {
					this.mask = (1l << width) - 1;
				}
				this.writeMask = 0;
			} else if (name.bitEnd != name.bitStart) {
				int actualWidth = (name.bitStart - name.bitEnd) + 1;
				if (actualWidth > 64)
					throw new IllegalArgumentException("Unsupported bitWidth:" + actualWidth);
				this.shift = name.bitEnd;
				this.mask = (1l << actualWidth) - 1;
				this.writeMask = ~(mask << shift);
			} else {
				this.shift = name.bitStart;
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
			if (name.isPred) {
				this.intr.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
			}
		}

		@Override
		public void setData(long data, int deltaCycle, int epsCycle) {
			long initial;
			initial = this.intr.storage[accessIndex];
			long current = initial & writeMask;
			this.intr.storage[accessIndex] = current | ((data & mask) << shift);
			if (name.isPred) {
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
			builder.append("LongAccess [intr=").append(intr).append(", shift=").append(shift).append(", mask=").append(mask).append(", writeMask=").append(writeMask)
					.append(", name=").append(name).append(", accessIndex=").append(accessIndex).append(", prev=").append(prev).append("]");
			return builder.toString();
		}

	}

	public static EncapsulatedAccess getInternal(InternalInformation ii, int accessIndex, boolean prev, HDLFrameInterpreter interpreter) {
		return new LongAccess(interpreter, ii, accessIndex, prev);
	}
}
