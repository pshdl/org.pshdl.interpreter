package org.pshdl.interpreter;

import java.math.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.pshdl.interpreter.utils.*;

public final class HDLFrameInterpreter {
	private static final int BIG_MARKER = 0x80000000;
	private static final int BIG_MASK = BIG_MARKER - 1;

	protected final ExecutableModel model;

	protected final long storage[], storage_prev[];
	protected final BigInteger big_storage[], big_storage_prev[];

	public static final Pattern aiFormatName = Pattern.compile("(.*?)(?:\\{(?:(\\d+)(?:\\:(\\d+))?)\\})?(\\" + FluidFrame.REG_POSTFIX + ")?");

	private final EncapsulatedAccess[] internals, internals_prev;
	private final int[] regIndex, regIndexTarget;
	private final Map<String, Integer> idx = new TreeMap<String, Integer>();
	private int deltaCycle = 0;

	private boolean printing;

	long[] deltaUpdates;
	private final ExecutableFrame frames[];

	public HDLFrameInterpreter(ExecutableModel model, boolean printing) {
		this.printing = printing;
		this.model = model;
		int currentIdx = 0;
		this.internals = new EncapsulatedAccess[model.internals.length];
		this.internals_prev = new EncapsulatedAccess[model.internals.length];
		for (int i = 0; i < model.internals.length; i++) {
			String in = model.internals[i];
			String basicName = ExecutableModel.getBasicName(in, false);
			int width = model.getWidth(in);
			Integer accessIndex = idx.get(basicName);
			if (accessIndex == null) {
				if ((width > 64)) {
					accessIndex = currentIdx++ | BIG_MARKER;
				} else {
					accessIndex = currentIdx++;
				}
				idx.put(basicName, accessIndex);
			}
			if (((accessIndex & BIG_MARKER) == BIG_MARKER)) {
				if (model.getRealWidth(in) == 1) {
					internals[i] = new SingleBigAccess(this, in, accessIndex & BIG_MASK, false);
					internals_prev[i] = new SingleBigAccess(this, in, accessIndex & BIG_MASK, true);
				} else if (in.indexOf('{') == -1) {
					internals[i] = new DirectBigAccess(this, in, accessIndex & BIG_MASK, false, width);
					internals_prev[i] = new DirectBigAccess(this, in, accessIndex & BIG_MASK, true, width);
				} else {
					System.out.println("HDLFrameInterpreter.HDLFrameInterpreter() Using rangeBitAccess for:" + in);
					internals[i] = new RangeBigAccess(this, in, accessIndex & BIG_MASK, false, width);
					internals_prev[i] = new RangeBigAccess(this, in, accessIndex & BIG_MASK, true, width);
				}
			} else {
				internals[i] = new LongAccess(this, in, accessIndex, false);
				internals_prev[i] = new LongAccess(this, in, accessIndex, true);
			}
		}
		regIndex = new int[model.registerOutputs.length];
		regIndexTarget = new int[model.registerOutputs.length];
		for (int i = 0; i < model.registerOutputs.length; i++) {
			int ridx = model.registerOutputs[i];
			String name = model.internals[ridx];
			regIndex[i] = idx.get(ExecutableModel.getBasicName(name, false));
			regIndexTarget[i] = idx.get(ExecutableModel.getBasicName(ExecutableModel.stripReg(name), false));
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
		if ((integer & BIG_MARKER) == BIG_MARKER) {
			big_storage[integer & BIG_MASK] = BigInteger.valueOf(value);
		} else {
			storage[integer] = value;
		}
	}

	public long getOutputLong(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		long res;
		if ((integer & BIG_MARKER) == BIG_MARKER)
			res = big_storage[integer & BIG_MASK].longValue();
		else
			res = storage[integer];
		// System.out.println("HDLFrameInterpreter.getOutputLong()name:" + name
		// + " is:" + res);
		int width = model.getWidth(name);
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
		BigInteger res;
		if ((integer & BIG_MARKER) == BIG_MARKER)
			res = big_storage[integer & BIG_MASK];
		res = BigInteger.valueOf(storage[integer]);
		int width = model.getWidth(name);
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
