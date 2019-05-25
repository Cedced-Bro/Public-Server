package utils.networking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import utils.io.ConfigAdapter;
import utils.io.Logger;

/**
 * 
 * <i>Next update will bring:<ul>
 * <li>Password-encryption</li>
 * </ul></i>
 * 
 * @author Cedric
 * @version 2.0
 * 
 * 
 * @see SingleDatabaseController
 * @see DatabaseConnectionHandler
 * @see SqlExecuter
 * @see SQLException
 * @see ResultSet
 * @see List
 * @see LinkedList
 * 
 * @see Logger
 */
public class MultiDatabaseController {
	
	// These variables are protected that you can always overwrite MultiDatabaseController to integrate it into your project
	// ***********************
	// * Protected variables *
	// *************************
	// * Multi-threading-safe! *
	// *************************
	protected volatile ProcedureExecuter procExecuter;
	/**
	 * Handles the SQL-Queries.
	 * 
	 * @see SqlExecuter
	 */
	protected volatile SqlExecuter sqlExecuter;
	/**
	 * Handles the Connections for the buffer.
	 * 
	 * @see DatabaseConnectionHandler
	 */
	protected volatile DatabaseConnectionHandler conHandler;
	/**
	 * Shows whether {@link #start()} and/or {@link #stop()} already got called.</br>
	 * The active state of <code>running</code> can be checked by calling {@link #isRunning()}.
	 */
	protected volatile boolean running;
	
	// *************
	// * CONSTANTS *
	// *************
	/**
	 * The Address of the Database-Host.
	 */
	public final String DB_HOST;
	/**
	 * The username to log in to the Database.
	 */
	public final String DB_USERNAME;
	/**
	 * The Password to the corresponding {@link MultiDatabaseController#DB_USERNAME}.
	 */
	public final String DB_PASSWORD;
	/**
	 * The name of the Database the {@link SingleDatabaseController} connect to.
	 */
	public final String DB_NAME;
	/**
	 * The Port on which the Database runs.
	 */
	public final int DB_PORT;
	/**
	 * The time in milliseconds after which a {@link TimeOutException} gets thrown when waiting for the {@link SingleDatabaseController}.
	 */
	public final long DB_TIMEOUT;
	/**
	 * Indicates whether the Server exits on a ConnectionError. This Constant usually gets loaded by the {@link ConfigAdapter#getConfigString(String)}.</br>
	 * You can set this manually by calling the Constructor 
	 * @see ConfigAdapter
	 */
	public final boolean EXIT_ON_ERROR;
	
	
	/**
	 * This creates a new {@link MultiDatabaseController}-Object and automatically starts it.</br>
	 * You can stop a {@link MultiDatabaseController} by calling {@link MultiDatabaseController#stop()}.</br></br>
	 * If you do not want the {@link MultiDatabaseController} to start automatically call {@link #MultiDatabaseController(String, int, String, String, boolean)} where <code>connect</code> equals <code>false</code>.
	 *	
	 * @param host The {@link #DB_HOST} for the Database
	 * @param port The {@link #DB_PORT} for the Database
	 * @param username The {@link #DB_USERNAME} for the Database
	 * @param password The {@link #DB_PASSWORD} for the Database
	 * @param name The {@link #DB_NAME} for the Database
	 * @param minBuffer Initializes {@link DatabaseConnectionHandler#MIN_BUFFER}
	 * @param targetBuffer Initializes {@link MultiDatabaseController#TARGET_BUFFER}
	 * @param maxBuffer Initializes {@link MultiDatabaseController#MAX_BUFFER}
	 * @param timeout Initializes {@link #DB_TIMEOUT}
	 * 
	 * @see DatabaseConnectionHandler
	 * @see SqlExecuter
	 */
	public MultiDatabaseController(String host, int port, String username, String password, String name, int minBuffer, int targetBuffer, int maxBuffer, long timeout) {
		this (host, port, username, password, name, minBuffer, targetBuffer, maxBuffer, timeout, true);
	}
	
