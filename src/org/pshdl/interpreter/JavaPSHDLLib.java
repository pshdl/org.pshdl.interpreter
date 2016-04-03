package org.pshdl.interpreter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.pshdl.interpreter.NativeRunner.IRunListener;
import org.pshdl.interpreter.utils.PSHDLFormatter;

public class JavaPSHDLLib {
	public static enum Assert {
		FATAL, ERROR, WARNING, INFO
	}

	public static enum Sync {
		ASYNC, SYNC
	}

	public static enum Active {
		LOW, HIGH
	}

	public static enum Edge {
		RISING, FALLING
	}

	public static enum TimeUnit {
		FS, PS, NS, US, MS, S;

		public String toDisplay() {
			if (this == US)
				return "µs";
			return name().toLowerCase();
		}
	}

	public class DefaultListener implements IRunListener {

		@Override
		public void printfReceived(String printf) {
			// TODO Auto-generated method stub

		}

		@Override
		public void assertionReceived(Assert assertLevel, String message) {
			logAssert(assertLevel.ordinal(), "%s %s:%s", now(), assertLevel, message);
			if (breakLevel.ordinal() >= assertLevel.ordinal())
				throw new AssertionError(format("%s %s:%s", now(), assertLevel, message));
		}

	}

	public IRunListener listener = new DefaultListener();

	private final IHDLInterpreter code;
	private final Logger logger = Logger.getLogger("pshdl");

	public JavaPSHDLLib(IHDLInterpreter code) {
		this.code = code;
	}

	public Assert breakLevel = Assert.ERROR;

	public void assertThat(boolean expression, long assertIdx, String message) {
		if (!expression) {
			final Assert assertLevel = getLevel(assertIdx);
			listener.assertionReceived(assertLevel, message);
		}
	}

	private Assert getLevel(long assertIdx) {
		return Assert.values()[(int) assertIdx];
	}

	private void logAssert(long assertIdx, String fmt, Object... objects) {
		final Assert level = getLevel(assertIdx);
		final String message = format(fmt, objects);
		Level logLevel = Level.OFF;
		switch (level) {
		case FATAL:
			logLevel = Level.SEVERE;
			break;
		case ERROR:
			logLevel = Level.SEVERE;
			break;
		case WARNING:
			logLevel = Level.WARNING;
			break;
		case INFO:
			logLevel = Level.INFO;
			break;
		}
		logger.log(logLevel, message);
	}

	private String format(String fmt, Object... objects) {
		return new PSHDLFormatter(fmt).format(objects).toString();
	}

	public void log(PSHDLFormatter formatter, Object... objects) {
		logger.log(Level.INFO, formatter.format(objects).toString());
	}

	public String now() {
		if (code instanceof IHDLTestbenchInterpreter) {
			final IHDLTestbenchInterpreter tb = (IHDLTestbenchInterpreter) code;
			return tb.getTime() + " " + tb.getTimeBase().toDisplay();
		}
		return "Delta-cycle: " + code.getDeltaCycle();
	}

	public static long pow(long base, long power) {
		long result = 1;
		long p = base;
		long n = power;
		while (n > 0) {
			if ((n % 2) != 0) {
				result = result * p;
			}
			p = p * p;
			n = n / 2;
		}
		return result;
	}
}
