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

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.pshdl.interpreter.access.BigAccesses;
import org.pshdl.interpreter.access.EncapsulatedAccess;
import org.pshdl.interpreter.access.EncapsulatedAccess.RegUpdater;
import org.pshdl.interpreter.access.LongAccesses;
import org.pshdl.interpreter.frames.BigIntegerFrame;
import org.pshdl.interpreter.frames.ExecutableFrame;
import org.pshdl.interpreter.frames.IDebugListener;
import org.pshdl.interpreter.frames.LongFrame;

public final class HDLFrameInterpreter implements IHDLBigInterpreter {
	public static class HDLFrameInterpreterFactory implements IHDLInterpreterFactory<HDLFrameInterpreter> {

		private final ExecutableModel model;
		private final IDebugListener listener;

		public HDLFrameInterpreterFactory(ExecutableModel model, IDebugListener listener) {
			super();
			this.model = model;
			this.listener = listener;
		}

		@Override
		public HDLFrameInterpreter newInstance() {
			return new HDLFrameInterpreter(model, listener, false);
		}

	}

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
	 * The storage for internals that have a width &lt;= 64
	 */
	public final long storage[], storage_prev[];
	/**
	 * The storage for internals that have a width &gt;=64;
	 */
	public final BigInteger big_storage[], big_storage_prev[];

	/**
	 * The {@link EncapsulatedAccess} for each accessIndex
	 */
	public final EncapsulatedAccess[] internals, internals_prev;

	/**
	 * An {@link EncapsulatedAccess} for the full variable width of a variable
	 */
	public final EncapsulatedAccess[] full;

	/**
	 * A mapping from baseName of internal to the accessIndex (for accessing
	 * {@link #internals} or {@link #internals_prev})
	 */
	private final Map<String, Integer> accessIdxMap = new TreeMap<>();

	/**
	 * A mapping from baseName to the index in {@link #full}
	 */
	private final Map<String, Integer> varIdxMap = new TreeMap<>();

	/**
	 * The current simulation deltaCycle. That is, how often the run method was
	 * called
	 */
	private int deltaCycle = 0;

	/**
	 * An index when a certain internal was last updated
	 */
	public final long[] deltaUpdates;

	/**
	 * The frames that get executed
	 */
	private final ExecutableFrame frames[];
	private final IDebugListener listener;

	public HDLFrameInterpreter(ExecutableModel model, IDebugListener listener) {
		this(model, listener, false);
	}

	public HDLFrameInterpreter(ExecutableModel model, IDebugListener listener, boolean forceBigInteger) {
		this.model = model;
		this.internals = new EncapsulatedAccess[model.internals.length];
		this.internals_prev = new EncapsulatedAccess[model.internals.length];
		this.full = new EncapsulatedAccess[model.variables.length];
		this.listener = listener;
		final int storageSize = createVarIndex(model);
		createInternals(model);
		this.storage = new long[storageSize];
		this.storage_prev = new long[storageSize];
		this.big_storage = new BigInteger[storageSize];
		this.big_storage_prev = new BigInteger[storageSize];
		for (int i = 0; i < big_storage.length; i++) {
			big_storage[i] = BigInteger.ZERO;
			big_storage_prev[i] = BigInteger.ZERO;
		}
		deltaUpdates = new long[storageSize];
		final Frame[] frames = model.frames;
		this.frames = new ExecutableFrame[frames.length];
		for (int i = 0; i < frames.length; i++) {
			if (forceBigInteger || (frames[i].maxDataWidth > 64)) {
				this.frames[i] = new BigIntegerFrame(listener, this, frames[i], internals, internals_prev);
			} else {
				this.frames[i] = new LongFrame(listener, this, frames[i], internals, internals_prev);
			}
		}
	}

	private int createVarIndex(ExecutableModel model) {
		int currentIdx = 0;
		for (int i = 0; i < model.variables.length; i++) {
			final VariableInformation vi = model.variables[i];
			varIdxMap.put(vi.name, i);
			int accessIndex = currentIdx;
			int size = 1;
			for (final int d : vi.dimensions) {
				size *= d;
			}
			currentIdx += size;
			final InternalInformation ii = new InternalInformation(vi.name, vi);
			if (vi.width > 64) {
				accessIdxMap.put(vi.name, accessIndex | BIG_MARKER);
				full[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, false, this);
			} else {
				accessIdxMap.put(vi.name, accessIndex);
				full[i] = LongAccesses.getInternal(ii, accessIndex, false, this);
			}
			if (vi.isRegister) {
				accessIndex = currentIdx;
				if (vi.width > 64) {
					accessIdxMap.put(vi.name + InternalInformation.REG_POSTFIX, accessIndex | BIG_MARKER);
				} else {
					accessIdxMap.put(vi.name + InternalInformation.REG_POSTFIX, accessIndex);
				}
				currentIdx += size;
			}
		}
		return currentIdx;
	}