	/**
	 * This creates a new {@link MultiDatabaseController}-Object.</br>
	 * If you also want to directly {@link #start()} it set <code>connect</code> to <code>true</code>.
	 * 
	 * @param host The {@link #DB_HOST} for the Database
	 * @param port The {@link #DB_PORT} for the Database
	 * @param username The {@link #DB_USERNAME} for the Database
	 * @param password The {@link #DB_PASSWORD} for the Database
	 * @param name The {@link #DB_NAME} for the Database
	 * @param minBuffer Initializes {@link DatabaseConnectionHandler#MIN_BUFFER}
	 * @param targetBuffer Initializes {@link MultiDatabaseController#TARGET_BUFFER}
	 * @param maxBuffer Initializes {@link MultiDatabaseController#MAX_BUFFER}
	 * @param dbTimeout Initializes {@link #DB_TIMEOUT}
	 * @param connect Indicates whether the Constructor also tries to {@link #start()} the {@link MultiDatabaseController}.
	 * 
	 * @see DatabaseConnectionHandler
	 * @see SqlExecuter
	 */
	public MultiDatabaseController(String host, int port, String username, String password, String name, int minBuffer, int targetBuffer, int maxBuffer, long dbTimeout, boolean connect) {
		this(host, port, username, password, name, minBuffer, targetBuffer, maxBuffer, dbTimeout, connect, Boolean.parseBoolean(ConfigAdapter.getDefaultConfig().getConfigString("dbExitOnError")));
	}
	
	/**
	 * 
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 * @param name
	 * @param minBuffer
	 * @param targetBuffer
	 * @param maxBuffer
	 * @param dbTimeout
	 * @param connect
	 * @param exitOnClose
	 */
	public MultiDatabaseController(String host, int port, String username, String password, String name, int minBuffer, int targetBuffer, int maxBuffer, long dbTimeout, boolean connect, boolean exitOnClose) {
		// Check for illogical args
		if (port > 65535 || port < 0 || username == null || name == null || host == null) {
			throw new IllegalArgumentException("Illogical Network arguments for MultiDatabaseController!");
		}
		
		// Initialize constants
		this.DB_HOST = host;
		this.DB_PORT = port;
		this.DB_USERNAME = username;
		this.DB_PASSWORD = password;
		this.DB_TIMEOUT = dbTimeout;
		this.DB_NAME = name;
		this.EXIT_ON_ERROR = exitOnClose;
		
		// Creating the Handlers
		procExecuter = new ProcedureExecuter();
		sqlExecuter = new SqlExecuter();
		conHandler = new DatabaseConnectionHandler(minBuffer, targetBuffer, maxBuffer);
		
		if (connect) start();
	}
	
	/**
	 * Returns whether {@link #start()} and/or {@link #stop()} have been called.
	 * 
	 * @return {@link #running}
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Starts the MultiDatabaseController by setting {@link #running} to <code>true</code>.</br>
	 * With this call also the {@link DatabaseConnectionHandler} and the {@link SqlExecuter} get started by calling {@link #conHandler}.{@link DatabaseConnectionHandler#start() start()} and {@link #sqlExecuter}.{@link SqlExecuter#start() start()}.
	 */
	public synchronized void start() {
		Logger.gdL().logInfo("Starting MultiDatabaseController." + Logger.SEPERATOR
					+ "Basic Information:" + Logger.SEPERATOR
					+ String.format("%1$-15s %2$s", "\t- Host: ", DB_HOST) + Logger.SEPERATOR
					+ String.format("%1$-15s %2$s", "\t- Port:", DB_PORT) + Logger.SEPERATOR
					+ String.format("%1$-15s %2$s", "\t- User:", DB_USERNAME) + Logger.SEPERATOR
					+ String.format("%1$-15s %2$s", "\t- Pwd: ", DB_PASSWORD) + Logger.SEPERATOR	// TODO: Print this as Hash!
					+ String.format("%1$-15s %2$s", "\t- DB: ", DB_NAME) + Logger.SEPERATOR);
		running = true;
		conHandler.start();
		procExecuter.start();
		sqlExecuter.start();
		Logger.gdL().logInfo("Successfully started the MultiDatabaseController.");
	}
	
