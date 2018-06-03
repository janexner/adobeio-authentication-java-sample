package com.exner.example.AdobeIOAccessSample;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

public class Tools {

	public static HttpClientBuilder makeHttpClientBuilderForDebug(String proxyHost, int proxyPort)
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpHost proxy = new HttpHost(proxyHost, proxyPort);
		builder.setProxy(proxy);

		// setup a Trust Strategy that allows all certificates.
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				return true;
			}
		}).build();
		builder.setSslcontext(sslContext);

		// don't check Hostnames, either.
		HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

		// here's the special part:
		// -- need to create an SSL Socket Factory, to use our weakened "trust
		// strategy";
		// -- and create a Registry, to register it.
		//
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory)
				.build();
		// now, we create connection-manager using our Registry.
		// -- allows multi-threaded use
		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		builder.setConnectionManager(connMgr);
		return builder;
	}

}
