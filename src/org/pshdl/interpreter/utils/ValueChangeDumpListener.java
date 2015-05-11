package org.pshdl.interpreter.utils;

import java.io.OutputStream;

import org.pshdl.interpreter.IChangeListener;
import org.pshdl.interpreter.IHDLTestbenchInterpreter.ITestbenchStepListener;
import org.pshdl.interpreter.VariableInformation;
import org.pshdl.interpreter.utils.ValueChangeDump.TimeBase;
import org.pshdl.interpreter.utils.ValueChangeDump.Variable;

public class ValueChangeDumpListener implements IChangeListener, ITestbenchStepListener {

	private final ValueChangeDump vcd;
	private final Variable[] vars;
	private long lastDC;

	public ValueChangeDumpListener(OutputStream os, VariableInformation[] varInfo, String... varNames) {
		vars = new Variable[varInfo.length];
		vcd = new ValueChangeDump(os, null, null, TimeBase.ps, null);
		if (varNames != null) {
			for (int varIdx = 0; varIdx < varInfo.length; varIdx++) {
				final VariableInformation vi = varInfo[varIdx];
				for (final String varRegex : varNames) {
					if (vi.name.matches(varRegex)) {
						addVariable(vi, varIdx);
						break;
					}
				}
			}
		}
	}

	public void addVariable(VariableInformation varInfo, int varIdx) {
		if ((varInfo.dimensions != null) && (varInfo.dimensions.length > 0))
			throw new IllegalArgumentException("Can not monitor arrays");
		vars[varIdx] = vcd.addVariable(varInfo.name, varInfo.width, varInfo.isRegister);
	}

	@Override
	public void valueChangedLong(long deltaCycle, VariableInformation varInfo, int varIdx, long oldValue, long newValue) {
		final Variable variable = vars[varIdx];
		if (variable != null) {
			if (lastDC != deltaCycle) {
				vcd.timeStamp(deltaCycle);
			}
			variable.recordValue(newValue);
		}
	}

	@Override
	public void valueChangedLongArray(long deltaCycle, VariableInformation varInfo, int varIdx, long[] oldValue, long[] newValue) {

	}

	@Override
	public void valueChangedPredicate(long deltaCycle, VariableInformation varInfo, int varIdx, boolean oldValue, boolean newValue, long oldUpdate, long newUpdate) {
		final Variable variable = vars[varIdx];
		if (variable != null) {
			if (lastDC != deltaCycle) {
				vcd.timeStamp(deltaCycle);
				lastDC = deltaCycle;
			}
			variable.recordValue(newValue ? 1 : 0);
		}
	}

	@Override
	public void valueChangedPredicateArray(long deltaCycle, VariableInformation varInfo, int varIdx, boolean[] oldValue, boolean[] newValue, long[] oldUpdate, long[] newUpdate) {

	}

	@Override
	public void testbenchStart() {
		vcd.dumpHeaders();
	}

	@Override
	public void testbenchEnd() {
	}

	@Override
	public boolean nextStep(long currentTime, long currentStep) {
		return true;
	}

}