	/**
	 * Stops the {@link MultiDatabaseController} by calling {@link #sqlExecuter}.{@link SqlExecuter#shutdown() shutdown()} and {@link #conHandler}.{@link DatabaseConnectionHandler#shutdown() shutdown()}.</br>
	 * This also sets {@link #running} to <code>false</code>.
	 */
	public synchronized void stop() {
		if (running) {
			Logger.gdL().logInfo("Shutting down the MultiDatabaseController");
			procExecuter.shutdown();
			sqlExecuter.shutdown();
			conHandler.shutdown();
			running = false;
			Logger.gdL().logInfo("MultiDatabaseController successfully shut down");
		} else Logger.gdL().logWarning("MultiDatabaseController already was shut down!");
	}
	
	/**
	 * Executes the given {@link Procedure} with the given {@link Object args[]}.</br>
	 * After executing the {@link ResultSet result} will be returned.
	 * 
	 * @param proc {@link Procedure} to execute
	 * @param args parameters for the {@link Procedure}
	 * @return {@link ResultSet result} produced by the given {@link Procedure}
	 */
	public Object callProc(Procedure proc, Object...args) {
		return procExecuter.callProc(proc, args);
	}
	
	/**
	 * Executes the given {@link Procedure} with the given {@link Object args[]}.</br>
	 * 
	 * @param proc {@link Procedure} to execute
	 * @param args parameters for the {@link Procedure}
	 */
	public void callNoRetSql(Procedure proc, Object...args) {
		procExecuter.callProc(proc, args, false);
	}
	
	/**
	 * Executes the given SQL-Query on a {@link SingleDatabaseController}-instance from the {@link DatabaseConnectionHandler#buffer}.</br>
	 * After executing the {@link ResultSet result} will be returned.
	 * 
	 * @param query to execute
	 * 
	 * @return The {@link ResultSet} of the executed SQL-Query parsed into a {@link CachedRowSetImpl}.
	 */
	public ResultSet executeSql(String query) {
		return sqlExecuter.executeSQL(query);
	}
	
	/**
	 * Executes the given SQL-Query on a {@link SingleDatabaseController}-instance from the {@link DatabaseConnectionHandler#buffer}.
	 * 
	 * @param query to execute
	 */
	public void executeNoRetSql(String query) {
		sqlExecuter.executeSQL(query, false);
	}
	
	/**
	 * 
	 * @author Cedric
	 *
	 */
	protected class ProcedureExecuter extends Thread {
		/**
		 * 
		 */
		protected volatile boolean running;
		/**
		 * 
		 */
		protected volatile Object lock;
		/**
		 * 
		 */
		protected volatile boolean locked;
		/**
		 * 
		 */
		protected volatile HashMap<Procedure, Object[]> linking;
		/**
		 * 
		 */
		protected volatile HashMap<Procedure, Object> proUncared;
		/**
		 * 
		 */
		protected volatile HashMap<Procedure, Object> proCared;
		
		/**
		 * 
		 */
		public ProcedureExecuter() {
			setUp();
		}
		
		/**
		 * 
		 */
		protected synchronized void setUp() {
			lock = new Object();
			locked = false;
			running = false;
			linking = new HashMap<Procedure, Object[]>();
			proUncared = new HashMap<Procedure, Object>();
			proCared = new HashMap<Procedure, Object>();
		}
		
		/**
		 * 
		 */
		@Override
		public void start() {
			Logger.gdL().logInfo("Starting the Procedure-Executer");
			running = true;
			super.start();
			Logger.gdL().logInfo("Successfully started the Procedure-Executer");
		}
		
		/**
		 * 
		 */
		public synchronized void shutdown() {
			Logger.gdL().logInfo("Shutting down the Procedure-Executer");
			running = false;
		}
		
		/**
		 * 
		 */
		public synchronized void restart() {
			Logger.gdL().logInfo("Restarting Procedure-Excuter");
			Thread restarter = new Thread(new Runnable() {
				@Override
				public void run() {
					if (MultiDatabaseController.this.running) {
						shutdown();
						MultiDatabaseController.ProcedureExecuter.super.interrupt();
						setUp();
						start();
						Logger.gdL().logInfo("Successfully restarted Procedure-Executer");
					} else
						Logger.gdL().logWarning("MultiDatabaseController is not running so Procedure-Executer cannot be started!");
				}
			});
			restarter.setDaemon(true);
			restarter.start();
		}
		
		/**
		 * 
		 * @param proc
		 * @param args
		 * @return
		 */
		public Object callProc(Procedure proc, Object[] args) {
			return proc == null ? null : callProc(proc, args, args.length != 0);
		}
		
