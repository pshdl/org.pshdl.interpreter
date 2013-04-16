package org.pshdl.interpreter;

import java.util.*;
import java.util.regex.*;

import org.pshdl.interpreter.utils.*;

public class InternalInformation {
	public static final Pattern aiFormatName = Pattern.compile("(.*?)" // baseName
			+ "((?:\\[\\d+\\])*)" // arrays
			+ "(?:\\{(?:(\\d+)" // first Digit if range
			+ "(?:\\:(\\d+))?)\\})?" // second Digit if range
			+ "(\\" + FluidFrame.REG_POSTFIX + ")?");
	public static final Pattern array = Pattern.compile("\\[(.*?)\\]");

	/**
	 * The basename is the name of the variable, it may contain
	 * {@link FluidFrame#PRED_PREFIX} and arrays
	 */
	public final String baseName;

	/**
	 * The full name is the base name, but also includes bit accesses and
	 * {@link FluidFrame#REG_POSTFIX}
	 */
	public final String fullName;

	/**
	 * If <code>true</code> this internal is the shadow register
	 */
	public final boolean isReg;

	/**
	 * If <code>true</code> this internal is a predicate
	 */
	public final boolean isPred;

	/**
	 * bitStart indicates the largest bit index of that access, while bitEnd
	 * indicates the lowest bit index. Both values can be -1 to indicate that no
	 * bit access is given. For single bit accesses both values are the same
	 */
	public final int bitStart, bitEnd;

	/**
	 * The baseWidth represents the width of the variable, whereas actualWidth
	 * represents the width as given by the bit accesses
	 */
	public final int baseWidth, actualWidth;

	/**
	 * The parsed values for array indices. Those are already included in the
	 * basename.
	 */
	public final int arrayIdx[];

	public InternalInformation(String baseName, boolean isReg, boolean isPred, int bitStart, int bitEnd, int baseWidth, int[] arrayIdx) {
		super();
		this.isReg = isReg;
		this.isPred = isPred;
		this.bitStart = bitStart;
		this.bitEnd = bitEnd;
		this.baseWidth = baseWidth;
		this.arrayIdx = arrayIdx;
		StringBuilder sb = new StringBuilder();
		if (isPred) {
			sb.append(FluidFrame.PRED_PREFIX);
		}
		sb.append(baseName);
		for (int idx : arrayIdx) {
			sb.append('[').append(idx).append(']');
		}
		this.baseName = sb.toString();
		if ((bitStart != -1) && (bitEnd != -1)) {
			this.actualWidth = (bitEnd - bitStart) + 1;
			sb.append('{');
			if (bitEnd == bitStart) {
				sb.append(bitStart);
			} else {
				sb.append(bitStart).append(':').append(bitEnd);
			}
			sb.append('}');
		} else {
			this.actualWidth = baseWidth;
		}
		if (isReg) {
			sb.append(FluidFrame.REG_POSTFIX);
		}
		this.fullName = sb.toString();
	}

	public InternalInformation(String fullName, int baseWidth) {
		super();
		this.fullName = fullName;
		this.isReg = fullName.endsWith(FluidFrame.REG_POSTFIX);
		this.baseWidth = baseWidth;
		this.isPred = fullName.startsWith(FluidFrame.PRED_PREFIX);
		Matcher matcher = aiFormatName.matcher(fullName);
		List<Integer> dims = new LinkedList<>();
		if (matcher.matches()) {
			this.baseName = matcher.group(1);
			if (matcher.group(3) == null) {
				this.bitStart = -1;
				this.bitEnd = -1;
				this.actualWidth = baseWidth;
			} else if (matcher.group(4) != null) {
				this.bitStart = Integer.parseInt(matcher.group(3));
				this.bitEnd = Integer.parseInt(matcher.group(4));
				this.actualWidth = (bitEnd - bitStart) + 1;
			} else {
				this.bitStart = this.bitEnd = Integer.parseInt(matcher.group(3));
				this.actualWidth = 1;
			}
			Matcher m = array.matcher(matcher.group(2));
			while (m.find()) {
				dims.add(Integer.parseInt(m.group(1)));
			}
		} else
			throw new IllegalArgumentException("Name:" + fullName + " is not valid!");
		arrayIdx = new int[dims.size()];
		for (int i = 0; i < arrayIdx.length; i++) {
			arrayIdx[i] = dims.get(i);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + actualWidth;
		result = (prime * result) + ((baseName == null) ? 0 : baseName.hashCode());
		result = (prime * result) + baseWidth;
		result = (prime * result) + bitEnd;
		result = (prime * result) + bitStart;
		result = (prime * result) + ((fullName == null) ? 0 : fullName.hashCode());
		result = (prime * result) + (isPred ? 1231 : 1237);
		result = (prime * result) + (isReg ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InternalInformation other = (InternalInformation) obj;
		if (actualWidth != other.actualWidth)
			return false;
		if (baseName == null) {
			if (other.baseName != null)
				return false;
		} else if (!baseName.equals(other.baseName))
			return false;
		if (baseWidth != other.baseWidth)
			return false;
		if (bitEnd != other.bitEnd)
			return false;
		if (bitStart != other.bitStart)
			return false;
		if (fullName == null) {
			if (other.fullName != null)
				return false;
		} else if (!fullName.equals(other.fullName))
			return false;
		if (isPred != other.isPred)
			return false;
		if (isReg != other.isReg)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "InternalInformation [baseName=" + baseName + ", fullName=" + fullName + ", isReg=" + isReg + ", isPred=" + isPred + ", bitStart=" + bitStart + ", bitEnd=" + bitEnd
				+ ", baseWidth=" + baseWidth + ", actualWidth=" + actualWidth + ", arrayDim=" + Arrays.toString(arrayIdx) + "]";
	}

	public String baseNameWithReg() {
		return isReg ? baseName + FluidFrame.REG_POSTFIX : baseName;
	}

}
