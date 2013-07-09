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

public abstract class EncapsulatedAccess {
	protected final HDLFrameInterpreter intr;
	public final InternalInformation ii;
	private final int accessIndex;
	public int targetAccessIndex = -1;
	public final boolean prev;
	public int offset;
	private final int[] dims;

	public class RegUpdater {
		public final int shadowAccessIdx;
		public final int accessIdx;
		public boolean isBig = ii.info.width > 64;

		public RegUpdater(int shadowAccessIdx, int accessIdx) {
			super();
			// This may be caused by a check on the edge as caused by the #null
			// output for reset frames
			this.shadowAccessIdx = shadowAccessIdx;
			this.accessIdx = accessIdx;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + accessIdx;
			result = (prime * result) + shadowAccessIdx;
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
			final RegUpdater other = (RegUpdater) obj;
			if (accessIdx != other.accessIdx)
				return false;
			if (shadowAccessIdx != other.shadowAccessIdx)
				return false;
			return true;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("RegUpdater [shadowAccessIdx=");
			builder.append(shadowAccessIdx);
			builder.append(", accessIdx=");
			builder.append(accessIdx);
			builder.append("]");
			return builder.toString();
		}

	}

	public EncapsulatedAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation ii, int accessIndex, boolean prev) {
		super();
		this.intr = hdlFrameInterpreter;
		this.accessIndex = accessIndex;
		this.prev = prev;
		this.ii = ii;
		this.dims = ii.info.dimensions.clone();
		if (dims.length > 0) {
			this.dims[dims.length - 1] = 1;
		}
		if (ii.fixedArray) {
			setOffset(ii.arrayIdx);
		}
	}

	public void setLastUpdate(int deltaCycle, int epsCycle) {
		intr.deltaUpdates[getAccessIndex()] = ((long) deltaCycle << 16l) | (epsCycle & 0xFFFF);
	}

	/**
	 * Check whether this register has been updated in this delta / eps cycle.
	 * Returns <code>true</code> when updating this register is not recommended.
	 * 
	 * @param deltaCycle
	 * @param epsCycle
	 * @return
	 */
	public boolean skip(int deltaCycle, int epsCycle) {
		final long local = intr.deltaUpdates[getAccessIndex()];
		final long dc = local >>> 16l;
		// Register was updated in previous delta cylce, that is ok
		if (dc < deltaCycle)
			return false;
		// Register was updated in this delta cycle but it is the same eps,
		// that is ok as well
		if ((dc == deltaCycle) && ((local & 0xFFFF) == epsCycle))
			return false;
		// Don't update
		return true;
	}

	public void setOffset(int... off) {
		offset = 0;
		if (off.length == 0)
			return;
		for (int i = 0; i < dims.length; i++) {
			final int o = off[i];
			offset += o * dims[i];
		}
	}

	/**
	 * Checks whether this data has been updated in this delta cycle
	 * 
	 * @param deltaCycle
	 * @param epsCycle
	 * @return <code>true</code> if it was calculated in this delta cycle,
	 *         <code>false</code> otherwise
	 */
	public boolean isFresh(int deltaCycle, int epsCycle) {
		final long raw = intr.deltaUpdates[getAccessIndex()];
		final boolean dc = (raw >>> 16l) == deltaCycle;
		final boolean ec = (raw & 0xFFFF) == epsCycle;
		return dc && ec;
	}

	public void generateRegupdate() {
		if (targetAccessIndex != -1) {
			intr.addRegUpdate(new RegUpdater(accessIndex + offset, targetAccessIndex + offset));
		}
	}

	public int getAccessIndex() {
		return accessIndex + offset;
	}

	public abstract BigInteger getDataBig();

	public abstract long getDataLong();

	public abstract void setDataLong(long data, int deltaCycle, int epsCycle);

	public abstract void setDataBig(BigInteger data, int deltaCycle, int epsCycle);

	public void fillDataBig(int arrayPos, int[] writeIndex, BigInteger a, int deltaCycle, int epsCycle) {
		int offset = 0;
		for (int i = 0; i < (arrayPos + 1); i++) {
			final int o = writeIndex[i];
			offset += o * dims[i];
		}
		int fill = 1;
		final int[] dims = ii.info.dimensions;
		for (int i = arrayPos + 1; i < dims.length; i++) {
			fill *= dims[i];
		}
		for (int i = offset; i < (offset + fill); i++) {
			this.offset = i;
			setDataBig(a, deltaCycle, epsCycle);
		}
	}

	public void fillDataLong(int arrayPos, int[] writeIndex, long a, int deltaCycle, int epsCycle) {
		int offset = 0;
		for (int i = 0; i < (arrayPos + 1); i++) {
			final int o = writeIndex[i];
			offset += o * dims[i];
		}
		int fill = 1;
		final int[] dims = ii.info.dimensions;
		for (int i = arrayPos + 1; i < dims.length; i++) {
			fill *= dims[i];
		}
		for (int i = offset; i < (offset + fill); i++) {
			this.offset = i;
			setDataLong(a, deltaCycle, epsCycle);
		}
	}

}