	private void createInternals(ExecutableModel model) {
		for (int i = 0; i < model.internals.length; i++) {
			final InternalInformation ii = model.internals[i];
			final String baseName = ii.baseName(false, true);
			final Integer accessIndex = accessIdxMap.get(baseName);
			if (accessIndex == null)
				throw new IllegalArgumentException("No accessIndex for:" + baseName);
			if (((accessIndex & BIG_MARKER) == BIG_MARKER)) {
				internals[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, false, this);
				internals_prev[i] = BigAccesses.getInternal(ii, accessIndex & BIG_MASK, true, this);
			} else {
				internals[i] = LongAccesses.getInternal(ii, accessIndex, false, this);
				internals_prev[i] = LongAccesses.getInternal(ii, accessIndex, true, this);
			}
		}
		for (final EncapsulatedAccess ea : internals) {
			if (ea.ii.isShadowReg) {
				final String baseName = ea.ii.baseName(false, false);
				final Integer idx = accessIdxMap.get(baseName);
				if (idx != null) {
					ea.targetAccessIndex = idx;
				} else {
					ea.targetAccessIndex = ea.getAccessIndex();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#setInput(java.lang.String,
	 * java.math.BigInteger, int)
	 */
	@Override
	public void setInput(String name, BigInteger value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#setInput(int,
	 * java.math.BigInteger, int)
	 */
	@Override
	public void setInput(int idx, BigInteger value, int... arrayIdx) {
		final EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		acc.setDataBig(value, deltaCycle, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#setInput(java.lang.String,
	 * long, int)
	 */
	@Override
	public void setInput(String name, long value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#setInput(int, long, int)
	 */
	@Override
	public void setInput(int idx, long value, int... arrayIdx) {
		final EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		acc.setDataLong(value, deltaCycle, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#getIndex(java.lang.String)
	 */
	@Override
	public int getIndex(String name) {
		final Integer integer = varIdxMap.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name + " valid names are:" + accessIdxMap.keySet());
		return integer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.pshdl.interpreter.IHDLInterpreter#getOutputLong(java.lang.String,
	 * int)
	 */
	@Override
	public long getOutputLong(String name, int... arrayIdx) {
		return getOutputLong(getIndex(name), arrayIdx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#getOutputLong(int, int)
	 */
	@Override
	public long getOutputLong(int idx, int... arrayIdx) {
		final EncapsulatedAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		return acc.getDataLong();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#getOutputBig(java.lang.String,
	 * int)
	 */
	@Override
	public BigInteger getOutputBig(String name, int... arrayIdx) {
		return getOutputBig(getIndex(name), arrayIdx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#getOutputBig(int, int)
	 */
	@Override
	public BigInteger getOutputBig(int idx, int... arrayIdx) {
		final EncapsulatedAccess acc = full[idx];
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
	final private Set<RegUpdater> updatedRegs = new LinkedHashSet<>();

	/*
	 * (non-Javadoc)
	 *
	 * @see org.pshdl.interpreter.IHDLInterpreter#run()
	 */
	@Override
	public void run() {
		deltaCycle++;
		int epsCycle = 0;
		do {
			updatedRegs.clear();
			epsCycle++;
			if (listener != null) {
				listener.startCycle(deltaCycle, epsCycle, this);
			}
			for (final ExecutableFrame ef : frames) {
				ef.execute(deltaCycle, epsCycle);
			}
			if (!updatedRegs.isEmpty()) {
				if (listener != null) {
					listener.copyingRegisterValues(this);
				}
				for (final RegUpdater ea : updatedRegs) {
					if (ea.isBig) {
						big_storage[ea.accessIdx & BIG_MASK] = big_storage[ea.shadowAccessIdx & BIG_MASK];
					} else {
						storage[ea.accessIdx] = storage[ea.shadowAccessIdx];
					}
				}
			}
		} while (!updatedRegs.isEmpty());
		if (listener != null) {
			listener.doneCycle(deltaCycle, this);
		}
		System.arraycopy(storage, 0, storage_prev, 0, storage.length);
		System.arraycopy(big_storage, 0, big_storage_prev, 0, big_storage.length);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Current Cycle:" + deltaCycle + "\n");
		for (final Entry<String, Integer> e : accessIdxMap.entrySet()) {
			sb.append('\t').append(e.getKey()).append("=").append(storage[e.getValue() & BIG_MASK]).append('\n');
		}
		return sb.toString();
	}

	@Override
	public String getName(int idx) {
		for (final Entry<String, Integer> e : varIdxMap.entrySet()) {
			if (e.getValue() == idx)
				return e.getKey();
		}
		throw new IllegalArgumentException("No such index:" + idx);
	}

	@Override
	public long getDeltaCycle() {
		return deltaCycle;
	}

	public void addRegUpdate(RegUpdater regUpdater) {
		updatedRegs.add(regUpdater);
	}

	@Override
	public void initConstants() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws Exception {
	}

	@Override
	public void setFeature(Feature feature, Object value) {
		switch (feature) {
		case disableEdges:
		case disableOutputRegs:
			if ((boolean) value)
				throw new IllegalArgumentException("Feature not supported");
		}
	}

	@Override
	public VariableInformation[] getVariableInformation() {
		return model.variables;
	}
}
