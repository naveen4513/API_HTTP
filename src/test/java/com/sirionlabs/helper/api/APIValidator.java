package com.sirionlabs.helper.api;

import com.sirionlabs.utils.commonUtils.CustomAssert;

public class APIValidator {

	private APIResponse response;

	APIValidator(APIResponse response) {
		this.response = response;
	}

	public void validateResponseCode(Integer expectedResponseCode, CustomAssert csAssert) {
		Integer actualResponseCode = response.getResponseCode();
		csAssert.assertTrue(actualResponseCode.equals(expectedResponseCode),
				"Expected Response Code: " + expectedResponseCode + " and Actual Response Code: " + actualResponseCode);
	}

	public void validateResponseMessage(String expectedMessage, CustomAssert csAssert) {
		String actualResponseMessage = response.getResponseMessage();
		csAssert.assertTrue(actualResponseMessage.trim().toLowerCase().contains(expectedMessage.trim().toLowerCase()),
				"Expected Message: " + expectedMessage + " and Actual Message: " + actualResponseMessage);
	}

	public APIResponse getResponse() {
		return response;
	}
}