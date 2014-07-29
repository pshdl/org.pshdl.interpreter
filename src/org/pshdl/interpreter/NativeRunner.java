package org.pshdl.interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class NativeRunner implements IHDLInterpreter {

	private final ExecutableModel model;
	private final Map<String, Integer> varIdx = new HashMap<>();
	private final PrintStream outPrint;
	private final BlockingDeque<String> responses = new LinkedBlockingDeque<>();
	public final StringWriter commentOutput = new StringWriter();

	public NativeRunner(final InputStream is, OutputStream os, ExecutableModel model) {
		this.model = model;
		outPrint = new PrintStream(os);
		final VariableInformation[] variables = model.variables;
		for (int i = 0; i < variables.length; i++) {
			final VariableInformation varI = variables[i];
			varIdx.put(varI.name, i);
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				final BufferedReader inRead = new BufferedReader(new InputStreamReader(is));
				String line = null;
				try {
					while ((line = inRead.readLine()) != null) {
						final String trimmedLine = line.trim();
						if (!trimmedLine.isEmpty()) {
							if (trimmedLine.charAt(0) == '#') {
								commentOutput.append(trimmedLine.substring(1));
							} else {
								responses.add(trimmedLine);
							}
						}
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		}, "NativeRunner InputReader").start();
	}

	@Override
	public void setInput(String name, long value, int... arrayIdx) {
		setInput(getIndex(name), value, arrayIdx);
	}

	@Override
	public void setInput(int idx, long value, int... arrayIdx) {
		if ((arrayIdx != null) && (arrayIdx.length != 0)) {
			if (arrayIdx.length > 1)
				throw new IllegalArgumentException("Multi dimensional arrays are currently not supported");
			send("sa", String.format("%d %d %x", idx, arrayIdx[0], value));
		} else {
			send("sn", String.format("%d %x", idx, value));
		}
	}

	@Override
	public int getIndex(String name) {
		final Integer idx = varIdx.get(name);
		if (idx == null)
			throw new IllegalArgumentException("The variable " + name + " is not found");
		return idx;
	}

	@Override
	public String getName(int idx) {
		return model.variables[idx].name;
	}

	@Override
	public long getOutputLong(String name, int... arrayIdx) {
		return getOutputLong(getIndex(name), arrayIdx);
	}

	@Override
	public long getOutputLong(int idx, int... arrayIdx) {
		String[] response;
		if ((arrayIdx != null) && (arrayIdx.length != 0)) {
			if (arrayIdx.length > 1)
				throw new IllegalArgumentException("Multiple dimension currently not supported");
			response = send("ga", String.format("%d %d", idx, arrayIdx[0]));
		} else {
			response = send("gn", String.format("%d", idx));
		}
		if (response.length == 3) {
			final int ridx = Integer.parseInt(response[0].trim());
			if (ridx != idx)
				throw new IllegalArgumentException("Returned incorrect idx, expected " + idx + " got:" + ridx);
			final int roff = Integer.parseInt(response[1].trim());
			if ((arrayIdx == null) || (arrayIdx.length == 0))
				throw new IllegalArgumentException("Returned an array offset, didn't expect one");
			if (roff != arrayIdx[0])
				throw new IllegalArgumentException("Returned incorrect arrayIdx, expected " + arrayIdx[0] + " got:" + roff);
			return new BigInteger(response[2].trim(), 16).longValue();
		}
		final int ridx = Integer.parseInt(response[0].trim());
		if (ridx != idx)
			throw new IllegalArgumentException("Returned incorrect idx, expected " + idx + " got:" + ridx);
		return new BigInteger(response[1].trim(), 16).longValue();
	}

	private String[] send(String command) {
		return send(command, null);
	}

	private String[] send(String command, String data) {
		if (data != null) {
			outPrint.println(command + " " + data);
		} else {
			outPrint.println(command);
		}
		outPrint.flush();
		try {
			final String response = responses.take();
			final String[] split = response.split(" ");
			final String[] res = new String[split.length - 1];
			if (split[0].equals(">" + command)) {
				for (int i = 1; i < split.length; i++) {
					final String string = split[i];
					res[i - 1] = string.trim();
				}
				return res;
			}
			throw new IllegalArgumentException("Did not expect the following response:" + response);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		send("rr");
	}

	@Override
	public long getDeltaCycle() {
		return Long.parseLong(send("dc")[0]);
	}

	@Override
	public void initConstants() {
		send("ic");
	}

	@Override
	public void close() throws Exception {
		send("xn");
	}

	@Override
	public void setFeature(Feature feature, Object value) {
		switch (feature) {
		case disableEdges:
			if ((boolean) value) {
				send("de", "1");
			} else {
				send("de", "0");
			}
			break;
		case disableOutputRegs:
			if ((boolean) value) {
				send("dr", "1");
			} else {
				send("dr", "0");
			}
			break;
		}
	}

}
