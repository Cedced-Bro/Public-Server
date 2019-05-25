package android.databasecontroller.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

import android.databasecontroller.server.io.ConnectionHandler;
import android.databasecontroller.server.io.ConsoleCommandHandler;
import android.databasecontroller.server.io.ConsoleSenderReceiver;
import utils.Maths;
import utils.io.ConfigAdapter;
import utils.io.Logger;
import utils.networking.ProcedureLoader;

/**
 * 
 * @author Cedric
 *
 */
public class Server {

	public static final String DEFAULT_CONFIG;
	private static FileOutputStream loggerFile = null;
	private static ConnectionHandler connectionHandler = null;

	private static Thread consoleSenderReceiver = null;

	private static boolean automaticRestart = false;

	private volatile static Object lock = new Object();
	private static boolean locked = false;
	private static boolean shuttingDown = false;

	static {
		DEFAULT_CONFIG = "serverdata/server.cfg";
	}

	public static void main(String[] args) throws InterruptedException {
		if (args.length > 0) {
			System.out.println("[I] User input detected.");
			if (!ConsoleCommandHandler.executeCommands(args)) {
				System.err.println("[?] Exiting...");
				return;
			}
		}

		Logger.gdL().logInfo("Starting Server");

		boolean logToFile = Boolean.parseBoolean(ConfigAdapter.getDefaultConfig().getConfigString("logToFile"));

		if (logToFile) {
			try {
				String logs = ConfigAdapter.getDefaultConfig().getConfigString("logFile");
				File f = new File(logs);
				if (f.getParentFile().mkdirs())
					Logger.gdL().logInfo("Created directories for the logging");
				if (f.exists()) {
					File old = new File(logs + ".old");
					Logger.gdL().logInfo("Overwriting " + old.getPath());
					if (old.exists())
						old.delete();
					f.renameTo(old);
				}
				if (f.createNewFile())
					Logger.gdL().logInfo("Created new File " + f.getName());
				loggerFile = new FileOutputStream(f, true);
			} catch (NullPointerException | IOException e) {
				Logger.gdL()
						.logError("An error occured while loading the config-File!" + Logger.SEPERATOR
								+ "If this error occurs more often make sure you did not edit it yourself!"
								+ Logger.SEPERATOR + "If you did delete it and let the program create a new one.");
				Logger.gdL().logException(e);
			}
		}

		try {
			Thread.sleep(750); // This is for loading letting the Logger log everything before overwriting it
								// TODO: Better variant
			if (logToFile)
				Logger.setDefaultLogger(new Logger(new OutputStream[] { System.out, loggerFile }, "[Server]"));
			else
				Logger.setDefaultLogger(new Logger(new OutputStream[] { System.out }, "[Server]"));
			Logger.gdL().logInfo("Successfully set the defaultLogger");

			Logger.gdL().logInfo("Starting Connection-Handler");
			connectionHandler = new ConnectionHandler(
					Integer.parseInt(ConfigAdapter.getDefaultConfig().getConfigString("chPort")),
					Integer.parseInt(ConfigAdapter.getDefaultConfig().getConfigString("chMaxConnections")
							.replace("MAX", Integer.MAX_VALUE + "").replace("MIN", "1")),
					ConfigAdapter.getDefaultConfig().getConfigString("chName"));

			Logger.gdL().logInfo("Starting ConsoleCommand-Receiver");

			consoleSenderReceiver = new Thread(new Runnable() {
				@Override
				public void run() {
					ConsoleSenderReceiver.start();
				}
			});
			consoleSenderReceiver.start();

			Logger.gdL().logInfo("Loading active Procedures");
			ProcedureLoader.initProcedures();
			Logger.gdL().logInfo("Procedure-Loading finished");
			// DO SOMETHING E.G. UI

			synchronized (lock) {
				locked = true;
				lock.wait((int) Maths.calculate(ConfigAdapter.getDefaultConfig().getConfigString("automaticClose")));
				locked = false;
			}
		} catch (Exception e) {
			Logger.gdL().logError("A fatal error occured... Exiting");
			Logger.gdL().logException(e);
		} finally {
			// Close Connection controlled
			if (!shuttingDown)
				shutdown();
		}
	}

	public static FileOutputStream getLogFile() {
		return loggerFile;
	}

	private static void selfRestart(boolean inner) {
		Logger.gdL().logInfo("Initializing Restarter");
		String restarterLoc = ConfigAdapter.getDefaultConfig().getConfigString("automaticRestarter");
		if (!inner)
			selfShutdown(false);
		File restarter = new File(restarterLoc);
		if (restarter.exists()) {
			try {
				StringBuilder cmd = new StringBuilder();
				cmd.append(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java ");
				for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments())
					cmd.append(jvmArg + " ");
				cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
				cmd.append(Server.class.getName());
				Runtime.getRuntime().exec(cmd.toString());
				System.exit(0);
			} catch (IOException e) {
				Logger.gdL().logError("An Error occured while trying to reload program! Execution: exec");
				Logger.gdL().logException(e);
			}
		} else {
			Logger.gdL().logWarning("\"" + restarterLoc + "\" does not exist... Exiting!");
		}
		System.exit(0);
	}

	private static void selfShutdown(boolean exit) {
		if (shuttingDown) {
			System.err.println("Already shutting down!");
			return;
		}

		shuttingDown = true;

		if (connectionHandler != null)
			connectionHandler.stop();

		ConsoleSenderReceiver.stop();

		Logger.gdL().shutdown();
		if (locked)
			synchronized (lock) {
				lock.notifyAll();
			}
		// This is to ensure that the Logger and the Main-Thread can shutdown properly
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (consoleSenderReceiver.isAlive())
			consoleSenderReceiver.interrupt();

		automaticRestart = Boolean
				.parseBoolean(ConfigAdapter.getDefaultConfig().getConfigString("restartOnAutomaticClose"));

		if (automaticRestart)
			selfRestart(true);
		else if (exit)
			System.exit(0);
	}

	public static void restart() {
		selfRestart(false);
	}

	public static synchronized void shutdown() {
		selfShutdown(true);
	}

	public static boolean getAutomaticRestart() {
		return automaticRestart;
	}
}