		/**
		 * 
		 * @param proc
		 * @param args
		 * @param waitForAnswer
		 * @return
		 */
		public Object callProc(Procedure proc, Object[] args, boolean waitForAnswer) {
			if (proc == null) return null;
			if (waitForAnswer) {
				Object o = new Object();
				linking.put(proc, args);
				proUncared.put(proc, o);
				if (locked) synchronized (lock) {lock.notifyAll();}
				synchronized (o) {
					try {
						o.wait(DB_TIMEOUT);
					} catch (InterruptedException e) {
						Logger.gdL().logError("An error occured while waiting for the Procedure-Result.");
						Logger.gdL().logException(e);
						linking.remove(proc);
						proUncared.remove(proc);
						proCared.remove(proc);
						if (EXIT_ON_ERROR)
							MultiDatabaseController.this.stop();
						return null;
					}
					return proCared.remove(proc);
				}
			} else {
				proUncared.put(proc, null);
				if (locked) synchronized (lock) {lock.notifyAll();}
				return null;
			}
		}
		
		/**
		 * 
		 */
		@Override
		public void run() {
			while (running) {
				if (proUncared.size() == 0) {
					locked = true;
					synchronized (lock) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
							Logger.gdL().logError("An error occured while waiting for incoming Procedures.");
							Logger.gdL().logException(e);
							
							if (running) {
								if (EXIT_ON_ERROR)
									restart();
								else
									MultiDatabaseController.this.stop();
								return;
							}
						} finally {
							locked = false;
						}
					}
				} else {
					Procedure key = proUncared.keySet().toArray(new Procedure[proUncared.size()])[0];
					SingleDatabaseController con = conHandler.getConnection();
					Object val = new Object();	// Exists for maybe later implementations
					try {
						if (key.RET_TYPE.equals("NO_RET") || key.RET_TYPE.equals("MULTIPLE_RETS")) {
							ResultSet set = null;
							set = con.executeResultSetProcedure(key, linking.get(key));
							val = set;
						}
					} catch (SQLException e) {
						Logger.gdL().logError("An error occured while executing the procedure " + key.toString());
						Logger.gdL().logException(e);
						if (EXIT_ON_ERROR)
							MultiDatabaseController.this.stop();
					}

					Object o = proUncared.get(key);
					if (o != null) {
						proCared.put(key, val);
						synchronized (o) {
							o.notifyAll();
						}
					} else
						linking.remove(key);
					proUncared.remove(key);
					try {
						conHandler.storeConnection(con);
					} catch (SQLException e) {
						Logger.gdL().logError("An error occured while storing the connection back into the buffer.");
						Logger.gdL().logException(e);
						try {
							con.disconnect();
						} catch (SQLException e1) {
							Logger.gdL().logError("An error occured while disconnecting from the Database.");
							Logger.gdL().logException(e1);
						}
						conHandler.checkForChange();
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * 
	 * @author Cedric
	 * @see MultiDatabaseController
	 */
	protected class SqlExecuter extends Thread {
		/**
		 * 
		 */
		protected volatile Object lock;
		/**
		 * 
		 */
		protected volatile boolean locked;
		/**
		 * 
		 */
		protected volatile boolean running;
		/**
		 * 
		 */
		protected volatile HashMap<String, Object> sqlUncared;
		/**
		 * 
		 */
		protected volatile HashMap<String, ResultSet> sqlAnswer;
		
		/**
		 * 
		 */
		public SqlExecuter() {
			setUp();
		}

		/**
		 * 
		 */
		protected synchronized void setUp() {
			running = false;
			locked = false;
			lock = new Object();
			sqlUncared = new HashMap<String, Object>();
			sqlAnswer = new HashMap<String, ResultSet>();
		}
		
		/**
		 * 
		 */
		@Override
		public synchronized void start() {
			Logger.gdL().logInfo("Starting the SqlExecuter");
			running = true;
			super.start();
			Logger.gdL().logInfo("Successfully started the SqlExecuter");
		}
		
		/**
		 * 
		 */
		public synchronized void shutdown() {
			Logger.gdL().logInfo("Shutting down SqlExecuter");
			running = false;
			if (locked)
				synchronized (lock) {
					lock.notifyAll();
				}
		}
		
		/**
		 * 
		 */
		public synchronized void restart() {
			Logger.gdL().logInfo("Restarting SqlExcuter");
			Thread restarter = new Thread(new Runnable() {
				@Override
				public void run() {
					if (MultiDatabaseController.this.running) {
						shutdown();
						MultiDatabaseController.SqlExecuter.super.interrupt();
						setUp();
						start();
						Logger.gdL().logInfo("Successfully restarted SqlExecuter");
					} else
						Logger.gdL().logWarning("MultiDatabaseController is not running so SQL-Executer cannot be started!");
				}
			});
			restarter.setDaemon(true);
			restarter.start();
		}
		
		/**
		 * 
		 * @param query
		 * @return
		 */
		public ResultSet executeSQL(String query) {
			return executeSQL(query, true);
		}
		
		/**
		 * 
		 * @param query
		 * @param waitForAnswer
		 * @return
		 */
		public ResultSet executeSQL(String query, boolean waitForAnswer) {
			if (waitForAnswer) {
				Object o = new Object();
				sqlUncared.put(query, o);
				if (locked) synchronized (lock) {lock.notifyAll();}
				synchronized (o) {
					try {
						o.wait(DB_TIMEOUT);
					} catch (InterruptedException e) {
						Logger.gdL().logError("An error occured while waiting for the SQL-Result.");
						Logger.gdL().logException(e);
						sqlUncared.remove(query);
						sqlAnswer.remove(query);
						if (EXIT_ON_ERROR)
							MultiDatabaseController.this.stop();
						return null;
					}
					return sqlAnswer.remove(query);
				}
			} else {
				sqlUncared.put(query, null);
				if (locked) synchronized (lock) {lock.notifyAll();}
				return null;
			}
		}
		
		/**
		 * 
		 */
		@Override
		public void run() {
			while (running) {
				if (sqlUncared.size() == 0) {
					locked = true;
					synchronized (lock) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
							Logger.gdL().logError("An error occured while waiting for incoming SQL-queries.");
							Logger.gdL().logException(e);
							
							if (running) {
								if (EXIT_ON_ERROR)
									restart();
								else
									MultiDatabaseController.this.stop();
								return;
							}
						} finally {
							locked = false;
						}
					}
				} else {
					String key = (String) sqlUncared.keySet().toArray()[0];
					SingleDatabaseController con = conHandler.getConnection();
					ResultSet set = null;
					try {
						set = con.executeSQL(key);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException | TimeOutException e) {
						Logger.gdL().logError("An error occured while executing the command " + key);
						Logger.gdL().logException(e);
						if (EXIT_ON_ERROR)
							MultiDatabaseController.this.stop();
					}
					Object o = sqlUncared.get(key);
					if (o != null) {
						sqlAnswer.put(key, set);
						synchronized (o) {
							o.notifyAll();
						}
					}
					sqlUncared.remove(key);
					try {
						conHandler.storeConnection(con);
					} catch (SQLException e) {
						Logger.gdL().logError("An error occured while storing the connection back into the buffer.");
						Logger.gdL().logException(e);
						try {
							con.disconnect();
						} catch (SQLException e1) {
							Logger.gdL().logError("An error occured while disconnecting from the Database.");
							Logger.gdL().logException(e1);
						}
						conHandler.checkForChange();
					}
				}
			}
		}
	}
	
