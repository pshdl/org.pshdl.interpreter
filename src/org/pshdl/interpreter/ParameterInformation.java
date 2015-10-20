package org.pshdl.interpreter;

/**
 * <ul>
 * <li>RWType rw. If <code>null</code>, {@link RWType#READ} is used as default.
 * </li>
 * <li>Type type. Can <b>not</b> be <code>null</code>.</li>
 * <li>HDLQualifiedName enumSpec. Can be <code>null</code>.</li>
 * <li>HDLQualifiedName ifSpec. Can be <code>null</code>.</li>
 * <li>ArrayList&lt;HDLFunctionParameter&gt; funcSpec. Can be <code>null</code>.
 * </li>
 * <li>HDLFunctionParameter funcReturnSpec. Can be <code>null</code>.</li>
 * <li>HDLVariable name. Can be <code>null</code>.</li>
 * <li>HDLExpression width. Can be <code>null</code>.</li>
 * <li>ArrayList&lt;HDLExpression&gt; dim. Can be <code>null</code>.</li>
 * <li>Boolean constant. Can <b>not</b> be <code>null</code>.</li>
 * </ul>
 *
 */
public class ParameterInformation {

	public final RWType rw;
	public final Type type;
	public final String enumSpec;
	public final String ifSpec;
	public final ParameterInformation[] funcSpec;
	public final ParameterInformation funcReturnSpec;
	public final String name;
	public final int width;
	public final int[] dim;
	public final Boolean constant;

	/**
	 *
	 * @param rw
	 * @param type
	 * @param enumSpec
	 * @param ifSpec
	 * @param funcSpec
	 * @param funcReturnSpec
	 * @param name
	 * @param width
	 * @param dim
	 * @param constant
	 */
	public ParameterInformation(RWType rw, Type type, String enumSpec, String ifSpec, ParameterInformation[] funcSpec, ParameterInformation funcReturnSpec, String name, int width,
			int[] dim, Boolean constant) {
		super();
		this.rw = rw;
		this.type = type;
		this.enumSpec = enumSpec;
		this.ifSpec = ifSpec;
		this.funcSpec = funcSpec;
		this.funcReturnSpec = funcReturnSpec;
		this.name = name;
		this.width = width;
		this.dim = dim;
		this.constant = constant;
	}

	public static enum RWType {
		RETURN(""), READ("-"), WRITE("+"), READWRITE("*");
		String str;

		RWType(String op) {
			this.str = op;
		}

		public static RWType getOp(String op) {
			for (final RWType ass : values()) {
				if (ass.str.equals(op))
					return ass;
			}
			return null;
		}

		@Override
		public String toString() {
			return str;
		}
	}

	public static enum Type {
		PARAM_ANY_INT("int<>"), PARAM_ANY_UINT("uint<>"), PARAM_ANY_BIT("bit<>"), PARAM_ANY_IF("interface<>"), PARAM_ANY_ENUM("enum<>"), PARAM_IF("interface"), PARAM_ENUM(
				"enum"), PARAM_FUNCTION("function"), PARAM_BIT("bit"), PARAM_UINT("uint"), PARAM_INT("int"), PARAM_STRING("string"), PARAM_BOOL("bool");
		String str;

		Type(String op) {
			this.str = op;
		}

		public static Type getOp(String op) {
			for (final Type ass : values()) {
				if (ass.str.equals(op))
					return ass;
			}
			return null;
		}

		@Override
		public String toString() {
			return str;
		}
	}

	public String simpleType() {
		switch (type) {
		case PARAM_ANY_BIT:
			return "anyBit";
		case PARAM_ANY_ENUM:
			return "anyEnum";
		case PARAM_ANY_IF:
			return "anyInterface";
		case PARAM_ANY_INT:
			return "anyInt";
		case PARAM_ANY_UINT:
			return "anyUInt";
		case PARAM_ENUM:
			return "E" + enumSpec;
		case PARAM_FUNCTION:
			return "Fp";
		case PARAM_IF:
			return "I" + ifSpec;
		case PARAM_BIT:
			if (width > 0)
				return "b" + width;
			return "b1";
		case PARAM_INT:
			if (width > 0)
				return "i" + width;
			return "i32";
		case PARAM_UINT:
			if (width > 0)
				return "u" + width;
			return "u32";
		case PARAM_BOOL:
			return "bool";
		case PARAM_STRING:
			return "s";
		}
		throw new IllegalArgumentException("Unexpected Parameter Type:" + type);
	}

	public String simpleSignature() {
		final String simpleType = simpleType();
		if (dim == null)
			return simpleType;
		final StringBuilder sb = new StringBuilder(simpleType);
		for (final int d : dim) {
			sb.append("d");
			if (d > 0) {
				sb.append(d);
			}
		}
		return sb.toString();
	}
}
