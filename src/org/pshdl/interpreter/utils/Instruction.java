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

public enum Instruction {
	noop(0, 0, "Does nothing"), //
	// Bit Accesses
	bitAccessSingle(1, 1, "Access a single bit", "bit"), //
	bitAccessSingleRange(1, 1, "Access a bit range", "from", "to"), //
	// Casts
	cast_int(1, 1, "Re-interprets the operand as int and resizes it", "targetSize", "currentSize"), //
	cast_uint(1, 1, "Re-interprets the operand as uint and resizes it", "targetSize", "currentSize"), //
	// Load operations
	loadConstant(0, 1, "Loads a value from the constant storage", "constantIdx"), //
	loadInternal(0, 1, "Loads a value from an internal", "inernalIdx"), //
	// Concatenation
	concat(2, 1, "Concatenate operands, assumes the width as indicated", "widthLeft", "widthRight"), //
	// Constants
	const0(0, 1, "Loads a zero to the stack"), //
	const1(0, 1, "Loads a 1 to the stack"), //
	const2(0, 1, "Loads a 2 to the stack"), //
	constAll1(0, 1, "Loads a all 1's constant to the stack with the given width", "width"), //
	// Execution control edges
	isFallingEdge(0, 0, "Checks for an falling edge on from an internal signal", "inernalIdx"), //
	isRisingEdge(0, 0, "Checks for an rising edge on from an internal signal", "inernalIdx"), //
	// Execution control predicates
	posPredicate(0, 0, "Checks if the given predicate has evaluated to true", "inernalIdx"), //
	negPredicate(0, 0, "Checks if the given predicate has evaluated to false", "inernalIdx"), //
	// Bit operations
	and(2, 1, "A binary & operation"), //
	or(2, 1, "A binary | operation"), //
	xor(2, 1, "A binary ^ operation"), //
	// Arithemetic operations
	div(2, 1, "An arithmetic / operation"), //
	minus(2, 1, "An arithmetic - operation"), //
	mul(2, 1, "An arithmetic * operation"), //
	plus(2, 1, "An arithmetic + operation"), //
	// Equality operations
	eq(2, 1, "An equality == operation"), //
	greater(2, 1, "An equality > operation"), //
	greater_eq(2, 1, "An equality >= operation"), //
	less(2, 1, "An equality < operation"), //
	less_eq(2, 1, "An equality <= operation"), //
	not_eq(2, 1, "An equality != operation"), //
	// Logical operations
	logiOr(2, 1, "A logical || operation"), //
	logiAnd(2, 1, "A logical && operation"), //
	logiNeg(1, 1, "Logically negates"), //
	// Negation operations
	arith_neg(1, 1, "Arithmetically negates"), //
	bit_neg(1, 1, "Bit inverts"), //
	// Shift operations
	sll(2, 1, "A shift << operation"), //
	sra(2, 1, "A shift >> operation"), //
	srl(2, 1, "A shift >>> operation"), //
	// Memory
	pushAddIndex(1, 0, "Pushes an additional index into the write stack for that memory"), //
	writeMemory(1, 0, "Writes the current item to the memory at the offset"), //
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
}