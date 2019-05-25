package android.databasecontroller.server.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

//import javax.imageio.ImageIO;

import de.gymnasium_hoechstadt.ghschulapp.network.Feedback;
import utils.io.ConfigAdapter;
import utils.io.Logger;

public class FeedBackWriter {
	
	public static final String feedbackFolder = ConfigAdapter.getDefaultConfig().getConfigString("feedbackLoc");

	static {
		Logger.gdL().logInfo("Checking for " + feedbackFolder);
		if (!new File(feedbackFolder).exists()) {
			if (checkForFolder()) {
				Logger.gdL().logInfo("Succesfully created " + feedbackFolder);
			} else {
				Logger.gdL().logWarning("Folder could not be created. This may cause errors!");
			}
		}
	}

	private static synchronized boolean checkForFolder() throws SecurityException {
		return new File(feedbackFolder).mkdirs();
	}

	private static boolean writeToFile(Feedback feedback, int amount) throws IOException {
		Logger.gdL().logInfo("Trying to write feedback or error");
		/*
		 * if (feedback.IMG.length != 0) { for (int i = 0; i < feedback.IMG.length; i++)
		 * { File f = new File(feedbackFolder + "img_" + amount + "_" + i + ".png");
		 * ImageIO.write(feedback.IMG[i], "png", f); } }
		 */
		File output = null;
		if (feedback.TYPE == Feedback.Type.FEEDBACK) {
			output = new File(feedbackFolder + "feedback_" + amount + ".txt");
		} else {
			output = new File(feedbackFolder + "error_" + amount + ".txt");
		}
		if (output.exists()) {
			return false;
		}
		output.createNewFile();
		PrintWriter w = new PrintWriter(new FileWriter(output));
		w.println(feedback.FEEDBACK.replace("</br>", Logger.SEPERATOR));
		w.flush();
		w.close();
		return true;
	}

	public static boolean storeFeedback(Feedback feedback) throws IOException, SecurityException {
		int amount = 0;
		for (String s : new File(feedbackFolder).list()) {
			if (feedback.TYPE == Feedback.Type.FEEDBACK && s.contains("feedback"))
				amount++;
			else if (feedback.TYPE == Feedback.Type.ERROR && s.contains("error"))
				amount++;
		}
		return writeToFile(feedback, amount);
	}
}
