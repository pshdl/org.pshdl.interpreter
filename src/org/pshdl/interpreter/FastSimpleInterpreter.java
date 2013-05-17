package org.pshdl.interpreter;

import java.math.*;
import java.util.*;
import java.util.Map.Entry;

import org.pshdl.interpreter.FastSimpleInterpreter.LongAccess.RegUpdater;
import org.pshdl.interpreter.frames.*;

public class FastSimpleInterpreter implements IHDLInterpreter {

	public final class LongAccess {

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

		/**
		 * 
		 */
		public final int shift;

		public int targetAccessIndex = -1;

		public final long writeMask;

		public LongAccess(InternalInformation name, int accessIndex, boolean prev) {
			super();
			this.accessIndex = accessIndex;
			this.prev = prev;
			this.ii = name;
			this.dims = name.info.dimensions.clone();
			if (dims.length > 0) {
				this.dims[dims.length - 1] = 1;
			}
			if (name.fixedArray) {
				setOffset(name.arrayIdx);
			}
			if ((name.bitStart == -1) && (name.bitEnd == -1)) {
				int width = name.info.width;
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

		public int getAccessIndex() {
			return accessIndex + offset;
		}

		public long getDataLong() {
			if (prev)
				return (storage_prev[getAccessIndex()] >> shift) & mask;
			return (storage[getAccessIndex()] >> shift) & mask;
		}

		public RegUpdater getRegUpdater() {
			return new RegUpdater(getAccessIndex(), targetAccessIndex + offset);
		}

		/**
		 * Checks whether this data has been updated in this delta cycle
		 * 
		 * @param deltaCycle
		 * @return <code>true</code> if it was calculated in this delta cycle,
		 *         <code>false</code> otherwise
		 */
		public boolean isFresh(int deltaCycle) {
			return (deltaUpdates[getAccessIndex()] >>> 16l) == deltaCycle;
		}

		public void setDataLong(long data, int deltaCycle, int epsCycle) {
			long current = storage[getAccessIndex()] & writeMask;
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
			for (int i = 0; i < dims.length; i++) {
				int o = off[i];
				offset += o * dims[i];
			}
		}

		public void fillDataLong(int arrayPos, int[] writeIndex, long a, int deltaCycle, int epsCycle) {
			int offset = 0;
			for (int i = 0; i < (arrayPos + 1); i++) {
				int o = writeIndex[i];
				offset += o * dims[i];
			}
			int fill = 1;
			int[] dims = ii.info.dimensions;
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
		 * @return
		 */
		public boolean skip(int deltaCycle, int epsCycle) {
			long local = deltaUpdates[getAccessIndex()];
			long dc = local >>> 16l;
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
			StringBuilder builder = new StringBuilder();
			builder.append("LongAccess [shift=").append(shift).append(", mask=").append(Long.toHexString(mask)).append(", writeMask=").append(Long.toHexString(writeMask))
					.append(", name=").append(ii).append(", accessIndex=").append(getAccessIndex()).append(", prev=").append(prev).append("]");
			return builder.toString();
		}

	}

	public long[] deltaUpdates;
	public LongAccess internals[];
	public LongAccess internals_prev[];
	public long storage[];
	public long storage_prev[];
	private LongAccess[] full;
	private FastFrame[] frames;
	private final Map<String, Integer> accessIdxMap = new TreeMap<String, Integer>();
	private final Map<String, Integer> varIdxMap = new TreeMap<String, Integer>();
	private int deltaCycle;
	private boolean disableEdge;

	public FastSimpleInterpreter(ExecutableModel model, boolean disableEdge) {
		Map<String, Integer> index = new HashMap<String, Integer>();
		int currentIdx = 0;
		for (VariableInformation var : model.variables) {
			index.put(var.name, currentIdx);
			int size = 1;
			for (int d : var.dimensions) {
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
		this.full = new LongAccess[model.variables.length];
		int storageSize = createInternals(model);
		createVarIndex(model);
		this.storage = new long[storageSize];
		this.storage_prev = new long[storageSize];
		deltaUpdates = new long[storageSize];
		Frame[] frames = model.frames;
		this.frames = new FastFrame[frames.length];
		for (int i = 0; i < frames.length; i++) {
			this.frames[i] = new FastFrame(this, frames[i], disableEdge);
		}
		this.disableEdge = disableEdge;
	}

	private void createVarIndex(ExecutableModel model) {
		for (int i = 0; i < model.variables.length; i++) {
			VariableInformation vi = model.variables[i];
			Integer accessIndex = accessIdxMap.get(vi.name);
			if (accessIndex != null) {
				full[i] = new LongAccess(new InternalInformation(vi.name, vi), accessIndex, false);
				varIdxMap.put(vi.name, i);
			}
		}
	}

	private int createInternals(ExecutableModel model) {
		int currentIdx = 0;
		for (int i = 0; i < model.internals.length; i++) {
			InternalInformation ii = model.internals[i];
			// System.out.println("HDLFrameInterpreter.createInternals()" + ii);
			String baseName = ii.baseName(false, true);
			Integer accessIndex = accessIdxMap.get(baseName);
			if (accessIndex == null) {
				accessIndex = currentIdx;
				int size = 1;
				for (int d : ii.info.dimensions) {
					size *= d;
				}
				currentIdx += size;
				// System.out.println("HDLFrameInterpreter.createInternals()Allocating:"
				// + size + " for " + baseName);
				accessIdxMap.put(baseName, accessIndex);
			}
			internals[i] = new LongAccess(ii, accessIndex, false);
			internals_prev[i] = new LongAccess(ii, accessIndex, true);
		}
		for (LongAccess ea : internals) {
			if (ea.ii.isShadowReg) {
				ea.targetAccessIndex = accessIdxMap.get(ea.ii.baseName(false, false));
			}
		}
		return currentIdx;
	}

	@Override
	public void run() {
		boolean regUpdated = false;
		this.deltaCycle++;
		int epsCycle = 0;
		List<RegUpdater> updatedRegs = new ArrayList<RegUpdater>();
		do {
			epsCycle++;
			regUpdated = false;
			for (FastFrame ef : frames) {
				boolean execute = ef.execute(deltaCycle, epsCycle);
				if (execute && ef.regUpdated) {
					updatedRegs.add(ef.outputAccess.getRegUpdater());
					regUpdated = true;
				}
			}
			if (regUpdated) {
				for (RegUpdater ea : updatedRegs) {
					storage[ea.accessIdx] = storage[ea.shadowAccessIdx];
				}
				updatedRegs.clear();
			}
		} while (regUpdated && !disableEdge);
		System.arraycopy(storage, 0, storage_prev, 0, storage.length);
	}

	@Override
	public void setInput(String name, long value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	@Override
	public void setInput(int idx, long value, int... arrayIdx) {
		LongAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		acc.setDataLong(value, deltaCycle, 0);
	}

	@Override
	public int getIndex(String name) {
		Integer integer = varIdxMap.get(name);
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
		LongAccess acc = full[idx];
		if (arrayIdx != null) {
			acc.setOffset(arrayIdx);
		}
		return acc.getDataLong();
	}

	@Override
	public void setInput(String name, BigInteger value, int... arrayIdx) {
		setInput(name, value.longValue(), arrayIdx);
	}

	@Override
	public void setInput(int idx, BigInteger value, int... arrayIdx) {
		setInput(idx, value.longValue(), arrayIdx);
	}

	@Override
	public BigInteger getOutputBig(String name, int... arrayIdx) {
		return BigInteger.valueOf(getOutputLong(name, arrayIdx));
	}

	@Override
	public BigInteger getOutputBig(int idx, int... arrayIdx) {
		return BigInteger.valueOf(getOutputLong(idx, arrayIdx));
	}

	@Override
	public void setPrinting(boolean b) {

	}

	@Override
	public String getName(int idx) {
		for (Entry<String, Integer> e : varIdxMap.entrySet()) {
			if (e.getValue() == idx)
				return e.getKey();
		}
		throw new IllegalArgumentException("No such index:" + idx);
	}
}
