package utils.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import android.databasecontroller.server.Server;
import android.databasecontroller.server.io.Updater;
import utils.Version;

/** 
 *	This class allows you to load and write one file using a Properties syntax.</br>
 *	On top of that the controller now also comes with an automatic updater exclusively implemented for the Android-DB-Server
 *
 * 	@author Cedric	
 *	@version 2.4
 *	@category util
 *
 **/
public class ConfigAdapter {
	
	// ******************
	// * Private fields *
	// ******************
	/**
	 * {@link PropertiesAdapter} which the Config-Adapter uses as base for it's I/O.
	 */
	private PropertiesAdapter properties;
	/**
	 * Path of the Config-File.
	 */
	private String path;
	/**
	 * {@link ConfigAdapter Default-ConfigAdapter} which can be used if there should only be one {@link ConfigAdapter}.</br>
	 * Note that you have to call {@link #getDefaultConfig()} to access this Object and {@link #updateDefaultPath(String)} to overwrite.
	 */
	private static ConfigAdapter defaultConfig;
	
	static {
		// Sets default values - They can be updated later with own ones
		if (!new File(Server.DEFAULT_CONFIG).exists()) {
			createConfigFile();
			defaultConfig.setConfigString("version", "0.0.0b");			// This is to trigger checkVersion() in every case
			defaultConfig.setConfigString("update", defaultConfig.getConfigString("allFiles"));
			defaultConfig.setConfigString("configVersion", "0.0.0b");	// This is to trigger the configUpdate to change everything back with next start
		} else
			defaultConfig = new ConfigAdapter(Server.DEFAULT_CONFIG);
		checkVersion();
	}
	
	// ****************
	// * Constructors *
	// ****************
	/**
	 * The Constructor of the ConfigAdater
	 */
	public ConfigAdapter(String path) {
		properties = new PropertiesAdapter();
		updatePath(path);
	}
	
	
	// *******************
	// * Private Methods *
	// *******************
	/**
	 * Method-Dummy for {@link Logger#logInfo(String)}:</br>
	 * If the {@link Logger#defaultLogger} is not initialized it uses a very simple method of telling that the given log is an Info by using the prefix <i><b>[I]</i></b>.</br>
	 * Otherwise the normal {@link Logger#logInfo(String) Logger.getDefaultLogger().logInfo(String)} gets called.
	 * 
	 * @param s {@link String Input-String} to log
	 */
	private static void logInfo(String s) {
		if (Logger.isDefaultInitialized()) Logger.gdL().logInfo(s);
		else System.out.println("[I] " + s);
	}
	
	/**
	 * Method-Dummy for {@link Logger#logWarning(String)}:</br>
	 * If the {@link Logger#defaultLogger} is not initialized it uses a very simple method of telling that the given log is a Warning by using the prefix <i><b>[?]</i></b>.</br>
	 * Otherwise the normal {@link Logger#logWarning(String) Logger.getDefaultLogger().logWarning(String)} gets called.
	 * 
	 * @param s {@link String Input-String} to log
	 */
	private static void logWarning(String s) {
		if (Logger.isDefaultInitialized()) Logger.gdL().logWarning(s);
		else System.out.println("[?] " + s);
	}
	
	/**
	 * Method-Dummy for {@link Logger#logError(String)}:</br>
	 * If the {@link Logger#defaultLogger} is not initialized it uses a very simple method of telling that the given log is an Error by using the prefix <i><b>[!]</i></b>.</br>
	 * Otherwise the normal {@link Logger#logError(String) Logger.getDefaultLogger().logError(String)} gets called.</br>
	 * Note that this dummy uses the {@link System#err Standard-Error-Output}.
	 * 
	 * @param s {@link String Input-String} to log
	 */
	private static void logError(String s) {
		if (Logger.isDefaultInitialized()) Logger.gdL().logError(s);
		else System.err.println("[!] " + s);
	}
	
	/**
	 * Method-Dummy for {@link Logger#logException(String)}:</br>
	 * If the {@link Logger#defaultLogger} is not initialized it uses a very simple method of telling that the given log is a {@link Throwable} by using the {@link System#err System-Error-Output}.</br>
	 * Otherwise the normal {@link Logger#logException(String) Logger.getDefaultLogger().logException(String)} gets called.
	 * 
	 * @param e {@link Throwable Input-Throwable} to log
	 */
	private static void logException(Throwable e) {
		if (Logger.isDefaultInitialized()) Logger.gdL().logException(e);
		else {
			StringBuilder builder = new StringBuilder();
			builder.append(e.toString() + Logger.SEPERATOR);
			for (StackTraceElement s : e.getStackTrace()) {
				builder.append("\tat " + s.toString() + Logger.SEPERATOR);
			}
			System.err.println(builder.toString());
		}
	}
	
	/**
	 * Updates the Config-Path of the properties. Just call this method if you changed path!
	 * 
	 * @return <b>true:</b> if updating was successful</br><b>false:</b> if an error occured while updating
	 */
	private boolean updateConfigPath() {
		try {
			logInfo("Loading File " + path);
			FileInputStream stream = new FileInputStream(new File(this.path));
			this.properties.updateProperties(stream, this.path);
			stream.close();
			return true;
		} catch (IOException e) {
			Logger.getDefaultLogger().logError("Loading " + path + " failed!");
			logException(e);
			return false;
		}
	}
	
