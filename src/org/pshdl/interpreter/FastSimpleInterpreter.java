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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess.RegUpdater;
import org.pshdl.interpreter.VariableInformation.Type;
import org.pshdl.interpreter.frames.FastFrame;

public class FastSimpleInterpreter implements IHDLInterpreter {

	public static class FastSimpleFactory implements IHDLInterpreterFactory<FastSimpleInterpreter> {

		private final ExecutableModel model;
		private final boolean disableEdge, disabledRegOutputlogic;

		public FastSimpleFactory(ExecutableModel model, boolean disableEdge, boolean disabledRegOutputlogic) {
			super();
			this.model = model;
			this.disableEdge = disableEdge;
			this.disabledRegOutputlogic = disabledRegOutputlogic;
		}

		@Override
		public FastSimpleInterpreter newInstance() {
			return new FastSimpleInterpreter(model, disableEdge, disabledRegOutputlogic);
		}

	}

	public class LongAccess {

		public class RegUpdater {
			public final int accessIdx;
			public final int shadowAccessIdx;

			public RegUpdater(int shadowAccessIdx, int accessIdx) {
				super();
				this.shadowAccessIdx = shadowAccessIdx == -1 ? accessIdx : shadowAccessIdx;
				this.accessIdx = accessIdx == -1 ? shadowAccessIdx : accessIdx;
			}
		}

		private final int accessIndex;
		private final int[] dims;
		public final InternalInformation ii;
		public final long mask;
		public int offset;

		public final boolean prev;

		public int shift;
		private final boolean isDynamicBit;

		public int targetAccessIndex = -1;

		public long writeMask;
		private int signShift;

