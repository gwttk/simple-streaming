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

import org.apache.commons.io.FileUtils;

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
		public CircularByteBuffer buf;
	}

	private HashMap<Socket, Downloader> activeDownloaders = new HashMap<>();
	private Socket currentSocket;
	private long uploaderBytes;

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
			CircularByteBuffer buf = new CircularByteBuffer(Launcher.BUFLEN * 4);
			byte[] bs = new byte[Launcher.BUFLEN];

			// add me to active downloaders
			Downloader me = new Downloader();
			me.t = System.currentTimeMillis();
			me.buf = buf;
			synchronized (activeDownloaders) {
				activeDownloaders.put(socket, me);
			}

			while (true) {
				// wait for buf
				if (buf.available() == 0) {
					Thread.yield();
					continue;
				}

				// buf ok, send it
				int n = me.buf.get(bs);
				os.write(bs, 0, n);

				me.t = System.currentTimeMillis();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			synchronized (activeDownloaders) {
				activeDownloaders.remove(socket);
			}
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
				Util.closeQuietly(currentSocket);
				currentSocket = socket;
				Util.execAsync("upload_thread", () -> upload_thread(socket));
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
		uploaderBytes = 0;
		try (Socket socket_ = socket) {
			InputStream is = socket.getInputStream();
			byte[] buf = new byte[Launcher.BUFLEN];

			while (true) {
				int len = is.read(buf);
				if (len == -1)
					return;
				uploaderBytes += len;

				synchronized (activeDownloaders) {
					for (Downloader downloader : activeDownloaders.values()) {
						int n = downloader.buf.put(buf, 0, len);
						int missing = len - n;
						if (missing > 0)
							System.out.println("missing " + missing);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** daemon cleaning expired players */
	private void remove_expired_player_thread() {
		long lastCheckTime = System.currentTimeMillis();
		long lastCheckBytes = uploaderBytes;
		while (true) {
			synchronized (activeDownloaders) {
				long now = System.currentTimeMillis();
				System.out.println("==downloader check==" + now);
				System.out.println("bytes uploaded: " + uploaderBytes);
				double byterate = (double) (uploaderBytes - lastCheckBytes) / (now - lastCheckTime) * 1000;
				String byteRate = FileUtils.byteCountToDisplaySize((long) byterate);
				System.out.println("bytes rate: " + byteRate + "/s");
				lastCheckTime = now;
				lastCheckBytes = uploaderBytes;
				for (Iterator<Entry<Socket, Downloader>> iterator = activeDownloaders.entrySet().iterator(); iterator
						.hasNext();) {
					Entry<Socket, Downloader> entry = iterator.next();
					Socket key = entry.getKey();
					Downloader playerInfo = entry.getValue();
					long last = playerInfo.t;
					if (now - last > 20000 && playerInfo.buf.available() > 0) {
						System.out.println(String.format("dead downloader: %s", key.getRemoteSocketAddress()));
						iterator.remove();
						try {
							key.close();
						} catch (IOException e) {
						}
					} else {
						System.out.println(String.format("active downloader: %s", key.getRemoteSocketAddress()));
					}
				}
			}
			Util.sleep(5000);
		}
	}

}
