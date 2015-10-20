package org.pshdl.interpreter;

import java.util.Arrays;

public class FunctionInformation {
	public final String name;
	public final String[] annotations;
	public final ParameterInformation returnType;
	public final ParameterInformation[] parameter;
	public final boolean isStatement;
	private String signature = null;

	public FunctionInformation(String name, boolean isStatement, ParameterInformation returnType, ParameterInformation[] arguments, String[] annotations) {
		super();
		this.name = name;
		this.returnType = returnType;
		this.parameter = arguments;
		this.annotations = annotations;
		this.isStatement = isStatement;
	}

	public String signature() {
		if (signature != null)
			return signature;
		final StringBuffer sb = new StringBuffer();
		if (returnType != null) {
			sb.append(returnType.simpleSignature()).append('_');
		}
		if (isStatement) {
			sb.append('s').append('_');
		}
		sb.append(name.replace('.', '_'));
		for (final ParameterInformation pi : parameter) {
			sb.append('_').append(pi.simpleSignature());
		}
		signature = sb.toString();
		return signature;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + Arrays.hashCode(parameter);
		result = (prime * result) + ((name == null) ? 0 : name.hashCode());
		result = (prime * result) + ((returnType == null) ? 0 : returnType.hashCode());
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
		final FunctionInformation other = (FunctionInformation) obj;
		if (!Arrays.equals(parameter, other.parameter))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (returnType != other.returnType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FunctionInformation [name=");
		builder.append(name);
		builder.append(", isStatement");
		builder.append(isStatement);
		builder.append(", returnType=");
		builder.append(returnType);
		builder.append(", arguments=");
		builder.append(Arrays.toString(parameter));
		builder.append(", annotations=");
		builder.append(Arrays.toString(annotations));
		builder.append("]");
		return builder.toString();
	}
}
