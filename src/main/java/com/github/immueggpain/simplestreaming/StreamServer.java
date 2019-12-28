package com.github.immueggpain.simplestreaming;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Start streaming server", name = "server", mixinStandardHelpOptions = true,
		version = Launcher.VERSTR)
public class StreamServer implements Callable<Void> {

	@Option(names = { "-p", "--port" }, required = true, description = "listening port")
	public int serverPort;

	private static class Player {
		/** time of the last packet received from this player */
		public long t;
		public InetSocketAddress saddr;
		public long pktCount = 0;
	}

	private HashMap<InetSocketAddress, Player> activePlayers = new HashMap<>();
	private DatagramSocket socket;

	@Override
	public Void call() throws Exception {
		Thread recvThread = Util.execAsync("recv_thread", () -> recv_thread(serverPort));
		Thread removeExpiredPlayerThread = Util.execAsync("remove_expired_player_thread",
				() -> remove_expired_player_thread());

		recvThread.join();
		removeExpiredPlayerThread.join();
		return null;
	}

	private void recv_thread(int listen_port) {
		try {
			// setup sockets
			InetAddress allbind_addr = InetAddress.getByName("0.0.0.0");
			socket = new DatagramSocket(listen_port, allbind_addr);

			// setup packet
			byte[] recvBuf = new byte[4096];
			DatagramPacket p = new DatagramPacket(recvBuf, recvBuf.length);

			// recv loop
			while (true) {
				p.setData(recvBuf);
				socket.receive(p);
				InetSocketAddress saddr = (InetSocketAddress) p.getSocketAddress();
				updatePlayerInfo(saddr, System.currentTimeMillis());
				broadcastPacket(saddr, p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** daemon cleaning expired players */
	private void remove_expired_player_thread() {
		while (true) {
			synchronized (activePlayers) {
				long now = System.currentTimeMillis();
				System.out.println("==player check==" + now);
				for (Iterator<Entry<InetSocketAddress, Player>> iterator = activePlayers.entrySet().iterator(); iterator
						.hasNext();) {
					Entry<InetSocketAddress, Player> entry = iterator.next();
					Player playerInfo = entry.getValue();
					long last = playerInfo.t;
					if (now - last > 60000) {
						System.out.println(String.format("dead player: %s, %d", playerInfo.saddr, playerInfo.pktCount));
						iterator.remove();
					} else {
						System.out
								.println(String.format("active player: %s, %d", playerInfo.saddr, playerInfo.pktCount));
					}
				}
			}
			Util.sleep(5000);
		}
	}

	private void updatePlayerInfo(InetSocketAddress saddr, long t) {
		synchronized (activePlayers) {
			Player player = activePlayers.get(saddr);
			if (player == null) {
				player = new Player();
				player.saddr = saddr;
				activePlayers.put(saddr, player);
			}
			player.t = t;
			player.pktCount++;
		}
	}

	private void broadcastPacket(InetSocketAddress source, DatagramPacket p) {
		synchronized (activePlayers) {
			for (Entry<InetSocketAddress, Player> entry : activePlayers.entrySet()) {
				InetSocketAddress dest = entry.getValue().saddr;

				if (dest.equals(source))
					continue;

				p.setSocketAddress(dest);
				try {
					socket.send(p);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
