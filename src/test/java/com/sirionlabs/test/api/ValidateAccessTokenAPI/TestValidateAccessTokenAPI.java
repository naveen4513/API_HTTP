package com.sirionlabs.test.api.ValidateAccessTokenAPI;
import com.sirionlabs.api.ValidateAccessToken.ValidateAccessToken;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.ValidateAccessToken.ValidateAccessTokenDto;
import com.sirionlabs.dto.ValidateAccessToken.ValidateAccessTokenDto;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
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
public class TestValidateAccessTokenAPI {
    private final static Logger logger = LoggerFactory.getLogger(TestValidateAccessTokenAPI.class);
    private String testingType;
   private String dataFilePath = "src/test/resources/TestConfig/APITestData/ValidateAccessToken";
   private String dataFileName = "ValidateAccessTokenAPIData.json";
    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException
    {
        List<Object[]> allTestData = new ArrayList<>();
        List<ValidateAccessTokenDto> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
        JSONArray jsonArr = new JSONArray(allJsonData);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    ValidateAccessTokenDto dtoObject = getValidateAccessTokenDtoObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (ValidateAccessTokenDto dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private ValidateAccessTokenDto getValidateAccessTokenDtoObjectFromJson(JSONObject jsonObj) {
        ValidateAccessTokenDto dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String header = jsonObj.getString("header");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("expectedResponseMessage");

            dtoObject = new ValidateAccessTokenDto(testCaseId, description, header, expectedStatusCode, expectedResponseMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting BulkUpdate User DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(ValidateAccessTokenDto dtoObject)  {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            String expectedErrorMessage=dtoObject.getExpectedResponseMessage();
            if (expectedErrorMessage.isEmpty()||expectedErrorMessage.equalsIgnoreCase(""))
            {
                String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
                JSONArray jsonArray=new JSONArray(allJsonData);
                boolean flag=false;
                for (int i=0; i<jsonArray.length();i++)
                {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);

                    if (jsonObj.getString("testCaseId").trim().equalsIgnoreCase(dtoObject.getTestCaseId())) {
                                 flag=true;
                                jsonObj.put("header",Check.getAuthorization());
                                dtoObject.setHeader(Check.getAuthorization());
                    }
                    jsonArray.put(i,jsonObj);
                    if (flag){break;}
                }
                FileWriter fileWriter=new FileWriter(dataFilePath+"/"+dataFileName);
                 fileWriter.write(jsonArray.toString());
                 fileWriter.flush();
            }
            HashMap<String,String> headers=new HashMap<>();
            headers.put("Authorization",dtoObject.getHeader());
            APIResponse apiResponse = ValidateAccessToken.getResponse(headers);
            int actualStatusCode = apiResponse.getResponseCode();
            int expectedStatusCode = Integer.parseInt(dtoObject.getExpectedStatusCode());
            csAssert.assertTrue(actualStatusCode == expectedStatusCode, "Actual Status Code {" + actualStatusCode + "} and Expected Status{" + expectedStatusCode + "} are Different");
            if (dtoObject.getExpectedResponseMessage().isEmpty() & dtoObject.getExpectedResponseMessage().equalsIgnoreCase("")) {
                JSONObject jsonObject = new JSONObject(apiResponse.getResponseBody());
                int userId = jsonObject.getInt("userId");
                List<List<String>> selectedUserId=new PostgreSQLJDBC().doSelect("select  id from app_user where login_id='"+Check.lastLoggedInUserName+"' and client_id = "+ new AdminHelper().getClientId() +" and active= true");
                     csAssert.assertTrue(selectedUserId.get(0).get(0).equalsIgnoreCase(String.valueOf(userId)),"userId Different");
            } else {
                JSONObject jsonObject = new JSONObject(apiResponse.getResponseBody());
                String actualErrorMessage=jsonObject.getString("errorMessage");
                csAssert.assertTrue(actualErrorMessage.equalsIgnoreCase(expectedErrorMessage),"actual error message "+actualErrorMessage+"and expected error message"+expectedErrorMessage+" are different");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }
}
