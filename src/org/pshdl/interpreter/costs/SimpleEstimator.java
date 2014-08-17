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
package org.pshdl.interpreter.costs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pshdl.interpreter.ExecutableModel;
import org.pshdl.interpreter.Frame;
import org.pshdl.interpreter.Frame.FastInstruction;
import org.pshdl.interpreter.InternalInformation;

public class SimpleEstimator {
	public static class ResourceCosts {
		public final ResourceCosts incomingCosts[];
		public final int delay;
		public final int area;
		public final int power;
		public final Frame frame;

		public ResourceCosts(Frame frame, int delay, int area, int power, ResourceCosts... incoming) {
			super();
			this.delay = delay;
			this.area = area;
			this.power = power;
			this.incomingCosts = incoming;
			this.frame = frame;
		}

		@Override
		public String toString() {
			return "ResourceCosts [delay=" + delay + ", area=" + area + ", power=" + power + "]";
		}

		public ResourceCosts add(ResourceCosts cost, int multiplier, int delayMultiplier) {
			final int newDelay = delay + (cost.delay * delayMultiplier);
			final int newArea = area + (cost.area * multiplier);
			final int newPower = power + (cost.power * multiplier);
			final ArrayList<ResourceCosts> ic = new ArrayList<>();
			if (incomingCosts != null) {
				ic.addAll(Arrays.asList(incomingCosts));
			}
			if (cost.incomingCosts != null) {
				for (int i = 0; i < multiplier; i++) {
					ic.addAll(Arrays.asList(cost.incomingCosts));
				}
			}
			return new ResourceCosts(frame, newDelay, newArea, newPower, ic.toArray(new ResourceCosts[ic.size()]));
		}
	}

	// In the Actel FPGA a block an either be a REG or a LUT, the area is as
	// such equal
	public static final ResourceCosts REG_COSTS = new ResourceCosts(null, 0, 4, 0);
	public static final ResourceCosts LUT_COSTS = new ResourceCosts(null, 1, 4, 1);
	public static final ResourceCosts ROUTING_COSTS = new ResourceCosts(null, 1, 1, 1);

	public static interface ResourceCostSelector {
		ResourceCosts selectCosts(List<ResourceCosts> incomingCosts);
	}

	public static class MaxAreaCostSelector implements ResourceCostSelector {

		@Override
		public ResourceCosts selectCosts(List<ResourceCosts> incomingCosts) {
			ResourceCosts max = new ResourceCosts(null, 0, 0, 0);
			for (final ResourceCosts rc : incomingCosts) {
				if (rc.area >= max.area) {
					max = rc;
				}
			}
			return max;
		}

	}

	public static class MaxDelayCostSelector implements ResourceCostSelector {

		@Override
		public ResourceCosts selectCosts(List<ResourceCosts> incomingCosts) {
			ResourceCosts max = new ResourceCosts(null, 0, 0, 0);
			for (final ResourceCosts rc : incomingCosts) {
				if (rc.delay >= max.delay) {
					max = rc;
				}
			}
			return max;
		}

	}

	public Map<String, ResourceCosts> estimateFrameCosts(ExecutableModel model, ResourceCostSelector costSelector) {
		final Map<String, ResourceCosts> res = new LinkedHashMap<>();
		final Map<Integer, List<ResourceCosts>> knownCosts = new LinkedHashMap<>();
		final Map<Integer, ResourceCosts> knownTotalCosts = new LinkedHashMap<>();
		for (final Frame f : model.frames) {
			ResourceCosts frameCosts = estimateFrame(f, model, knownCosts, costSelector);
			final List<ResourceCosts> totalIncoming = new ArrayList<>();
			for (final ResourceCosts rc : frameCosts.incomingCosts) {
				totalIncoming.add(knownTotalCosts.get(rc.frame.uniqueID));
			}
			final ResourceCosts incoming = costSelector.selectCosts(totalIncoming);
			if (incoming != null) {
				frameCosts = frameCosts.add(incoming, 1, 1);
			}
			knownTotalCosts.put(f.uniqueID, frameCosts);
			final InternalInformation internal = model.internals[f.outputId];
			ResourceCosts varCosts = res.get(internal.info.name);
			if (varCosts == null) {
				varCosts = frameCosts;
			} else {
				varCosts = costSelector.selectCosts(Arrays.asList(frameCosts, varCosts));
			}
			res.put(internal.info.name, varCosts);
		}
		return res;
	}

	public ResourceCosts estimateFrame(Frame f, ExecutableModel model, Map<Integer, List<ResourceCosts>> knownCosts, ResourceCostSelector costSelector) {
		final List<ResourceCosts> incoming = new ArrayList<>();
		for (final int dep : f.internalDependencies) {
			final List<ResourceCosts> list = knownCosts.get(dep);
			if (list != null) {
				incoming.add(costSelector.selectCosts(list));
			}
		}
		ResourceCosts current = new ResourceCosts(f, 0, 0, 0, incoming.toArray(new ResourceCosts[incoming.size()]));
		for (final FastInstruction i : f.instructions) {
			final int targetWidth = i.arg1 >>> 1;
			switch (i.inst) {
			// Usually cost about one LUT per bit
			case and:
			case bit_neg:
			case eq:
			case not_eq:
			case or:
			case xor:
				current = current.add(LUT_COSTS, targetWidth, 1);
				break;
			// Usually cost about one adder
			case arith_neg:
			case greater:
			case greater_eq:
			case less:
			case less_eq:
			case minus:
			case plus:
				// Assume a ripple carry adder (which is not the best estimation
				// here)
				current = current.add(LUT_COSTS, targetWidth * 2, targetWidth);
				break;
			// Virtually free on most FPGAs (usually just occupy routing
			// resources)
			case bitAccessSingle:
			case bitAccessSingleRange:
			case sll:
			case srl:
			case sra:
			case cast_int:
			case cast_uint:
			case concat:
			case const0:
			case const1:
			case const2:
			case constAll1:
			case loadConstant:
				// current = current.add(ROUTING_COSTS, 1, 1);
				break;
			case div:
			case mod:
			case pow:
				current = current.add(LUT_COSTS, targetWidth * targetWidth * 10, targetWidth * 10);
				break;
			case mul:
				current = current.add(LUT_COSTS, targetWidth * targetWidth, targetWidth);
				break;
			// These would be realized as multiplexers
			case posPredicate:
			case negPredicate:
				current = current.add(LUT_COSTS, model.internals[f.outputId].actualWidth, 1);
				break;
			// Only exists due to the nature of the byte code
			case noop:
			case isFallingEdge:
			case isRisingEdge:
			case loadInternal:
			case writeInternal:
				break;
			// This is probably a block ram memory access
			case pushAddIndex:
				current = current.add(ROUTING_COSTS, targetWidth, 1);
				break;
			// Single bit ops
			case logiAnd:
			case logiNeg:
			case logiOr:
				current = current.add(LUT_COSTS, 1, 1);
				break;
			}
		}
		List<ResourceCosts> list = knownCosts.get(f.outputId);
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(current);
		knownCosts.put(f.outputId, list);
		return current;
	}
}
