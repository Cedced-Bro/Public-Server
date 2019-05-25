package utils.networking;

public class Procedure {
	/**
	 * 
	 */
	public final String NAME;
	/**
	 * 
	 */
	public final String RET_TYPE;
	/**
	 * 
	 */
	public final String[] PARAM;
	/**
	 * 
	 */
	public final String ALIAS;
	
	/**
	 * 
	 */
	public Procedure(String name, String retType, String...param) {
		this(name, retType, null, param);
	}
	
	/**
	 * 
	 * @param name
	 * @param retType
	 * @param param
	 * @throws IllegalArgumentException
	 */
	public Procedure(String name, String retType, String alias, String...param) {
		if (name == null || retType == null) throw new IllegalArgumentException("Name and returnType cannot be null!");
		this.NAME = name;
		this.RET_TYPE = retType;
		this.ALIAS = alias;
		this.PARAM = param;
	}

	/**
	 * 
	 * @param procedure
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 * @return
	 */
	public static Procedure getFromString(String procedure) {
		if (procedure == null) throw new NullPointerException("Given Procedure cannot be null!");
		String retType = procedure.substring(0, procedure.indexOf(" "));
		//jesko: Modified{
		String name;
		if(procedure.indexOf(" ") > procedure.indexOf("(") || procedure.indexOf(" ") < 0){
			name = procedure.substring(0, procedure.indexOf("("));
			retType = "";
		} else name = procedure.substring(procedure.indexOf(" ") + 1, procedure.indexOf("("));
		//}
		String alias = null;
		if (procedure.contains("=")) alias = procedure.substring(procedure.indexOf("=")+1, procedure.length());
		return new Procedure(name, retType, alias, procedure.substring(procedure.indexOf("(") + 1, procedure.lastIndexOf(")")).replace(" ", "").split(","));
	}
	
	/**
	 * Overrides {@link Object#toString()}.</br>
	 * Prints the values of the Procedure.
	 */
	@Override
	public String toString() {
		StringBuilder params = new StringBuilder();
		for (String s : PARAM) params.append(s + ", ");
		params.replace(params.length()-2, params.length(), "");
		return RET_TYPE + " " + NAME + "(" + params.toString() + ")" + (ALIAS == null ? "" : "=" + ALIAS);
	}
}
