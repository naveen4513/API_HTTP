package com.sirionlabs.helper.api;

import java.util.HashMap;
import java.util.Map;

public class APIResponse {

	private int responseCode;
	private String responseBody;
	private Map<String, String> headers;
	private String responseMessage;

	public APIResponse() {
		headers = new HashMap<>();
	}

	public Integer getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}


	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	String getHeader(String name) {
		return headers.get(name);
	}

	public void setHeader(String name, String value) {
		this.headers.put(name, value);
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
}