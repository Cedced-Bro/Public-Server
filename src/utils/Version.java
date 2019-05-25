package utils;

/**
 * This class can store versions based on {@link #MAJOR}, {@link #MINOR}, {@link #SNAPSHOT} and {@link #BETA} changes.</br>
 * These then can be {@link #compare(Version) compared} and {@link #toString() printed} out again.
 * 
 * @author Cedric
 * @version 1.0
 *
 */
public class Version {

	/**
	 * Equivalent to the first part of a {@link Version}.
	 */
	public final int MAJOR;
	/**
	 * Equivalent to the second part of a {@link Version} and after {@link #MAJOR}.
	 */
	public final int MINOR;
	/**
	 * Equivalent to the third part of a {@link Version} and after {@link #MINOR}
	 */
	public final int SNAPSHOT;
	/**
	 * Optional. Indicates whether {@link Version} is in Beta-phases or not.
	 */
	public final boolean BETA;
	
	/**
	 * Creates a new {@link Version} based on {@link #MAJOR}, {@link #MINOR}, {@link #SNAPSHOT} and {@link #BETA}.
	 * 
	 * @param major {@link #MAJOR}
	 * @param minor {@link #MINOR}
	 * @param snapshot {@link #SNAPSHOT}
	 * @param beta {@link #BETA}
	 */
	public Version(int major, int minor, int snapshot, boolean beta) {
		this.MAJOR = major;
		this.MINOR = minor;
		this.SNAPSHOT = snapshot;
		this.BETA = beta;
	}

	/**
	 * Creates a new {@link Version} based on the given {@link String} representation.
	 * 
	 * @param version representation of a {@link Version}
	 */
	public Version(String version) {
		try {
			this.MAJOR = Integer.parseInt(version.substring(0, version.indexOf('.')));
			this.MINOR = Integer.parseInt(version.substring(version.indexOf('.')+1, version.indexOf('.', version.indexOf('.')+1)));
			String snapshot = version.substring(version.indexOf('.', version.indexOf('.')+1)+1);
			if (snapshot.contains("b")) {
				this.SNAPSHOT = Integer.parseInt(snapshot.substring(0, snapshot.indexOf('b')));
				this.BETA = true;
			} else {
				this.SNAPSHOT = Integer.parseInt(snapshot);
				this.BETA = false;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("The given Version String does not match the Version-String pattern!", e);
		}
	}
	
	/**
	 * Copies the given {@link Version} to a new instance.
	 * 
	 * @param version {@link Version} to be copied.
	 */
	public Version(Version version) {
		this(version.MAJOR, version.MINOR, version.SNAPSHOT, version.BETA);
	}
	
	/**
	 * Checks whether the given {@link Version} is higher than the own one.
	 * 
	 * @param vers Version to compare with
	 * @return true if the given {@link Version} is greater than the local {@link Version}
	 *  false if otherwise
	 */
	public boolean compare(Version vers) {
		if (MAJOR > vers.MAJOR) return true;
		else if (MINOR > vers.MINOR && MAJOR == vers.MAJOR) return true;
		else if (SNAPSHOT > vers.SNAPSHOT && MAJOR == vers.MAJOR && MINOR == vers.MINOR) return true;
		else if (vers.BETA && !BETA && MAJOR == vers.MAJOR && MINOR == vers.MINOR && SNAPSHOT == vers.SNAPSHOT) return true;
		else return false;
	}
	
	/**
	 * Converts {@link Version Versions} to {@link String Strings} again.
	 * 
	 * @see Object#toString()
	 */
	public String toString() {
		return MAJOR + "." + MINOR + "." + SNAPSHOT + (BETA ? "b" : "");
	}
	
	/**
	 * Returns the {@link #MAJOR} Attribute.
	 * 
	 * @return {@link #MAJOR}
	 */
	public int getMajor() {
		return MAJOR;
	}

	/**
	 * Returns the {@link #MINOR} Attribute.
	 * 
	 * @return {@link #MINOR}
	 */
	public int getMinor() {
		return MINOR;
	}

	/**
	 * Returns the {@link #SNAPSHOT} Attribute.
	 * 
	 * @return {@link #SNAPSHOT}
	 */
	public int getSnapshot() {
		return SNAPSHOT;
	}

	/**
	 * Returns the {@link #BETA} Attribute.
	 * 
	 * @return {@link #BETA}
	 */
	public boolean isBeta() {
		return BETA;
	}
}
