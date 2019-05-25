package android.databasecontroller.server.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;

import android.databasecontroller.server.User;
import de.gymnasium_hoechstadt.ghschulapp.network.Feedback;
import utils.Maths;
import utils.io.CSVConverter;
import utils.io.ConfigAdapter;
import utils.io.Logger;
import utils.io.Serializer;
import utils.networking.Procedure;
import utils.networking.ProcedureLoader;
import utils.networking.UnknownCommandException;

/**
 * This class is used to start new Server-Client {@link Socket}.</br>
 * These get Initialized by {@link ConnectionHandler}.</br></br>
 * The class handles the complete Network-Traffic produced by the {@link InputStream} and {@link OutputStream} of the {@link #SOCKET}
 * 
 * @author Cedric
 * @version 1.0.0 (developer-version)
 * @category networking
 */
public class ServerConnection implements Runnable {
	
	/**
	 * The ID is a by every new connection incrementing number to identify the right {@link Socket} which may causes {@link Exception}.
	 */
	private final long ID;
	/**
	 * This {@link String} makes it easier to identify that Messages in the logs come from here.
	 */
	private final String PREFIX;
	/**
	 * Incrementing number to make a unique {@link #PREFIX} for each object.
	 */
	private static long lastId;
	/**
	 * 
	 */
	private static int timeout;
	/**
	 * The connected Socket
	 */
	private final SSLSocket SOCKET;
	/**
	 * 
	 */
	private final BufferedReader INPUT;
	/**
	 * 
	 */
	private final PrintWriter OUTPUT;
	/**
	 * 
	 */
	private final ConnectionHandler HANDLER;
	/**
	 * 
	 */
	private User usr;
	/**
	 * 
	 */
	private volatile Object lock;
	/**
	 * 
	 */
	private volatile String listeningMessage;
	/**
	 * 
	 */
	private volatile boolean listen;
	/**
	 * 
	 */
	private volatile boolean stopping;
	
	static {
		lastId = 0;
		timeout = (int) Maths.calculate(ConfigAdapter.getDefaultConfig().getConfigString("timeout"));
		Logger.gdL().logInfo("Timeout was set to " + timeout + " milliseconds");
	}
	
	public ServerConnection(SSLSocket socket, ConnectionHandler handler) throws IOException {
		this.ID = lastId++;
		this.PREFIX = "[" + ID + "] ";
		this.HANDLER = handler;
		this.SOCKET = socket;
		this.listeningMessage = null;
		this.listen = true;
		this.stopping = false;
		this.INPUT = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.OUTPUT = new PrintWriter(socket.getOutputStream(), true);

		this.lock = new Object();
		logInfo("Giving id " + ID + " to " + SOCKET.getInetAddress() + ":" + SOCKET.getPort());
	}
	
	private boolean authenticate(String username, String password) {
		
		ResultSet rs = null;
		boolean authenticated = false;
		
		if (HANDLER.DATABASE.isRunning()) {
			rs = HANDLER.DATABASE.executeSql("SELECT `passwortUeberpruefen`('" + username + "', '" + password + "') AS `passwortUeberpruefen`");
			try {
				rs.next();
				authenticated = rs.getBoolean(1);
			} catch(SQLException e) {
				logError("Client couldn't be authenticated");
				logException(e);
			}
			if (authenticated) usr = new User(username, User.Permission.STANDARD);
		} else {
			authenticated = true;
			usr = new User (username, User.Permission.NOT_AUTHENTICATED);
		}
		
		return authenticated;
	}
	
