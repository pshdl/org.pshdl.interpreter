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
package org.pshdl.interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class NativeRunner implements IHDLInterpreter {

	private final ExecutableModel model;
	private final Map<String, Integer> varIdx = new LinkedHashMap<>();
	private final PrintStream outPrint;
	private final BlockingDeque<String> responses = new LinkedBlockingDeque<>();
	public final StringWriter commentOutput = new StringWriter();
	private final Process process;
	private final int timeOutInSeconds;
	public final StringBuilder testInput = new StringBuilder();

	public NativeRunner(final InputStream is, OutputStream os, ExecutableModel model, Process process, int timeOutInSeconds, String name) {
		this.model = model;
		this.process = process;
		try {
			outPrint = new PrintStream(os, true, "UTF-8");
		} catch (final UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		}
		final VariableInformation[] variables = model.variables;
		for (int i = 0; i < variables.length; i++) {
			final VariableInformation varI = variables[i];
			varIdx.put(varI.name, i);
		}
		this.timeOutInSeconds = timeOutInSeconds;
		new Thread(new Runnable() {

			@Override
			public void run() {
				final BufferedReader inRead = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
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
		}, "NativeRunner InputReader:" + name).start();
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
		try {
			final int exitValue = process.exitValue();
			throw new RuntimeException("The process died with return code:" + exitValue);
		} catch (final Exception e) {
		}
		if (data != null) {
			outPrint.println(command + " " + data);
			testInput.append(command + " " + data + "\n");
		} else {
			outPrint.println(command);
			testInput.append(command + "\n");
		}
		outPrint.flush();
		try {
			final String response = responses.pollFirst(timeOutInSeconds, TimeUnit.SECONDS);
			if (response == null)
				throw new IllegalArgumentException("TimeOut during communication");
			final String[] split = response.split(" ");
			final String[] res = new String[split.length - 1];
			if (split[0].equals(">" + command)) {
				for (int i = 1; i < split.length; i++) {
					final String string = split[i];
					res[i - 1] = string.trim();
				}
				return res;
			}
			System.out.println(testInput);
			throw new IllegalArgumentException("Did not expect the following response:" + response + responses);
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

	private boolean closed = false;

	@Override
	public void close() throws Exception {
		if (closed)
			throw new IllegalStateException("Runner already closed");
		closed = true;
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

	@Override
	public VariableInformation[] getVariableInformation() {
		return model.variables;
	}

}
