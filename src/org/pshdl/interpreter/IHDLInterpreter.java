package org.pshdl.interpreter;

import java.math.*;

public interface IHDLInterpreter {

	public abstract void setInput(String name, BigInteger value, int... arrayIdx);

	public abstract void setInput(int idx, BigInteger value, int... arrayIdx);

	public abstract void setInput(String name, long value, int... arrayIdx);

	public abstract void setInput(int idx, long value, int... arrayIdx);

	public abstract int getIndex(String name);

	public abstract String getName(int idx);

	public abstract long getOutputLong(String name, int... arrayIdx);

	public abstract long getOutputLong(int idx, int... arrayIdx);

	public abstract BigInteger getOutputBig(String name, int... arrayIdx);

	public abstract BigInteger getOutputBig(int idx, int... arrayIdx);

	public abstract void run();

	public abstract int getDeltaCycle();

}