package android.databasecontroller.server.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import android.databasecontroller.server.Server;
import utils.Maths;
import utils.io.ConfigAdapter;
import utils.io.Logger;
import utils.networking.ProcedureLoader;
import utils.networking.Security;

public class ConsoleSenderReceiver {
	
	private static SSLServerSocket socket;
	
	public final static String keyStoreLoc;
	public final static String keyStoreType;
	public final static char[] keyStorePwd;
	
	public final static int port;
	public final static boolean exitOnIssue;
	
	static {
		keyStoreLoc = ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderReceiverKeyStore");
		keyStoreType = ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderReceiverKeyStoreType");
		keyStorePwd = ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderReceiverKeyStorePwd").toCharArray();
	
		port = (int) Maths.calculate(ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderPort"));
		exitOnIssue = Boolean.parseBoolean(ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderExitOnIssue"));
	}
	
	private static void handle(String cmd) throws FileNotFoundException, IOException {
		Logger.gdL().logInfo("Command " + cmd + " received via ConsoleReceiver-Socket!");
		switch(cmd) {
		case "RESTART":
			stop();
			Server.restart();
			break;
		case "STOP":
			stop();
			Server.shutdown();
			break;
		case "RELOAD":
			ProcedureLoader.initProcedures();
			break;
		default:
			Logger.gdL().logWarning("Unknown Console-Command " + cmd);
		}
	}
	
	/**
	 * This method starts the {@link ConsoleSenderReceiver}-{@link SSLServerSocket}.</br>
	 * It is recommended to start this method in a new {@link Thread} as it will automatically block the current {@link Thread}!
	 */
	public static void start() {
		if (!Boolean.parseBoolean(ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderActive"))) {
			Logger.gdL().logWarning("The ConsoleSender-Receiver was disabled by config");
			return;
		}
		try {
			Logger.gdL().logInfo("Creating ConsoleSender-Receiver on port " + port);
			SSLContext context = Security.getSSLContext(keyStoreType, keyStoreLoc, false, keyStorePwd);
			socket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(port);
			socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
			socket.setEnabledProtocols(socket.getSupportedProtocols());
			try {
				while (!socket.isClosed()) {
					SSLSocket con = (SSLSocket) socket.accept();
					String cmd = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
					con.close();
					handle(cmd);
				}
			} catch(SecurityException e) {
				Logger.gdL().logWarning("ConsoleSender-Receiver was closed");
			}				
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException | NoSuchProviderException | UnrecoverableKeyException e) {
			Logger.gdL().logError("An error occured while trying to load ConsoleSender-Receiver on port " + port);
			Logger.gdL().logException(e);
			if (exitOnIssue) {
				Logger.gdL().logWarning("Shutting down Server");
				Server.shutdown();
			}
		}
	}

	public static void stop() {
		try {
			if (socket == null || socket.isClosed())
				Logger.gdL().logWarning("The ConsoleSender-Receiver is not active but tried to be closed");
			else {
				Logger.gdL().logInfo("Closing ConsoleSender-Receiver and releasing port " + port);
				socket.close();
			}
		} catch (IOException e) {
			Logger.gdL().logError("An error occured while closing the ConsoleCommand-Receiver");
			Logger.gdL().logException(e);
		}
	}
}
