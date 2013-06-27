package org.pshdl.interpreter.utils;

import java.io.*;
import java.math.*;
import java.util.*;

import org.pshdl.interpreter.*;

public class ComparisonInterpreter implements IHDLInterpreter {

	public static class ConsoleReporter implements DiffReport {
		private final PrintStream out;

		public ConsoleReporter() {
			this(System.out);
		}

		public ConsoleReporter(PrintStream out) {
			this.out = out;
		}

		@Override
		public void reportOutputLongDiff(long aVal, long bVal, String name, int... idx) {
			String sIdx = "";
			if (idx != null) {
				sIdx = Arrays.toString(idx);
			}
			out.printf("%s%s aVal=%X bVal=%X\n", name, sIdx, aVal, bVal);
		}

		@Override
		public void reportOutputBigDiff(BigInteger aVal, BigInteger bVal, String name, int... idx) {
			String sIdx = "";
			if (idx != null) {
				sIdx = Arrays.toString(idx);
			}
			out.printf("%s%s aVal=%X bVal=%X\n", name, sIdx, aVal, bVal);
		}

		@Override
		public void reportDeltaCycleDiff(int deltaCycleA, int deltaCycleB) {
			out.printf("Delta Cycle diff, a: %d b: %d", deltaCycleA, deltaCycleB);
		}

	}

	public static interface DiffReport {

		void reportOutputLongDiff(long aVal, long bVal, String name, int... idx);

		void reportOutputBigDiff(BigInteger aVal, BigInteger bVal, String name, int... idx);

		void reportDeltaCycleDiff(int deltaCycleA, int deltaCycleB);

	}

	private final IHDLInterpreter b;
	private final IHDLInterpreter a;
	private final List<Integer> varListA = new ArrayList<Integer>();
	private final List<Integer> varListB = new ArrayList<Integer>();
	private final Map<String, Integer> varIdx = new HashMap<String, Integer>();
	private final DiffReport report;
	private final ExecutableModel em;

	public ComparisonInterpreter(IHDLInterpreter a, IHDLInterpreter b, ExecutableModel em, DiffReport report) {
		this.a = a;
		this.b = b;
		this.em = em;
		if (report != null) {
			this.report = report;
		} else {
			this.report = new ConsoleReporter();
		}
		final VariableInformation[] variables = em.variables;
		for (int i = 0; i < variables.length; i++) {
			final VariableInformation v = variables[i];
			varIdx.put(v.name, i);
			if (!"#null".equals(v.name)) {
				varListA.add(a.getIndex(v.name));
				varListB.add(b.getIndex(v.name));
			} else {
				varListA.add(-1);
				varListB.add(-1);
			}
		}
	}

	public void checkAllVarsLong() {
		final VariableInformation[] variables = em.variables;
		for (int i = 0; i < variables.length; i++) {
			final VariableInformation v = variables[i];
			if (!"#null".equals(v.name)) {
				if (v.dimensions.length == 0) {
					final long aVal = a.getOutputLong(varListA.get(i));
					final long bVal = b.getOutputLong(varListB.get(i));
					if (aVal != bVal) {
						report.reportOutputLongDiff(aVal, bVal, v.name);
					}
				} else {
					final int[] arrIdx = new int[v.dimensions.length];
					for (int j = 0; j < v.dimensions[0]; j++) {
						arrIdx[0] = j;
						final long aVal = a.getOutputLong(varListA.get(i), arrIdx);
						final long bVal = b.getOutputLong(varListB.get(i), arrIdx);
						if (aVal != bVal) {
							report.reportOutputLongDiff(aVal, bVal, v.name, arrIdx);
						}
					}
				}
			}
		}
	}

	@Override
	public void setInput(String name, BigInteger value, int... arrayIdx) {
		a.setInput(name, value, arrayIdx);
		b.setInput(name, value, arrayIdx);
	}

	@Override
	public void setInput(int idx, BigInteger value, int... arrayIdx) {
		a.setInput(varListA.get(idx), value, arrayIdx);
		b.setInput(varListB.get(idx), value, arrayIdx);
	}

	@Override
	public void setInput(String name, long value, int... arrayIdx) {
		a.setInput(name, value, arrayIdx);
		b.setInput(name, value, arrayIdx);
	}

	@Override
	public void setInput(int idx, long value, int... arrayIdx) {
		a.setInput(varListA.get(idx), value, arrayIdx);
		b.setInput(varListB.get(idx), value, arrayIdx);
	}

	@Override
	public int getIndex(String name) {
		return varIdx.get(name);
	}

	@Override
	public String getName(int idx) {
		return null;
	}

	@Override
	public long getOutputLong(String name, int... arrayIdx) {
		final long aVal = a.getOutputLong(name, arrayIdx);
		final long bVal = b.getOutputLong(name, arrayIdx);
		if (aVal != bVal) {
			report.reportOutputLongDiff(aVal, bVal, name);
		}
		return aVal;
	}

	@Override
	public long getOutputLong(int idx, int... arrayIdx) {
		final long aVal = a.getOutputLong(varListA.get(idx), arrayIdx);
		final long bVal = b.getOutputLong(varListB.get(idx), arrayIdx);
		if (aVal != bVal) {
			report.reportOutputLongDiff(aVal, bVal, em.variables[idx].name);
		}
		return aVal;
	}

	@Override
	public BigInteger getOutputBig(String name, int... arrayIdx) {
		final BigInteger aVal = a.getOutputBig(name, arrayIdx);
		final BigInteger bVal = b.getOutputBig(name, arrayIdx);
		if (!aVal.equals(bVal)) {
			report.reportOutputBigDiff(aVal, bVal, name);
		}
		return aVal;
	}

	@Override
	public BigInteger getOutputBig(int idx, int... arrayIdx) {
		final BigInteger aVal = a.getOutputBig(varListA.get(idx), arrayIdx);
		final BigInteger bVal = b.getOutputBig(varListB.get(idx), arrayIdx);
		if (!aVal.equals(bVal)) {
			report.reportOutputBigDiff(aVal, bVal, em.variables[idx].name);
		}
		return aVal;
	}

	@Override
	public void run() {
		a.run();
		b.run();
	}

	@Override
	public int getDeltaCycle() {
		final int deltaCycleA = a.getDeltaCycle();
		final int deltaCycleB = b.getDeltaCycle();
		if (deltaCycleA != deltaCycleB) {
			report.reportDeltaCycleDiff(deltaCycleA, deltaCycleB);
		}
		return deltaCycleA;
	}

}
