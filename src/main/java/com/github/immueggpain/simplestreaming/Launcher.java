package com.github.immueggpain.simplestreaming;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(description = "Streaming with OBS", name = "simple-streaming", mixinStandardHelpOptions = true,
		version = Launcher.VERSTR, subcommands = { HelpCommand.class, StreamServer.class, BMPPeer.class })
public class Launcher implements Callable<Void> {

	public static final String VERSTR = "0.0.1";
	public static final int LOCAL_PORT = 2233;
	public static final int LOCAL_OVPN_PORT = 1194;

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
