package org.pshdl.interpreter;

import java.math.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.pshdl.interpreter.utils.*;

public final class HDLFrameInterpreter {
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
			String basicName = getBasicName(in);
			int width = model.getWidth(in);
			Integer accessIndex = idx.get(basicName);
			if (accessIndex == null) {
				if (width > 64) {
					accessIndex = -(currentIdx++);
				} else {
					accessIndex = currentIdx++;
				}
				idx.put(basicName, accessIndex);
			}
			if (accessIndex < 0) {
				// System.out.println("HDLFrameInterpreter.HDLFrameInterpreter() Using RangeBigAccess for:"
				// + in);
				if (width == 1) {
					internals[i] = new SingleBigAccess(this, in, -accessIndex, false);
					internals_prev[i] = new SingleBigAccess(this, in, -accessIndex, true);
				} else if (in.indexOf('{') == -1) {
					internals[i] = new DirectBigAccess(this, in, -accessIndex, false, width);
					internals_prev[i] = new DirectBigAccess(this, in, -accessIndex, true, width);
				} else {
					internals[i] = new RangeBigAccess(this, in, -accessIndex, false, width);
					internals_prev[i] = new RangeBigAccess(this, in, -accessIndex, true, width);
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
			regIndex[i] = idx.get(getBasicName(name));
			regIndexTarget[i] = idx.get(getBasicName(ExecutableModel.stripReg(name)));
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
			if (frames[i].maxDataWidth > 64) {
				// System.out.println("HDLFrameInterpreter.HDLFrameInterpreter() Using BigFrame for:"
				// + frames[i].uniqueID);
				this.frames[i] = new BigIntegerFrame(frames[i], printing, internals, internals_prev);
			} else {
				this.frames[i] = new LongFrame(frames[i], printing, internals, internals_prev);
			}
		}
	}

	private String getBasicName(String inName) {
		String name = inName;
		int openBrace = name.indexOf('{');
		if (openBrace != -1) {
			name = name.substring(0, openBrace);
		}
		if (inName.endsWith(FluidFrame.REG_POSTFIX))
			return name + FluidFrame.REG_POSTFIX;
		return name;
	}

	public void setInput(String name, BigInteger value) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		if (integer < 0) {
			big_storage[-integer] = value;
		} else {
			storage[integer] = value.longValue();
		}
	}

	public void setInput(String name, long value) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		if (integer < 0) {
			big_storage[-integer] = BigInteger.valueOf(value);
		} else {
			storage[integer] = value;
		}
	}

	public long getOutputLong(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		if (integer < 0)
			return big_storage[-integer].longValue();
		return storage[integer];
	}

	public BigInteger getOutputBig(String name) {
		Integer integer = idx.get(name);
		if (integer == null)
			throw new IllegalArgumentException("Could not find a variable named:" + name);
		if (integer < 0)
			return big_storage[-integer];
		return BigInteger.valueOf(storage[integer]);
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
					if (j >= 0) {
						storage[regIndexTarget[i]] = storage[j];
					} else {
						big_storage[-regIndexTarget[i]] = big_storage[-j];
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