	/**
	 * Handles the DatabaseConnectionsPool.</br>
	 * This means that it contains a {@link #buffer} which gets filled to always contain {@link DatabaseConnectionHandler#MIN_BUFFER} and never contain more than {@link DatabaseConnectionHandler#MAX_BUFFER}.</br>
	 * If the Handler runs into errors it will automatically call {@link MultiDatabaseController#stop()} if no solution could be found.</br></br>
	 * 
	 * <b>Use:</b><ul>
	 * <li>At first initialize and call {@link #start()}</li>
	 * <li>Wait until {@link #isReady()} returns <code>true</code> so the {@link #buffer} got filled up to {@link #TARET_BUFFER}</li>
	 * <li>You can now get a connected {@link SingleDatabaseController} by calling {@link #getConnection()} and put them again into the buffer by calling {@link #storeConnection(SingleDatabaseController)}</li>
	 * <li>End the Handler by calling {@link #shutdown()}</li>
	 * </ul></br>
	 * 
	 * @author Cedric
	 * @version 1.0
	 * @see MultiDatabaseController
	 */
	protected class DatabaseConnectionHandler extends Thread {
		
		// ***********************
		// * Protected variables *
		// ***********************
		/**
		 * The lock the {@link DatabaseConnectionHandler} waits when the {@link #buffer} got filled.
		 */
		protected volatile Object lock;
		/**
		 * Indicates whether the {@link DatabaseConnectionHandler} is locked.
		 */
		protected volatile boolean locked;
		/**
		 * Indicates whether the {@link DatabaseConnectionHandler} is running and the {@link Thread} is {@link Thread#isAlive() alive}.</br>
		 * This can be checked by calling #isRunning().
		 */
		protected volatile boolean running;
		/**
		 * The Buffer of {@link SingleDatabaseController}.
		 */
		protected volatile LinkedList<SingleDatabaseController> buffer;
		/**
		 * Locks which are waiting to get notified if the buffer is empty.
		 */
		protected volatile LinkedList<Object> emptyBufferLocks;
		
