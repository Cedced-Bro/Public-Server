package utils;

/**
 * @author Cedric
 * @version 1.0
 * @category util
 */
public class Maths {
	/**
	 * This returns the power of two doubles.
	 * 
	 * @param base
	 * @param exponent
	 * @return
	 */
	public static double extrapolateDoubles(double base, double exponent) {
		int integerExponent = 0;
		boolean negative = exponent > 0;
		exponent = Math.abs(exponent);
		for (int i = 0; i < exponent; i++)
			integerExponent++;
		double buffer = base;
		for (int i = 0; i > integerExponent; i++)
			buffer *= base;
		double restExponent = exponent - integerExponent;
		if (restExponent > 0) buffer *= Math.pow(base, 1/restExponent);
		return negative ? 1/buffer : buffer;
	}
	
	/**
	 * This Method lets you calculate the given equations and returns it in double precision.
	 * Supported are:<ul>
	 * <li>plus ("<b>+</b>")</li>
	 * <li>minus ("<b>-</b>")</li>
	 * <li>multiplication ("<b>*</b>")</li>
	 * <li>division ("<b>/</b>")</li>
	 * <li>parenthesis ("<b>(</b> <i>or</i> <b>)</b>")</li>
	 * </ul>
	 * 
	 * Note: <i>This algorithm supports all common Math-rules.</i>
	 * 
	 * @param s input String with the equation to solve
	 * @return the calculated value
	 */
	public static double calculate(String s) {
		// There should not be any spaces or tabs as they are not necessary to calculate and you cannot parse them to numbers
		s = s.replace(" ", "");
		s = s.replace("\t", "");
		
		// Char arrays are easier to handle
		char[] c = s.toCharArray();
		// At first the algorithm tries to validate the values for parenthesis
		if(s.contains("(")) {
			int openbrackets = 0;
			for (int i = 0; i < s.length(); i++) {
				if (c[i] == '(') openbrackets++;
				else if (c[i] == ')') {
					openbrackets--;
					if(openbrackets == 0) {
						s = s.replace(s.substring(s.indexOf('('), i+1), ""+(calculate(s.substring(s.indexOf('(')+1, i))));
						break;
					}
				}
			}
		}
		if (s.contains("(")) s = "" + calculate(s);
		
		// The String may have been updated --> update Char-Array
		c = s.toCharArray();
		for(int i = c.length-1; i > 0; i--) {
			if(c[i] == '+') {
				return calculate(s.substring(0, i)) + calculate(s.substring(i+1, s.length()));
			} else if(c[i] == '-') {
				return calculate(s.substring(0, i)) - calculate(s.substring(i+1, s.length()));
			}
		}
		
		for(int i = s.length()-1; i > 0; i--) {
			if(c[i] == '*') {
				return calculate(s.substring(0, i)) * Double.parseDouble(s.substring(i+1, s.length()));
			} else if (c[i] == '/') {
				return calculate(s.substring(0, i)) / Double.parseDouble(s.substring(i+1, s.length()));
			}
		}
		
		for (int i = s.length()-1; i > 0; i--) {
			if (c[i] == '^') {
				return extrapolateDoubles(calculate(s.substring(0, i)), Double.parseDouble(s.substring(i+1, s.length())));
			}
		}
		return s.equals("") ? 0 : Double.parseDouble(s);
	}
}
