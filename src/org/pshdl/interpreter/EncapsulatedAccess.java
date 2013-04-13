package org.pshdl.interpreter;

import java.math.*;
import java.util.regex.*;

import org.pshdl.interpreter.utils.*;

public abstract class EncapsulatedAccess {
	/**
	 * 
	 */
	private final HDLFrameInterpreter hdlFrameInterpreter;
	public final String name;
	public final int accessIndex;
	public final boolean prev;
	protected boolean isPredicate;
	public final int bitStart;
	public final int bitEnd;

	public EncapsulatedAccess(HDLFrameInterpreter hdlFrameInterpreter, String name, int accessIndex, boolean prev) {
		super();
		this.hdlFrameInterpreter = hdlFrameInterpreter;
		this.accessIndex = accessIndex;
		this.prev = prev;
		this.isPredicate = name.startsWith(FluidFrame.PRED_PREFIX);
		Matcher matcher = HDLFrameInterpreter.aiFormatName.matcher(name);
		if (matcher.matches()) {
			this.name = matcher.group(1);
			if (matcher.group(2) == null) {
				bitStart = -1;
				bitEnd = -1;
			} else if (matcher.group(3) != null) {
				bitStart = Integer.parseInt(matcher.group(2));
				bitEnd = Integer.parseInt(matcher.group(3));
			} else {
				bitStart = bitEnd = Integer.parseInt(matcher.group(2));
			}
		} else
			throw new IllegalArgumentException("Name:" + name + " is not valid!");
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