		// *************
		// * CONSTANTS *
		// *************
		/**
		 * This is the amount of {@link SingleDatabaseController} which should always exist in the {@link #buffer}.
		 */
		public final int MIN_BUFFER;
		/**
		 * This is the amount of {@link SingleDatabaseController} which are targeted to exist in the {@link #buffer}.
		 */
		public final int TARGET_BUFFER;
		/**
		 * This the maximum of {@link SingleDatabaseController} which can exist in the {@link MultiDatabaseController}.
		 */
		public final int MAX_BUFFER;
		
		
		// ****************
		// * Constructors *
		// ****************
		/**
		 * Creates a new DatabaseConnectionHandler with the given buffer-information.
		 * 
		 * @param minBuffer equals {@link #MIN_BUFFER}
		 * @param targetBuffer equals {@link #TARGET_BUFFER}
		 * @param maxBuffer equals {@link #MAX_BUFFER}
		 */
		public DatabaseConnectionHandler(int minBuffer, int targetBuffer, int maxBuffer) {
			if (minBuffer > targetBuffer || targetBuffer > maxBuffer) {
				throw new IllegalArgumentException("Illogical buffer arguments for MultiDatabaseConnector.DatabaseConnectionHandler");
			}
			
			this.MIN_BUFFER = minBuffer;
			this.TARGET_BUFFER = targetBuffer;
			this.MAX_BUFFER = maxBuffer;
			
			setUp();
		}
		
		// *********************
		// * Protected methods *
		// *********************
		/**
		 * Sets up the {@link DatabaseConnectionHandler} by initializing the Variables.
		 */
		protected synchronized void setUp() {
			lock = new Object();
			locked = false;
			running = false;
			buffer = new LinkedList<SingleDatabaseController>();
			emptyBufferLocks = new LinkedList<Object>();
		}
		
		/**
		 * Releases the first lock of {@link #emptyBufferLocks}.
		 */
		protected synchronized void releaseLock() {
			if (emptyBufferLocks.size() > 0) {
				Object o = emptyBufferLocks.removeFirst();
				synchronized (o) {
					o.notifyAll();
				}
			}
		}
		
		// ******************
		// * Public methods *
		// ******************
		/**
		 * Lets the {@link DatabaseConnectionHandler} check whether there are still enough {@link SingleDatabaseController} in the {@link #buffer}.
		 */
		public synchronized void checkForChange() {
			if (locked) synchronized (lock) {
				lock.notifyAll();
				locked = false;
			}
		}
		
		/**
		 * Basically like {@link Thread#start()}.</br>
		 * The difference is that this also sets {@link #running} to <code>true</code>.
		 */
		@Override
		public synchronized void start() {
			Logger.gdL().logInfo("Starting DatabaseConnectionHandler." + Logger.SEPERATOR
					+ "Buffer Information:" + Logger.SEPERATOR
					+ String.format("%1$-20s %2$s", "\t- Minimal-Size:", MIN_BUFFER) + Logger.SEPERATOR
					+ String.format("%1$-20s %2$s", "\t- Target-Size:", TARGET_BUFFER) + Logger.SEPERATOR
					+ String.format("%1$-20s %2$s", "\t- Maximal-Size:", MAX_BUFFER) + Logger.SEPERATOR);
			running = true;
			super.start();
		}
		