	private synchronized String listen() {
		Thread listeningThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listeningMessage = null;
					listeningMessage = INPUT.readLine(); // takes about 2 sec. for a devices first connect
				} catch (IOException e) {
					if (listen && !stopping && !SOCKET.isInputShutdown()) {
						logError("An error occured while listening");
						logException(e);
					}
				}
				synchronized(lock) {
					lock.notifyAll();
				}
			}
		});
		listeningThread.start();
		synchronized(lock) {
			try {
				lock.wait(timeout);
				if (listeningMessage == null || SOCKET.isInputShutdown() || !listen) {
					listeningThread.interrupt();
					if (!stopping) close();
				} else
					return listeningMessage;
			} catch (InterruptedException e) {
				logError("An Error occured while awaiting incoming messages!");
				logException(e);
			}
		}
		return listeningMessage == null ? "fff" : listeningMessage;
	}
	
	private boolean send(String msg) {
		try {
			OUTPUT.println(msg);
			return true;
		} catch(Exception e) {
			logError("An error occured while sending the message");
			logException(e);
			return false;
		}
	}
	
	// TODO check whether HANDLER.DATABASE.isRunning()
	// TODO: Check why resource is triggered
	@SuppressWarnings("resource")
	private void process(String msg) throws ClassNotFoundException {
		String cmd = msg;
		
		
		// THIS IS THE COMMAND HANDLER! Insert commands into the existing try brackets or create own messages
		
		
		try {
			if (usr.PERMISSION.equals(User.Permission.ROOT)) {
			}
			if (msg.length() > 6 && msg.substring(0, 4).equals("PROC") && usr.PERMISSION.isAuthenticated()) {
			} else {
				throw new UnknownCommandException("Unknown command!");
			}
		} catch (UnknownCommandException e) {
			logError("A Client-Error occured while executing " + msg + ". The message is not in the right format!");
			logException(e);
			send("STOP>>UNK_CMD"); // --> The Client should automatically disconnect and display the error to the User
			if (!stopping) close();
		} catch (SQLException | SecurityException | IOException e) {
			logError("A Client-Error occured while executing " + msg + ".");
			logException(e);
			send("STOP>>UNK_ERR"); // --> The Client should automatically disconnect
			close();
		}
		
		if (msg.equals("fff"))
			msg = "STOP>>ERR";
		
		if (msg.contains("ERR"))
			logWarning("An error occured while trying to execute command " + cmd);
		
		if (!send(msg))
			logWarning("Message \"" + msg + "\" could not be sent.");
	}
	
	public void logInfo(String s) {
		Logger.gdL().logInfo(PREFIX+s);
	}
	
	public void logWarning(String s) {
		Logger.gdL().logWarning(PREFIX+s);
	}
	
	public void logError(String s) {
		Logger.gdL().logError(PREFIX+s);
	}
	
	public void logException(Throwable e) {
		Logger.gdL().logException(e);
	}
	
	public synchronized void close() {
		listen = false;
		synchronized (lock) {
			lock.notifyAll();	
		}
		if(stopping) {
			StringBuilder builder = new StringBuilder();
			for (StackTraceElement element : Thread.currentThread().getStackTrace())
				builder.append(Logger.SEPERATOR + element.toString());
			logWarning("You can stop the Connection just once!" + builder.toString());
			return;
		}
		logInfo("Connection with " + SOCKET.getInetAddress() + ":" + SOCKET.getPort() + " will be closed");
		stopping = true;
		if (SOCKET.isConnected() && !SOCKET.isClosed())
			try {
				SOCKET.close();
			} catch (IOException e) {
				logError("An Error occured while disconnecting from the Client " + SOCKET.getInetAddress() + ":" + SOCKET.getPort());
				logException(e);
			}
		HANDLER.removeConnection();
		if (HANDLER.isLocked()) {
			synchronized(HANDLER) {
				HANDLER.notifyAll();
			}
		}
		
		logInfo("Connection with " + SOCKET.getInetAddress() + ":" + SOCKET.getPort() + " successfully closed");
	}
	
	public long getId() {
		return ID;
	}
	
	@Override
	public void run() {
		try {
			// Intializing User with authentification-process if it is not a localhost attempt. In this case the user is considered to have root permissions!
			if (SOCKET.getInetAddress().isLoopbackAddress()) {
				this.usr = new User("root", User.Permission.ROOT);
				send("AUTH>>ROOT");
			} else {
				String s = listen();
				if (s.length() > 6 && s.substring(0, 6).equals("AUTH>>")) {
					String username = s.substring(6, s.indexOf("<>"));
					String password = s.substring(s.indexOf("<>")+2);
					if (username.equals("NO_AUTH") && password.equals("NO_AUTH")) {
						this.usr = new User("NOT_AUTHENTICATED_USER", User.Permission.NOT_AUTHENTICATED);
						send("SUC");
					} else if (!authenticate(username, password)) {
						send("ERR>>UNK_USR");
						if (!stopping) close();
					} else send("AUTH>>" + usr.USERNAME);
				} else {
					usr = null;
					if (!stopping) close();
				}
			}
			
			logInfo("Starting listening...");
			while (listen && (SOCKET.isConnected() && !SOCKET.isClosed()) && HANDLER.isRunning()) {
				String msg = listen();
				if (!listen) break;
				process(msg);
			}
			logInfo("Stopping listening...");
			
			if (!stopping) close();
			
			logInfo("Disconnected Client");
		} catch (Exception | Error e) {
			if ((SOCKET.isConnected() && !SOCKET.isClosed())) {
				logError("An unexpected exception occured!");
				logException(e);
				if (!stopping) close();
			} else
				logWarning("Client " + SOCKET.getInetAddress() + ":" + SOCKET.getPort() + " disconnected");
		}
	}

}
