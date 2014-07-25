package org.pshdl.interpreter;

import java.io.PrintStream;

public interface IChangeListener {

	public static class PrintStreamListener implements IChangeListener {
		private final PrintStream out;

		public PrintStreamListener() {
			this(System.out);
		}

		public PrintStreamListener(PrintStream out) {
			this.out = out;
		}

		@Override
		public void valueChangedLong(long deltaCycle, String name, long oldValue, long newValue) {
			out.printf("%6d %20s old:%8d new:%8d%n", deltaCycle, name, oldValue, newValue);
		}

		@Override
		public void valueChangedLongArray(long deltaCycle, String name, long[] oldValue, long[] newValue) {
			out.printf("%6d %20s ", deltaCycle, name);
			for (int i = 0; i < newValue.length; i++) {
				if (oldValue[i] != newValue[i]) {
					out.printf("%3d old:%8d new: %8d", i, oldValue[i], newValue[i]);
				}
			}
		}

		@Override
		public void valueChangedPredicate(long deltaCycle, String name, boolean oldValue, boolean newValue, long oldUpdate, long newUpdate) {
			out.printf("%6d %20s old:%b new:%b%n", deltaCycle, name, oldValue, newValue);
		}

		@Override
		public void valueChangedPredicateArray(long deltaCycle, String name, boolean[] oldValue, boolean[] newValue, long[] oldUpdate, long[] newUpdate) {
			out.printf("%6d %20s ", deltaCycle, name);
			for (int i = 0; i < newValue.length; i++) {
				if (oldValue[i] != newValue[i]) {
					out.printf("%3d old:%b new: %b", i, oldValue[i], newValue[i]);
				}
			}
		}
	}

	public void valueChangedLong(long deltaCycle, String name, long oldValue, long newValue);

	public void valueChangedLongArray(long deltaCycle, String name, long oldValue[], long newValue[]);

	public void valueChangedPredicate(long deltaCycle, String name, boolean oldValue, boolean newValue, long oldUpdate, long newUpdate);

	public void valueChangedPredicateArray(long deltaCycle, String name, boolean oldValue[], boolean newValue[], long oldUpdate[], long newUpdate[]);
}
