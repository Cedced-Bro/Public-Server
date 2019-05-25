package data;

import java.io.File;
import java.io.IOException;

public class automaticRestarter {
	public static void main(String[] args) {
		System.err.println("THIS SHOULD NOT BE EXECUTED BY A USER!");
		try {
			Thread.sleep(1000);
			Runtime.getRuntime().exec(System.getProperty("java.home") + File.pathSeparator + "bin" + File.pathSeparator + "java -jar " + args[0]);
		} catch (IOException | InterruptedException e) {
			// This should not be called by a user!
		}
	}
}
