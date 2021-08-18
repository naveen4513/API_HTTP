package com.sirionlabs.helper.jwt;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.RefreshToken;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.TimerTask;

public class JWTAccessTokenHelper extends TimerTask {

	private final static Logger logger = LoggerFactory.getLogger(JWTAccessTokenHelper.class);
	private RefreshToken refreshObj = new RefreshToken();

	@Override
	public void run() {

		logger.info("Hitting Refresh Access Token API at Time: [{}]", new Date());
		String refreshTokenAPIResponse = refreshObj.hitRefreshTokenAPI();

		try {
			if (ParseJsonResponse.validJsonResponse(refreshTokenAPIResponse)) {
				JSONObject jsonObj = new JSONObject(refreshTokenAPIResponse);

				if (jsonObj.has("accessToken")) {
					APIUtils.accessToken = jsonObj.getString("accessToken");
					Check.authorization = jsonObj.getString("accessToken");
				} else {
					logger.warn("Couldn't get New Access Token in Refresh Token API Response. Response: [{}]", refreshTokenAPIResponse);

					//Temporary Code below to Debug US Sandbox Issue.
					String textToSave = refreshTokenAPIResponse + "\n" + "Refresh Token: " + Check.refreshToken + "\n" + "Time: " + new Date();
					FileUtils.saveResponseInFile("Refresh Access Token Error Details.txt", textToSave);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Hitting Refresh Access Token API. {}", e.getMessage());
			FileUtils.saveResponseInFile("Refresh Access Token API Response.txt", refreshTokenAPIResponse);
		}
	}
}