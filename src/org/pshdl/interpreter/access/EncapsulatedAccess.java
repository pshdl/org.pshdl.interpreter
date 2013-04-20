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
			this.shadowAccessIdx = shadowAccessIdx;
			this.accessIdx = accessIdx;
		}
	}

	public EncapsulatedAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation ii, int accessIndex, boolean prev) {
		super();
		this.intr = hdlFrameInterpreter;
		this.accessIndex = accessIndex;
		this.prev = prev;
		this.ii = ii;
		this.dims = ii.info.dimensions;
		if (dims.length > 0) {
			this.dims[dims.length - 1] = 1;
		}
		if (ii.fixedArray) {
			setOffset(ii.arrayStart);
		}
	}

	public void setLastUpdate(int deltaCycle, int epsCycle) {
		intr.deltaUpdates[getAccessIndex()] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
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
		long local = intr.deltaUpdates[getAccessIndex()];
		long dc = local >>> 16;
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
			int o = off[i];
			offset += o * dims[i];
		}
	}

	/**
	 * Checks whether this data has been updated in this delta cycle
	 * 
	 * @param deltaCycle
	 * @return <code>true</code> if it was calculated in this delta cycle,
	 *         <code>false</code> otherwise
	 */
	public boolean isFresh(int deltaCycle) {
		return (intr.deltaUpdates[getAccessIndex()] >>> 16) == deltaCycle;
	}

	public abstract BigInteger getDataBig();

	public abstract long getDataLong();

	public abstract void setDataLong(long data, int deltaCycle, int epsCycle);

	public abstract void setDataBig(BigInteger data, int deltaCycle, int epsCycle);

	public RegUpdater getRegUpdater() {
		return new RegUpdater(getAccessIndex(), targetAccessIndex + offset);
	}

	public int getAccessIndex() {
		return accessIndex + offset;
	}

}
