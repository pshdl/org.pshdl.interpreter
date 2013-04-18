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
		public void setDataBig(BigInteger data, int deltaCycle, int epsCycle) {
			long current = intr.storage[accessIndex] & writeMask;
			this.intr.storage[accessIndex] = current | ((data.longValue() & mask) << shift);
			if (name.isPred) {
				this.intr.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
			}
		}

		@Override
		public void setDataLong(long data, int deltaCycle, int epsCycle) {
			long current = intr.storage[accessIndex] & writeMask;
			this.intr.storage[accessIndex] = current | ((data & mask) << shift);
			if (name.isPred) {
				this.intr.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
			}
		}

		@Override
		public BigInteger getDataBig() {
			if (prev)
				return BigInteger.valueOf((intr.storage_prev[accessIndex] >> shift) & mask);
			return BigInteger.valueOf((intr.storage[accessIndex] >> shift) & mask);
		}

		@Override
		public long getDataLong() {
			if (prev)
				return (intr.storage_prev[accessIndex] >> shift) & mask;
			return (intr.storage[accessIndex] >> shift) & mask;
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
