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

import org.pshdl.interpreter.*;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.access.*;
import org.pshdl.interpreter.utils.*;

public abstract class ExecutableFrame {

	protected final FastInstruction[] instructions;

	protected int currentPos = 0;
	protected final EncapsulatedAccess internals[], internals_prev[];

	protected final int outputID;

	public boolean regUpdated;
	public final int uniqueID;
	public final EncapsulatedAccess outputAccess;
	protected final int[] writeIndex = new int[10];

	private HDLFrameInterpreter fir;

	public ExecutableFrame(HDLFrameInterpreter fir, Frame f, EncapsulatedAccess[] internals, EncapsulatedAccess[] internals_prev) {
		this.fir = fir;
		this.internals = internals;
		this.internals_prev = internals_prev;
		this.outputID = f.outputId;
		this.uniqueID = f.uniqueID;
		this.instructions = f.instructions;
		this.outputAccess = internals[outputID];
	}

	public abstract void execute(int deltaCycle, int epsCycle);

	public static void main(String[] args) {
		for (Instruction i : Instruction.values()) {
			System.out.println("protected static final int " + i.name() + "=" + i.ordinal() + ";");
		}
	}

	protected boolean isPrinting() {
		return fir.printing;
	}
}
