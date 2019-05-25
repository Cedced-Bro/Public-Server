package utils.networking;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Security {
	public static SSLContext getSSLContext(String keyStoreType, String keyStoreLoc, boolean inJar, char[] keyStorePwd) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, NoSuchProviderException, UnrecoverableKeyException {
		KeyStore keystore = KeyStore.getInstance(keyStoreType);
		InputStream in;
		if (inJar) in = Security.class.getResourceAsStream(keyStoreLoc);
		else in = new FileInputStream(new File(keyStoreLoc));
		keystore.load(in, keyStorePwd);
		
		TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustFactory.init(keystore);
		
		KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		factory.init(keystore, keyStorePwd);
		
		SSLContext context = SSLContext.getInstance("TLSv1");
		context.init(factory.getKeyManagers(), trustFactory.getTrustManagers(), new SecureRandom());
		
		return context;
	}
}
