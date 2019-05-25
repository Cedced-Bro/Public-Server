package utils.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.Base64;

public class Serializer {
	public static String objectToString(Object o) throws IOException {
		ByteArrayOutputStream bOut = null;
		ObjectOutputStream oOut = null;
		try {
			if (o == null) return objectToString(new Object());
			bOut = new ByteArrayOutputStream();
			oOut = new ObjectOutputStream(bOut);
			oOut.writeObject(o);
			oOut.flush();
			bOut.flush();
			return new String (Base64.getEncoder().encode(bOut.toByteArray()));
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			oOut.close();
			bOut.close();
		}
		return null;
	}
	
	public static Object getObject(String object) throws ClassNotFoundException, IOException, SQLException {
		if (object == null) return getObject(objectToString(new Object()));
		byte b[] = Base64.getDecoder().decode(object.getBytes());
		ByteArrayInputStream bIn = new ByteArrayInputStream(b);
		ObjectInputStream oIn = new ObjectInputStream(bIn);
		return oIn.readObject();
	}
}
