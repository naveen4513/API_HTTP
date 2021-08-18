package com.sirionlabs.utils.commonUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Value {

	public static String getMD5(String value) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.update(value.getBytes(), 0, value.length());

		String md5Value = new BigInteger(1, m.digest()).toString(16);

		while (md5Value.length() < 32)
			md5Value = "0" + md5Value;

		return md5Value;
	}
}
