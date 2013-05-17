package org.pshdl.interpreter.frames;

import java.math.*;

import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.*;

public interface IDebugListener {

	void skippingHandledEdge(int frameUniqueID, InternalInformation internal, boolean risingEdge, Object frame);

	void skippingNotAnEdge(int frameUniqueID, InternalInformation internal, boolean risingEdge, Object frame);

	void skippingPredicateNotFresh(int frameUniqueID, InternalInformation internal, boolean positive, Object frame);

	void skippingPredicateNotMet(int frameUniqueID, InternalInformation internal, boolean positive, BigInteger data, Object frame);

	void twoArgOp(int frameUniqueID, BigInteger b, FastInstruction fi, BigInteger a, BigInteger res, Object frame);

	void oneArgOp(int frameUniqueID, FastInstruction fi, BigInteger a, BigInteger res, Object frame);

	void noArgOp(int frameUniqueID, FastInstruction fi, BigInteger res, Object frame);

	void emptyStack(int frameUniqueID, FastInstruction fi, Object frame);

	void writingResult(int frameUniqueID, InternalInformation internal, BigInteger res, Object frame);

	void loadingInternal(int frameUniqueID, InternalInformation internal, BigInteger value, Object frame);

	void startFrame(int frameUniqueID, int deltaCycle, int epsCycle, Object frame);

	void startCycle(int deltaCycle, int epsCycle, IHDLInterpreter interpreter);

	void copyingRegisterValues(IHDLInterpreter hdlFrameInterpreter);

	void doneCycle(int deltaCycle, IHDLInterpreter interpreter);

}
