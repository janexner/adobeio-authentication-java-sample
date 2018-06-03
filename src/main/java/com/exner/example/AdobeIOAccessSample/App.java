package com.exner.example.AdobeIOAccessSample;

import static io.jsonwebtoken.SignatureAlgorithm.RS256;
import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;

/**
 * Hello world!
 *
 */
public class App {
	private static final String AUTH_SERVER_FQDN = "ims-na1.adobelogin.com";
	private static final String AUTH_ENDPOINT = "/ims/exchange/jwt/";

	public static void main(String[] args) {
		System.out.println("Authenticating with Adobe.io.");

		System.out.println("Prep - setting variables...");
		String secretKeyFileName = "secret.key";
		// from the Integration Overview screen
		// get there via https://console.adobe.io
		String apiKey = "<put your API Key here>";
		String techAccountID = "<put your technical account ID here>";
		String organizationID = "<put your organization ID here>";
		String clientSecret = "<put your client secret here>";
		// from the JWT screen
		// get there via https://console.adobe.io
		String metaContexts[] = new String[] { "ent_reactor_admin_sdk" };
		String apiHostFQDN = "mc-api-activation-reactor.adobe.io";
		String apiEndpoint = "/properties/";
		// from Launch UI
		// get there through https://marketing.adobe.com/activation
		String popertyID = "<put your property ID here>";

		try {
			System.out.println("Step 1 - generating a JWT...");
			// Set expirationDate in milliseconds since epoch to 24 hours ahead of now
			Long expirationTime = System.currentTimeMillis() / 1000 + 86400L;

			// Secret key as byte array. Secret key file should be in DER encoded format.
			byte[] privateKeyFileContent = Files.readAllBytes(Paths.get(secretKeyFileName));
			// Create the private key
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			KeySpec ks = new PKCS8EncodedKeySpec(privateKeyFileContent);
			RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(ks);

			// Create JWT payload
			HashMap<String, Object> jwtClaims = new HashMap<String, Object>();
			jwtClaims.put("iss", organizationID);
			jwtClaims.put("sub", techAccountID);
			jwtClaims.put("exp", expirationTime);
			jwtClaims.put("aud", "https://" + AUTH_SERVER_FQDN + "/c/" + apiKey);
			for (int i = 0; i < metaContexts.length; i++) {
				jwtClaims.put("https://" + AUTH_SERVER_FQDN + "/s/" + metaContexts[i], TRUE);
			}

			// Create the final JWT token
			String jwtToken = Jwts.builder().setClaims(jwtClaims).signWith(RS256, privateKey).compact();

			System.out.println("Step 2 - getting an access token...");
			String accessToken;
			HttpHost authServer = new HttpHost(AUTH_SERVER_FQDN, 443, "https");
			HttpPost authPostRequest = new HttpPost(AUTH_ENDPOINT);
			authPostRequest.addHeader("Cache-Control", "no-cache");
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("client_id", apiKey));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			params.add(new BasicNameValuePair("jwt_token", jwtToken));
			authPostRequest.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
			// create two HttpClientBuilders for testing purposes
			// the first one uses a Charles Proxy - meaning you can see all traffic
			// the second is standard and works without Charles
			HttpClientBuilder clientBuilderForDebuggingWithCharles = makeHttpClientBuilderForDebug();
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			// use either of the HttpClientBuilders here
			CloseableHttpClient httpclient = clientBuilderForDebuggingWithCharles.build();
			HttpResponse response = httpclient.execute(authServer, authPostRequest);
			if (200 != response.getStatusLine().getStatusCode()) {
				throw new IOException("Server returned error: " + response.getStatusLine().getReasonPhrase());
			}
			HttpEntity entity = response.getEntity();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jnode = mapper.readValue(entity.getContent(), JsonNode.class);
			accessToken = jnode.get("access_token").textValue();

			System.out.println("Step 3 - use API (aka 'done').");
			HttpHost apiHost = new HttpHost(apiHostFQDN, 443, "https");
			HttpGet getRequest = new HttpGet(apiEndpoint + popertyID);
			getRequest.addHeader("Accept", "application/vnd.api+json;revision=1");
			getRequest.addHeader("Content-Type", "application/vnd.api+json");
			getRequest.addHeader("Authorization", "Bearer " + accessToken);
			getRequest.addHeader("X-Api-Key", apiKey);
			// send the request
			HttpResponse httpResponse = httpclient.execute(apiHost, getRequest);
			if (200 != httpResponse.getStatusLine().getStatusCode()) {
				throw new IOException("Server returned error: " + httpResponse.getStatusLine().getReasonPhrase());
			}
			HttpEntity entity1 = httpResponse.getEntity();
			// parse the response
			System.out.println(EntityUtils.toString(entity1));
		} catch (IOException e) {
			System.err.println("Problem: " + e.getLocalizedMessage());
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Problem: " + e.getLocalizedMessage());
		} catch (InvalidKeySpecException e) {
			System.err.println("Problem: " + e.getLocalizedMessage());
		} catch (KeyManagementException e) {
			System.err.println("Problem: " + e.getLocalizedMessage());
		} catch (KeyStoreException e) {
			System.err.println("Problem: " + e.getLocalizedMessage());
		}
	}

	private static HttpClientBuilder makeHttpClientBuilderForDebug()
			throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpHost proxy = new HttpHost("localhost", 8888);
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
