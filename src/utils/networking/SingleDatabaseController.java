package utils.networking;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import utils.io.ConfigAdapter;
import utils.io.Logger;

/**
 * This class is an easy but on the same hand powerful adapter to work with JDBC-Databases.</br>
 * The difference to the {@link MultiDatabaseController_old} is that this class just handles one connection instead of multiple connections.</br>
 * This is very useful to execute one SQL-Query after the other.</br>
 * If you need multiple threads at the same time to communicate with a Database use {@link MultiDatabaseController_old}.</br></br>
 * 
 * <b>Note:</b></br>
 * You will need a MySql-Connector. For optimal experience use the already tested mysql-connector-java-8.0.11.jar or higher.
 * 
 * <i>Next update will bring:<ul>
 * <li>Password-Hashing</li>
 * </ul></i>
 * 
 * @author Cedric
 * @version 1.1
 * @category utils.network
 */
public class SingleDatabaseController {

	// **********
	// * Fields *
	// **********
	private long timeout;
	private Connection con;
	private Exception thrownException;

	// *********************
	// * Public Attributes *
	// *********************
	public final int PORT;
	public final String HOST;
	public final String USR;
	public final String PWD;
	public final String DATABASE_NAME;
	public final String CONNECTION_DETAILS;
	
	public final static String DEFAULT_CONNECTION_ARGUMENTS;
	
	static {
		DEFAULT_CONNECTION_ARGUMENTS = ConfigAdapter.getDefaultConfig().getConfigString("dbDefaultConnectionArguments");
	}

	// ****************
	// * Constructors *
	// ****************
	/**
	 * Prepares a {@link Connection} to a Database by saving Database information.</br>
	 * If <code>setConnection</code> is <code>true</code> it will also try to {@link #connect()}.</br>
	 * Please use an own {@link Thread} for connecting as it may take up to 20 sec.
	 *
	 * @param host the host for the database (db)
	 * @param port the port, on which the db runs
	 * @param usr the username for the db
	 * @param pwd the password for the db
	 * @param databaseName the db name, to which the connection is needed
	 * @param setConnection whether also a new connection should be established and {@link #connect()} gets called
	 * @throws SQLException This exception gets thrown if an error occurs while connecting to the db
	 * @throws ClassNotFoundException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws IllegalAccessException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws InstantiationException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws TimeOutException Gets thrown if host does not answer
	 */
	public SingleDatabaseController(String host, int port, String usr, String pwd, String databaseName, boolean setConnection) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, TimeOutException {
		this(host, port, usr, pwd, databaseName, null, true);
	}
	
	/**
	 * Prepares a {@link Connection} to a Database by saving Database information.
	 *
	 * @param host the host for the database
	 * @param port the port, on which the database runs
	 * @param usr the username for the database
	 * @param pwd the password for the database
	 * @param databaseName the database name, to which the connection is needed
	 */
	public SingleDatabaseController(String host, int port, String usr, String pwd, String databaseName) {
		this(host, port, usr, pwd, databaseName, null);
	}
	
	/**
	 * Prepares a {@link Connection} to a Database by saving Database information.</br>
	 * If <code>setConnection</code> is <code>true</code> it will also try to {@link #connect()}.</br>
	 * Please use an own {@link Thread} for connecting as it may take up to 20 sec.
	 *
	 * @param host the host for the database (db)
	 * @param port the port, on which the db runs
	 * @param usr the username for the db
	 * @param pwd the password for the db
	 * @param databaseName the db name, to which the connection is needed
	 * @param connectionArgs attributes with which the Controller gets started
	 * @param setConnection whether also a new connection should be established and {@link #connect()} gets called
	 * @throws SQLException This exception gets thrown if an error occurs while connecting to the db
	 * @throws ClassNotFoundException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws IllegalAccessException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws InstantiationException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws TimeOutException Gets thrown if host does not answer
	 */
	public SingleDatabaseController(String host, int port, String usr, String pwd, String databaseName, String connectionArgs, boolean setConnection) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, TimeOutException {
		this(host, port, usr, pwd, databaseName, connectionArgs);
		if (setConnection) connect();
	}
	
	/**
	 * Prepares a {@link Connection} to a Database by saving Database information.
	 *
	 * @param host the host for the database
	 * @param port the port, on which the database runs
	 * @param usr the username for the database
	 * @param pwd the password for the database
	 * @param databaseName the database name, to which the connection is needed
	 * @param connectionArgs attributes with which the Controller gets started
	 */
	public SingleDatabaseController(String host, int port, String usr, String pwd, String databaseName, String connectionArgs) {
		this.HOST = host;
		this.PORT = port;
		this.USR = usr;
		this.PWD = pwd;
		this.DATABASE_NAME = databaseName;
		if (connectionArgs == null) CONNECTION_DETAILS = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + DEFAULT_CONNECTION_ARGUMENTS;
		else CONNECTION_DETAILS = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + connectionArgs;
	}

	// *******************
	// * Private Methods *
	// *******************

