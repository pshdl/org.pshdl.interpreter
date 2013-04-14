package org.pshdl.interpreter.access;

import java.math.*;
import org.pshdl.interpreter.*;

public abstract class EncapsulatedAccess {
	/**
	 * 
	 */
	private final HDLFrameInterpreter hdlFrameInterpreter;
	public final InternalInformation name;
	public final int accessIndex;
	public final boolean prev;

	public EncapsulatedAccess(HDLFrameInterpreter hdlFrameInterpreter, InternalInformation name, int accessIndex, boolean prev) {
		super();
		this.hdlFrameInterpreter = hdlFrameInterpreter;
		this.accessIndex = accessIndex;
		this.prev = prev;
		this.name = name;

	}

	public void setLastUpdate(int deltaCycle, int epsCycle) {
		this.hdlFrameInterpreter.deltaUpdates[accessIndex] = (deltaCycle << 16) | (epsCycle & 0xFFFF);
	}

	public boolean skip(int deltaCycle, int epsCycle) {
		long local = this.hdlFrameInterpreter.deltaUpdates[accessIndex];
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

	public boolean isFresh(int deltaCycle) {
		return (this.hdlFrameInterpreter.deltaUpdates[accessIndex] >>> 16) == deltaCycle;
	}

	public abstract BigInteger getDataBig();

	public abstract long getDataLong();

	public abstract void setData(long data, int deltaCycle, int epsCycle);

	public abstract void setData(BigInteger data, int deltaCycle, int epsCycle);
}