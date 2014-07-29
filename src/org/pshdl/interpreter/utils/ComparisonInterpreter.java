/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *
 *     Copyright (C) 2013 Karsten Becker (feedback (at) pshdl (dot) org)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     This License does not grant permission to use the trade names, trademarks,
 *     service marks, or product names of the Licensor, except as required for
 *     reasonable and customary use in describing the origin of the Work.
 *
 * Contributors:
 *     Karsten Becker - initial API and implementation
 ******************************************************************************/
package org.pshdl.interpreter.utils;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pshdl.interpreter.ExecutableModel;
import org.pshdl.interpreter.IHDLBigInterpreter;
import org.pshdl.interpreter.IHDLInterpreter;
import org.pshdl.interpreter.VariableInformation;

public class ComparisonInterpreter implements IHDLBigInterpreter {

	public static class ConsoleReporter implements DiffReport {
		private final PrintStream out;

		public ConsoleReporter() {
			this(System.out);
		}

		public ConsoleReporter(PrintStream out) {
			this.out = out;
		}

		@Override
		public void reportOutputLongDiff(long deltaCycle, long aVal, long bVal, String name, int... idx) {
			String sIdx = "";
			if ((idx != null) && (idx.length != 0)) {
				sIdx = Arrays.toString(idx);
			}
			out.printf("deltaCycle:%5d %s%s aVal=%X bVal=%X%n", deltaCycle, name, sIdx, aVal, bVal);
		}

		@Override
		public void reportOutputBigDiff(long deltaCycle, BigInteger aVal, BigInteger bVal, String name, int... idx) {
			String sIdx = "";
			if ((idx != null) && (idx.length != 0)) {
				sIdx = Arrays.toString(idx);
			}
			out.printf("deltaCycle:%5d %s%s aVal=%X bVal=%X%n", deltaCycle, name, sIdx, aVal, bVal);
		}

		@Override
		public void reportDeltaCycleDiff(long deltaCycleA, long deltaCycleB) {
			out.printf("Delta Cycle diff, a: %d b: %d%n", deltaCycleA, deltaCycleB);
		}

	}

	public static interface DiffReport {

		void reportOutputLongDiff(long deltaCycle, long aVal, long bVal, String name, int... idx);

		void reportOutputBigDiff(long deltaCycle, BigInteger aVal, BigInteger bVal, String name, int... idx);

		void reportDeltaCycleDiff(long deltaCycleA, long deltaCycleB);

	}

	private final IHDLInterpreter b;
	private final IHDLBigInterpreter bBig;
	private final IHDLInterpreter a;
	private final IHDLBigInterpreter aBig;
	private final List<Integer> varListA = new ArrayList<>();
	private final List<Integer> varListB = new ArrayList<>();
	private final Map<String, Integer> varIdx = new HashMap<>();
	private final DiffReport report;
	private final ExecutableModel em;
	private final boolean terminate;

	public ComparisonInterpreter(IHDLInterpreter a, IHDLInterpreter b, ExecutableModel em, DiffReport report, boolean terminate) {
		this.a = a;
		this.aBig = BigInterpreterAdapter.adapt(a);
		this.b = b;
		this.bBig = BigInterpreterAdapter.adapt(b);
		this.em = em;
		this.terminate = terminate;
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
				varListA.add(getIndexOf(a, v));
				varListB.add(getIndexOf(b, v));
			} else {
				varListA.add(-1);
				varListB.add(-1);
			}
		}
	}

	protected int getIndexOf(IHDLInterpreter a, final VariableInformation v) {
		try {
			return a.getIndex(v.name);
		} catch (final Exception e) {
			if (v.name.startsWith(em.moduleName))
				return a.getIndex(v.name.substring(em.moduleName.length() + 1));
			return a.getIndex(em.moduleName + "." + v.name);
		}
	}

	public void checkAllVarsLong() {
		final VariableInformation[] variables = em.variables;
		boolean hasDiff = false;
		for (int i = 0; i < variables.length; i++) {
			final VariableInformation v = variables[i];
			if (!"#null".equals(v.name)) {
				if (v.dimensions.length == 0) {
					final long aVal = a.getOutputLong(varListA.get(i));
					final long bVal = b.getOutputLong(varListB.get(i));
					if (aVal != bVal) {
						hasDiff = true;
						report.reportOutputLongDiff(getDeltaCycle(), aVal, bVal, v.name);
					}
				} else {
					final int[] arrIdx = new int[v.dimensions.length];
					for (int j = 0; j < v.dimensions[0]; j++) {
						arrIdx[0] = j;
						final long aVal = a.getOutputLong(varListA.get(i), arrIdx);
						final long bVal = b.getOutputLong(varListB.get(i), arrIdx);
						if (aVal != bVal) {
							hasDiff = true;
							report.reportOutputLongDiff(getDeltaCycle(), aVal, bVal, v.name, arrIdx);
						}
					}
				}
			}
		}
		if (hasDiff && terminate)
			throw new RuntimeException("A mismatch has been found");
	}

	@Override
	public void setInput(String name, BigInteger value, int... arrayIdx) {
		aBig.setInput(name, value, arrayIdx);
		bBig.setInput(name, value, arrayIdx);
	}

	@Override
	public void setInput(int idx, BigInteger value, int... arrayIdx) {
		aBig.setInput(varListA.get(idx), value, arrayIdx);
		bBig.setInput(varListB.get(idx), value, arrayIdx);
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
			report.reportOutputLongDiff(getDeltaCycle(), aVal, bVal, name);
		}
		return aVal;
	}

	@Override
	public long getOutputLong(int idx, int... arrayIdx) {
		final long aVal = a.getOutputLong(varListA.get(idx), arrayIdx);
		final long bVal = b.getOutputLong(varListB.get(idx), arrayIdx);
		if (aVal != bVal) {
			report.reportOutputLongDiff(getDeltaCycle(), aVal, bVal, em.variables[idx].name);
		}
		return aVal;
	}

	@Override
	public BigInteger getOutputBig(String name, int... arrayIdx) {
		final BigInteger aVal = aBig.getOutputBig(name, arrayIdx);
		final BigInteger bVal = bBig.getOutputBig(name, arrayIdx);
		if (!aVal.equals(bVal)) {
			report.reportOutputBigDiff(getDeltaCycle(), aVal, bVal, name);
		}
		return aVal;
	}

	@Override
	public BigInteger getOutputBig(int idx, int... arrayIdx) {
		final BigInteger aVal = aBig.getOutputBig(varListA.get(idx), arrayIdx);
		final BigInteger bVal = bBig.getOutputBig(varListB.get(idx), arrayIdx);
		if (!aVal.equals(bVal)) {
			report.reportOutputBigDiff(getDeltaCycle(), aVal, bVal, em.variables[idx].name);
		}
		return aVal;
	}

	@Override
	public void run() {
		a.run();
		b.run();
	}

	@Override
	public long getDeltaCycle() {
		final long deltaCycleA = a.getDeltaCycle();
		final long deltaCycleB = b.getDeltaCycle();
		if (deltaCycleA != deltaCycleB) {
			report.reportDeltaCycleDiff(deltaCycleA, deltaCycleB);
		}
		return deltaCycleA;
	}

	@Override
	public void initConstants() {
		a.initConstants();
		b.initConstants();
	}

	@Override
	public void close() throws Exception {
		a.close();
		b.close();
	}

	@Override
	public void setFeature(Feature feature, Object value) {
		a.setFeature(feature, value);
		b.setFeature(feature, value);
	}

}
