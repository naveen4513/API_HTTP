package com.sirionlabs.utils.commonUtils;

import java.net.URLEncoder;
import java.util.Map;

public class UrlEncodedString {

	public static String getUrlEncodedString(Map<String, String> params) {
		try {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			for (Map.Entry<String, String> entry : params.entrySet()) {

				if (first)
					first = false;
				else
					result.append("&");

				result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				result.append("=");
				result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			}

			return result.toString();
		} catch (Exception e) {
			return "";
		}
	}
}
