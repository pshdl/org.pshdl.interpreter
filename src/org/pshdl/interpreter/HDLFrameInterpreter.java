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
package org.pshdl.interpreter;

import java.math.*;
import java.util.*;
import java.util.Map.Entry;

import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.frames.*;

public final class HDLFrameInterpreter {
	private static final int BIG_MARKER = 0x80000000;
	private static final int BIG_MASK = BIG_MARKER - 1;

	public final ExecutableModel model;

	public final long storage[], storage_prev[];
	public final BigInteger big_storage[], big_storage_prev[];

	private final EncapsulatedAccess[] internals, internals_prev;
	private final int[] regIndex, regIndexTarget;
	private final int[] widths;
	private final Map<String, Integer> idx = new TreeMap<String, Integer>();
	private int deltaCycle = 0;

	private boolean printing;

	public final long[] deltaUpdates;
	private final ExecutableFrame frames[];

	public HDLFrameInterpreter(ExecutableModel model, boolean printing) {
		this.printing = printing;
		this.model = model;
		int currentIdx = 0;
		this.internals = new EncapsulatedAccess[model.internals.length];
		this.internals_prev = new EncapsulatedAccess[model.internals.length];
		this.widths = new int[internals.length];
		for (int i = 0; i < model.internals.length; i++) {
			InternalInformation ii = model.internals[i];
			Integer accessIndex = idx.get(ii.baseNameWithReg());
			if (accessIndex == null) {
				if ((ii.baseWidth > 64)) {
					accessIndex = currentIdx++ | BIG_MARKER;
				} else {
					accessIndex = currentIdx++;
				}
				idx.put(ii.baseNameWithReg(), accessIndex);
			}
			widths[i] = ii.baseWidth;
			if (((accessIndex & BIG_MARKER) == BIG_MARKER)) {
				internals[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, false, this);
				internals_prev[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, true, this);
			} else {
				internals[i] = LongAccesses.getInternal(ii, accessIndex, false, this);
				internals_prev[i] = LongAccesses.getInternal(ii, accessIndex, true, this);
			}
		}
		regIndex = new int[model.registerOutputs.length];
		regIndexTarget = new int[model.registerOutputs.length];
		for (int i = 0; i < model.registerOutputs.length; i++) {
			int ridx = model.registerOutputs[i];
			InternalInformation name = model.internals[ridx];
			regIndex[i] = idx.get(name.baseNameWithReg());
			regIndexTarget[i] = idx.get(name.baseName);
		}
		storage = new long[currentIdx];
		storage_prev = new long[currentIdx];
		big_storage = new BigInteger[currentIdx];
		big_storage_prev = new BigInteger[currentIdx];
		for (int i = 0; i < big_storage.length; i++) {
			big_storage[i] = BigInteger.ZERO;
			big_storage_prev[i] = BigInteger.ZERO;
		}
		deltaUpdates = new long[currentIdx];
		Frame[] frames = model.frames;
		this.frames = new ExecutableFrame[frames.length];
		for (int i = 0; i < frames.length; i++) {
			if ((frames[i].maxDataWidth > 64)) {
				// System.out.println("HDLFrameInterpreter.HDLFrameInterpreter() Using BigFrame for:"
				// + frames[i].uniqueID);
				this.frames[i] = new BigIntegerFrame(frames[i], printing, internals, internals_prev);
			} else {
				this.frames[i] = new LongFrame(frames[i], printing, internals, internals_prev);
			}
		}
	}

	public void setInput(String name, BigInteger value) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		if ((integer & BIG_MARKER) == BIG_MARKER) {
			big_storage[integer & BIG_MASK] = value;
		} else {
			storage[integer] = value.longValue();
		}
	}

	public void setInput(String name, long value) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		setInput(integer, value);
	}

	public void setInput(int idx, long value) {
		if ((idx & BIG_MARKER) == BIG_MARKER) {
			big_storage[idx & BIG_MASK] = BigInteger.valueOf(value);
		} else {
			storage[idx] = value;
		}
	}

	public int getIndex(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name + " valid names are:" + idx.keySet());
		return integer;
	}

	public long getOutputLong(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		return getOutputLong(integer);
	}

	public long getOutputLong(int idx) {
		long res;
		int width;
		if ((idx & BIG_MARKER) == BIG_MARKER) {
			res = big_storage[idx & BIG_MASK].longValue();
			width = widths[idx & BIG_MASK];
		} else {
			res = storage[idx];
			width = widths[idx];
		}
		if (width < 64) {
			long mask = (1l << width) - 1;
			return res & mask;
		}
		return res;
	}

	public BigInteger getOutputBig(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		return getOutputBig(integer);
	}

	public BigInteger getOutputBig(int idx) {
		BigInteger res;
		if ((idx & BIG_MARKER) == BIG_MARKER) {
			res = big_storage[idx & BIG_MASK];
		}
		res = BigInteger.valueOf(storage[idx]);
		int width = widths[idx & BIG_MASK];
		BigInteger mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
		return res.and(mask);
	}

	/*
	 * SignedCastTest.main() cast (int<8>) -2 to (uint<16>) : FFFE
	 * SignedCastTest.main() cast (int<8>) -2 to (int<16>) : FFFE
	 * SignedCastTest.main() cast (int<16>) -255 to (int<8>) : 01
	 * SignedCastTest.main() cast (int<16>) -255 to (int<32>) : FFFFFF01
	 * SignedCastTest.main() cast (uint<16>) 65535 to (int<32>) : 0000FFFF
	 */

	public void run() {
		boolean regUpdated = false;
		deltaCycle++;
		int epsCycle = 0;
		do {
			epsCycle++;
			if (printing) {
				System.out.println("Starting cylce:" + deltaCycle + "." + epsCycle);
			}
			regUpdated = false;
			for (ExecutableFrame ef : frames) {
				if (printing) {
					System.out.println("\tExecuting frame:" + ef.uniqueID);
				}
				ef.execute(deltaCycle, epsCycle);
				if (ef.regUpdated) {
					regUpdated = true;
				}
			}
			if (regUpdated) {
				for (int i = 0; i < regIndex.length; i++) {
					int j = regIndex[i];
					if ((j & BIG_MARKER) == BIG_MARKER) {
						big_storage[regIndexTarget[i] & BIG_MASK] = big_storage[j & BIG_MASK];
					} else {
						storage[regIndexTarget[i]] = storage[j];
					}
				}
			}
		} while (regUpdated);
		System.arraycopy(storage, 0, storage_prev, 0, storage.length);
		System.arraycopy(big_storage, 0, big_storage_prev, 0, big_storage.length);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Current Cycle:" + deltaCycle + "\n");
		for (Entry<String, Integer> e : idx.entrySet()) {
			sb.append('\t').append(e.getKey()).append("=").append(storage[e.getValue()]).append('\n');
		}
		return sb.toString();
	}
}
