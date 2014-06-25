package org.pshdl.interpreter;

public interface IHDLTestbenchInterpreter extends IHDLInterpreter {
	public static interface ITestbenchStepListener {
		public boolean nextStep(long currentTime, long currentStep);
	}

	public void runTestbench(long maxTime, long maxSteps, ITestbenchStepListener listener);
}
