package com.github.immueggpain.simplestreaming;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

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
				System.out.println("new client");
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
			byte[] buf = new byte[1024 * 8];

			RandomAccessFile file = new RandomAccessFile(filepath, "r");
			long length = file.length();
			file.seek(length);

			long ct = copyLarge(file, os, buf);

			System.out.println("close socket " + ct);
			file.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static long copyLarge(final RandomAccessFile input, final OutputStream output, final byte[] buffer)
			throws IOException {
		long count = 0;
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

}
