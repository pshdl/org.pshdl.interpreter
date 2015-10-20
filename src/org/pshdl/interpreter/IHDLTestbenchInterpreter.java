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

import org.pshdl.interpreter.JavaPSHDLLib.TimeUnit;

public interface IHDLTestbenchInterpreter extends IHDLInterpreter {
	public static interface ITestbenchStepListener {
		/**
		 * When called, the test-bench execution starts
		 */
		public void testbenchStart();

		/**
		 * When called, the test-bench is done
		 */
		public void testbenchEnd();

		/**
		 *
		 * @param currentTime
		 *            the current time in
		 *            {@link IHDLTestbenchInterpreter#getTimeBase()} units
		 * @param currentStep
		 *            the current delta cycle
		 * @return <code>true</code> if the execution should continue,
		 *         <code>false</code> if it should stop
		 */
		public boolean nextStep(long currentTime, long currentStep);
	}

	/**
	 * Returns the current simulation time in {@link #getTimeBase()} units
	 *
	 * @return the current simulation time in {@link #getTimeBase()} units
	 */
	public long getTime();

	/**
	 * The time-base for the simulation.
	 * {@link IHDLTestbenchInterpreter#getTime()} will be in that unit
	 *
	 * @return time-base for the simulation
	 */
	public TimeUnit getTimeBase();

	/**
	 *
	 * @param maxTime
	 *            the maximum amount of time until the simulation stops
	 * @param maxSteps
	 *            the maximum amounts of steps (delta cycles) until the
	 *            simulation stops
	 * @param listener
	 *            a listener that can monitor the progress
	 * @param main
	 *            will be called to update the model
	 */
	public void runTestbench(long maxTime, long maxSteps, ITestbenchStepListener listener, Runnable main);
}
