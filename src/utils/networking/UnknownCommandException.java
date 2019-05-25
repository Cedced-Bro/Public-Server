package utils.networking;

public class UnknownCommandException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1688213831125948486L;

	public UnknownCommandException() {
		super("Unknown Command");
	}
	
	public UnknownCommandException(String msg) {
		super(msg);
	}
	
}