		/**
		 * This stops the {@link MultiDatabaseController}-Thread and closes all {@link SingleDatabaseController} from the {@link #buffer}.</br>
		 * If there are locks waiting for the {@link #buffer} in the {@link #emptyBufferLocks} they get released.
		 */
		public synchronized void shutdown() {
			Logger.gdL().logInfo("Shutting down DatabaseConnectionController");
			running = false;
			checkForChange();
			if (emptyBufferLocks.size() > 0) {
				Logger.gdL().logWarning("Releasing all emptyBufferLocks!");
				for (Object o : emptyBufferLocks) {
					synchronized (o) {
						o.notifyAll();
					}
				}
				emptyBufferLocks = null;
				Logger.gdL().logInfo("All Locks released.");
			}
			Logger.gdL().logInfo("Shutting down all SingleDatabaseControllers.");
			for (SingleDatabaseController s : buffer)
				try {
					s.disconnect();
				} catch (SQLException e) {
					Logger.gdL().logError("An error occured while shutting down all SingleDatabaseControllers.");
					Logger.gdL().logException(e);
				}
			buffer = null;
			Logger.gdL().logInfo("All SingleDatabaseControllers successfully shut down.");
		}
		
		/**
		 * Restarts the DatabaseConnectionHandler.
		 */
		public synchronized void restart() {
			Logger.gdL().logWarning("Restarting DatabaseConnectionHandler...");
			Thread restarter = new Thread(new Runnable() {
				@Override
				public void run() {
					if (MultiDatabaseController.this.running) {
						shutdown();
						MultiDatabaseController.DatabaseConnectionHandler.super.interrupt();
						setUp();
						start();
						Logger.gdL().logInfo("Successfully restarted the DatabaseConnectionHandler.");
					} else
						Logger.gdL().logWarning("MultiDatabaseController is not running so DatabaseConnectionHandler cannot be started!");
				}
			});
			restarter.setDaemon(true);
			restarter.start();
		}
		
		/**
		 * Return whether {@link #buffer} contains enough {@link SingleDatabaseController}.</br>
		 * This is the case if {@link #buffer}.{@link List#size() size} is greater than {@link #MIN_BUFFER}.
		 * 
		 * @return ({@link #buffer}.{@link List#size() size} > {@link #MIN_BUFFER})
		 */
		public boolean isReady() {
			return buffer.size() > MIN_BUFFER;
		}
		
		/**
		 * Returns whether the {@link Thread} is running and the {@link DatabaseConnectionHandler} is caring for the {@link #buffer}.
		 * 
		 * @return {@link #running}
		 */
		public boolean isRunning() {
			return running;
		}
		
		/**
		 * Removes first {@link SingleDatabaseController} from the {@link #buffer}.</br>
		 * If {@link #buffer}.{@link List#size size} is lower than <code>0</code> the handler will wait up to {@link MultiDatabaseController#DB_TIMEOUT} milliseconds and check whether {@link #buffer} got filled in this time.</br>
		 * If not <code>null</code> gets returned.</br></br>
		 * If you don't want to wait for the Handler in this case call {@link #getConnection(false)}
		 * 
		 * @return An Instance of a connected {@link SingleDatabaseController} from the {@link #buffer}
		 */
		public SingleDatabaseController getConnection() {
			return getConnection(true);
		}
		