	/**
	 * Checks whether there is a newer {@link Version} of the AndroidServer.</br>
	 * If so all the files which were updated get exported from /data/.
	 */
	private static void checkVersion() {
		PropertiesAdapter template = new PropertiesAdapter();
		try {
			template.updateProperties(ConfigAdapter.class.getResourceAsStream("/data/defaultConfig.cfg"), "/data/defaultConfig.cfg");

			Version currentVersion = new Version(ConfigAdapter.getDefaultConfig().getConfigString("version"));
			
			Version latest = new Version(template.getProperty("configVersion"));
			if (latest.compare(new Version(defaultConfig.getConfigString("configVersion")))) {
				logInfo("Updating Config due to a new Version [" + latest.toString() + "]");
				createConfigFile();
			} else logInfo("Config up to date (Current Version: " + latest.toString());
			
			logInfo("Checking for new Version... (Current Version: " + currentVersion.toString() + ")");
			
			latest = new Version(template.getProperty("version"));
			if (latest.compare(currentVersion)) {
				logInfo("Updating Server due to a new Version [" + latest.toString() + "]");
				String updates = template.getProperty("update");
				if (updates.equals("[]")) {
					logInfo("No files to update");
					return;
				}
				ArrayList<String> filesToUpdate = new ArrayList<String>();
				for (String s : updates.split(",")) {
					s = s.replace("\"", "").trim();
					filesToUpdate.add(template.getProperty(s));
				}
				String[] upd = new String[filesToUpdate.size()];
				upd = filesToUpdate.toArray(upd);
				Updater.update(upd, latest);
			}
			
		} catch (IOException e) {
			logError("An error occured while checking version.");
			logException(e);
		} catch (NullPointerException e) {
			logError("Config seems to be corrupt... Exporting defaultConfig!");
			logException(e);
			createConfigFile();
			logWarning("Restart the Server please!");
		}
	}

	/**
	 * Creates a new {@link Version} for the {@link #defaultConfig}.</br>
	 * Returns whether export was successful or not.
	 * 
	 * @return If the export was successful <code>true</code> or if any error occurs <code>false</code> gets returned.
	 */
	private static boolean createConfigFile() {
		try {
			File f = new File(Server.DEFAULT_CONFIG);
			if (f.getParentFile().mkdirs()) {
				logInfo("Created directories for the Config-File");
			}
			if (f.createNewFile()) {
				logInfo("Created " + f.getName());
			} else {
				defaultConfig.properties.updateProperties(null, null);
				f.delete();
				logInfo("Deleted " + f.getName());
				f.createNewFile();
				logInfo("Created " + f.getName());
			}
			if (Files.copy(ConfigAdapter.class.getResourceAsStream("/data/defaultConfig.cfg"), Paths.get(f.getAbsolutePath(), new String[0]), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING }) != 0L) {
				logInfo("Successfully exported default-Config");
			}
			defaultConfig = new ConfigAdapter(Server.DEFAULT_CONFIG);
			logInfo("Succesfully updated default-Config to " + defaultConfig.getConfigString("configVersion"));
			return true;
		} catch (Exception e) {
			logError("An error occured while creating the config-File!");
			logException(e);
		}
		return false;
	}
	
	// ******************
	// * Public Methods *
	// ******************
	/**
	 * Returns a value assigned to the key in the Config-File.
	 * 
	 * @param key is to specify which value should be loaded
	 * @return Value which is assigned to key
	 */
	public String getConfigString(String key) {
		String s = properties.getProperty(key);
		if (s == null) {
			logWarning("ConfigString " + key + " couldn't be loaded");
			throw new NullPointerException("ConfigString " + key + " does not exist");
		}
		return s;
	}
	
	/**
	 * Sets the value assigned to key of the Config-File to a new value.
	 * 
	 * @param key is to specify which value should be updated
	 * @param value new value for the assigned value linked to the key
	 */
	public void setConfigString(String key, String value) {
		try {
			properties.setProperty(key, value, new File(path));
		} catch (IOException e) {
			logError("You can just overwrite log-files which got installed!");
			logException(e);
		}
	}
	
	/**
	 * Gives all the keys synced to the linked {@link #properties Properties-File}.
	 * 
	 * @return {@link String String[]} of all the stored keys in the Config-File
	 */
	public String[] getPropertyKeys() {
		return properties.getPropertyKeys();
	}
	
	/**
	 * Updates the location of the Config-File.</br>
	 * 
	 * @param path the location of the Config-File
	 */
	public boolean updatePath(String path) {
		this.path = path;
		return updateConfigPath();
	}
	
	/**
	 * Updates the location of the defaultConfig-File if you don't want to use the default-location.</br>
	 * <b>default-path:</b> <i>data/config.cfg</i>
	 * 
	 * @param path the location of the Config-File
	 */
	public static boolean updateDefaultPath(String path) {
		defaultConfig.updatePath(path);
		return defaultConfig.updateConfigPath();
	}
	
	/**
	 * Returns the default Config-Adapter
	 * 
	 * @return the default ConfigAdapter
	 */
	public static ConfigAdapter getDefaultConfig() {
		return defaultConfig;
	}
}
