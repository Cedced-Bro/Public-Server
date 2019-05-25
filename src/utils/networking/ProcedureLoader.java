package utils.networking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import utils.io.ConfigAdapter;
import utils.io.Logger;
import utils.io.PropertiesAdapter;

/**
 * 
 * @author Cedric
 *
 */
public class ProcedureLoader {
	
	/**
	 * 
	 */
	private static PropertiesAdapter properties;
	/**
	 * 
	 */
	private static HashMap<String, Procedure[]> activeProcedures;
	/**
	 * 
	 */
	private static final String[] dataTypes;
	
	static {
		dataTypes = new String[] {"NO_RET", "BIT", "TINYINT", "SMALLINT", "INTEGER", "BIGINT", "FLOAT", "DOUBLE", "NUMERIC", "DECIMAL", "CHAR", "VARCHAR", "LONGVARCHAR", "DATE", "TIME", "TIMESTAMP", "BINARY", "VARBINARY", "LONGVARBINARY", "BLOB", "CLOB", "MULTIPLE_RETS"};
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void initProcedures() throws FileNotFoundException, IOException {
		properties = new PropertiesAdapter(new File(ConfigAdapter.getDefaultConfig().getConfigString("dbActiveProcedures")));
		
		activeProcedures = new HashMap<String, Procedure[]>();
		
		boolean error = false;
		
		for (String s : dataTypes) {
			String configString = properties.getProperty(s).replace("\t", "");
			if (configString.contains(";")) {
				ArrayList<Procedure> procedures = new ArrayList<Procedure>();
				for (String s2 : configString.split(";")) {
					try {
						s2 = s2.replace("{", "").replace("}", "");
						if (!s2.equals("")) procedures.add(Procedure.getFromString(s + " " + s2));
					} catch(NullPointerException | IndexOutOfBoundsException | IllegalArgumentException e) {
						Logger.gdL().logError("Procedures for datatype " + s + " failed!");
						Logger.gdL().logException(e);
						error = true;
						continue;
					}
				}
				activeProcedures.put(s, procedures.toArray(new Procedure[procedures.size()]));
			} else if (configString.equals("{}")) activeProcedures.put(s, new Procedure[] {});
			else activeProcedures.put(s, new Procedure[] {Procedure.getFromString(s + " " + (configString = configString.replace(" ", "")).substring(1, configString.length()-1))});
		}
		
		if (error) Logger.gdL().logWarning("Could not load all Procedures successful due to config-Errors!");
	}

	/**
	 * Returns the {@link Procedure} identified by the given name.</br>
	 * If the {@link Procedure} is not active <code>null</code> will be returned.
	 * 
	 * @param name Name of the {@link Procedure} to be returned
	 * @return {@link Procedure} with the given name
	 */
	public static Procedure getProcedureByName(String name) {
		for (Procedure[] ps : activeProcedures.values())
			for (Procedure p : ps)
				if (p.NAME.equals(name) || p.ALIAS.equals(name))
					return p;
		return null;
	}	
}
