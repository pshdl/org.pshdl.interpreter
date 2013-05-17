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

import java.util.*;
import java.util.regex.*;

/**
 * This class contains all information about an internal
 * 
 * @author Karsten Becker
 * 
 */
public class InternalInformation {
	/**
	 * An internal that ends with this string is the shadow register of a
	 * regular internal
	 */
	public static final String REG_POSTFIX = "$reg";

	/**
	 * Predicates start with this string
	 */
	public static final String PRED_PREFIX = "$Pred_";

	/**
	 * A regular expression for extracting all parts of the internal.
	 * <ul>
	 * <li>group(1) = baseName</li>
	 * <li>group(2) = arrays</li>
	 * <li>group(3) = bitStart</li>
	 * <li>group(4) = bitEnd</li>
	 * <li>group(5) = {@link #REG_POSTFIX}</li>
	 * </ul>
	 */
	public static final Pattern aiFormatName = Pattern.compile("(.*?)" // baseName
			+ "((?:\\[.*?\\])*)" // arrays
			+ "(?:\\{(?:(\\d+)" // first Digit if range
			+ "(?:\\:(\\d+))?)\\})?" // second Digit if range
			+ "(\\" + REG_POSTFIX + ")?");
	public static final Pattern array = Pattern.compile("\\[(.*?)\\]");

	/**
	 * The full name is the base name, but also includes bit accesses and
	 * {@link FluidFrame#REG_POSTFIX}
	 */
	public final String fullName;

	/**
	 * If <code>true</code> this internal is the shadow register
	 */
	public final boolean isShadowReg;

	/**
	 * If <code>true</code> this internal is a predicate
	 */
	public final boolean isPred;

	/**
	 * bitStart indicates the largest bit index of that access, while bitEnd
	 * indicates the lowest bit index. Both values can be -1 to indicate that no
	 * bit access is given. For single bit accesses both values are the same
	 */
	public final int bitStart, bitEnd;

	/**
	 * The baseWidth represents the width of the variable, whereas actualWidth
	 * represents the width as given by the bit accesses
	 */
	public final int actualWidth;

	/**
	 * The indices for each dimension. A -1 indicates that this dimension is not
	 * fixed
	 */
	public final int arrayIdx[];

	/**
	 * If <code>true</code> this internal is always accessing the same array
	 * indices
	 */
	public final boolean fixedArray;

	/**
	 * A reference to the variable
	 */
	public final VariableInformation info;

	public InternalInformation(boolean isShadowReg, boolean isPred, int bitStart, int bitEnd, int[] arrayIdx, VariableInformation info) {
		super();
		this.isShadowReg = isShadowReg;
		this.isPred = isPred;
		this.bitStart = bitStart;
		this.bitEnd = bitEnd;
		this.arrayIdx = arrayIdx;
		boolean isFixed = true;
		for (int element : arrayIdx) {
			if (element == -1) {
				isFixed = false;
			}
		}
		this.fixedArray = isFixed;
		this.info = info;
		StringBuilder sb = new StringBuilder();
		sb.append(info.name);
		if (isFixed) {
			for (int idx : arrayIdx) {
				sb.append('[').append(idx).append(']');
			}
		}
		if ((bitStart != -1) && (bitEnd != -1)) {
			this.actualWidth = (bitEnd - bitStart) + 1;
			sb.append('{');
			if (bitEnd == bitStart) {
				sb.append(bitStart);
			} else {
				sb.append(bitStart).append(':').append(bitEnd);
			}
			sb.append('}');
		} else {
			this.actualWidth = info.width;
		}
		if (isShadowReg) {
			sb.append(REG_POSTFIX);
		}
		this.fullName = sb.toString();
	}

	public InternalInformation(String fullName, VariableInformation info) {
		super();
		this.fullName = fullName;
		this.isShadowReg = fullName.endsWith(REG_POSTFIX);
		this.isPred = fullName.startsWith(PRED_PREFIX);
		this.info = info;
		Matcher matcher = aiFormatName.matcher(fullName);
		List<Integer> arrIdx = new LinkedList<Integer>();
		if (matcher.matches()) {
			if (matcher.group(3) == null) {
				this.bitStart = -1;
				this.bitEnd = -1;
				this.actualWidth = info.width;
			} else if (matcher.group(4) != null) {
				this.bitStart = Integer.parseInt(matcher.group(3));
				this.bitEnd = Integer.parseInt(matcher.group(4));
				this.actualWidth = (bitEnd - bitStart) + 1;
			} else {
				this.bitStart = this.bitEnd = Integer.parseInt(matcher.group(3));
				this.actualWidth = 1;
			}
			Matcher m = array.matcher(matcher.group(2));
			while (m.find()) {
				arrIdx.add(Integer.parseInt(m.group(1)));
			}
		} else
			throw new IllegalArgumentException("Name:" + fullName + " is not valid!");
		this.arrayIdx = new int[arrIdx.size()];
		for (int i = 0; i < arrIdx.size(); i++) {
			arrayIdx[i] = arrIdx.get(i);
		}
		this.fixedArray = true;
	}

	/**
	 * Returns the baseName with varying suffixes
	 * 
	 * @param includeArray
	 *            if <code>true</code> and the array is a {@link #fixedArray}
	 *            then then the arrays are included in the string
	 * @param includeReg
	 *            if <code>true</code> and the internal {@link #isShadowReg} is
	 *            <code>true</code>, then {@value #REG_POSTFIX} is appended
	 * @return the name including array and reg if chosen too. The baseName can
	 *         potentially contain {@value #PRED_PREFIX}
	 */
	public String baseName(boolean includeArray, boolean includeReg) {
		StringBuilder sb = new StringBuilder();
		sb.append(info.name);
		if (includeArray && fixedArray) {
			for (int idx : arrayIdx) {
				sb.append('[').append(idx).append(']');
			}
		}
		if (isShadowReg && includeReg) {
			sb.append(REG_POSTFIX);
		}
		return sb.toString();
	}

	/**
	 * The static version of {@link #baseName(boolean, boolean)}
	 * 
	 * @param name
	 *            the string to transform
	 * @param includeArray
	 *            if <code>true</code> and the array is a {@link #fixedArray}
	 *            then then the arrays are included in the string
	 * @param includeReg
	 *            if <code>true</code> and the internal {@link #isShadowReg} is
	 *            <code>true</code>, then {@value #REG_POSTFIX} is appended
	 * @return the name including array and reg if chosen too. The baseName can
	 *         potentially contain {@value #PRED_PREFIX}
	 */
	public static String getBaseName(String name, boolean includeArray, boolean includeReg) {
		Matcher matcher = aiFormatName.matcher(name);
		if (matcher.matches()) {
			String baseName = matcher.group(1);
			if (includeArray) {
				baseName += matcher.group(2);
			}
			if (includeReg && name.endsWith(REG_POSTFIX)) {
				baseName += REG_POSTFIX;
			}
			return baseName;
		}
		throw new IllegalArgumentException("Not a well formed name:" + name);
	}

	/**
	 * If the string should end with {@value #REG_POSTFIX}, it returns a string
	 * without it.
	 * 
	 * @param string
	 * @return
	 */
	public static String stripReg(String string) {
		if (string.endsWith(REG_POSTFIX))
			return string.substring(0, string.length() - 4);
		return string;
	}

	@Override
	public String toString() {
		return fullName;
	}

}
