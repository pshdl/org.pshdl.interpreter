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

import java.util.Formatter;

public enum Instruction {
	noop(0, 0, "Does nothing"), //
	// Bit Accesses
	bitAccessSingle(1, 1, "Access a single bit", "bit", "internalIdx"), //
	bitAccessSingleRange(1, 1, "Access a bit range", "from", "to"), //
	// Casts
	cast_int(1, 1, "Re-interprets the operand as int and resizes it", "targetSize", "currentSize"), //
	cast_uint(1, 1, "Re-interprets the operand as uint and resizes it", "targetSize", "currentSize"), //
	// Load operations
	loadConstant(0, 1, "Loads a value from the constant storage", "constantIdx", "type"), //
	loadConstantString(0, 1, "Loads a string from the constant storage", "constantIdx"), //
	loadInternal(0, 1, "Loads a value from an internal", "internalIdx"), //
	// Functions
	invokeFunction(0, 1, "Invokes a function", "functionIdx", "argCount"),
	// Concatenation
	concat(2, 1, "Concatenate operands, assumes the width as indicated", "widthLeft", "widthRight"), //
	// Constants
	const0(0, 1, "Loads a zero to the stack"), //
	const1(0, 1, "Loads a 1 to the stack"), //
	const2(0, 1, "Loads a 2 to the stack"), //
	constAll1(0, 1, "Loads a all 1's constant to the stack with the given width", "width"), //
	// Execution control edges
	isFallingEdge(0, 0, "Checks for an falling edge on from an internal signal", "internalIdx"), //
	isRisingEdge(0, 0, "Checks for an rising edge on from an internal signal", "internalIdx"), //
	// Execution control predicates
	posPredicate(0, 0, "Checks if the given predicate has evaluated to true", "internalIdx"), //
	negPredicate(0, 0, "Checks if the given predicate has evaluated to false", "internalIdx"), //
	// Bit operations
	and(2, 1, "A binary & operation", "targetSizeWithType"), //
	or(2, 1, "A binary | operation", "targetSizeWithType"), //
	xor(2, 1, "A binary ^ operation", "targetSizeWithType"), //
	// Arithemetic operations
	div(2, 1, "An arithmetic / operation", "targetSizeWithType"), //
	minus(2, 1, "An arithmetic - operation", "targetSizeWithType"), //
	mul(2, 1, "An arithmetic * operation", "targetSizeWithType"), //
	plus(2, 1, "An arithmetic + operation", "targetSizeWithType"), //
	mod(2, 1, "An arithmetic % operation", "targetSizeWithType"), //
	pow(2, 1, "An arithmetic ** operation", "targetSizeWithType"), //
	// Equality operations
	eq(2, 1, "An equality == operation, result is boolean (0/1)"), //
	greater(2, 1, "An equality > operation, result is boolean (0/1)"), //
	greater_eq(2, 1, "An equality >= operation, result is boolean (0/1)"), //
	less(2, 1, "An equality < operation, result is boolean (0/1)"), //
	less_eq(2, 1, "An equality <= operation, result is boolean (0/1)"), //
	not_eq(2, 1, "An equality != operation, result is boolean (0/1)"), //
	// Logical operations
	logiOr(2, 1, "A logical || operation, result is boolean (0/1)"), //
	logiAnd(2, 1, "A logical && operation, result is boolean (0/1)"), //
	logiNeg(1, 1, "Logically negates, result is boolean (0/1)"), //
	// Negation operations
	arith_neg(1, 1, "Arithmetically negates", "targetSizeWithType"), //
	bit_neg(1, 1, "Bit inverts", "targetSizeWithType"), //
	// Shift operations
	sll(2, 1, "A shift << operation", "targetSizeWithType"), //
	sra(2, 1, "A shift >> operation", "targetSizeWithType"), //
	srl(2, 1, "A shift >>> operation", "targetSizeWithType"), //
	// Memory
	pushAddIndex(1, 0, "Pushes an additional index into the write stack for that memory", "internalIdx", "bitIdx"), //
	writeInternal(1, 0, "Writes a value to an internal (and every array position)", "internalIdx"), //
	;
	public final int argCount;
	public final String description;
	public final int push;
	public final int pop;
	public final String[] args;

	Instruction(int pop, int push, String desc, String... args) {
		if (args != null) {
			this.argCount = args.length;
		} else {
			this.argCount = 0;
		}
		this.push = push;
		this.pop = pop;
		this.description = desc;
		this.args = args;
	}

	public int toByte() {
		if (argCount == 1)
			return ordinal() | 0x40;
		if (argCount == 2)
			return ordinal() | 0x80;
		return ordinal();
	}

	public static void main(String[] args) {
		final Formatter f = new Formatter();
		f.format("Name                  |byte| Stack|Description   | first argument | second argument%n");
		f.format(":--------------------:|----|------|--------------|----------------|----------------%n");
		for (final Instruction i : Instruction.values()) {
			f.format("%21s |0x%02X| %s | %s | %s | %s%n", i.name(), i.toByte(), toStack(i), i.description, i.argCount > 0 ? i.args[0] : "", i.argCount > 1 ? i.args[1] : "");
		}
		System.out.println(f);
	}

	private static String toStack(Instruction i) {
		if ((i.pop == 0) && (i.push == 0))
			return "±0";
		final StringBuilder sb = new StringBuilder();
		if (i.pop != 0) {
			sb.append(-i.pop).append(' ');
		}
		if (i.push != 0) {
			sb.append('+').append(i.push);
		}
		return sb.toString();
	}
}