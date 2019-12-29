package com.github.immueggpain.simplestreaming;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import org.apache.commons.lang3.ArrayUtils;

public final class Util {

	/** good to use */
	public static Thread execAsync(String name, Runnable runnable) {
		Thread t = new Thread(runnable, name);
		t.start();
		return t;
	}

	/** ignore InterruptedException */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignore) {
		}
	}

	/** ignore InterruptedException */
	public static void join(Thread thread) {
		try {
			thread.join();
		} catch (InterruptedException ignore) {
		}
	}

	public static void printStackTrace(Throwable e) {
		e.printStackTrace();
	}

	public static byte[] encrypt(Cipher encrypter, Key secretKey, byte[] input, int offset, int length)
			throws GeneralSecurityException {
		// we need init every time because we want random iv
		encrypter.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] iv = encrypter.getIV();
		byte[] encrypedBytes = encrypter.doFinal(input, offset, length);
		return ArrayUtils.addAll(iv, encrypedBytes);
	}

	public static byte[] decrypt(Cipher decrypter, Key secretKey, byte[] input, int offset, int length)
			throws GeneralSecurityException {
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, input, offset, 12);
		decrypter.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
		byte[] decryptedBytes = decrypter.doFinal(input, offset + 12, length - 12);
		return decryptedBytes;
	}

	public static void closeQuietly(final Closeable sock) {
		if (sock != null) {
			try {
				sock.close();
			} catch (final IOException ioe) {
				// ignored
			}
		}
	}

}
