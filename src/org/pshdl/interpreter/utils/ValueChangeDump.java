package org.pshdl.interpreter.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

public class ValueChangeDump {
	private static int idCounter = 0;

	public class Variable implements Comparable<Variable> {

		public final String name;
		public final int width;
		public final String shortCode;
		public final boolean isReg;
		public long lastValue = 0;

		public Variable(String name, int width, boolean isReg) {
			super();
			this.name = "top/" + name;
			this.width = width;
			this.isReg = isReg;
			int thisId = idCounter++;
			final StringBuilder sb = new StringBuilder();
			do {
				final int rem = thisId % 93;
				final char code = (char) ('!' + rem);
				sb.append(code);
				thisId /= 93;
			} while (thisId > 0);
			this.shortCode = sb.toString();
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(isReg ? "wire " : "reg ");
			sb.append(width).append(' ');
			sb.append(shortCode).append(' ');
			sb.append(name.substring(name.lastIndexOf('/') + 1));
			return sb.toString();
		}

		public CharSequence recordValue(long value) {
			if (value == lastValue)
				return "";
			return forceRecord(value);
		}

		public CharSequence forceRecord(long value) {
			lastValue = value;
			final StringBuilder sb = new StringBuilder();
			if (width == 1) {
				sb.append(value & 1);
			} else {
				sb.append('b');
				for (int i = width - 1; i >= 0; i--) {
					final long newValue = value >> i;
					sb.append(newValue & 1);
				}
				sb.append(' ');
			}
			sb.append(shortCode);
			stream.println(sb);
			return sb;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + (isReg ? 1231 : 1237);
			result = (prime * result) + ((name == null) ? 0 : name.hashCode());
			result = (prime * result) + width;
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
			final Variable other = (Variable) obj;
			if (isReg != other.isReg)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (width != other.width)
				return false;
			return true;
		}

		@Override
		public int compareTo(Variable o) {
			return name.compareTo(o.name);
		}

	}

	public static enum TimeBase {
		s, ms, us, ns, ps, fs;
	}

	public final String comment;
	public final Date date;
	public final TimeBase timeBase;
	public final String version;
	public final List<Variable> variables = new ArrayList<>();
	private final PrintStream stream;

	public ValueChangeDump(OutputStream os) {
		this(os, null, null, TimeBase.ps, null);
	}

	/**
	 *
	 * @param os
	 * @param comment
	 *            can be <code>null</code>
	 * @param date
	 *            can be <code>null</code>
	 * @param timeBase
	 * @param version
	 *            can be <code>null</code>
	 */
	public ValueChangeDump(OutputStream os, String comment, Date date, TimeBase timeBase, String version) {
		super();
		this.stream = new PrintStream(os);
		this.comment = comment;
		this.date = date;
		this.timeBase = timeBase;
		this.version = version;
	}

	public void dumpHeaders() {
		if (date != null) {
			printLine("date", new SimpleDateFormat().format(date));
		}
		if (version != null) {
			printLine("version", version);
		}
		if (comment != null) {
			printLine("comment", comment);
		}
		printLine("timescale", 1 + " " + timeBase);
		final TreeMap<String, Set<Variable>> sortedVars = new TreeMap<>();
		for (final Variable variable : variables) {
			final int last = variable.name.lastIndexOf('/');
			final String modName = variable.name.substring(0, last + 1);
			Set<Variable> list = sortedVars.get(modName);
			if (list == null) {
				list = new TreeSet<>();
				sortedVars.put(modName, list);
			}
			list.add(variable);
		}
		final Stack<String> scope = new Stack<>();
		for (final Entry<String, Set<Variable>> variable : sortedVars.entrySet()) {
			final String modName = variable.getKey();
			final String[] modules = modName.split("/");
			int matchingScopes = 0;
			for (int i = 0; i < modules.length; i++) {
				if (scope.size() <= i) {
					break;
				}
				if (scope.get(i).equals(modules[i])) {
					matchingScopes++;
				} else {
					break;
				}
			}
			final int scopeCount = scope.size() - matchingScopes;
			for (int i = 0; i < scopeCount; i++) {
				scope.pop();
				printLine("upscope", null);
			}
			for (int i = matchingScopes; i < modules.length; i++) {
				final String push = modules[i];
				scope.push(push);
				printLine("scope", "module " + push);
			}
			for (final Variable var : variable.getValue()) {
				printLine("var", var.toString());
			}
		}
		while (!scope.isEmpty()) {
			scope.pop();
			printLine("upscope", null);
		}
		printLine("enddefinitions", null);
		dumpVars();
	}

	public void dumpVars() {
		final StringBuilder sb = new StringBuilder();
		sb.append('\n');
		for (final Variable variable : variables) {
			sb.append(variable.recordValue(0)).append('\n');
		}
		printLine("dumpvars", sb.toString());
	}

	public void timeStamp(long deltaCycle) {
		stream.println("#" + deltaCycle);
	}

	public static void main(String[] args) {
		final ValueChangeDump vcd = new ValueChangeDump(System.out, "My comment", new Date(), TimeBase.ps, "1.0");
		vcd.addVariable("a/x/v", 1, false);
		vcd.addVariable("b/c", 8, false);
		vcd.addVariable("b/d/z", 8, false);
		vcd.addVariable("b/e/z", 8, false);
		vcd.addVariable("c/d/e", 16, true);
		vcd.dumpHeaders();
	}

	public Variable addVariable(String name, int width, boolean isReg) {
		final Variable newVar = new Variable(name, width, isReg);
		variables.add(newVar);
		return newVar;
	}

	public void addVariables(Variable... variable) {
		if (variable == null)
			return;
		variables.addAll(Arrays.asList(variable));
	}

	private void printLine(String headerName, String content) {
		stream.print("$");
		stream.print(headerName);
		if (content != null) {
			stream.print(" ");
			stream.print(content);
			if (!content.endsWith("\n")) {
				stream.print(' ');
			}
			stream.println("$end");
		} else {
			stream.println(" $end");
		}
	}

}
