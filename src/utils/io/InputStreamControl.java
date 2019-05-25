package utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Customizable Implementation of the {@link InputStream}.</br>
 * At the moment for example {@link InputStream#close()} got overwritten to prevent autoclosing Stream-Handlers from closing important {@link Stream Streams} simultaniously.
 * </br>
 * This is the direct opposite implementation of the {@link OuputStreamControl}.
 * 
 * @author Cedric
 * @version 1.0
 * 
 * @see InputStream
 * @see OutputStreamControl
 */
public class InputStreamControl extends InputStream {

	/**
	 * {@link InputStream} to control.
	 */
	private final InputStream inputStream;
	
	/**
	 * Creates a new InputStreamController
	 * 
	 * @param inputStream {@link InputStream} which should be controlled
	 */
	public InputStreamControl(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	
	@Override
	public int read() throws IOException {
		return inputStream.read();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return inputStream.read(b);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return inputStream.read(b, off, len);
	}
	
	@Override
	public long skip(long n) throws IOException {
		return inputStream.skip(n);
	}
	
	@Override
	public int available() throws IOException {
		return inputStream.available();
	}

	@Override
	public synchronized void mark(int readlimit) {
		inputStream.mark(readlimit);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		inputStream.reset();
	}
	
	@Override
	public boolean markSupported() {
		return inputStream.markSupported();
	}
	
	/**
	 * Instead of closing the {@link #inputStream} this method actually does nothing.</br>
	 * This is implemented to prevent auto-Closers from for example closing {@link System#in}.</br></br>
	 * If you want to close the {@link #inputStream} anyways call {@link #closeInput()}.
	 */
	@Override
	public void close() {
		// Does nothing as this Stream should not be closed by autoClosers.
	}
	
	/**
	 * The actual {@link InputStream#close()}-Method.
	 * 
	 * @throws IOException gets thrown if {@link InputStream#close()} fails
	 * 
	 * @see {@link InputStream#close()}
	 */
	public void closeInput() throws IOException {
		inputStream.close();
	}
}
