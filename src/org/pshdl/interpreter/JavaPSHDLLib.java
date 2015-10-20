package org.pshdl.interpreter;

import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		FS, PS, NS, US, MS, S
	}

	private final IHDLInterpreter code;
	private final Logger logger = Logger.getLogger("pshdl");

	public JavaPSHDLLib(IHDLInterpreter code) {
		this.code = code;
	}

	public Assert breakLevel = Assert.ERROR;

	public void assertThat(boolean expression, long assertIdx, String message) {
		final Assert assertLevel = getLevel(assertIdx);
		if (!expression) {
			if (breakLevel.ordinal() >= assertIdx)
				throw new AssertionError(format("%s %s:%s", now(), assertLevel, message));
			printf(assertIdx, "%s %s:%s", now(), assertLevel, message);
		}
	}

	private Assert getLevel(long assertIdx) {
		return Assert.values()[(int) assertIdx];
	}

	private void printf(long assertIdx, String fmt, Object... objects) {
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
		try (Formatter formatter = new Formatter()) {
			return formatter.format(fmt, objects).toString();
		}
	}

	private String now() {
		if (code instanceof IHDLTestbenchInterpreter) {
			final IHDLTestbenchInterpreter tb = (IHDLTestbenchInterpreter) code;
			return tb.getTime() + " " + tb.getTimeBase().name().toLowerCase();
		}
		return "Delta-cycle: " + code.getDeltaCycle();
	}

	public static long pow(long a, long n) {
		long result = 1;
		long p = a;
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
