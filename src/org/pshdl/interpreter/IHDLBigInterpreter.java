/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *
 *     Copyright (C) 2014 Karsten Becker (feedback (at) pshdl (dot) org)
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
package org.pshdl.interpreter;

import java.math.BigInteger;

public interface IHDLBigInterpreter extends IHDLInterpreter {
	public static class BigInterpreterAdapter implements IHDLBigInterpreter {
		private final IHDLInterpreter interpeter;

		private BigInterpreterAdapter(IHDLInterpreter interpreter) {
			this.interpeter = interpreter;
		}

		public static IHDLBigInterpreter adapt(IHDLInterpreter interpreter) {
			if (interpreter instanceof IHDLBigInterpreter) {
				final IHDLBigInterpreter bi = (IHDLBigInterpreter) interpreter;
				return bi;
			}
			return new BigInterpreterAdapter(interpreter);
		}

		@Override
		public void close() throws Exception {
			interpeter.close();
		}

		@Override
		public void setFeature(Feature feature, Object value) {
			interpeter.setFeature(feature, value);
		}

		@Override
		public void setInput(String name, long value, int... arrayIdx) {
			interpeter.setInput(name, value, arrayIdx);
		}

		@Override
		public void setInput(int idx, long value, int... arrayIdx) {
			interpeter.setInput(idx, value, arrayIdx);
		}

		@Override
		public int getIndex(String name) {
			return interpeter.getIndex(name);
		}

		@Override
		public String getName(int idx) {
			return interpeter.getName(idx);
		}

		@Override
		public long getOutputLong(String name, int... arrayIdx) {
			return interpeter.getOutputLong(name, arrayIdx);
		}

		@Override
		public long getOutputLong(int idx, int... arrayIdx) {
			return interpeter.getOutputLong(idx, arrayIdx);
		}

		@Override
		public void run() {
			interpeter.run();
		}

		@Override
		public void initConstants() {
			interpeter.initConstants();
		}

		@Override
		public long getDeltaCycle() {
			return interpeter.getDeltaCycle();
		}

		@Override
		public BigInteger getOutputBig(String name, int... arrayIdx) {
			return BigInteger.valueOf(getOutputLong(name, arrayIdx));
		}

		@Override
		public BigInteger getOutputBig(int idx, int... arrayIdx) {
			return BigInteger.valueOf(getOutputLong(idx, arrayIdx));
		}

		@Override
		public void setInput(String name, BigInteger value, int... arrayIdx) {
			setInput(name, value.longValue(), arrayIdx);
		}

		@Override
		public void setInput(int idx, BigInteger value, int... arrayIdx) {
			setInput(idx, value.longValue(), arrayIdx);
		}
	}

	public abstract BigInteger getOutputBig(String name, int... arrayIdx);

	public abstract BigInteger getOutputBig(int idx, int... arrayIdx);

	public abstract void setInput(String name, BigInteger value, int... arrayIdx);

	public abstract void setInput(int idx, BigInteger value, int... arrayIdx);
}
