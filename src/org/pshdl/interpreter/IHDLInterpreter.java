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
package org.pshdl.interpreter;

import java.math.*;

public interface IHDLInterpreter {

	public abstract void setInput(String name, BigInteger value, int... arrayIdx);

	public abstract void setInput(int idx, BigInteger value, int... arrayIdx);

	public abstract void setInput(String name, long value, int... arrayIdx);

	public abstract void setInput(int idx, long value, int... arrayIdx);

	public abstract int getIndex(String name);

	public abstract String getName(int idx);

	public abstract long getOutputLong(String name, int... arrayIdx);

	public abstract long getOutputLong(int idx, int... arrayIdx);

	public abstract BigInteger getOutputBig(String name, int... arrayIdx);

	public abstract BigInteger getOutputBig(int idx, int... arrayIdx);

	public abstract void run();

	public abstract int getDeltaCycle();

}