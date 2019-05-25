package android.databasecontroller.server.io;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.sasl.AuthenticationException;

import utils.Maths;
import utils.Vector2D;
import utils.io.ConfigAdapter;
import utils.io.Logger;
import utils.networking.MultiDatabaseController;

/**
 * Lets you open a Server-Port for incoming Connections.</br></br>
 * 
 * <b>Use:</b> Instantiate a new ConnectionHandler.<br>
 * Use a client to connect to that Server over the specified port.</br></br>
 * 
 * <i>Next Update brings:</i><ul>
 * <li>basic DDoS-Protection</li>
 * <li>Encryption + Certificates</li>
 * </ul>
 * 
 * @author Cedric
 * @version 1.0
 * @category networking
 */
public class ConnectionHandler {
	
	/**
	 * The Listening-Thread for incoming Connections on the {@link #serverSocket}.
	 */
	private Thread worker;
	/**
	 * The Socket Clients can connect to if {@link #lock} is not blocking them.
	 */
	private volatile SSLServerSocket serverSocket;
	/**
	 * Indicates whether the ConnectionHandler is running or not.
	 */
	private volatile boolean running;
	/**
	 * When equal to {@link #MAX_CONNECTIONS} the lock will be triggered.
	 */
	private volatile int connections;
	/**
	 * Indicates whether the limit of {@link #connections} equals {@link #MAX_CONNECTIONS}.
	 * If so the {@link #serverSocket} will automatically block all incoming connections by {@link Thread#wait()}.
	 */
	private volatile boolean locked;
	
	/**
	 * Shows how often a person from a certain ip connected
	 */
	private volatile ConcurrentHashMap<String, Vector2D<Integer, Long>> ddosProtection;
	
	// *************
	// * Constants *
	// *************
	/**
	 * This ConnectionHandler as instance.
	 */
	private final ConnectionHandler INSTANCE;
	/**
	 * The maximum of Connections to the Server. If the maximum is reached no more connections can be established until a connection closes.
	 */
	public final int MAX_CONNECTIONS;
	/**
	 * The Port the Server will be listening on. If the Port is blocked the program will end immediately.
	 */
	public final int PORT;
	/**
	 * The Name of this ConnectionHandler to recognize logs easier.
	 */
	public final String NAME;
	
	/**
	 * The {@link MultiDatabaseController} for this {@link ConnectionHandler}.
	 */
	public final MultiDatabaseController DATABASE;
	
