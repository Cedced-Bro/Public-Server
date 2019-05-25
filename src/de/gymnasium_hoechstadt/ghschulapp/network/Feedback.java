package de.gymnasium_hoechstadt.ghschulapp.network;

//import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Feedback implements Serializable {

	public static final String SEPERATOR = "</br></br>";
	public final String FEEDBACK;
	public final Type TYPE;
//	public final BufferedImage[] IMG;
	
	/**
	 * This is the Serial-Version-UID to allow secure Serializing
	 */
	private static final long serialVersionUID = 744529923957530727L;

	public enum Type {
		FEEDBACK,
		ERROR;
	}
	
	public Feedback(String feedback) {
		this(feedback, Type.FEEDBACK);
	}
	
	public Feedback(String feedback, Type type) {
		this.FEEDBACK = feedback;
		this.TYPE = type;
//		this(feedback, type, new BufferedImage[0]);
	}
	
//	public Feedback(String feedback, BufferedImage[] imgs) {
//		this(feedback, Type.FEEDBACK, imgs);
//	}
	
//	public Feedback(String feedback, Type type, BufferedImage[] imgs) {
//		this.FEEDBACK = feedback;
//		this.TYPE = type;
//		this.IMG = imgs;
//	}
	
}
