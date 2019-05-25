package android.databasecontroller.server;

import java.io.Serializable;

/**
 * A class to handle Users in different situations. With this class a User can be given rights of also modifying the Server directly.</br>
 * <i>Note that you will have to init a new User if you want to change either the {@link #USERNAME}, the {@link #PASSWORD} or the {@link #PERMISSION}!</i>
 *
 * @author Cedric
 * @version 1.0
 * 
 * @see Serializable
 */
public class User implements Serializable {
	/**
	 */
	private static final long serialVersionUID = -6370638385077184704L;
	
	/**
	 * Permission-Groups which are possible on this Server.</br></br>
	 * 
	 * Following Users exist:
	 * <ul>
	 * <li>{@link #STANDARD}: A User with standard permissions so he can execute the procedures.</li>
	 * <li>{@link #ROOT}: A Superuser who can directly execute SQL-Queries and manipulate the Server.</br>
	 * <i>Note that this User can only be a local User at the moment!</i></li>
	 * </ul>
	 * @author Cedric
	 *
	 */
	public enum Permission {
		STANDARD(true), ROOT(true), NOT_AUTHENTICATED(true);
		
		private boolean authenticated;
		
		private Permission(boolean authenticated) {
			this.authenticated = authenticated;
		}
		
		public boolean isAuthenticated() {
			return authenticated;
		}
	}
	
	/**
	 * The Username of a {@link User}.
	 */
	public final String USERNAME;
	/**
	 * The Password of a {@link User}.
	 */
	public final String PASSWORD;
	/**
	 * The {@link Permission} a {@link User} has.
	 */
	public final Permission PERMISSION;
	
	/**
	 * Initializes a new {@link User} without a {@link #PASSWORD password} to enhance security.
	 * 
	 * @param usr
	 * @param permission
	 * @see #User(String, String, Permission)
	 */
	public User(String usr, Permission permission) {
		this(usr, null, permission);
	}
	
	/**
	 * Initializes a new {@link User} with a {@link #USERNAME username}, {@link #PASSWORD password} and his {@link #PERMISSION permissions}.
	 * 
	 * @param usr to set {@link #USERNAME}
	 * @param pwd to set {@link #PASSWORD}
	 * @param permission to set {@link #PERMISSION}
	 */
	public User(String usr, String pwd, Permission permission) {
		USERNAME = usr;
		PASSWORD = pwd;
		PERMISSION = permission;
	}
}
