/*
 */

package com.googlecode.batchfb;

import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * <p>This will decipher and validate the OAuth 2.0 cookie you get from Facebook.  You can recognize it because
 * it starts with "fbsr_".  This code does *not* work with the older "fbs_" OAuth 1.0 cookie.</p>
 * 
 * <p>This is the ONLY class in batchfb that relies on commons-codec.  This is just for the Base64 decoder,
 * which intelligently handles "urlsafe" base64 strings with _ and - instead of + and *.  The base64 decoder
 * in JAXB doesn't, so we need the 3rd party lib.</p>
 */
public class FacebookCookie
{
	/** */
	private static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/** */
	@JsonProperty("algorithm")
	String algorithm;

	/** */
	@JsonProperty("user_id")
	long fbId;
	public long getFbId() { return this.fbId; }

	@JsonProperty("oauth_token")
	String accessToken;
	/** Despite being present in Python running samples app, this doesn't seem to actually show up. Expect it to be null. */
	public String getAccessToken() { return this.accessToken; }

	@JsonProperty("code")
	String code;
	/** This shows up in the cookie payload instead of oauth_token, but isn't documented */
	public String getCode() { return this.code; }

	/**
	 * Decodes and validates the cookie.
	 * @param cookie is the fbsr_YOURAPPID cookie from Facebook
	 * @param appSecret is your application secret from the Facebook Developer application console
	 * @throws IllegalStateException if the cookie does not validate 
	 */
	public static FacebookCookie decode(String cookie, String appSecret)
	{
		// Parsing and verifying signature seems to be poorly documented, but here's what I've found:
		// Look at parseSignedRequest() in https://github.com/facebook/php-sdk/blob/master/src/base_facebook.php
		// Python version:  https://developers.facebook.com/docs/samples/canvas/

		try {
			String[] parts = cookie.split("\\.");
			byte[] sig = Base64.decodeBase64(parts[0]);
			byte[] json = Base64.decodeBase64(parts[1]);
			byte[] plaintext = parts[1].getBytes();	// careful, we compute against the base64 encoded version

			FacebookCookie decoded = MAPPER.readValue(json, FacebookCookie.class);

			// "HMAC-SHA256" doesn't work, but "HMACSHA256" does.
			String algorithm = decoded.algorithm.replace("-", "");

			SecretKey secret = new SecretKeySpec(appSecret.getBytes(), algorithm);
			
			Mac mac = Mac.getInstance(algorithm);
			mac.init(secret);
			byte[] digested = mac.doFinal(plaintext);
			
			if (!Arrays.equals(sig, digested))
				throw new IllegalStateException("Signature failed");

			return decoded;
			
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to decode cookie", ex);
		}
	}
}