	/**
	 * 
	 */
	public final int MAX_CON_ATTEMPTS_PER_MINUTE;
	
	
	/**
	 * Creates a new ConnectionHandler using: <ul>
	 * <li>{@link #PORT} as ServerPort</li>
	 * <li>{@link #MAX_CONNECTIONS} as maxConnections available</li>
	 * <li>{@link #NAME} as a name to recognize a specific ConnectionHandler</li>
	 * </ul>
	 * These things can <b>not</b> be changed afterwards to ensure consistency within a Server session.
	 * 
	 * @param port equals {@link #PORT} 
	 * @param maxConnections equals {@link #MAX_CONNECTIONS}
	 * @param name equals {@link #NAME}
	 */
	public ConnectionHandler(int port, int maxConnections, String name) {
		// Checking parameters for illogical information
		if (maxConnections < 1) throw new IllegalArgumentException("You need to set the maximum Connections at least to 1");
		if (port < 1 || port > 65535) throw new IllegalArgumentException("The listening port can just be between 1 and 65535");
		
		// Loading data for MultiDatabaseConnectionHandler
		String dbHost = ConfigAdapter.getDefaultConfig().getConfigString("dbHost");
		int dbPort = Integer.parseInt(ConfigAdapter.getDefaultConfig().getConfigString("dbPort"));
		String dbUsr = ConfigAdapter.getDefaultConfig().getConfigString("dbUsername");
		String dbPwd = ConfigAdapter.getDefaultConfig().getConfigString("dbPassword");
		String dbName = ConfigAdapter.getDefaultConfig().getConfigString("dbName");
		int maxBuf = Integer.parseInt(ConfigAdapter.getDefaultConfig().getConfigString("dbConnectionBufferMax"));
		int targetBuf = Integer.parseInt(ConfigAdapter.getDefaultConfig().getConfigString("dbConnectionBufferTarget"));
		int minBuf = Integer.parseInt(ConfigAdapter.getDefaultConfig().getConfigString("dbConnectionBufferMin"));
		int maxConAttempts = (int) Maths.calculate(ConfigAdapter.getDefaultConfig().getConfigString("chDDosProtection"));
		long timeout = (long) Maths.calculate(ConfigAdapter.getDefaultConfig().getConfigString("dbTimeout"));
		
		// Setting up fields
		this.MAX_CON_ATTEMPTS_PER_MINUTE = maxConAttempts;
		this.MAX_CONNECTIONS = maxConnections;
		this.PORT = port;
		this.NAME = name;
		this.DATABASE = new MultiDatabaseController(dbHost, dbPort, dbUsr, dbPwd, dbName, minBuf, targetBuf, maxBuf, timeout, true);
		this.INSTANCE = this;
		this.connections = 0;
		this.locked = false;
		this.ddosProtection = new ConcurrentHashMap<String, Vector2D<Integer, Long>>();
		
		worker = new Thread(new Runnable() {

			/**
			 * Removes connections which long enough did not try to connect.</br>
			 * </br>
			 * <i>Note: This is just a first implementation of a DDoS-Protection and should be rewritten if used in bigger Applications.</i> 
			 */
			private void cleanUpDdos() {
				for (String s : ddosProtection.keySet())
					if (ddosProtection.get(s).data2+60000 > System.currentTimeMillis()) ddosProtection.remove(s);
			}
			
			@Override
			public void run() {
				running = true;

				// START Setting up Listener
				Logger.gdL().logInfo("Setting up Server-Socket - " + NAME);
				
				// Load Certificates
				Logger.gdL().logInfo("Importing SSL-Key and -Truststore");
				String keyStoreFile = ConfigAdapter.getDefaultConfig().getConfigString("sslKeyStore");
				String keyStorePassword = ConfigAdapter.getDefaultConfig().getConfigString("sslKeyStorePwd");
				String keyStoreType = ConfigAdapter.getDefaultConfig().getConfigString("sslKeyStoreType");
				String trustStore = ConfigAdapter.getDefaultConfig().getConfigString("sslTrustStore").replace("@DEFAULT", System.getProperty("java.home") + "/lib/security/cacerts");
				String trustStorePassword = ConfigAdapter.getDefaultConfig().getConfigString("sslTrustStorePwd").replace("@DEFAULT", "changeit");
				String trustStoreType = ConfigAdapter.getDefaultConfig().getConfigString("sslTrustStoreType");
				System.setProperty("javax.net.ssl.keyStore", keyStoreFile);
				System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
				System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
				System.setProperty("javax.net.ssl.trustStore", trustStore);
				System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
				if (!trustStoreType.equals("@DEFAULT"))
					System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
				
				try {
					Logger.gdL().logInfo("Opening Server-Socket on Port " + PORT + " - " + NAME);
					serverSocket = (SSLServerSocket) ((SSLServerSocketFactory) SSLServerSocketFactory.getDefault()).createServerSocket(port, maxConnections);
				    serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
					serverSocket.setEnabledProtocols(serverSocket.getSupportedProtocols());
				} catch (IOException e) {
					Logger.gdL().logError("An error occured while opening the ServerListener! Exiting... - " + NAME);
					Logger.gdL().logException(e);
				}
				// END Setting up Listener
				
				Logger.gdL().logInfo("Starting listening on port " + PORT + " - " + NAME);
				
				// START Listening
				while(running) {
					try {
						// Wait for incoming Connections which will be given into a new ServerConnection
						SSLSocket socket = (SSLSocket) serverSocket.accept();
						
						// DDoS-Protection
						cleanUpDdos();
						
						// Probing for DDoS
						String inetAddress = socket.getInetAddress().getHostAddress();
						if (!ddosProtection.containsKey(inetAddress)) {
							Vector2D<Integer, Long> vec = new Vector2D<Integer, Long>(0, System.currentTimeMillis());
							ddosProtection.put(inetAddress, vec);
						} else {
							Vector2D<Integer, Long> result = ddosProtection.get(inetAddress);
							int amounts = result.data1;
							long time = result.data2;
							if (amounts > MAX_CON_ATTEMPTS_PER_MINUTE) {
								Logger.gdL().logWarning("Possible DDoS-Attack detected from " + inetAddress + "! Disconnecting...");
								new PrintStream(socket.getOutputStream(), true).println("ERROR>>DDOS");
								socket.close();
								Logger.gdL().logWarning("Disconnected potential DDoS-Attacker " + inetAddress);
								// extra Security
								if (amounts > (MAX_CON_ATTEMPTS_PER_MINUTE + 10)) time += 1000;
							}
							amounts++;
							ddosProtection.put(inetAddress, new Vector2D<Integer, Long>(amounts, time));
						}
						try {
							Thread t = new Thread(new ServerConnection(socket, INSTANCE));
							t.setDaemon(true);
							t.start();
							Logger.gdL().logInfo("Started Connection with " + socket.getInetAddress() + ":" + socket.getPort() + " - " + NAME);
						} catch(IOException e) {
							if (e instanceof AuthenticationException) {
								Logger.gdL().logWarning("Wrong password and/or username for " + socket.getInetAddress() + ":" + socket.getPort() + " - " + NAME);
							} else {
								Logger.gdL().logError("An error occured while trying to open new connection to " + socket.getInetAddress() + ":" + socket.getPort() + " - " + NAME);
								Logger.gdL().logException(e);
							}
						}

						// MAX_CONNECTIONS-functionality
						connections++;
						if (connections == MAX_CONNECTIONS) {
							Logger.gdL().logWarning("Max-Connections reached! Setting lock - " + NAME);
							try {
								synchronized(INSTANCE) {
									locked=true;
									INSTANCE.wait();
								}
							} catch (InterruptedException e) {
								Logger.gdL().logError("An error occured while locking the Server");
								Logger.gdL().logException(e);
							}
							locked=false;
							Logger.gdL().logInfo("Releasing lock - " + NAME);
						}
					} catch (IOException e) {
						Logger.gdL().logError("An error occured while opening new Connection! Exiting... - " + NAME);
						Logger.gdL().logException(e);
						if (!running) {
							try {
								serverSocket.close();
							} catch (IOException e1) {
								Logger.gdL().logError("Could not close socket on port " + port + "!");
								Logger.gdL().logException(e1);
							}
							return;
						}
					}
				}
				// END Listening
				
				Logger.gdL().logInfo("Stopping listening - " + NAME);
				
				// START Closing Socket
				try {
					Logger.gdL().logInfo("Closing Server-Socket - " + NAME);
					serverSocket.close();
				} catch (IOException e) {
					Logger.gdL().logWarning("Couldn't close Server-Socket safely! - " + NAME);
					Logger.gdL().logException(e);
				}
				// END Closing Socket
			}
			
		});
		
		// Starting ConnectionHandler
		worker.start();
		
	}
	
