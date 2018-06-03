package com.exner.example.AdobeIOAccessSample;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
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

		App app = new App();
		try {
			String accessToken = app.getAccessToken(secretKeyFileName, apiKey, techAccountID, organizationID,
					clientSecret, metaContexts);

			System.out.println("Step 3 - use API (aka 'done').");
			// create two HttpClientBuilders for testing purposes
			// the first one uses a Charles Proxy - meaning you can see all traffic
			// the second is standard and works without Charles
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			// use either of the HttpClientBuilders here
			CloseableHttpClient httpclient = clientBuilder.build();
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
		} catch (Exception e) {
			fail("Problem: " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}
}
