package com.sirionlabs.test.api.bulkIntegration;

import com.sirionlabs.api.bulkIntegration.BulkUpdateUser;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListDataAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.bulkIntegration.BulkUpdateUserDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.test.api.listRenderer.TestListDataAPI;
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

import java.io.IOException;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestBulkUpdateUserAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestListDataAPI.class);

    private String testingType;
    private int clientId;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        clientId = new AdminHelper().getClientId();
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/BulkIntegration";
        String dataFileName = "bulkUpdateUserAPIData.json";

        List<BulkUpdateUserDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    BulkUpdateUserDTO dtoObject = getBulkUpdateUserDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (BulkUpdateUserDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private BulkUpdateUserDTO getBulkUpdateUserDTOObjectFromJson(JSONObject jsonObj) {
        BulkUpdateUserDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String payload = jsonObj.getJSONArray("payload").toString();
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("expectedResponseMessage");

            dtoObject = new BulkUpdateUserDTO(testCaseId, description, payload, expectedStatusCode, expectedResponseMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting BulkUpdate User DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(BulkUpdateUserDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        Map<String, List<String>> allUserDataAccordingToLoginId = new HashMap<>();
        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
//            APIResponse bulkUpdateUserResponse;
            JSONArray payloadJsonArr = new JSONArray(dtoObject.getPayload());
            List<String> loginIdForUserUpdate = new ArrayList<>();
            for (int i = 0; i < payloadJsonArr.length(); i++) {
                JSONObject jsonObj = new JSONObject(payloadJsonArr.getJSONObject(i).toString());
                loginIdForUserUpdate.add(jsonObj.getString("loginId"));
            }
            //all user meta data according to login id before bulk update user api hit
            allUserDataAccordingToLoginId = getSelectDataAccordingToLoginId(loginIdForUserUpdate);
            //hit bulk update user api and get api response
            APIResponse apiResponse = BulkUpdateUser.getResponse(dtoObject.getPayload());
            // actual api response
            int actualApiResponseCode = apiResponse.getResponseCode();
            //expected api response
            int expectedApiResponseCode = Integer.parseInt(dtoObject.getExpectedStatusCode());
            //check actual and expected api response
            csAssert.assertTrue(expectedApiResponseCode == actualApiResponseCode, "Actual API Response {" + actualApiResponseCode + "} and Expected API Response {" + expectedApiResponseCode + "}  Are Different");
            // check the result update in database according to change the user information
            // passing three parameter all user information after hit bulk update user api ,payload data,csAssert
            if (dtoObject.getExpectedResponseMessage().isEmpty()) {
                checkUpdateDataPresentInDb(getSelectDataAccordingToLoginId(loginIdForUserUpdate), dtoObject.getPayload(), csAssert);
            } else {
                String loginIdDoesNotExistInDataBase = new JSONObject(apiResponse.getResponseBody()).names().getString(0);
                String actualErrorMessage = new JSONObject(apiResponse.getResponseBody()).getJSONArray(loginIdDoesNotExistInDataBase).getJSONObject(0).get("errorMessage").toString();
                String expectedResponseMessage = dtoObject.getExpectedResponseMessage();
                csAssert.assertTrue(expectedResponseMessage.trim().toLowerCase().equalsIgnoreCase(actualErrorMessage.trim().toLowerCase()), "Actual response message {" + actualErrorMessage + "} and expected response  message are different {" + expectedResponseMessage + "}");
                if (actualErrorMessage.equalsIgnoreCase("LoginId not found") || actualErrorMessage.equalsIgnoreCase("LoginId cannot be blank or more than 512 characters")) {
                    loginIdForUserUpdate.remove(loginIdDoesNotExistInDataBase);
                    checkDataNotUpdateInDbForNegativeTesting(getSelectDataAccordingToLoginId(loginIdForUserUpdate), dtoObject.getPayload(), loginIdDoesNotExistInDataBase, csAssert);
                } else if (actualErrorMessage.equalsIgnoreCase("First name cannot be blank or more than 512 characters") || actualErrorMessage.equalsIgnoreCase("Last name cannot be blank or more than 512 characters")) {
                    checkDataNotUpdateInDbForNegativeTesting(getSelectDataAccordingToLoginId(loginIdForUserUpdate), dtoObject.getPayload(), null, csAssert);
                }

            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        } finally {
            updateDataAfterTestCaseIntoDb(allUserDataAccordingToLoginId);
        }

        csAssert.assertAll();
    }

    public Map<String, List<String>> getSelectDataAccordingToLoginId(List<String> loginIdForUserBeforeUpdate) throws Exception {
        Map<String, List<String>> allUserDataAccordingToLoginId = new HashMap<>();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        for (String loginId : loginIdForUserBeforeUpdate) {
            postgreSQLJDBC.doSelect("select first_name,last_name,contact_no,dynamic_metadata from app_user where login_id='" + loginId + "' and client_id=" + clientId).forEach(selectedData -> {
                List<String> selectedDataAccordingToLoginID = new ArrayList<>();
                selectedDataAccordingToLoginID.add(selectedData.get(0));
                selectedDataAccordingToLoginID.add(selectedData.get(1));
                selectedDataAccordingToLoginID.add(selectedData.get(2));
                selectedDataAccordingToLoginID.add(selectedData.get(3));
                allUserDataAccordingToLoginId.put(loginId, selectedDataAccordingToLoginID);
            });
        }
        return allUserDataAccordingToLoginId;
    }

    private void updateDataAfterTestCaseIntoDb(Map<String, List<String>> allUserDataAccordingToLoginId) {
        try {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            for (Map.Entry<String, List<String>> loginIDAndMetaData : allUserDataAccordingToLoginId.entrySet()) {
                postgreSQLJDBC.updateDBEntry("UPDATE app_user SET first_name='" + loginIDAndMetaData.getValue().get(0) + "',last_name='" + loginIDAndMetaData.getValue().get(1) + "',contact_no='" + loginIDAndMetaData.getValue().get(2) + "',dynamic_metadata='" + loginIDAndMetaData.getValue().get(3) + "' where login_id='" + loginIDAndMetaData.getKey() + "' and client_id=" + clientId);
            }
        } catch (Exception e) {
            throw new SkipException(e.getMessage());
        }

    }

    private void checkDataNotUpdateInDbForNegativeTesting(Map<String, List<String>> allSelectedMetaDataAccordingToLoginId, String payloadUpdate, String loginIdDoesNotExistInDataBase, CustomAssert customAssert) {
        JSONArray jsonArray = new JSONArray(payloadUpdate);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObj = new JSONObject(jsonArray.getJSONObject(i).toString());
            if (loginIdDoesNotExistInDataBase != null && jsonObj.getString("loginId").trim().toLowerCase().equalsIgnoreCase(loginIdDoesNotExistInDataBase.toLowerCase().trim())) {
                continue;
            }
            String loginId = jsonObj.getString("loginId");
            List<String> userMetaData = allSelectedMetaDataAccordingToLoginId.get(loginId);
            JSONArray jsonArray1 = jsonObj.names();
            if (jsonArray1.length() < 5) {
                List<String> payloadJsonObjectName = new ArrayList<>();
                for (int j = 0; j < jsonArray1.length(); j++) {
                    payloadJsonObjectName.add(jsonArray1.getString(j));
                }
                if (!payloadJsonObjectName.contains("firstName") || !payloadJsonObjectName.contains("lastName") || !payloadJsonObjectName.contains("contactNo")) {
                    continue;
                }
            }
            if (jsonObj.getString("firstName").equalsIgnoreCase(userMetaData.get(0))) {
                customAssert.assertTrue(false, "first name ");
            }
            if (jsonObj.getString("lastName").equalsIgnoreCase(userMetaData.get(1))) {
                customAssert.assertTrue(false, "last name");
            }
            if (jsonObj.getString("contactNo").equalsIgnoreCase(userMetaData.get(2))) {
                customAssert.assertTrue(false, "contact number");
            }
            if (jsonObj.getString("dynamicMetadataJson").equalsIgnoreCase(userMetaData.get(3))) {
                customAssert.assertTrue(false, "dynamicMetadata");
            }
            // String dynamicMetadataJson = jsonObj.getString("dynamicMetadataJson");
        }

    }

    private void checkUpdateDataPresentInDb(Map<String, List<String>> allSelectedMetaDataAccordingToLoginId, String payloadUpdate, CustomAssert customAssert) {
        JSONArray jsonArray = new JSONArray(payloadUpdate);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObj = new JSONObject(jsonArray.getJSONObject(i).toString());
            String loginId = jsonObj.getString("loginId");
            List<String> userMetaData = allSelectedMetaDataAccordingToLoginId.get(loginId);
            if (!jsonObj.getString("firstName").equalsIgnoreCase(userMetaData.get(0))) {
                customAssert.assertTrue(false, "first name  different ");
            }
            if (!jsonObj.getString("lastName").equalsIgnoreCase(userMetaData.get(1))) {
                customAssert.assertTrue(false, "last name  different ");
            }
            if (!jsonObj.getString("contactNo").equalsIgnoreCase(userMetaData.get(2))) {
                customAssert.assertTrue(false, "contact number  different ");
            }
            if (!jsonObj.getString("dynamicMetadataJson").equalsIgnoreCase(userMetaData.get(3))) {
                customAssert.assertTrue(false, "dynamicMetadata is different");
            }
        }
    }
}