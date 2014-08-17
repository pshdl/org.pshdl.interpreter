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

import java.io.PrintStream;

public interface IChangeListener {

	public static class PrintStreamListener implements IChangeListener {
		private final PrintStream out;

		public PrintStreamListener() {
			this(System.out);
		}

		public PrintStreamListener(PrintStream out) {
			this.out = out;
		}

		@Override
		public void valueChangedLong(long deltaCycle, String name, long oldValue, long newValue) {
			out.printf("%6d %20s old:%8d new:%8d%n", deltaCycle, name, oldValue, newValue);
		}

		@Override
		public void valueChangedLongArray(long deltaCycle, String name, long[] oldValue, long[] newValue) {
			out.printf("%6d %20s ", deltaCycle, name);
			for (int i = 0; i < newValue.length; i++) {
				if (oldValue[i] != newValue[i]) {
					out.printf("%3d old:%8d new: %8d", i, oldValue[i], newValue[i]);
				}
			}
		}

		@Override
		public void valueChangedPredicate(long deltaCycle, String name, boolean oldValue, boolean newValue, long oldUpdate, long newUpdate) {
			out.printf("%6d %20s old:%b new:%b%n", deltaCycle, name, oldValue, newValue);
		}

		@Override
		public void valueChangedPredicateArray(long deltaCycle, String name, boolean[] oldValue, boolean[] newValue, long[] oldUpdate, long[] newUpdate) {
			out.printf("%6d %20s ", deltaCycle, name);
			for (int i = 0; i < newValue.length; i++) {
				if (oldValue[i] != newValue[i]) {
					out.printf("%3d old:%b new: %b", i, oldValue[i], newValue[i]);
				}
			}
		}
	}

	public void valueChangedLong(long deltaCycle, String name, long oldValue, long newValue);

	public void valueChangedLongArray(long deltaCycle, String name, long oldValue[], long newValue[]);

	public void valueChangedPredicate(long deltaCycle, String name, boolean oldValue, boolean newValue, long oldUpdate, long newUpdate);

	public void valueChangedPredicateArray(long deltaCycle, String name, boolean oldValue[], boolean newValue[], long oldUpdate[], long newUpdate[]);
}
