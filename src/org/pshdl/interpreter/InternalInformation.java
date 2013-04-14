package org.pshdl.interpreter;

import java.util.regex.*;

import org.pshdl.interpreter.utils.*;

public class InternalInformation {
	public static final Pattern aiFormatName = Pattern.compile("(.*?)(?:\\{(?:(\\d+)(?:\\:(\\d+))?)\\})?(\\" + FluidFrame.REG_POSTFIX + ")?");

	public final String baseName;
	public final String fullName;
	public final boolean isReg;
	public final boolean isPred;
	public final int bitStart, bitEnd;
	public final int baseWidth, actualWidth;

	public InternalInformation(String fullName, int baseWidth) {
		super();
		this.fullName = fullName;
		this.isReg = fullName.endsWith(FluidFrame.REG_POSTFIX);
		this.baseWidth = baseWidth;
		this.isPred = fullName.startsWith(FluidFrame.PRED_PREFIX);
		Matcher matcher = aiFormatName.matcher(fullName);
		if (matcher.matches()) {
			this.baseName = matcher.group(1);
			if (matcher.group(2) == null) {
				this.bitStart = -1;
				this.bitEnd = -1;
				this.actualWidth = baseWidth;
			} else if (matcher.group(3) != null) {
				this.bitStart = Integer.parseInt(matcher.group(2));
				this.bitEnd = Integer.parseInt(matcher.group(3));
				this.actualWidth = (bitEnd - bitStart) + 1;
			} else {
				this.bitStart = this.bitEnd = Integer.parseInt(matcher.group(2));
				this.actualWidth = 1;
			}
		} else
			throw new IllegalArgumentException("Name:" + fullName + " is not valid!");
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
		StringBuilder builder = new StringBuilder();
		builder.append("InternalInformation [baseName=").append(baseName).append(", fullName=").append(fullName).append(", isReg=").append(isReg).append(", isPred=")
				.append(isPred).append(", bitStart=").append(bitStart).append(", bitEnd=").append(bitEnd).append(", baseWidth=").append(baseWidth).append(", actualWidth=")
				.append(actualWidth).append("]");
		return builder.toString();
	}

	public String baseNameWithReg() {
		return isReg ? baseName + FluidFrame.REG_POSTFIX : baseName;
	}

}
