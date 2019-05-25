package android.databasecontroller.server.io;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Scanner;

import utils.Version;
import utils.io.ConfigAdapter;
import utils.io.InputStreamControl;
import utils.io.Logger;

public class Updater {
	
	/**
	 * Synchronizes the defaultBuffer with the {@link System#out}.
	 */
	private static void synchronizeWithLogger() {
		if (!Logger.gdL().isBufferEmpty()) {
			synchronized (Logger.gdL().getBufferLock()) {
				try {
					Logger.gdL().getBufferLock().wait();
				} catch (InterruptedException e) {
					Logger.gdL().logError("An error occured while waiting for an empty Logger-Buffer");
					Logger.gdL().logException(e);
				}
			}
		}
	}
	
	/**
	 * Asks the User whether he wants to continue with something.</br>
	 * <i>NOTE: It is recommended to first call {@link #synchronizeWithLogger()} to have no bugs in your I/O.</i>
	 * 
	 * @return <code>true</code> if the user wants to continue <code>false</code> otherwise
	 */
	private static boolean continueWork() {
		Scanner scan = new Scanner(new InputStreamControl(System.in));
		boolean continueWork = false;
		for (int i = 0; i < 3; i++) {
			System.out.print("Do you want to continue [Y/n]: ");
			String input = scan.nextLine().toLowerCase();
			if (input.equals("yes") || input.equals("y") || input.equals("true")) {
				continueWork = true;
				break;
			} else if (input.equals("no") || input.equals("n") || input.equals("false")) {
				continueWork = false;
				break;
			} else {
				System.err.println("Answer was not clear. [" + (i+1) + "/3]");
			}
		}
		scan.close();
		
		return continueWork;
	}
	
