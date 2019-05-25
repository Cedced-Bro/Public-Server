package utils.io;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class CSVConverter { 
	
	public static final String SEPERATOR;
	public static final String LINE_SEPERATOR; // Here Logger.SEPERATOR is not used as this can vary on different systems
	
	static {
		SEPERATOR = ",";
		LINE_SEPERATOR = "</br>";
	}
	
	private static String parseObject(Object o) throws IOException {
		if (o instanceof Boolean) return "" + ((Boolean) o);
		else if (o instanceof Byte) return "" + ((Byte) o);
		else if (o instanceof Short) return "" + ((Short) o);
		else if (o instanceof Character) return "" + ((Character) o);
		else if (o instanceof Integer) return "" + ((Integer) o);
		else if (o instanceof Long) return "" + ((Long) o);
		else if (o instanceof Float) return "" + ((Float) o);
		else if (o instanceof Double) return "" + ((Double) o);
		else if (o instanceof String) return (String) o;
		else return Serializer.objectToString(o);
	}
	
	public static String toCSV(ResultSet set) throws SQLException, IOException {
		ResultSetMetaData metaData = set.getMetaData();
		int numOfCol = metaData.getColumnCount();
		int length = 0;
		
		// Initializes all ColumnNames
		StringBuilder res = new StringBuilder();
		for (int i = 1; i <= numOfCol; i++) res.append(metaData.getColumnName(i) + SEPERATOR);
		length = res.length();
		res.replace(length-SEPERATOR.length(), length, "");
		
		set.beforeFirst();
		
		while(set.next()) {
			res.append(LINE_SEPERATOR);
			for (int i = 1; i <= numOfCol; i++) res.append(parseObject(set.getObject(i)).trim().replace(",", "<comma>") + SEPERATOR);
			length = res.length();
			res.replace(length-SEPERATOR.length(), length, "");
		}
		
		return res.toString();
	}
	
	/**
	 * This method usually makes a ResultSet out of a CSV-based String. This will not be needed on the Server so it is not implemented!
	 * 
	 * @param csv
	 * @return
	 */
	@Deprecated
	public static ResultSet getResultSet(String csv) throws NoSuchMethodException {
		throw new NoSuchMethodException("This method is not implemented on the Server!");
	}
}
