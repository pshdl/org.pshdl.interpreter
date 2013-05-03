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
import org.pshdl.interpreter.access.EncapsulatedAccess.RegUpdater;
import org.pshdl.interpreter.frames.*;

public final class HDLFrameInterpreter {
	/**
	 * Marker that an accessIndex is of kind BigInteger. The accessIndex is
	 * binary ored with this
	 */
	private static final int BIG_MARKER = 0x80000000;
	/**
	 * A mask to convert to get the real accessIndex if it is ored with
	 * BIG_MARKER
	 */
	private static final int BIG_MASK = BIG_MARKER - 1;

	/**
	 * The model to execute
	 */
	public final ExecutableModel model;

	/**
	 * The storage for internals that have a width <= 64
	 */
	public final long storage[], storage_prev[];
	/**
	 * The storage for internals that have a width >=64;
	 */
	public final BigInteger big_storage[], big_storage_prev[];

	/**
	 * The {@link EncapsulatedAccess} for each accessIndex
	 */
	private final EncapsulatedAccess[] internals, internals_prev;

	/**
	 * An {@link EncapsulatedAccess} for the full variable width of a variable
	 */
	private final EncapsulatedAccess[] full;

	/**
	 * A mapping from baseName of internal to the accessIndex (for accessing
	 * {@link #internals} or {@link #internals_prev})
	 */
	private final Map<String, Integer> accessIdxMap = new TreeMap<String, Integer>();

	/**
	 * A mapping from baseName to the index in {@link #full}
	 */
	private final Map<String, Integer> varIdxMap = new TreeMap<String, Integer>();

	/**
	 * The current simulation deltaCycle. That is, how often the run method was
	 * called
	 */
	private int deltaCycle = 0;

	/**
	 * If <code>true</code> debug information are printed
	 */
	public boolean printing;

	/**
	 * An index when a certain internal was last updated
	 */
	public final long[] deltaUpdates;

	/**
	 * The frames that get executed
	 */
	private final ExecutableFrame frames[];

	public HDLFrameInterpreter(ExecutableModel model, boolean printing) {
		this.printing = printing;
		this.model = model;
		int currentIdx = 0;
		this.internals = new EncapsulatedAccess[model.internals.length];
		this.internals_prev = new EncapsulatedAccess[model.internals.length];
		this.full = new EncapsulatedAccess[model.variables.length];
		currentIdx = createInternals(model, currentIdx);
		createVarIndex(model);
		this.storage = new long[currentIdx];
		this.storage_prev = new long[currentIdx];
		this.big_storage = new BigInteger[currentIdx];
		this.big_storage_prev = new BigInteger[currentIdx];
		for (int i = 0; i < big_storage.length; i++) {
			big_storage[i] = BigInteger.ZERO;
			big_storage_prev[i] = BigInteger.ZERO;
		}
		deltaUpdates = new long[currentIdx];
		Frame[] frames = model.frames;
		this.frames = new ExecutableFrame[frames.length];
		for (int i = 0; i < frames.length; i++) {
			if (frames[i].maxDataWidth > 64) {
				this.frames[i] = new BigIntegerFrame(this, frames[i], internals, internals_prev);
			} else {
				this.frames[i] = new LongFrame(this, frames[i], internals, internals_prev);
			}
		}
	}

	private void createVarIndex(ExecutableModel model) {
		for (int i = 0; i < model.variables.length; i++) {
			VariableInformation vi = model.variables[i];
			Integer accessIndex = accessIdxMap.get(vi.name);
			if (accessIndex != null) {
				if (vi.width > 64) {
					full[i] = BigAccesses.getInternal(new InternalInformation(vi.name, vi), accessIndex & BIG_MASK, false, this);
				} else {
					full[i] = LongAccesses.getInternal(new InternalInformation(vi.name, vi), accessIndex, false, this);
				}
				varIdxMap.put(vi.name, i);
			}
		}
	}

	private int createInternals(ExecutableModel model, int currentIdx) {
		for (int i = 0; i < model.internals.length; i++) {
			InternalInformation ii = model.internals[i];
			// System.out.println("HDLFrameInterpreter.createInternals()" + ii);
			String baseName = ii.baseName(false, true);
			Integer accessIndex = accessIdxMap.get(baseName);
			if (accessIndex == null) {
				if ((ii.info.width > 64)) {
					accessIndex = currentIdx | BIG_MARKER;
				} else {
					accessIndex = currentIdx;
				}
				int size = 1;
				for (int d : ii.info.dimensions) {
					size *= d;
				}
				currentIdx += size;
				// System.out.println("HDLFrameInterpreter.createInternals()Allocating:"
				// + size + " for " + baseName);
				accessIdxMap.put(baseName, accessIndex);
			}
			if (((accessIndex & BIG_MARKER) == BIG_MARKER)) {
				internals[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, false, this);
				internals_prev[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, true, this);
			} else {
				internals[i] = LongAccesses.getInternal(ii, accessIndex, false, this);
				internals_prev[i] = LongAccesses.getInternal(ii, accessIndex, true, this);
			}
		}
		for (EncapsulatedAccess ea : internals) {
			if (ea.ii.isShadowReg) {
				ea.targetAccessIndex = accessIdxMap.get(ea.ii.baseName(false, false));
			}
		}
		return currentIdx;
	}

	public void setInput(String name, BigInteger value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	public void setInput(int idx, BigInteger value, int... arrayIdx) {
		EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		acc.setDataBig(value, deltaCycle, 0);
	}

	public void setInput(String name, long value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	public void setInput(int idx, long value, int... arrayIdx) {
		EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		acc.setDataLong(value, deltaCycle, 0);
	}

	public int getIndex(String name) {
		Integer integer = varIdxMap.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name + " valid names are:" + accessIdxMap.keySet());
		return integer;
	}

	public long getOutputLong(String name, int... arrayIdx) {
		return getOutputLong(getIndex(name), arrayIdx);
	}

	public long getOutputLong(int idx, int... arrayIdx) {
		EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		return acc.getDataLong();
	}

	public BigInteger getOutputBig(String name, int... arrayIdx) {
		return getOutputBig(getIndex(name), arrayIdx);
	}

	public BigInteger getOutputBig(int idx, int... arrayIdx) {
		EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		return acc.getDataBig();
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
		List<RegUpdater> updatedRegs = new ArrayList<EncapsulatedAccess.RegUpdater>();
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
					updatedRegs.add(ef.outputAccess.getRegUpdater());
					regUpdated = true;
				}
			}
			if (regUpdated) {
				for (RegUpdater ea : updatedRegs) {
					if (printing) {
						System.out.println("\tCopying register:" + ea.accessIdx + " from:" + ea.shadowAccessIdx);
					}
					if (ea.isBig) {
						big_storage[ea.accessIdx & BIG_MASK] = big_storage[ea.shadowAccessIdx & BIG_MASK];
					} else {
						storage[ea.accessIdx] = storage[ea.shadowAccessIdx];
					}
				}
				updatedRegs.clear();
			}
		} while (regUpdated);
		System.arraycopy(storage, 0, storage_prev, 0, storage.length);
		System.arraycopy(big_storage, 0, big_storage_prev, 0, big_storage.length);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Current Cycle:" + deltaCycle + "\n");
		for (Entry<String, Integer> e : accessIdxMap.entrySet()) {
			sb.append('\t').append(e.getKey()).append("=").append(storage[e.getValue() & BIG_MASK]).append('\n');
		}
		return sb.toString();
	}

	public void setPrinting(boolean b) {
		printing = true;
	}
}
