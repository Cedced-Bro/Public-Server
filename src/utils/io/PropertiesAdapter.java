package utils.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * 
 * @author Cedric
 * @version 1.0
 * @category util</br></br>
 * 
 * This class is an Adapter for writing or reading Files with the Properties-Syntax.
 * 
 */
public class PropertiesAdapter {
	
	// ******************
	// * Private fields *
	// ******************
	private Properties properties;
	private String path;
	private boolean shutdownHookApplied;
	
	// ****************
	// * Constructors *
	// ****************
	/**
	 * Gets a new Object of Property-Adapter.</br>
	 * <i><b>Note:</b> Just other utils-Adapters can access this Constructor.</i>
	 */
	PropertiesAdapter() {
		properties = new Properties();
	}
	
	/**
	 * Get a new Object of Property-Adapter relating to a specified file.
	 * 
	 * @param file the File with which you want to use the Adapter
	 * @throws FileNotFoundException Gets thrown if file is not accessible or does not exist
	 * @throws IOException If an Error occurs while initializing either the Properties or the FileInputStream relating to it
	 */
	public PropertiesAdapter(File file) throws FileNotFoundException, IOException {
		this();
		updateProperties(new FileInputStream(file), file.getPath());
	}
	
	// *******************
	// * Private Methods *
	// *******************
	private String format(String s) {
		if (s == null) return s;
		if (s == "") return s;
		s = s.replace("\t", "");
		if (s.contains("#")) {
			int helperIndex = 0;
			int index = 0;
			while (index > -2) {
				if ((helperIndex = s.indexOf("#", index)) > -1) {
					index = helperIndex+1;
					if (s.charAt(helperIndex-1) != '\\') {
						s = s.substring(0, helperIndex);
						index = -2;
					}
				} else
					index = -2;
				
				if (index == -2)
					s = s.replaceAll("\\#", "#");
			}
		}
		if (!(s.charAt(0) == '[') && s.contains("\"")) {
			int start = -1;
			int end = -1;
			int helperIndex = 0;
			while (start == -1 && helperIndex != -1) {
				helperIndex = s.indexOf("\"", helperIndex);
				if (helperIndex != 0) {
					if (s.charAt(helperIndex-1) != '\\') {
						start = helperIndex;
					} else helperIndex++;
				} else start = helperIndex;
			}
			helperIndex = 0;
			for (int i = s.lastIndexOf('"'); i > start && end == -1; i--) {
				if (s.charAt(i) == '"' && s.charAt(i-1) != '\\') end = i;
			}
			if (start != -1 && end != -1)
				s = s.substring(start+1, end);
		}
		s = s.replace("\\t", "\t");
		return s;
	}
	
	
	// ******************
	// * Public Methods *
	// ******************
	/**
	 * Returns a value stored in a properties-File linked to the given key.
	 * 
	 * @param key which the value you want is linked to
	 * @return The value which is linked to the given key in the properties-File
	 */
	public String getProperty(String key) {
		if (Logger.isDefaultInitialized()) Logger.gdL().logInfo("Trying to load " + key + " from " + path);
		return format(properties.getProperty(key));
	}
	
	/**
	 * Stores a new value linked to a key in a properties-File.
	 * 
	 * @param key the key with which you can find stored values in a properties-File
	 * @param value the value which will be written into the properties-File
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void setProperty(String key, String value, File out) throws FileNotFoundException, IOException {
		if (Logger.isDefaultInitialized()) Logger.gdL().logInfo("Trying to set " + key + " to " + value + " in " + path);
		else System.out.println("[I] Setting " + key + " to " + value);
		properties.setProperty(key, value);
		if (Logger.isDefaultInitialized() && !shutdownHookApplied) Logger.gdL().logInfo("Adding shutdown-Hook for Properties-Saves after shutdown");
		if (!shutdownHookApplied) {
			if (Logger.isDefaultInitialized()) Logger.gdL().logInfo("Setting shutdown-Hook for Properties-Saves");
			else System.out.println("[I] Adding shutdown-Hook!");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					BufferedReader reader = null;
					PrintWriter writer = null;
					try {
						reader = new BufferedReader(new FileReader(out));
						StringBuilder builder = new StringBuilder();
						String str = null;
						while ((str = reader.readLine()) != null) builder.append(str + Logger.SEPERATOR);
						reader.close();
						
						writer = new PrintWriter(out);
						for (String s : builder.toString().split(Logger.SEPERATOR)) {
							if (s.contains("=") && !s.substring(s.indexOf("=")).equals(properties.getProperty(s.substring(0, s.indexOf("="))))) s = s.substring(0, s.indexOf("=")+1) + properties.getProperty(s.substring(0, s.indexOf("=")));
							System.out.println(s);
							writer.println(s);
						}
						writer.println(builder.toString());
						writer.flush();
						System.out.println("[I] Successfully updated Properties " + out.getPath());
					} catch (IOException e) {
						System.err.println("[!] An error occured while saving properties");
						e.printStackTrace();
					}
					try {
						reader.close();
						writer.close();
					} catch (IOException e) {
						// Should not occur
						System.err.println("[!] Could not close streams");
						e.printStackTrace();
					}
				}
			});
		}
		shutdownHookApplied = true;
	}
	
	/**
	 * Updates the file you want to use the Adapter with.
	 * 
	 * @param inputStream InputStream of the File with which you want to use the Adapter
	 * @throws FileNotFoundException Gets thrown if file is not accessible or does not exist
	 * @throws IOException If an Error occurs while initializing either the Properties or the FileInputStream relating to it
	 */
	public void updateProperties(InputStream inputStream, String path) throws IOException {
		if (Logger.isDefaultInitialized()) Logger.gdL().logInfo("Updating Properties " + this.path + " to " + path);
		this.path = path;
		if (inputStream != null) {
			if (properties == null)
				properties = new Properties();
			properties.load(inputStream);
			inputStream.close();
		}
		else {
			properties = null;
			System.gc();
		}
	}
	
	public String getPath() {
		return path;
	}
	
	/**
	 * Gives all the keys synced to the linked {@link #properties Properties-File}.
	 * 
	 * @return {@link String String[]} of all the stored keys in the Properties-File
	 */
	public String[] getPropertyKeys() {
		return properties.keySet().toArray(new String[properties.size()]);
	}
}
