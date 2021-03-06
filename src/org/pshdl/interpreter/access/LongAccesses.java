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
package org.pshdl.interpreter.access;

import java.math.BigInteger;

import org.pshdl.interpreter.HDLFrameInterpreter;
import org.pshdl.interpreter.InternalInformation;
import org.pshdl.interpreter.VariableInformation.Type;

public class LongAccesses {

	private static final class LongAccess extends EncapsulatedAccess {
		/**
		 *
		 */
		private final HDLFrameInterpreter intr;
		public final int signShift;
		public int shift;
		public final long mask;
		public long writeMask;
		private final boolean isDynamicBit;

		public LongAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.intr = hdlFrameInterpreter;
			if ((name.actualWidth != name.info.width) || (name.info.type != Type.INT)) {
				signShift = 0;
			} else {
				signShift = 64 - name.actualWidth;
			}
			isDynamicBit = name.bitEnd == -1;
			if ((name.bitStart == InternalInformation.undefinedBit) && (name.bitEnd == InternalInformation.undefinedBit)) {
				final int width = name.info.width;
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
				final int actualWidth = (name.bitStart - name.bitEnd) + 1;
				if (actualWidth > 64)
					throw new IllegalArgumentException("Unsupported bitWidth:" + actualWidth);
				this.shift = name.bitEnd;
				if (actualWidth == 64) {
					this.mask = 0xFFFFFFFFFFFFFFFFL;
					this.writeMask = 0;
				} else {
					this.mask = (1l << actualWidth) - 1;
					this.writeMask = ~(mask << shift);
				}
			} else {
				this.shift = name.bitStart;
				this.mask = 1;
				this.writeMask = ~(mask << shift);
			}
		}

		@Override
		public void setDataBig(BigInteger data, int deltaCycle, int epsCycle) {
			final int accessIndex = getAccessIndex();
			final long val = intr.storage[accessIndex];
			final long current = val & writeMask;
			final long newVal = current | ((data.longValue() & mask) << shift);
			this.intr.storage[accessIndex] = newVal;
			if (ii.isPred) {
				setLastUpdate(deltaCycle, epsCycle);
			}
			if (newVal != val) {
				generateRegupdate();
			}
		}

		@Override
		public void setDataLong(long data, int deltaCycle, int epsCycle) {
			final int accessIndex = getAccessIndex();
			final long val = intr.storage[accessIndex];
			final long current = val & writeMask;
			final long newVal = current | ((data & mask) << shift);
			this.intr.storage[accessIndex] = newVal;
			if (ii.isPred) {
				setLastUpdate(deltaCycle, epsCycle);
			}
			if (newVal != val) {
				generateRegupdate();
			}
		}

		@Override
		public BigInteger getDataBig() {
			return BigInteger.valueOf(getDataLong());
		}

		@Override
		public long getDataLong() {
			final int accessIndex = getAccessIndex();
			final long rawVal;
			if (prev) {
				rawVal = intr.storage_prev[accessIndex];
			} else {
				rawVal = intr.storage[accessIndex];
			}
			return (((rawVal >> shift) & mask) << signShift) >> signShift;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("LongAccess [shift=").append(shift).append(", mask=").append(Long.toHexString(mask)).append(", writeMask=").append(Long.toHexString(writeMask))
					.append(", name=").append(ii).append(", accessIndex=").append(getAccessIndex()).append(", prev=").append(prev).append("]");
			return builder.toString();
		}

		@Override
		public void setBitOffset(int bitOffset) {
			if (isDynamicBit) {
				this.shift = bitOffset;
				this.writeMask = ~(mask << shift);
			}
		}

	}

	public static EncapsulatedAccess getInternal(InternalInformation ii, int accessIndex, boolean prev, HDLFrameInterpreter interpreter) {
		return new LongAccess(interpreter, ii, accessIndex, prev);
	}
}
