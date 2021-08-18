package com.sirionlabs.test.common;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.FirstOtp;
import com.sirionlabs.api.commonAPI.Otp;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.UserAccessHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestTwoFactorAuthentication extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestTwoFactorAuthentication.class);

    private static String configFilePath;
    private static String configFileName;
    private static  AdminHelper admin;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TFAConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TFAConfigFileName");
    }

    @AfterClass
	public void afterClass() {
        new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @Test
    public void testTwoFactorAuthentication() {
        CustomAssert csAssert = new CustomAssert();

        try {

             admin = new AdminHelper();
            int clientId = admin.getClientId();

            logger.info("Starting Test: Validating Two Factor Authentication.");

            String otpApiUrl = Otp.getAPIPath();
            Map<String, String> defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);

            String userName = defaultProperties.get("username");
            String userPassword = defaultProperties.get("password");

            Integer userId = getUserId(userName);
            String passCode = getPassCode(userName, userPassword, userId);

            String payload = Otp.getPayload(passCode, userId.toString(), clientId);
            logger.info("Hitting Otp API.");
            String otpResponse = executor.post(otpApiUrl, Otp.getHeaders(), payload).getResponse().getResponseBody();

            if (ParseJsonResponse.validJsonResponse(otpResponse)) {
                logger.info("Validating FirstOtp Response.");
                JSONObject jsonObj = new JSONObject(otpResponse);
                String targetUrl = jsonObj.getString("targetUrl");
                Boolean error = true;

                if (jsonObj.has("error") && !jsonObj.getBoolean("error"))
                    error = false;

                if (!targetUrl.equalsIgnoreCase(ConfigureEnvironment.getCompleteHostUrl() + "/welcome") || error) {
                    csAssert.assertTrue(false, "Target URL Returned by Otp API doesn't contain Expected TargetUrl. Two Factor Authentication failed.");
                }
            } else {
                csAssert.assertTrue(false, "Otp API Response is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Two Factor Authentication. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private Integer getUserId(String userName) {

            admin.loginWithClientAdminUser();
        UserAccessHelper accessHelperObj = new UserAccessHelper();
        return accessHelperObj.getIdForUser(userName);
    }

    private String getPassCode(String userName, String userPassword, Integer userId) {
        Check checkObj = new Check();
        checkObj.hitCheck(userName, userPassword);

        String firstOtpAPIPath = FirstOtp.getAPIPath(userId);
        HashMap<String, String> firstOtpHeaders = FirstOtp.getHeaders();

        logger.info("Hitting FirstOtp API.");
        Integer firstOtpResponseCode = executor.get(firstOtpAPIPath, firstOtpHeaders).getResponse().getResponseCode();

        if (firstOtpResponseCode != 200) {
            throw new SkipException("FirstOtp API Request failed. Hence couldn't get PassCode.");
        }
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

        String query = "select expected_otp from otp_session_info where user_id = " + userId + " order by id desc limit 1";
        logger.info("Fetching Otp from DB using Query [{}]", query);

        try {
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                return results.get(0).get(0);
            }

            throw new SkipException("Couldn't get Otp from DB.");
        } catch (Exception e) {
            throw new SkipException("Exception while Getting Otp from DB. " + e.getMessage());
        }
    }
}