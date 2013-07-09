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
package org.pshdl.interpreter.frames;

import java.math.*;

import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.*;

public interface IDebugListener {

	void skippingHandledEdge(int frameUniqueID, InternalInformation internal, boolean risingEdge, Object frame);

	void skippingNotAnEdge(int frameUniqueID, InternalInformation internal, boolean risingEdge, Object frame);

	void skippingPredicateNotFresh(int frameUniqueID, InternalInformation internal, boolean positive, Object frame);

	void skippingPredicateNotMet(int frameUniqueID, InternalInformation internal, boolean positive, BigInteger data, Object frame);

	void twoArgOp(int frameUniqueID, BigInteger b, FastInstruction fi, BigInteger a, BigInteger res, Object frame);

	void oneArgOp(int frameUniqueID, FastInstruction fi, BigInteger a, BigInteger res, Object frame);

	void noArgOp(int frameUniqueID, FastInstruction fi, BigInteger res, Object frame);

	void emptyStack(int frameUniqueID, FastInstruction fi, Object frame);

	void writingResult(int frameUniqueID, InternalInformation internal, BigInteger res, Object frame);

	void loadingInternal(int frameUniqueID, InternalInformation internal, BigInteger value, Object frame);

	void startFrame(int frameUniqueID, int deltaCycle, int epsCycle, Object frame);

	void startCycle(int deltaCycle, int epsCycle, IHDLInterpreter interpreter);

	void copyingRegisterValues(IHDLInterpreter hdlFrameInterpreter);

	void doneCycle(int deltaCycle, IHDLInterpreter interpreter);

	void writeInternal(int uniqueID, int arrayPos, int[] writeIndex, BigInteger value, InternalInformation ii, Object longFrame);

}