	/**
	 * Internal method for connecting to the Database.</br>
	 * Please use an own {@link Thread} for connecting as it may take up to 20 sec.</br></br>
	 * <i><b>Note:</b></br>
	 * This method needs a JDBC Controller installed to run. If there is no Controller there cannot be established a Connection.</i>
	 *
	 * @throws SQLException This Exception gets thrown if an error occurs while connecting to the db
	 * @throws ClassNotFoundException Gets thrown if SQL-Driver couldn't be loaded or is not installed
	 * @throws IllegalAccessException Gets thrown if SQL-Driver couldn't be loaded or is not installed
	 * @throws InstantiationException Gets thrown if SQL-Driver couldn't be loaded or is not installed
	 * @throws TimeOutException Gets thrown if host does not answer
	 */
	private void connect() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, TimeOutException {
		thrownException = null;
		// You will need a JDBC Controller
		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
		Object lock = new Object();
		
		Thread connectingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// TODO: Show password-Hash
					Logger.gdL().logInfo("Trying to bind Server to Database '" + DATABASE_NAME + "' (" + CONNECTION_DETAILS + " with usr: " + USR + ")");
					con = DriverManager.getConnection(CONNECTION_DETAILS, USR, PWD);
					Logger.gdL().logInfo("Successfully connected to Database '" + DATABASE_NAME + "'");
				} catch (SQLException exception) {
					thrownException = exception;
				} finally {
					synchronized(lock) {
						lock.notifyAll();
					}
				}
			}
		});
		
		connectingThread.start();
		
		synchronized(lock) {
			try {
				lock.wait(timeout);
				Thread.sleep(100);						// Ensures that Thread really dies.
				if (connectingThread.isAlive()) {
					Logger.gdL().logError("Connection to Database '" + DATABASE_NAME + "' failed");
					connectingThread.interrupt();
					throw new TimeOutException("It took longer than " + (int)(timeout/1000) + " seconds to establish a Connection");
				}
			} catch (InterruptedException e) {
				Logger.gdL().logWarning("Something interrupted the waiting... The Server may behave differently");
				Logger.gdL().logException(e);
			}
		}
		
		if (thrownException instanceof SQLException) throw (SQLException) thrownException;
	}

	
	// ******************
	// * Public Methods *
	// ******************

	/**
	 * Executes given SQL-queries and returns the result in a {@link ResultSet}.
	 *
	 * @param query The SQL-query which gets executed
	 * @return the result of that SQL-query
	 * @throws SQLException Gets thrown if an error occurs while executing the query
	 * @throws ClassNotFoundException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws IllegalAccessException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws InstantiationException Gets thrown if SQL-Driver couldn't be loaded
	 * @throws TimeOutException Gets thrown if host does not answer
	 */
	public synchronized ResultSet executeSQL(String query) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, TimeOutException {
		if (!isConnected()) connect();
		Statement st = con.createStatement();
		st.setQueryTimeout((int) timeout / 1000);
		return st.executeQuery(query);
	}
	
	/**
	 * 
	 * @param proc
	 * @param args
	 * @return
	 * @throws SQLException
	 * @throws ClassCastException
	 * @throws NumberFormatException
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	public synchronized ResultSet executeResultSetProcedure(Procedure proc, Object...args) throws SQLException, ClassCastException, NumberFormatException, NullPointerException, IndexOutOfBoundsException {
		StringBuilder b = new StringBuilder();
		if (args.length > 0) b.append("(");
		for (int i = 0; i < args.length-1; i++) b.append("?, ");
		if (args.length > 0) b.append("?)");
		CallableStatement call = con.prepareCall("{CALL " + proc.NAME + b.toString() + "}");
		for (int i = 0; i < args.length; i++)
			call.setObject(i + 1, args[i]);
		call.setQueryTimeout((int)timeout/1000);
		call.execute();
		return call.getResultSet();
	}
	
	/**
	 * Checks if there is valid {@link Connection} to the Database.
	 *
	 * @return whether there is a valid connection to the database
	 * @throws SQLException Gets thrown if an error occurs while checking
	 */
	public boolean isConnected() throws SQLException {
		if (con == null) return false;
		return !(con.isClosed());
	}

	/**
	 * Closes the {@link Connection} {@link #con} to the Database.
	 *
	 * @throws SQLException Gets throws if an error occurs while disconnecting
	 */
	public synchronized void disconnect() throws SQLException {
		con.close();
	}
	
	/**
	 * Sets a new {@link #timeout} in milliseconds.
	 * 
	 * @param milliseconds overrides current {@link #timeout}
	 */
	public synchronized void setTimeout(long milliseconds) {
		Logger.gdL().logInfo("Setting Timeout for DatabaseConnection to " + milliseconds + " milliseconds");
		this.timeout = milliseconds;
	}
	
	/**
	 * Returns the {@link #timeout} in milliseconds.
	 * 
	 * @return {@link #timeout} in milliseconds
	 */
	public long getTimeout() {
		return timeout;
	}
}