	/**
	 * Probes SUDO Password. If it is correct <code>true</code> gets returned <code>false</code> otherwise.
	 * 
	 * @param password the password which gets probed
	 * @return <code>true</code> if the password is correct <code>false</code> otherwise
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private static boolean probePassword(char[] password) throws IOException, InterruptedException {
		boolean ret = false;
		File tempFile = new File("checkForAdmin.sh");
		tempFile.deleteOnExit();
		if (!tempFile.exists()) {
			tempFile.createNewFile();
			FileWriter w = new FileWriter(tempFile);
			String ls = System.lineSeparator();
			w.write("#!/bin/bash" + ls);
			w.write("if [ -w /usr/bin/yes ]; then echo \"yes\" >> test.txt; fi");
			w.flush();
			w.close();
		}
		String[] cmd = {"bash", "-c", "chmod a+x \"" + tempFile.getAbsolutePath() + "\" && echo '" + Arrays.toString(password) + "' | sudo \""+tempFile.getAbsolutePath() + "\""};
		Runtime.getRuntime().exec(cmd).waitFor();
		File test = new File("test.txt");
		if (test.exists()) {
			test.delete();
			return true;
		}
		return ret;
	}
	
	/**
	 * Reads the Root-Password out of the console.
	 * 
	 * @param consoleInput whether {@link Console} or {@link Scanner} (unsecure) is used for password input. 
	 * @return password if the user gives a valid password, null otherwise.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private static char[] askPassword(boolean consoleInput) throws IOException, InterruptedException {
		char[] result = null;
		Scanner scan = null;
		Console console = null;
		
		if (consoleInput)
			console = System.console();
		else
			scan = new Scanner(new InputStreamControl(System.in));

		for (int i = 0; i < 3; i++) {
			System.out.print("[sudo] password: ");
			if (consoleInput) {
				result = console.readPassword();
			} else
				result = scan.nextLine().toCharArray();
			if (!probePassword(result)) {
				System.err.println("Wrong password [" + (i+1) + "/3]");
				result = null;
			} else break;
		}
		
		if (scan != null) scan.close();
		
		return result;
	}
	
	private static void privateExecute(String command) throws IOException {
		File tempFile = new File("private_executer.sh");
		tempFile.deleteOnExit();
		if (!tempFile.exists()) {
			tempFile.createNewFile();
			FileWriter w = new FileWriter(tempFile);
			String ls = System.lineSeparator();
			w.write("#!/bin/bash" + ls);
			w.write("if [ \"$1\" != \"\" ]" + ls);
			w.write("  then" + ls);
			w.write("    set +o history" + ls);
			w.write("    eval $1" + ls);
			w.write("    set -o history"+ls);
			w.write("fi" + ls);
			w.flush();
			w.close();
		}
		String[] cmd = {"bash", "-c", "chmod a+x \"" + tempFile.getAbsolutePath() + "\" && \""+tempFile.getAbsolutePath() + "\" \"" + command + "\""};
		Runtime.getRuntime().exec(cmd);
	}
	
	/**
	 * Updates the Server to the given version by exporting the default-files.</br>
	 * In Linux also a command gets created in /usr/bin to ensure easy access if wanted so.
	 * 
	 * @param filesToUpdate OutputFilePaths to update
	 * @param version new Version after Update
	 */
	public static void update(String[] filesToUpdate, Version version) {
		Logger.gdL().logInfo("Updating program to " + version.toString());
		
		File file;
		for (String fileName : filesToUpdate) {
			Logger.gdL().logInfo("Exporting " + fileName);
			file = new File(fileName);
			if (file.mkdirs()) Logger.gdL().logInfo("Successfully created " + file.getParent());
			if (file.exists()) {
				file.delete();
				Logger.gdL().logInfo("Deleted " + file.getAbsolutePath());
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				Logger.gdL().logError("Could not create/update file " + fileName);
				Logger.gdL().logException(e);
				continue;
			}
			try {
				Files.copy(ConfigAdapter.class.getResourceAsStream("/data/" + file.getName()), Paths.get(fileName, new String[0]), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
			} catch (IOException e) {
				Logger.gdL().logError("Updating " + fileName + " failed due to export-errors!");
				Logger.gdL().logException(e);
			}
			Logger.gdL().logInfo("Successfully updated " + fileName);
		}
		
		if (System.getProperty("os.name").contains("Linux") && !new File("/usr/bin/androidserver").exists()) {
			boolean success = false;
			
			Logger.gdL().logInfo("Asking for Linux-Command-Creation.");
			Console cons = System.console();
			char[] password = null;
			try {
				if (cons == null) {
					Logger.gdL().logWarning("No console installed. If you like to continue anyway, there might be a security risk as the password will also be directly displayed on the screen.");
					synchronizeWithLogger();
					if (continueWork()) {
						password = askPassword(false);
					} else
						System.out.println("Not continuing...");
				} else {
					synchronizeWithLogger();
					if (continueWork()) {
						password = askPassword(true);
					}
				}
				
				if (password != null) {
					File tempScript = File.createTempFile("androidcommand_creater_script", null);
					
					PrintWriter writer = new PrintWriter(new FileWriter(tempScript));
					writer.println("#!/bin/bash");
					writer.println("echo \"java -jar " + Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + " \\$@\" >> /usr/bin/androidserver"
							+ System.lineSeparator() + "chmod 777 /usr/bin/androidserver");
					writer.close();
					
					privateExecute("echo '" + Arrays.toString(password) + "' | sudo chmod 777 " + tempScript.getAbsolutePath());
					privateExecute("echo '" + Arrays.toString(password) + "' | sudo " + tempScript.getAbsolutePath());
					
					try {
						Process p = Runtime.getRuntime().exec("/usr/bin/androidserver ?");
						BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()));
						String s = "";
						String s2 = "";
						while ((s2 = r.readLine()) != null)
							s.concat(s2);
						if (s == "")
							success = true;
					} catch(IOException e) {
						success = false;
					}
				}
				
			} catch (IOException | URISyntaxException | InterruptedException e) {
				Logger.gdL().logError("An error occured while creating the /usr/bin/androidserver quickcommand");
				Logger.gdL().logException(e);
			} finally {
				password = null;
			}
			
			if (success) {
				Logger.gdL().logInfo("Successfully created quickCommand /usr/bin/androidserver");
			} else {
				Logger.gdL().logWarning("The Server could not create a Linux-Command for the Server!");
			}
		}
		
		ConfigAdapter.getDefaultConfig().setConfigString("version", version.toString());
		
		Logger.gdL().logInfo("Successfully updated program to " + version.toString());
		
		System.gc();
	}
}
