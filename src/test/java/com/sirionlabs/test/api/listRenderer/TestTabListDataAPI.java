package com.sirionlabs.test.api.listRenderer;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.dto.listRenderer.TabListDataDTO;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.dbHelper.AuditLogsDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTabListDataAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestListDataAPI.class);

    private String testingType;
    private ListRendererTabListData tabListDataObj = new ListRendererTabListData();

    private String lastLoggedInUserName = Check.lastLoggedInUserName;
    private String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;

        new AdminHelper().loginWithClientAdminUser();
    }

    @AfterClass
    public void afterClass() {
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
    }

    @DataProvider
    public Object[][] dataProviderForAuditLogJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/ListRenderer";
        String dataFileName = "TabListDataAuditLogAPIData.json";

        List<TabListDataDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    TabListDataDTO dtoObject = getTabListDataDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (TabListDataDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private TabListDataDTO getTabListDataDTOObjectFromJson(JSONObject jsonObj) {
        TabListDataDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int tabId = jsonObj.getInt("tabId");
            int entityTypeId = jsonObj.getInt("entityTypeId");
            int recordId = jsonObj.getInt("recordId");
            int clientId = jsonObj.getInt("clientId");
            boolean isAdmin = jsonObj.getBoolean("isAdmin");

            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new TabListDataDTO(testCaseId, description, tabId, entityTypeId, recordId, clientId, isAdmin, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowButtonsShow DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderForAuditLogJson")
    public void testAuditLogData(TabListDataDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String payload = tabListDataObj.getPayload(dtoObject.getEntityTypeId(), dtoObject.getOffset(), dtoObject.getSize(), dtoObject.getOrderByColumnName(),
                    dtoObject.getOrderDirection(), dtoObject.getFilterJson());

            tabListDataObj.hitListRendererTabListData(dtoObject.getTabId(), dtoObject.getEntityTypeId(), dtoObject.getRecordId(), payload, dtoObject.getAdmin());
            String tabListDataResponse = tabListDataObj.getTabListDataJsonStr();

            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();
            JSONObject jsonObj = new JSONObject(tabListDataResponse);

            if (expectedErrorMessage == null) {
                if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                    //Validate Audit Log Data.
                    String selectedColumns = "id, action_id, comment, requested_by, completed_by";

                    List<List<String>> allExpectedAuditLogData = AuditLogsDbHelper.getAllOtherAuditLogsForEntityIdAndEntityTypeId(selectedColumns,
                            dtoObject.getEntityTypeId(), dtoObject.getRecordId(), dtoObject.getClientId(), dtoObject.getOrderByColumnName(), dtoObject.getOrderDirection());

                    if (allExpectedAuditLogData == null || allExpectedAuditLogData.isEmpty()) {
                        throw new SkipException("Couldn't Get All Audit Logs Data from DB.");
                    }

                    //Validate Filtered Count in API Response.
                    int filteredCount = jsonObj.getInt("filteredCount");
                    csAssert.assertTrue(filteredCount == allExpectedAuditLogData.size(), "Expected Audit Logs Count: " +
                            allExpectedAuditLogData.size() + " and Actual Filtered Count in API Response: " + filteredCount);

                    //Validate correct no of records returned in Response.
                    JSONArray jsonArr = jsonObj.getJSONArray("data");
                    int expectedNoOfRecords = Math.min((filteredCount - dtoObject.getOffset()), dtoObject.getSize());

                    if (expectedNoOfRecords == jsonArr.length()) {
                        //Validate Data
                        String actionNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "action_name");
                        String requestedByColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "requested_by");
                        String completedByColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "completed_by");
                        String commentColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "comment");
                        String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "history");

                        Map<Integer, String> userDetailsMap = new HashMap<>();

                        for (int i = 0; i < jsonArr.length(); i++) {
                            logger.info("Audit Log #{} Validation.", (i + 1));
                            List<String> auditLogDataInDb = allExpectedAuditLogData.get(i);

                            //Validate Action Name
                            String actualActionName = jsonArr.getJSONObject(i).getJSONObject(actionNameColumnId).getString("value");
                            String expectedActionName = TabListDataHelper.getExpectedAuditLogActionName(auditLogDataInDb.get(1), dtoObject.getEntityTypeId());

                            csAssert.assertTrue(actualActionName.equalsIgnoreCase(expectedActionName), "Expected Action Name: " +
                                    expectedActionName + " and Actual Action Name: " + actualActionName);

                            //Validate Requested By
                            String actualRequestedBy = jsonArr.getJSONObject(i).getJSONObject(requestedByColumnId).getString("value");
                            int userIdInDb = Integer.parseInt(auditLogDataInDb.get(3));
                            String expectedRequestedBy;

                            if (userDetailsMap.containsKey(userIdInDb)) {
                                expectedRequestedBy = userDetailsMap.get(userIdInDb);
                            } else {
                                List<String> userData = AppUserDbHelper.getUserDataFromUserId("first_name, last_name", userIdInDb);
                                String userName = userData.get(0) + " " + userData.get(1);

                                userDetailsMap.put(userIdInDb, userName);
                                expectedRequestedBy = userName;
                            }

                            csAssert.assertTrue(actualRequestedBy.equalsIgnoreCase(expectedRequestedBy), "Expected Requested By Value: " +
                                    expectedRequestedBy + " and Actual Requested By Value: " + actualRequestedBy);

                            //Validate Completed By
                            String actualCompletedBy = jsonArr.getJSONObject(i).getJSONObject(completedByColumnId).getString("value");
                            userIdInDb = Integer.parseInt(auditLogDataInDb.get(4));
                            String expectedCompletedBy;

                            if (userDetailsMap.containsKey(userIdInDb)) {
                                expectedCompletedBy = userDetailsMap.get(userIdInDb);
                            } else {
                                List<String> userData = AppUserDbHelper.getUserDataFromUserId("first_name, last_name", userIdInDb);
                                String userName = userData.get(0) + " " + userData.get(1);

                                userDetailsMap.put(userIdInDb, userName);
                                expectedCompletedBy = userName;
                            }

                            csAssert.assertTrue(actualCompletedBy.equalsIgnoreCase(expectedCompletedBy), "Expected Completed By Value: " +
                                    expectedCompletedBy + " and Actual Completed By Value: " + actualCompletedBy);

                            //Validate Comment
                            String actualCommentValue = jsonArr.getJSONObject(i).getJSONObject(commentColumnId).getString("value");
                            String expectedCommentValue = (auditLogDataInDb.get(2) == null) ? "No" : "Yes";

                            csAssert.assertTrue(actualCommentValue.equalsIgnoreCase(expectedCommentValue), "Expected Comment Value: " +
                                    expectedCommentValue + " and Actual Comment Value: " + actualCommentValue);

                            //Validate History
                            String actualHistoryValue = jsonArr.getJSONObject(i).getJSONObject(historyColumnId).getString("value");
                            String expectedHistoryValue = auditLogDataInDb.get(0) + "/" + dtoObject.getEntityTypeId();

                            csAssert.assertTrue(actualHistoryValue.contains(expectedHistoryValue), "Expected History Value: " +
                                    expectedHistoryValue + " and Actual History Value: " + actualHistoryValue);
                        }
                    } else {
                        csAssert.assertTrue(false, "Expected No of Audit Logs: " + expectedNoOfRecords + " and Actual No of Logs: " + jsonArr.length());
                    }

                } else {
                    csAssert.assertTrue(false, "TabListData API Response for Audit Log Tab Id " + dtoObject.getTabId() + " of EntityTypeId " +
                            dtoObject.getEntityTypeId() + " and Record Id " + dtoObject.getRecordId() + " is an Invalid JSON.");
                }
            } else {
                //Validate Error Message
                String errorMessage = jsonObj.getString("errorMessage");
                csAssert.assertTrue(errorMessage.toLowerCase().contains(expectedErrorMessage.toLowerCase()), "Expected Error Message: [" + expectedErrorMessage +
                        "] and Actual Error Message: [" + errorMessage + "]");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }
}