package com.github.immueggpain.simplestreaming;

import java.io.File;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Start streaming server with a file", name = "sf", mixinStandardHelpOptions = true,
		version = Launcher.VERSTR)
public class Serve implements Callable<Void> {

	@Option(names = { "-p", "--port" }, required = true, description = "listening port")
	public int serverPort;

	@Option(names = { "-f", "--file" }, required = true, description = "path to ts file")
	public String filepath;

	private ServerSocket serverSocket;

	@Override
	public Void call() throws Exception {
		Thread acceptThread = Util.execAsync("accept_thread", () -> accept_thread(serverPort));

		acceptThread.join();
		return null;
	}

	private void accept_thread(int listen_port) {
		try {
			// setup sockets
			serverSocket = new ServerSocket(listen_port);

			while (true) {
				Socket socket = serverSocket.accept();
				Thread sendThread = Util.execAsync("send_thread", () -> send_thread(socket));
				sendThread.join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void send_thread(Socket socket) {
		try {
			OutputStream os = socket.getOutputStream();
			byte[] buf = new byte[1024 * 64];

			FileUtils.copyFile(new File(filepath), os);
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
