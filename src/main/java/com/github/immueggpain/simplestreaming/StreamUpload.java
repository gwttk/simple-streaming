package com.github.immueggpain.simplestreaming;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Start uploader", name = "upload", mixinStandardHelpOptions = true, version = Launcher.VERSTR)
public class StreamUpload implements Callable<Void> {

	@Option(names = { "-p", "--port" }, required = true, description = "server's upload port")
	public int serverPort;
	@Option(names = { "-s", "--server" }, required = true, description = "server's name(ip or domain)")
	public String serverName;
	@Option(names = { "-f", "--file" }, required = true, description = "path to ts file")
	public String filepath;

	@Override
	public Void call() throws Exception {
		try {
			Socket socket = new Socket(serverName, serverPort);

			OutputStream os = socket.getOutputStream();
			byte[] buf = new byte[Launcher.BUFLEN];

			RandomAccessFile file = new RandomAccessFile(filepath, "r");
			file.seek(file.length());

			try {
				while (true) {
					long pos = copyLarge(file, os, buf);
					// to EOF, check if file is overwriten
					if (file.length() < pos) {
						file.close();
						file = new RandomAccessFile(filepath, "r");
						file.seek(file.length());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println("close socket");
			file.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static long copyLarge(final RandomAccessFile input, final OutputStream output, final byte[] buffer)
			throws IOException {
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return input.getFilePointer();
	}

}
