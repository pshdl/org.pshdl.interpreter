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

public class BigAccesses {
	private static final class DirectBigAccess extends EncapsulatedAccess {

		private final HDLFrameInterpreter hdlFrameInterpreter;

		public DirectBigAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.hdlFrameInterpreter = hdlFrameInterpreter;
		}

		@Override
		public void setDataBig(BigInteger data, int deltaCycle, int epsCycle) {
			final int accessIndex = getAccessIndex();
			final BigInteger val = this.hdlFrameInterpreter.big_storage[accessIndex];
			this.hdlFrameInterpreter.big_storage[accessIndex] = data;
			if (ii.isPred) {
				setLastUpdate(deltaCycle, epsCycle);
			}
			if (!val.equals(data)) {
				generateRegupdate();
			}
		}

		@Override
		public void setDataLong(long dataIn, int deltaCycle, int epsCycle) {
			final BigInteger data = BigInteger.valueOf(dataIn);
			setDataBig(data, deltaCycle, epsCycle);
		}

		@Override
		public BigInteger getDataBig() {
			if (prev)
				return this.hdlFrameInterpreter.big_storage_prev[getAccessIndex()];
			return this.hdlFrameInterpreter.big_storage[getAccessIndex()];
		}

		@Override
		public long getDataLong() {
			return getDataBig().longValue();
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("DirectBigAccess [name=").append(ii).append(", accessIndex=").append(getAccessIndex()).append(", prev=").append(prev).append("]");
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
		public void setDataBig(BigInteger data, int deltaCycle, int epsCycle) {
			final int accessIndex = getAccessIndex();
			final BigInteger val = this.hdlFrameInterpreter.big_storage[accessIndex];
			BigInteger newVal;
			if (BigInteger.ZERO.equals(data.and(BigInteger.ONE))) {
				newVal = val.clearBit(bit);
			} else {
				newVal = val.setBit(bit);
			}
			this.hdlFrameInterpreter.big_storage[accessIndex] = newVal;
			if (ii.isPred) {
				setLastUpdate(deltaCycle, epsCycle);
			}
			if (!val.equals(newVal)) {
				generateRegupdate();
			}
		}

		@Override
		public void setDataLong(long dataIn, int deltaCycle, int epsCycle) {
			final BigInteger data = BigInteger.valueOf(dataIn);
			setDataBig(data, deltaCycle, epsCycle);
		}

		@Override
		public BigInteger getDataBig() {
			if (prev)
				return this.hdlFrameInterpreter.big_storage_prev[getAccessIndex()].testBit(bit) ? BigInteger.ONE : BigInteger.ZERO;
			return this.hdlFrameInterpreter.big_storage[getAccessIndex()].testBit(bit) ? BigInteger.ONE : BigInteger.ZERO;
		}

		@Override
		public long getDataLong() {
			return getDataBig().longValue();
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append(ii).append(" SingleBigAccess [bit=").append(bit).append("]");
			return builder.toString();
		}

	}

	private static final class RangeBigAccess extends EncapsulatedAccess {
		private final HDLFrameInterpreter hdlFrameInterpreter;
		private final BigInteger writeMask;
		private final BigInteger mask;
		private final int shift;

		public RangeBigAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
			super(hdlFrameInterpreter, name, accessIndex, prev);
			this.hdlFrameInterpreter = hdlFrameInterpreter;
			this.mask = BigInteger.ONE.shiftLeft(name.actualWidth).subtract(BigInteger.ONE);
			this.shift = name.bitEnd;
			this.writeMask = BigInteger.ZERO.setBit(name.actualWidth);
		}

		@Override
		public void setDataBig(BigInteger data, int deltaCycle, int epsCycle) {
			final int accessIndex = getAccessIndex();
			final BigInteger initial = this.hdlFrameInterpreter.big_storage[accessIndex];
			final BigInteger current = initial.and(writeMask);
			final BigInteger newVal = current.or(data.and(mask).shiftLeft(shift));
			this.hdlFrameInterpreter.big_storage[accessIndex] = newVal;
			if (ii.isPred) {
				setLastUpdate(deltaCycle, epsCycle);
			}
			if (!initial.equals(newVal)) {
				generateRegupdate();
			}
		}

		@Override
		public void setDataLong(long dataIn, int deltaCycle, int epsCycle) {
			final BigInteger data = BigInteger.valueOf(dataIn);
			setDataBig(data, deltaCycle, epsCycle);
		}

		@Override
		public BigInteger getDataBig() {
			if (prev)
				return (this.hdlFrameInterpreter.big_storage_prev[getAccessIndex()].shiftRight(shift)).and(mask);
			return (this.hdlFrameInterpreter.big_storage[getAccessIndex()].shiftRight(shift)).and(mask);
		}

		@Override
		public long getDataLong() {
			return getDataBig().longValue();
		}

		@Override
		public String toString() {
			return "RangeBigAccess [writeMask=" + writeMask + ", mask=" + mask + ", shift=" + shift + "]";
		}

	}

	public static EncapsulatedAccess getInternal(InternalInformation ii, int accessIndex, boolean prev, HDLFrameInterpreter interpreter) {
		if (ii.actualWidth == ii.info.width)
			return new DirectBigAccess(interpreter, ii, accessIndex, prev);
		if (ii.actualWidth == 1)
			return new SingleBigAccess(interpreter, ii, accessIndex, prev);
		return new RangeBigAccess(interpreter, ii, accessIndex, prev);
	}
}