		public LongAccess(InternalInformation name, int accessIndex, boolean prev) {
			super();
			if ((name.actualWidth != name.info.width) || (name.info.type != Type.INT)) {
				signShift = 0;
			} else {
				signShift = 64 - name.actualWidth;
			}
			this.accessIndex = accessIndex;
			this.prev = prev;
			this.ii = name;
			this.isDynamicBit = name.bitEnd == -1;
			this.dims = name.info.dimensions.clone();
			if (dims.length > 0) {
				this.dims[dims.length - 1] = 1;
			}
			if (name.fixedArray) {
				setOffset(name.arrayIdx);
			}
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

		public int getAccessIndex() {
			return accessIndex + offset;
		}

		public long getDataLong() {
			final int accessIndex = getAccessIndex();
			final long rawVal;
			if (prev) {
				rawVal = storage_prev[accessIndex];
			} else {
				rawVal = storage[accessIndex];
			}
			return (((rawVal >> shift) & mask) << signShift) >> signShift;
		}

		public RegUpdater getRegUpdater() {
			return new RegUpdater(getAccessIndex(), targetAccessIndex + offset);
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
			final long raw = deltaUpdates[getAccessIndex()];
			final boolean dc = (raw >>> 16l) == deltaCycle;
			final boolean ec = (raw & 0xFFFF) == epsCycle;
			return dc && ec;
		}

		public void setDataLong(long data, int deltaCycle, int epsCycle) {
			final long current = storage[getAccessIndex()] & writeMask;
			storage[getAccessIndex()] = current | ((data & mask) << shift);
			if (ii.isPred) {
				setLastUpdate(deltaCycle, epsCycle);
			}
		}

		public void setLastUpdate(int deltaCycle, int epsCycle) {
			deltaUpdates[getAccessIndex()] = ((long) deltaCycle << 16l) | (epsCycle & 0xFFFF);
		}

		public void setOffset(int... off) {
			offset = 0;
			if (off.length == 0)
				return;
			final int lastIndex = dims.length - 1;
			int rowSize = 1;
			for (int i = lastIndex; i >= 0; i--) {
				offset += rowSize * off[i];
				rowSize *= dims[i];
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

		/**
		 * Check whether this register has been updated in this delta / eps
		 * cycle. Returns <code>true</code> when updating this register is not
		 * recommended.
		 *
		 * @param deltaCycle
		 * @param epsCycle
		 * @return <code>true</code> when updating this register is not
		 *         recommended.
		 */
		public boolean skip(int deltaCycle, int epsCycle) {
			final long local = deltaUpdates[getAccessIndex()];
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

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("LongAccess [shift=").append(shift).append(", mask=").append(Long.toHexString(mask)).append(", writeMask=").append(Long.toHexString(writeMask))
					.append(", name=").append(ii).append(", accessIndex=").append(getAccessIndex()).append(", prev=").append(prev).append("]");
			return builder.toString();
		}

		public void setBitOffset(int bitOffset) {
			if (isDynamicBit) {
				this.shift = bitOffset;
				this.writeMask = ~(mask << shift);
			}
		}

	}

	public long[] deltaUpdates;
	public LongAccess internals[];
	public LongAccess internals_prev[];
	public long storage[];
	public long storage_prev[];
	private final LongAccess[] full;
	private final FastFrame[] frames;
	private final Map<String, Integer> accessIdxMap = new TreeMap<>();
	private final Map<String, Integer> varIdxMap = new TreeMap<>();
	private int deltaCycle;
	private boolean disabledRegOutputlogic;
	private final VariableInformation varInfo[];

	public FastSimpleInterpreter(ExecutableModel model, boolean disableEdge, boolean disabledRegOutputlogic) {
		this.disabledRegOutputlogic = disabledRegOutputlogic;
		final Frame[] frames = model.frames;
		this.frames = new FastFrame[frames.length];
		this.full = new LongAccess[model.variables.length];
		final Map<String, Integer> index = new LinkedHashMap<>();
		int currentIdx = 0;
		for (final VariableInformation var : model.variables) {
			index.put(var.name, currentIdx);
			int size = 1;
			for (final int d : var.dimensions) {
				size *= d;
			}
			currentIdx += size;
			if (var.isRegister) {
				index.put(var.name + InternalInformation.REG_POSTFIX, currentIdx);
				currentIdx += size;
			}
		}
		this.internals = new LongAccess[model.internals.length];
		this.internals_prev = new LongAccess[model.internals.length];
		final int storageSize = createVarIndex(model);
		createInternals(model);
		this.storage = new long[storageSize];
		this.storage_prev = new long[storageSize];
		deltaUpdates = new long[storageSize];
		for (int i = 0; i < frames.length; i++) {
			this.frames[i] = new FastFrame(this, frames[i], disableEdge);
		}
		this.varInfo = model.variables;
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
			accessIdxMap.put(vi.name, accessIndex);
			full[i] = new LongAccess(new InternalInformation(vi.name, vi), accessIndex, false);
			if (vi.isRegister) {
				accessIndex = currentIdx;
				accessIdxMap.put(vi.name + InternalInformation.REG_POSTFIX, accessIndex);
				currentIdx += size;
			}
		}
		return currentIdx;
	}

	private void createInternals(ExecutableModel model) {
		for (int i = 0; i < model.internals.length; i++) {
			final InternalInformation ii = model.internals[i];
			// System.out.println("HDLFrameInterpreter.createInternals()" + ii);
			final String baseName = ii.baseName(false, true);
			final Integer accessIndex = accessIdxMap.get(baseName);
			if (accessIndex == null)
				throw new IllegalArgumentException("No idx for:" + baseName);
			internals[i] = new LongAccess(ii, accessIndex, false);
			internals_prev[i] = new LongAccess(ii, accessIndex, true);
		}
		for (final LongAccess ea : internals) {
			if (ea.ii.isShadowReg) {
				final String baseName = ea.ii.baseName(false, false);
				final Integer idx = accessIdxMap.get(baseName);
				if (idx != null) {
					ea.targetAccessIndex = idx;
				} else {
					ea.targetAccessIndex = ea.accessIndex;
				}
			}
		}
	}

	@Override
	public void run() {
		boolean regUpdated = false;
		this.deltaCycle++;
		int epsCycle = 0;
		final List<RegUpdater> updatedRegs = new ArrayList<>();
		do {
			epsCycle++;
			regUpdated = false;
			for (final FastFrame ef : frames) {
				final boolean execute = ef.execute(deltaCycle, epsCycle);
				if (execute && !ef.regUpdates.isEmpty()) {
					updatedRegs.addAll(ef.regUpdates);
					regUpdated = true;
				}
			}
			if (regUpdated) {
				for (final RegUpdater ea : updatedRegs) {
					storage[ea.accessIdx] = storage[ea.shadowAccessIdx];
				}
				updatedRegs.clear();
			}
		} while (regUpdated && !disabledRegOutputlogic);
		System.arraycopy(storage, 0, storage_prev, 0, storage.length);
	}

	@Override
	public void setInput(String name, long value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	@Override
	public void setInput(int idx, long value, int... arrayIdx) {
		final LongAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		acc.setDataLong(value, deltaCycle, 0);
	}

	@Override
	public int getIndex(String name) {
		final Integer integer = varIdxMap.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name + " valid names are:" + accessIdxMap.keySet());
		return integer;
	}

	@Override
	public long getOutputLong(String name, int... arrayIdx) {
		return getOutputLong(getIndex(name), arrayIdx);
	}

	@Override
	public long getOutputLong(int idx, int... arrayIdx) {
		final LongAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		return acc.getDataLong();
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

	@Override
	public String toString() {
		try (final Formatter f = new Formatter()) {
			f.format("Dump of deltaCycle %d%n", deltaCycle);
			for (final LongAccess la : internals) {
				f.format("\t%20s: 0x%04x%n", la.ii.fullName, la.getDataLong());
			}
			return f.toString();
		}
	}

	@Override
	public void initConstants() {

	}

	@Override
	public void close() throws Exception {
	}

	@Override
	public void setFeature(Feature feature, Object value) {
		switch (feature) {
		case disableOutputRegs:
			disabledRegOutputlogic = (boolean) value;
			break;
		case disableEdges:
			for (final FastFrame fastFrame : frames) {
				fastFrame.disableEdge = (boolean) value;
			}
			break;

		}
	}

	@Override
	public VariableInformation[] getVariableInformation() {
		return varInfo;
	}
}
