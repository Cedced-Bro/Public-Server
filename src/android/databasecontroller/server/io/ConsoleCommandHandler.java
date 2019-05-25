package android.databasecontroller.server.io;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import android.databasecontroller.server.Server;
import utils.Maths;
import utils.io.ConfigAdapter;
import utils.io.Logger;
import utils.networking.Security;

/**
 * This class executes all Console-Commands over the String[] args of {@link Server#main(String[] args)}</br>
 * See {@link #USAGE} for a using-guide.</br>
 * See {@link #listHelp()} for more information.
 * 
 * @author Cedric
 * @version 1.0
 */
public class ConsoleCommandHandler {
	
	/**
	 * Explains Usage of the AndroidServer.
	 */
	public final static String USAGE;
	
	public final static String keyStoreLoc;
	public final static String keyStoreType;
	public final static char[] keyStorePwd;
	
	public final static int port;
	
	static {
		USAGE = "Usage: 'java -jar <path/to/server.jar> [<arg0>] [<arg1>] ...' Use help or ? for help";
		keyStoreLoc = ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderKeyStore");
		keyStoreType = ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderKeyStoreType");
		keyStorePwd = ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderKeyStorePwd").toCharArray();
	
		port = (int) Maths.calculate(ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderPort"));
	}
	
	/**
	 * Prints out a Help-Screen in the normal Console-Output.
	 * 
	 * @see System#out
	 * @see StringBuilder
	 */
	private static void listHelp() {
		StringBuilder builder = new StringBuilder();
		builder.append("Help for the Android-DB-Server:" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - help | ?: Help Menu") + "Current command" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - restart") + "Restarts the Server" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - stop") + "Stops the Server" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - start") + "Starts the Server (equal to call with no args)" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - config \"<argToChange>\" \"<newValue>\" [true/y(es)]") + "For permanent changes set the 3rd arg to yes or true" + Logger.SEPERATOR + String.join("", Collections.nCopies(60, " ")) + "NOTE: The Server will automatically start if the change is not permanent!" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - reload") + "Reloads active procedures" + Logger.SEPERATOR);
		// Not implemented yet
		builder.append(String.format("%1$-60s", " - blacklist <ip>") + "Blacklists a certain IP-Adress (not implemented yet)" + Logger.SEPERATOR);
		builder.append(String.format("%1$-60s", " - whitelist <ip>") + "Whitelists a certain IP-Adress (not implemented yet)"+ Logger.SEPERATOR);
		builder.append(Logger.SEPERATOR + "In Linux the command /usr/bin/AndroidServer exists as well.");
		System.out.println(builder.toString());
	}
	
	private static void communicateWithRunningServer(String command) {
		if (!Boolean.parseBoolean(ConfigAdapter.getDefaultConfig().getConfigString("consoleSenderActive"))) {
			System.out.println("[?] The ConsoleSender-Receiver was disabled by config");
			return;
		}
		try {
			System.out.println();
			SSLContext context = Security.getSSLContext(keyStoreType, keyStoreLoc, false, keyStorePwd);
			SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket("127.0.0.1", port);
			socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
			socket.setEnabledProtocols(socket.getSupportedProtocols());
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			writer.println(command);
			writer.close();
			socket.close();
		} catch (NumberFormatException | IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | UnrecoverableKeyException e) {
			System.err.println("[!] Communication with Server failed!");
			e.printStackTrace();
		}
	}
	
	public static boolean executeCommands(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (String s : args) sb.append(s + " ");
		sb.subSequence(0, sb.length()-1);
		System.out.println("[I] Command: " + sb.toString());
		switch(args[0]) {
		case "?":
		case "help":
			listHelp();
			return false;
		case "start":
			return true;
		case "restart":
			communicateWithRunningServer("RESTART");
			return false;
		case "stop":
			communicateWithRunningServer("STOP");
			return false;
		case "reload":
			communicateWithRunningServer("RELOAD");
			return false;
		case "config":
			boolean returnVal = false;
			try {
				if (args.length > 3 && ((args[3].toLowerCase()).equals("yes") || args[3].equals("true") || args[3].equals("y"))) {
					returnVal = false;
				} else if (args.length == 3 || ((args[3].toLowerCase()).equals("no") || args[3].equals("false") || args[3].equals("n"))) {
					// If it is no perma change version just gets set to 0.0.0b so that by next start it will automatically be replaced
					ConfigAdapter.getDefaultConfig().setConfigString("configVersion", "0.0.0b");
					returnVal = true;
				} else throw new Exception("Usage!");
				
				ConfigAdapter.getDefaultConfig().setConfigString(args[1], args[2]);
			} catch (Exception e) {
				System.out.println(Arrays.toString(args));
				System.err.println(USAGE);
			}
			return returnVal;
		case "blacklist":
			return false;
		case "whitelist":
			return false;
		default:
			System.err.println(USAGE);
			return false;
		}
	}
}
