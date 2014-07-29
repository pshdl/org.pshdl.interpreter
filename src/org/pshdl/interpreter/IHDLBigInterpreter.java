package org.pshdl.interpreter;

import java.math.BigInteger;

public interface IHDLBigInterpreter extends IHDLInterpreter {
	public static class BigInterpreterAdapter implements IHDLBigInterpreter {
		private final IHDLInterpreter interpeter;

		private BigInterpreterAdapter(IHDLInterpreter interpreter) {
			this.interpeter = interpreter;
		}

		public static IHDLBigInterpreter adapt(IHDLInterpreter interpreter) {
			if (interpreter instanceof IHDLBigInterpreter) {
				final IHDLBigInterpreter bi = (IHDLBigInterpreter) interpreter;
				return bi;
			}
			return new BigInterpreterAdapter(interpreter);
		}

		@Override
		public void close() throws Exception {
			interpeter.close();
		}

		@Override
		public void setFeature(Feature feature, Object value) {
			interpeter.setFeature(feature, value);
		}

		@Override
		public void setInput(String name, long value, int... arrayIdx) {
			interpeter.setInput(name, value, arrayIdx);
		}

		@Override
		public void setInput(int idx, long value, int... arrayIdx) {
			interpeter.setInput(idx, value, arrayIdx);
		}

		@Override
		public int getIndex(String name) {
			return interpeter.getIndex(name);
		}

		@Override
		public String getName(int idx) {
			return interpeter.getName(idx);
		}

		@Override
		public long getOutputLong(String name, int... arrayIdx) {
			return interpeter.getOutputLong(name, arrayIdx);
		}

		@Override
		public long getOutputLong(int idx, int... arrayIdx) {
			return interpeter.getOutputLong(idx, arrayIdx);
		}

		@Override
		public void run() {
			interpeter.run();
		}

		@Override
		public void initConstants() {
			interpeter.initConstants();
		}

		@Override
		public long getDeltaCycle() {
			return interpeter.getDeltaCycle();
		}

		@Override
		public BigInteger getOutputBig(String name, int... arrayIdx) {
			return BigInteger.valueOf(getOutputLong(name, arrayIdx));
		}

		@Override
		public BigInteger getOutputBig(int idx, int... arrayIdx) {
			return BigInteger.valueOf(getOutputLong(idx, arrayIdx));
		}

		@Override
		public void setInput(String name, BigInteger value, int... arrayIdx) {
			setInput(name, value.longValue(), arrayIdx);
		}

		@Override
		public void setInput(int idx, BigInteger value, int... arrayIdx) {
			setInput(idx, value.longValue(), arrayIdx);
		}
	}

	public abstract BigInteger getOutputBig(String name, int... arrayIdx);

	public abstract BigInteger getOutputBig(int idx, int... arrayIdx);

	public abstract void setInput(String name, BigInteger value, int... arrayIdx);

	public abstract void setInput(int idx, BigInteger value, int... arrayIdx);
}
