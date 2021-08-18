package com.sirionlabs.helper.api;

import com.sirionlabs.api.commonAPI.Check;

import java.util.HashMap;

public class ApiHeaders {

	public static HashMap<String, String> getContentTypeAsJsonOnlyHeader() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		return headers;
	}

	public static HashMap<String, String> getEmailDefaultHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json");
		headers.put("Content-Type", "application/json");
		return headers;
	}

	public static HashMap<String, String> getContentTypeAsJsonOnlyHeaderWithAuthorization(){
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", Check.getAuthorization());

		return headers;
	}
    public static HashMap<String,String> getOnlyHeaderWithAuthorization() {
		HashMap<String,String> headers=new HashMap<>();
		headers.put("Authorization", Check.getAuthorization());
		return headers;
	}
	public static HashMap<String, String> addHeader(HashMap<String, String> existingHeaders, String header, String value) {
		existingHeaders.put(header, value);
		return existingHeaders;
	}

	public static HashMap<String, String> getDefaultLegacyHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("Content-Type", "application/json;charset=UTF-8");

		return headers;
	}

	public static HashMap<String, String> getDefaultAcceptEncodingHeader() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "gzip, deflate");

		return headers;
	}

	public static HashMap<String, String> getDefaultHeadersForClientAdminAPIs() {
		HashMap<String, String> headers = getDefaultAcceptEncodingHeader();
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

		return headers;
	}
	public static HashMap<String,String> getMeetingNoteCreateAPIHeader()
	{
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept-Language","en-IN,en-GB;q=0.9,en-US;q=0.8,en;q=0.7");
		return headers;
	}
}
