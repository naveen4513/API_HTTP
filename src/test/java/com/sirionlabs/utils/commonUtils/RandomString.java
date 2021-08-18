package com.sirionlabs.utils.commonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

public class RandomString {

	private final static Logger logger = LoggerFactory.getLogger(RandomString.class);

	public static String getRandomAlphaNumericString(int stringLength) {
		String acceptedChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		return getRandomAlphaNumericString(acceptedChars, stringLength);
	}

	public static String getRandomAlphaNumericString(String acceptedChars, int stringLength) {
		try {
			SecureRandom rnd = new SecureRandom();
			StringBuilder sb = new StringBuilder(stringLength);

			for (int i = 0; i < stringLength; i++)
				sb.append(acceptedChars.charAt(rnd.nextInt(acceptedChars.length())));

			return sb.toString();
		} catch (Exception e) {
			logger.error("Exception while generating Random AlphaNumeric String with Accepted Chars [{}] of Length {}. {}", acceptedChars, stringLength, e.getStackTrace());
			return null;
		}
	}
}