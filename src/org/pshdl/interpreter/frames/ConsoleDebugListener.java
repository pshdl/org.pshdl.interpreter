package org.pshdl.interpreter.frames;

import java.io.*;
import java.math.*;
import java.util.*;

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.Frame.FastInstruction;

public class ConsoleDebugListener implements IDebugListener {
	private final PrintStream out;
	private ExecutableModel em;
	private Set<Integer> frames = new HashSet<Integer>();
	private Set<String> internals = new HashSet<String>();
	private Set<Integer> framesOps = new HashSet<Integer>();
	private Set<Integer> deltas = new HashSet<Integer>();
	public boolean dumpAllFrames = true;
	public boolean dumpAllFramesOps = false;
	private boolean enable = true;
	public boolean enableDefault = true;

	public ConsoleDebugListener(ExecutableModel em) {
		this(em, System.out);
	}

	public ConsoleDebugListener(ExecutableModel em, PrintStream out) {
		this.out = out;
		this.em = em;
	}

	public boolean doShow(int frameUniqueID, InternalInformation internal) {
		return enable && (dumpAllFrames || frames.contains(frameUniqueID) || internals.contains(internal.fullName));
	}

	private boolean doShowOp(int frameUniqueID) {
		return enable && (framesOps.contains(frameUniqueID));
	}

	public void addDelta(int... delta) {
		for (int i : delta) {
			System.out.println("ConsoleDebugListener.addDelta() Adding delta: " + i);
			this.deltas.add(i);
		}
		enableDefault = false;
	}

	public void addInternal(String... internal) {
		for (String i : internal) {
			System.out.println("ConsoleDebugListener.addInternal() Adding internal:" + i);
			this.internals.add(i);
		}
		enableDefault = false;
	}

	public void addFrames(boolean ops, int... frames) {
		for (int i : frames) {
			System.out.println("ConsoleDebugListener.addFrames() Adding frame:" + i);
			this.frames.add(i);
			if (ops) {
				this.framesOps.add(i);
			}
		}
		dumpAllFrames = false;
		enableDefault = false;
	}

	public void addInternalsThatMatch(String internalRegex, boolean ops) {
		for (InternalInformation ii : em.internals) {
			if (ii.fullName.matches(internalRegex)) {
				addInternal(ii.fullName);
			}
		}
	}

	public void addFramesThatReadInternal(String internalRegex, boolean ops) {
		for (Frame f : em.frames) {
			for (int i : f.internalDependencies) {
				if (em.internals[i].fullName.matches(internalRegex)) {
					addFrames(ops, f.uniqueID);
				}
			}
		}
	}

	/**
	 * Adds frames to the list of interesting frames that match the given
	 * internal.
	 * 
	 * @param internalRegex
	 *            '$' signs are automatically escaped
	 * @param ops
	 *            if true also the instructions will be shown
	 */
	public void addFramesThatWriteInternal(String internalRegex, boolean ops) {
		internalRegex = internalRegex.replaceAll("\\$", "\\\\\\$");
		for (Frame f : em.frames) {
			if (em.internals[f.outputId].fullName.matches(internalRegex)) {
				addFrames(ops, f.uniqueID);
			}
		}
	}

	public void addFramesThatUseInternal(String internalRegex, boolean ops) {
		addFramesThatReadInternal(internalRegex, ops);
		addFramesThatWriteInternal(internalRegex, ops);
	}

	@Override
	public void skippingHandledEdge(int frameUniqueID, InternalInformation internal, boolean risingEdge, Object frame) {
		if (doShow(frameUniqueID, internal)) {
			out.printf("\tSkipping frame %d, already handled %s edge on internal %s\n", frameUniqueID, risingEdge ? "rising" : "falling", internal);
		}
	}

	@Override
	public void skippingNotAnEdge(int frameUniqueID, InternalInformation internal, boolean risingEdge, Object frame) {
		if (doShow(frameUniqueID, internal)) {
			out.printf("\tSkipping frame %d, not an %s edge on internal %s\n", frameUniqueID, risingEdge ? "rising" : "falling", internal);
		}
	}

	@Override
	public void skippingPredicateNotFresh(int frameUniqueID, InternalInformation internal, boolean positive, Object frame) {
		if (doShow(frameUniqueID, internal)) {
			out.printf("\tSkipping frame %d because the %s predicate %s is not fresh enough\n", frameUniqueID, positive ? "positive" : "negative", internal);
		}
	}

	@Override
	public void skippingPredicateNotMet(int frameUniqueID, InternalInformation internal, boolean positive, BigInteger data, Object frame) {
		if (doShow(frameUniqueID, internal)) {
			out.printf("\tSkipping frame %d because the %s predicate %s was not met. Value was: 0x%s\n", frameUniqueID, positive ? "positive" : "negative", internal,
					data.toString(16));
		}

	}

	@Override
	public void twoArgOp(int frameUniqueID, BigInteger b, FastInstruction fi, BigInteger a, BigInteger res, Object frame) {
		if (doShowOp(frameUniqueID)) {
			out.printf("\t\tExecuting 0x%s %s 0x%s = 0x%s\n", b.toString(16), fi.toString(em), a.toString(16), res.toString(16));
		}
	}

	@Override
	public void oneArgOp(int frameUniqueID, FastInstruction fi, BigInteger a, BigInteger res, Object frame) {
		if (doShowOp(frameUniqueID)) {
			out.printf("\t\tExecuting %s 0x%s = 0x%s\n", fi.toString(em), a.toString(16), res.toString(16));
		}

	}

	@Override
	public void noArgOp(int frameUniqueID, FastInstruction fi, BigInteger res, Object frame) {
		if (doShowOp(frameUniqueID)) {
			out.printf("\t\tExecuting %s = 0x%s\n", fi.toString(em), res.toString(16));
		}
	}

	@Override
	public void emptyStack(int frameUniqueID, FastInstruction fi, Object frame) {
		if (doShowOp(frameUniqueID)) {
			out.printf("\t\tExecuting %s did not give a result\n", fi.toString(em));
		}

	}

	@Override
	public void writingResult(int frameUniqueID, InternalInformation internal, BigInteger res, Object frame) {
		if (doShow(frameUniqueID, internal)) {
			out.printf("\tWriting result 0x%s to %s\n", res.toString(16), internal);
		}
	}

	@Override
	public void loadingInternal(int frameUniqueID, InternalInformation internal, BigInteger value, Object frame) {
		if (doShow(frameUniqueID, internal)) {
			out.printf("\t\tLoaded internal %s with value: 0x%s\n", internal, value.toString(16));
		}
	}

	@Override
	public void startFrame(int frameUniqueID, int deltaCycle, int epsCycle, Object frame) {
		if (enable && (dumpAllFrames || frames.contains(frameUniqueID))) {
			out.printf("Starting frame: %d at delta:%d eps:%d\n", frameUniqueID, deltaCycle, epsCycle);
		}
	}

	@Override
	public void startCycle(int deltaCycle, int epsCycle, IHDLInterpreter interpreter) {
		enable = deltas.contains(deltaCycle) || enableDefault;
		if (enable) {
			out.printf("Starting cycle delta:%d eps:%d\n", deltaCycle, epsCycle);
		}
	}

	@Override
	public void copyingRegisterValues(IHDLInterpreter hdlFrameInterpreter) {
		if (enable) {
			out.printf("Copying registers\n");
		}
	}

	@Override
	public void doneCycle(int deltaCycle, IHDLInterpreter interpreter) {
		if (enable) {
			out.printf("Done with cycle:%d\n", deltaCycle);
		}
	}

}
