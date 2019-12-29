package com.github.immueggpain.simplestreaming;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(description = "Streaming with OBS", name = "simple-streaming", mixinStandardHelpOptions = true,
		version = Launcher.VERSTR,
		subcommands = { HelpCommand.class, StreamUpload.class, StreamServer.class, Serve.class })
public class Launcher implements Callable<Void> {

	public static final String VERSTR = "0.1.3";
	public static final int LOCAL_PORT = 2233;
	public static final int LOCAL_OVPN_PORT = 1194;
	public static final int BUFLEN = 1024 * 1024 * 2;

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Launcher()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Void call() throws Exception {
		CommandLine.usage(this, System.out);
		return null;
	}

}
