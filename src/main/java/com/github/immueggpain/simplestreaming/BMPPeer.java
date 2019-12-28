package com.github.immueggpain.simplestreaming;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Start BMP client", name = "client", mixinStandardHelpOptions = true, version = Launcher.VERSTR)
public class BMPPeer implements Callable<Void> {

	@Option(names = { "-p", "--port" }, required = true, description = "server's port")
	public int serverPort;
	@Option(names = { "-s", "--server" }, required = true, description = "server's name(ip or domain)")
	public String serverName;

	@Override
	public Void call() throws Exception {
		return null;
	}

}