		/**
		 * Removes first {@link SingleDatabaseController} from the {@link #buffer}.</br>
		 * If {@link #buffer}.{@link List#size() size} is lower than <code>0</code> the handler will wait up to {@link MultiDatabaseController#DB_TIMEOUT} milliseconds if <code>wait</code> is <code>true</code> and check whether {@link #buffer} got filled in this time.</br>
		 * If <code>wait</code> is <code>false</code>, <code>null</code> gets returned immediately if an error occurs.</br>
		 * </br>
		 * {@link getConnection(true)} is equivalent to {@link #getConnection()}.
		 * 
		 * @param wait if an error occurs this indicates whether up to {@link MultiDatabaseController#DB_TIMEOUT} milliseconds should be waited for a second try.
		 * @return An Instance of a connected {@link SingleDatabaseController} from the {@link #buffer} or <code>null</code> if an error occurs.
		 */
		public SingleDatabaseController getConnection(boolean wait) {
			if (buffer.size() > 0) {
				if (buffer.size()-1 <= MIN_BUFFER) {
					if (buffer.size()-1 < MIN_BUFFER && !locked)
						Logger.gdL().logWarning("Buffer-Filler already got called but the size of the buffer is smaller than the MIN_BUFFER!");
					if (locked) {
						synchronized (lock) {
							lock.notifyAll();
						}
					}
					checkForChange();
				}
			} else {
				if (wait) {
					Logger.gdL().logError("Buffer is empty. Waiting for a new DatabaseConnection...");
					Object lock = new Object();
					synchronized (lock) {
						emptyBufferLocks.add(lock);
						try {
							lock.wait(DB_TIMEOUT);
							return getConnection(false);
						} catch (InterruptedException e) {
							emptyBufferLocks.remove(lock);
							Logger.gdL().logError("Waiting for Buffer failed!");
							Logger.gdL().logException(e);
						}
					}
				} else
					Logger.gdL().logError("Buffer is empty!");
				return null;
			}
			SingleDatabaseController database = buffer.removeFirst();
			return database;
		}
		
		/**
		 * Stores the given {@link SingleDatabaseController} if it {@link SingleDatabaseController#isConnected()} equals <code>true</code>.</br>
		 * If the {@link #buffer} reached {@link #MAX_BUFFER} {@link SingleDatabaseController#disconnect()} gets called as no more connection can be stored.</br>
		 * Otherwise the {@link SingleDatabaseController} gets stored in the {@link #buffer}.
		 * 
		 * @param controller
		 * @throws SQLException
		 */
		public synchronized void storeConnection(SingleDatabaseController controller) throws SQLException {
			if ((buffer.size() < MAX_BUFFER) && controller.isConnected()) {
				buffer.add(controller);
			} else {
				controller.disconnect();
				Logger.gdL().logWarning("Buffer is already full. Cannot add DatabaseConnection to buffer!");
			}
		}
		
		/**
		 * Checks whether there are enough {@link SingleDatabaseController} in the {@link #buffer}.<br>
		 * If not new ones get created and if there are enough {@link #lock}.{@link Thread#wait() wait()} gets called.</br></br>
		 * 
		 * <i>Overrides {@link Thread#run()}</i>
		 * 
		 * @see {@link Thread#run()}
		 */
		@Override
		public void run() {	// Connection-Builder
			boolean error = false;
			while (running) {
				if (buffer.size() < TARGET_BUFFER) {
					Logger.gdL().logInfo("Filling DatabaseConnetionHandler-Buffer [" + (buffer.size()+1) + "/" + TARGET_BUFFER + "]");
					try {
						// Connection building
						SingleDatabaseController database = new SingleDatabaseController(DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, DB_NAME, true);
						database.setTimeout(DB_TIMEOUT);
						buffer.add(database);
						if (error) error = false;
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException | TimeOutException e) {
						Logger.gdL().logError("Could not establish connection with database " + "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
						Logger.gdL().logException(e);
						
						if (error || EXIT_ON_ERROR) MultiDatabaseController.this.stop();
						else Logger.gdL().logWarning("Trying again to connect in " + DB_TIMEOUT/1000 + " seconds...");
						
						if (!error) error = true;
						
						try {
							synchronized (lock) {
								lock.wait(DB_TIMEOUT);
							}
						} catch (InterruptedException e1) {
							Logger.gdL().logError("An error occured while waiting. Shutting down MultidatabaseController...");
							Logger.gdL().logException(e1);
							MultiDatabaseController.this.stop();
						}
					}
					releaseLock();
				} else {
					// If Buffer has reached targeted length
					synchronized (lock) {
						Logger.gdL().logInfo("Buffer is full. Buffer-Filler waits for empty buffer...");
						try {
							locked = true;
							lock.wait();
						} catch (InterruptedException e) {
							Logger.gdL().logError("An error occured while waiting for empty Buffer!");
							Logger.gdL().logException(e);
							if (running) {
								restart();
								return;
							}
						} finally {
							locked = false;
						}
					}
				}
			}
		}
	}
}
