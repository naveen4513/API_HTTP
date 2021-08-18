package com.sirionlabs.test.api.QRCodeGeneration;

import com.sirionlabs.api.QRCodeGeneration.QrCodeGenerationAPI;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.QRCodeGeneration.QrCodeGenerationAPIDto;
import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.test.api.ValidateAccessTokenAPI.TestValidateAccessTokenAPI;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.jboss.aerogear.security.otp.api.Base32;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Listeners(value = MyTestListenerAdapter.class)
public class TestQRCodeGenerationAPI {
    private final static Logger logger = LoggerFactory.getLogger(TestValidateAccessTokenAPI.class);
    private String testingType;
    private String dataFilePath = "src/test/resources/TestConfig/APITestData/QRCodeGeneration";
    private String dataFileName = "QrCodeGenerationAPIData.json";
    public static String twoFactorAuthenticationSecretSalt = "B2374TNIQ3HKC446";
    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        List<QrCodeGenerationAPIDto> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
        JSONArray jsonArr = new JSONArray(allJsonData);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    QrCodeGenerationAPIDto dtoObject = getQrCodeGenerationAPIDtoObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }

        }

        for (QrCodeGenerationAPIDto dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private QrCodeGenerationAPIDto getQrCodeGenerationAPIDtoObjectFromJson(JSONObject jsonObj) {
        QrCodeGenerationAPIDto dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String header = jsonObj.getString("header");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("expectedResponseMessage");

            dtoObject = new QrCodeGenerationAPIDto(testCaseId, description, header, expectedStatusCode, expectedResponseMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting BulkUpdate User DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(QrCodeGenerationAPIDto dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            String expectedErrorMessage = dtoObject.getExpectedResponseMessage();
            if (expectedErrorMessage.isEmpty() || expectedErrorMessage.equalsIgnoreCase("")) {
                String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
                JSONArray jsonArray = new JSONArray(allJsonData);
                boolean flag = false;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);

                    if (jsonObj.getString("testCaseId").trim().equalsIgnoreCase(dtoObject.getTestCaseId())) {
                        flag = true;
                        jsonObj.put("header", Check.getAuthorization());
                        dtoObject.setHeader(Check.getAuthorization());
                    }
                    jsonArray.put(i, jsonObj);
                    if (flag) {
                        break;
                    }
                }
                FileWriter fileWriter = new FileWriter(dataFilePath + "/" + dataFileName);
               fileWriter.write(jsonArray.toString());
                fileWriter.flush();
            }
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", dtoObject.getHeader());
            APIResponse apiResponse = QrCodeGenerationAPI.getResponse(headers);
            int actualStatusCode = apiResponse.getResponseCode();
            int expectedStatusCode = Integer.parseInt(dtoObject.getExpectedStatusCode());
            String apiResponseBody = apiResponse.getResponseBody();
            csAssert.assertTrue(actualStatusCode == expectedStatusCode, "Actual Status Code {" + actualStatusCode + "} and Expected Status{" + expectedStatusCode + "} are Different");
            if (ParseJsonResponse.validJsonResponse(apiResponseBody)) {
                if (dtoObject.getExpectedResponseMessage().isEmpty() & dtoObject.getExpectedResponseMessage().equalsIgnoreCase("")) {

                    JSONObject jsonObject = new JSONObject(apiResponseBody);
                    AccountInfo accountInfo = new AccountInfo();
                    csAssert.assertTrue(jsonObject.getString("label").equalsIgnoreCase("sirionlabs"), "label name different in API response body");
                   String secretKey= getSecretKey(accountInfo.getUserId(),new AdminHelper().getClientId());
                   String actualPassCode=getBase32SecretKey(secretKey);
                    csAssert.assertTrue(jsonObject.getString("secret").equalsIgnoreCase(actualPassCode), "secret key different ");
                } else {
                    JSONObject jsonObject = new JSONObject(apiResponseBody);
                    String actualErrorMessage = jsonObject.getString("errorMessage");
                    csAssert.assertTrue(actualErrorMessage.contains(expectedErrorMessage), "actual error message " + actualErrorMessage + "and expected error message" + expectedErrorMessage + " are different");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }
    private static String getBase32SecretKey(String secretKey){
        return new java.lang.String(Base32.encode(secretKey.getBytes()));
    }
    private static String getSecretKey(Integer userId, Integer clientId) {
        return prepareClientIdHash(clientId.toString() + userId) + twoFactorAuthenticationSecretSalt;
    }
    private static String prepareClientIdHash(String in) {
        return in.replace('_', '2')
                .replace('1', '3')
                .replace('8', '4')
                .replace('9', '5')
                .replace('0', '6');
    }
}
