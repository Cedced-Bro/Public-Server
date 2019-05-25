package utils.networking;

@SuppressWarnings("serial")
public class TimeOutException extends Exception {
	/**
	 * 
	 */
    public TimeOutException() {
    	super ();
    }
    /**
     * @param msg
     */
	public TimeOutException(String msg) {
    	super (msg);
    }
	public TimeOutException(Throwable cause) {
		super (cause);
	}
	public TimeOutException(String msg, Throwable cause) {
		super (msg, cause);
	}
	public TimeOutException(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super (msg, cause, enableSuppression, writableStackTrace);
	}
}
