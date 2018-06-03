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
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;

public class App {
	private static final String AUTH_SERVER_FQDN = "ims-na1.adobelogin.com";
	private static final String AUTH_ENDPOINT = "/ims/exchange/jwt/";

	public String getAccessToken(String secretKeyFileName, String apiKey, String techAccountID, String organizationID,
			String clientSecret, String[] metaContexts, HttpClient httpClient)
			throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, KeyManagementException,
			KeyStoreException, ClientProtocolException, JsonParseException, JsonMappingException {
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
		HttpHost authServer = new HttpHost(AUTH_SERVER_FQDN, 443, "https");
		HttpPost authPostRequest = new HttpPost(AUTH_ENDPOINT);
		authPostRequest.addHeader("Cache-Control", "no-cache");
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("client_id", apiKey));
		params.add(new BasicNameValuePair("client_secret", clientSecret));
		params.add(new BasicNameValuePair("jwt_token", jwtToken));
		authPostRequest.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
		HttpResponse response = httpClient.execute(authServer, authPostRequest);
		if (200 != response.getStatusLine().getStatusCode()) {
			throw new IOException("Server returned error: " + response.getStatusLine().getReasonPhrase());
		}
		HttpEntity entity = response.getEntity();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jnode = mapper.readValue(entity.getContent(), JsonNode.class);
		String accessToken = jnode.get("access_token").textValue();
		return accessToken;
	}

}