	/**
	 * Subtracts 1 from the running {@link #connections}.
	 */
	synchronized void removeConnection() {
		connections--;
	}
	
	/**
	 * Returns whether the ConnectionHandler is running or not.
	 * 
	 * @return {@link #running}
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Returns whether the ConnectionHandler is {@link #locked} or not.
	 * 
	 * @return {@link #locked}
	 */
	public boolean isLocked() {
		return locked;
	}
	
	/**
	 * Stops the {@link ConnectionHandler}.</br>
	 * There are 2 possibilities for this:</br>
	 * <ul><li>If the {@link #lock} has been set then {@link #lock} will just be removed</li>
	 * <li>Else a Poisoning-Pill-Connection will be used to interrupt the waiting-State of the {@link #serverSocket} which causes {@link #worker} to wait</li></ul>
	 */
	public void stop() {
		running = false;
		
		// This will release the lock from the worker
		SSLSocket poiseningPill;
		try {
			// Possible Way 1
			if (locked) {
				Logger.gdL().logInfo("Closing the Server by unlocking the [Lock] - " + NAME);
				synchronized(INSTANCE) {
					INSTANCE.notify();
				}
			}
			// Possible Way 2
			else {
				Logger.gdL().logInfo("Initializing [Poisening-Pill] to close the Server-Socket - " + NAME);
				poiseningPill = (SSLSocket) SSLSocketFactory.getDefault().createSocket("localhost", PORT);
				poiseningPill.close();
			}

			// Waits until Thread actually died --> The Server will securely be shutdown
			try {
				worker.join(1000);
			} catch (InterruptedException e) {
				Logger.gdL().logError("An Error occured while waiting for the worker to join! - " + NAME);
				Logger.gdL().logException(e);
			}
			Logger.gdL().logInfo("Server-Socket successfully closed - " + NAME);
			
			Logger.gdL().logInfo("Shutting down DB-Controller");
			DATABASE.stop();
			Logger.gdL().logInfo("DB-Controller successfully closed");
			
		} catch (IOException e) {
			// The exception gets raised when the ServerSocket is already closed due to a new connection
			Logger.gdL().logWarning("The connection to the Server is already closed. This means there was an other connection before this one. - " + NAME);
		}
			
	}
	
}
