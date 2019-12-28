package com.github.immueggpain.simplestreaming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Start streaming server", name = "server", mixinStandardHelpOptions = true,
		version = Launcher.VERSTR)
public class StreamServer implements Callable<Void> {

	@Option(names = { "-u", "--upload-port" }, required = true, description = "upload listening port")
	public int uploadPort;

	@Option(names = { "-d", "--download-port" }, required = true, description = "download listening port")
	public int downloadPort;

	private static class Downloader {
		/** time of the last packet send to this downloader */
		public long t;
		public byte[] buf;
		public volatile int buflen;
	}

	private HashMap<Socket, Downloader> activeDownloaders = new HashMap<>();

	@Override
	public Void call() throws Exception {
		Thread uploadThread = Util.execAsync("upload_thread", () -> upload_thread());
		Thread downloadThread = Util.execAsync("download_thread", () -> download_thread());
		Thread removeExpiredPlayerThread = Util.execAsync("remove_expired_player_thread",
				() -> remove_expired_player_thread());

		uploadThread.join();
		downloadThread.join();
		removeExpiredPlayerThread.join();
		return null;
	}

	private void download_thread() {
		ServerSocket serverSocket = null;
		try {
			// setup sockets
			serverSocket = new ServerSocket(downloadPort);

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("new downloader");
				Util.execAsync("download_thread", () -> download_thread(socket));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
	}

	private void download_thread(Socket socket) {
		try (Socket socket_ = socket) {
			OutputStream os = socket.getOutputStream();
			byte[] buf = new byte[Launcher.BUFLEN];

			// add me to active downloaders
			Downloader me = new Downloader();
			me.t = System.currentTimeMillis();
			me.buf = buf;
			me.buflen = 0;
			synchronized (activeDownloaders) {
				activeDownloaders.put(socket, me);
			}

			while (true) {
				// wait for buf
				if (me.buflen == 0) {
					Thread.sleep(200);
					continue;
				}

				// buf ok, send it
				os.write(me.buf, 0, me.buflen);

				me.t = System.currentTimeMillis();
				me.buflen = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void upload_thread() {
		ServerSocket serverSocket = null;
		try {
			// setup sockets
			serverSocket = new ServerSocket(uploadPort);

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("new uploader");
				Thread uploadThread = Util.execAsync("upload_thread", () -> upload_thread(socket));
				uploadThread.join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
	}

	private void upload_thread(Socket socket) {
		try (Socket socket_ = socket) {
			InputStream is = socket.getInputStream();
			byte[] buf = new byte[Launcher.BUFLEN];

			int len = is.read(buf);
			if (len == -1)
				return;

			synchronized (activeDownloaders) {
				for (Downloader downloader : activeDownloaders.values()) {
					if (downloader.buflen == 0) {
						System.arraycopy(buf, 0, downloader.buf, 0, len);
						downloader.buflen = len;
					}
				}
			}

			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** daemon cleaning expired players */
	private void remove_expired_player_thread() {
		while (true) {
			synchronized (activeDownloaders) {
				long now = System.currentTimeMillis();
				System.out.println("==player check==" + now);
				for (Iterator<Entry<Socket, Downloader>> iterator = activeDownloaders.entrySet().iterator(); iterator
						.hasNext();) {
					Entry<Socket, Downloader> entry = iterator.next();
					Socket key = entry.getKey();
					Downloader playerInfo = entry.getValue();
					long last = playerInfo.t;
					if (playerInfo.buflen != 0 && now - last > 20000) {
						System.out.println(String.format("dead player: %s", key.getRemoteSocketAddress()));
						iterator.remove();
						try {
							key.close();
						} catch (IOException e) {
						}
					} else {
						System.out.println(String.format("active player: %s", key.getRemoteSocketAddress()));
					}
				}
			}
			Util.sleep(5000);
		}
	}

}
