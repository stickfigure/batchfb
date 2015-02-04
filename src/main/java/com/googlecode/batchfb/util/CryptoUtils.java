package com.googlecode.batchfb.util;

import org.apache.commons.codec.binary.Hex;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 */
public class CryptoUtils {

	/**
	 * See https://developers.facebook.com/docs/graph-api/securing-requests
	 */
	public static String makeAppSecretProof(String appSecret, String accessToken) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(), "HmacSHA256");
			mac.init(secretKey);
			byte[] bytes = mac.doFinal(accessToken.getBytes());

			return Hex.encodeHexString(bytes);

		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}
}
