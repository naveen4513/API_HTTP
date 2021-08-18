package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.clientAdmin.specialEntityField.*;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.SpecialEntityFieldPropertyDbHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestWOR340 {

    private final static Logger logger = LoggerFactory.getLogger(TestWOR340.class);

    private final int dataSetSize = 5;
    private int newlyCreatedFieldId;

    @BeforeClass
    public void beforeClass() {
        new AdminHelper().loginWithClientAdminUser();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            //1020 is dummy record id in special_entity_field_property_mapping
            String query = "delete from special_entity_field_property_mapping where id !=1020";
            sqlObj.deleteDBEntry(query);
        } catch (Exception e) {
            logger.error("Exception while Deleting Special Entity Field Data from DB having Id "+ e.getMessage());
        }
        finally {
            sqlObj.closeConnection();
        }
    }

    @AfterClass
    public void afterClass() {
        new Check().hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @DataProvider
    public Object[][] dataProvider() throws Exception {
        List<Object[]> allTestData = new ArrayList<>();

        logger.info("Hitting CreateForm API.");
        String createFormResponse = SpecialEntityFieldCreateForm.getCreateFormResponse().getResponseBody();

        List<Integer> allPrimaryEntityIds = SpecialEntityFieldCreateForm.getAllPrimaryEntityTypeIdsFromResponse(createFormResponse);
        List<Integer> allSecondaryEntityIds = SpecialEntityFieldCreateForm.getAllSecondaryEntityTypeIdsFromResponse(createFormResponse);

        if (allPrimaryEntityIds == null || allSecondaryEntityIds == null) {
            throw new Exception("Couldn't get Primary and Secondary Entity Options.");
        }

        //Randomly selecting Primary Entities.
        int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allPrimaryEntityIds.size() - 1, dataSetSize);

        //Randomly selecting Secondary Entity.
        int randomSecondaryEntity = RandomNumbers.getRandomNumberWithinRangeIndex(0, allSecondaryEntityIds.size() - 1);
        int secondaryEntityTypeId = allSecondaryEntityIds.get(randomSecondaryEntity);
        String secondaryEntityName = ConfigureConstantFields.getEntityNameById(secondaryEntityTypeId);

        logger.info("Hitting SecondaryEntity API for Entity {}", secondaryEntityName);
        String secondaryEntityResponse = SpecialEntityFieldSecondaryEntity.getCreateFormResponse(secondaryEntityTypeId).getResponseBody();
        List<Map<String, String>> allSecondaryEntityFields = SpecialEntityFieldSecondaryEntity.getAllSecondaryEntityFieldsMap(secondaryEntityResponse);

        for (int i = 0; i < dataSetSize; i++) {
            int primaryEntityTypeId = allPrimaryEntityIds.get(randomNumbers[i]);

            if (primaryEntityTypeId == secondaryEntityTypeId) {
                continue;
            }

            String primaryEntityName = ConfigureConstantFields.getEntityNameById(primaryEntityTypeId);

            //Randomly Select Primary Entity Custom Field.
            logger.info("Hitting PrimaryEntity API for Entity {}", primaryEntityName);
            String primaryEntityResponse = SpecialEntityFieldPrimaryEntity.getCreateFormResponse(primaryEntityTypeId).getResponseBody();
            List<Map<String, String>> allPrimaryEntityCustomFields = SpecialEntityFieldPrimaryEntity.getAllPrimaryEntityCustomFieldsMap(primaryEntityResponse);

            if (allPrimaryEntityCustomFields.size() == 0) {
                logger.warn("No Custom Field available for Primary Entity: {}", primaryEntityName);
                continue;
            }

            int randomPrimaryEntityFieldId = RandomNumbers.getRandomNumberWithinRangeIndex(0, allPrimaryEntityCustomFields.size() - 1);

            String primaryEntityCustomFieldId = allPrimaryEntityCustomFields.get(randomPrimaryEntityFieldId).get("id");
            String primaryEntityCustomFieldName = allPrimaryEntityCustomFields.get(randomPrimaryEntityFieldId).get("name");

            //Randomly Select Secondary Entity Custom Field.
            if (allSecondaryEntityFields.size() == 0) {
                logger.warn("No Field available for Secondary Entity");
            }

            int[] randomNumbersForSecondaryEntityFields = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allSecondaryEntityFields.size() - 1, 2);

            Map<String, String> secondaryEntityFieldsMap = new HashMap<>();
            for (int randomNumber : randomNumbersForSecondaryEntityFields) {
                secondaryEntityFieldsMap.put(allSecondaryEntityFields.get(randomNumber).get("id"), allSecondaryEntityFields.get(randomNumber).get("name"));
            }

            allTestData.add(new Object[]{primaryEntityTypeId, secondaryEntityTypeId, primaryEntityName, secondaryEntityName, primaryEntityCustomFieldId,
                    primaryEntityCustomFieldName, secondaryEntityFieldsMap});
        }

        logger.info("Total Flows to Test : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProvider")
    public void testWOR340EndToEnd(int primaryEntityTypeId, int secondaryEntityTypeId, String primaryEntityName, String secondaryEntityName,
                                   String primaryEntityCustomFieldId, String primaryEntityCustomFieldName, Map<String, String> secondaryEntityFields) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Special Entity Field for Primary Entity {}, Secondary Entity {}, Primary Entity Custom Field {} and Secondary Entity Fields {}",
                    primaryEntityName, secondaryEntityName, primaryEntityCustomFieldName, secondaryEntityFields);

            logger.info("Creating Special Entity Field.");
            String payloadForCreate = SpecialEntityFieldCreate.getPayload(primaryEntityCustomFieldName, primaryEntityCustomFieldId, primaryEntityTypeId,
                    secondaryEntityTypeId, secondaryEntityFields, true);

            APIResponse createResponse = SpecialEntityFieldCreate.getCreateResponse(payloadForCreate);
            int createResponseCode = createResponse.getResponseCode();

            csAssert.assertTrue(createResponseCode == 200, "Create API Response Code failed. Expected Code: 200 and Actual Code: " + createResponseCode);

            String responseBody = createResponse.getResponseBody();
            if (ParseJsonResponse.validJsonResponse(responseBody)) {
                 newlyCreatedFieldId = new JSONObject(responseBody).getJSONObject("header").getJSONObject("response").getInt("entityId");

                logger.info("Special Entity Field created with Id: " + newlyCreatedFieldId);

                //Validate Listing
                validateListing(newlyCreatedFieldId, primaryEntityName, secondaryEntityName, primaryEntityCustomFieldName, secondaryEntityFields, true, csAssert);

                //Validate Audit Log
                validateAuditLog(newlyCreatedFieldId, "saved", csAssert);

                //Validate Edit
                validateEdit(newlyCreatedFieldId, primaryEntityTypeId, secondaryEntityTypeId, primaryEntityName, secondaryEntityName, primaryEntityCustomFieldName,
                        primaryEntityCustomFieldId, secondaryEntityFields, csAssert);

                //Delete Newly Created Special Entity Field.
                logger.info("Deleting Newly Created Special Entity Field having Id: " + newlyCreatedFieldId);

            } else {
                csAssert.assertTrue(false, "Create API Response Code is an Invalid JSON.");
            }
        } catch (SkipException e) {
            logger.warn("Skipping Test Case: " + e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Special Entity Field for Primary Entity " + primaryEntityName +
                    " and Secondary Entity " + secondaryEntityName + ". " + e.getMessage());
        }
        finally {
            if (newlyCreatedFieldId!=0)
                SpecialEntityFieldPropertyDbHelper.deleteFieldDataInDb(newlyCreatedFieldId);
        }

        csAssert.assertAll();
    }

    private void validateListing(int fieldId, String primaryEntityName, String secondaryEntityName, String primaryEntityCustomFieldName,
                                 Map<String, String> allSecondaryEntityCustomFields, boolean active, CustomAssert csAssert) {
        try {
            logger.info("Validating Listing for Special Field Id: {} of Primary Entity {} and Secondary Entity {}", fieldId, primaryEntityName, secondaryEntityName);

            ListRendererListData listDataObj = new ListRendererListData();
            listDataObj.hitListRendererListData(490, true, ListDataHelper.getPayloadForListData(332, 1, 0), null);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject dataObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);

                //Validate Id
                String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                csAssert.assertTrue(dataObj.getJSONObject(idColumn).getString("value").contains(String.valueOf(fieldId)), "Newly Created Field Id: " +
                        fieldId + " not found in ListData API Response.");

                //Validate Primary Entity Custom Field
                String columnName = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "primaryentitycustomfield");
                String actualValue = dataObj.getJSONObject(columnName).getString("value");
                String expectedPrimaryEntityCustomFieldName = Jsoup.clean(primaryEntityCustomFieldName,
                        Whitelist.none()).replace("&nbsp;", "").replace("&amp;","&");
                csAssert.assertTrue(actualValue.equalsIgnoreCase(expectedPrimaryEntityCustomFieldName), "Primary Entity Custom Field Validation failed. Expected Value: " +
                        expectedPrimaryEntityCustomFieldName + " and Actual Value: " + actualValue);

                //Validate Active Value
                columnName = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "active");
                actualValue = dataObj.getJSONObject(columnName).getString("value");
                csAssert.assertTrue(actualValue.equalsIgnoreCase(String.valueOf(active)), "Active Value failed. Expected Value: " + active + " and Actual Value: " +
                        actualValue);

                //Validate Secondary Entity Fields
                columnName = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "secondaryentityfields");
                actualValue = dataObj.getJSONObject(columnName).getString("value");
                List<String> actualEntityFields = new ArrayList<>(Arrays.asList(actualValue.split(",")));

                if (actualEntityFields.size() == allSecondaryEntityCustomFields.size()) {
                    for (Map.Entry<String, String> expectedEntityFieldMap : allSecondaryEntityCustomFields.entrySet()) {
                        String expectedEntityFieldName = expectedEntityFieldMap.getValue();
                        csAssert.assertTrue(actualEntityFields.contains(expectedEntityFieldName), "Secondary Entity Field " + expectedEntityFieldName +
                                " not found in ListData API Response.");
                    }
                } else {
                    csAssert.assertTrue(false, "Secondary Entity Fields Validation failed. No of Expected Fields: " +
                            allSecondaryEntityCustomFields.size() + " and No of Actual Fields: " + actualEntityFields.size());
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for ListId 332 is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing for Field Id: " + fieldId + " of Primary Entity " +
                    primaryEntityName + " and Secondary Entity " + secondaryEntityName + ". " + e.getMessage());
        }
    }

    private void validateAuditLog(int fieldId, String expectedActionName, CustomAssert csAssert) {
        try {
            logger.info("Validating Special Entity Field Audit Logs.");
            ListRendererTabListData tabListDataObj = new ListRendererTabListData();

            String payload = "{\"filterMap\":{\"entityTypeId\":332,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            tabListDataObj.hitListRendererTabListData(61, 332, fieldId, payload, true);
            String auditLogTabResponse = tabListDataObj.getTabListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
                JSONArray jsonArr = new JSONObject(auditLogTabResponse).getJSONArray("data");
                String actionColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "action_name");
                String actionValue = jsonArr.getJSONObject(0).getJSONObject(actionColumnId).getString("value");

                csAssert.assertTrue(actionValue.equalsIgnoreCase(expectedActionName), "Expected Action Name: " + expectedActionName +
                        " and Actual Action Name: " + actionValue);

                String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "history");
                String historyValue = jsonArr.getJSONObject(0).getJSONObject(historyColumnId).getString("value");
                Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                FieldHistory historyObj = new FieldHistory();
                String fieldHistoryResponse = historyObj.hitFieldHistory(historyId, 332, true);

                if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {
                    JSONObject jsonObj = new JSONObject(fieldHistoryResponse);
                    jsonArr = jsonObj.getJSONArray("value");

                    if (!jsonObj.isNull("errorMessage")) {
                        csAssert.assertTrue(false, "Error in Field History API Response: " + jsonObj.getString("errorMessage"));
                    } else {
                        if (expectedActionName.equalsIgnoreCase("saved")) {
                            csAssert.assertTrue(jsonArr.length() == 0, "Expected History Value length: 0 and Actual Value length: " + jsonArr.length());
                        } else {
                            csAssert.assertTrue(jsonArr.length() > 0, "History Value Array is empty.");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Field History API Response for Field Id: " + fieldId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Audit Log of Special Entity Field Id: " + fieldId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log for Special Entity Field Id: " + fieldId + ". " + e.getMessage());
        }
    }

    private void validateEdit(int fieldId, int primaryEntityTypeId, int secondaryEntityTypeId, String primaryEntityName, String secondaryEntityName,
                              String primaryEntityCustomFieldName, String primaryEntityCustomFieldId,
                              Map<String, String> secondaryEntityOriginalFields, CustomAssert csAssert) {
        try {
            logger.info("Validating Special Entity Field Edit.");
            validateEditGetAPI(fieldId, csAssert);

            String editPayload = SpecialEntityFieldEditPost.getPayload(fieldId, primaryEntityCustomFieldName, primaryEntityCustomFieldId, primaryEntityTypeId,
                    secondaryEntityTypeId, secondaryEntityOriginalFields, false);
            String editResponse = SpecialEntityFieldEditPost.getUpdateResponse(editPayload).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(editResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(editResponse);

                if (status.equalsIgnoreCase("success")) {
                    validateListing(fieldId, primaryEntityName, secondaryEntityName, primaryEntityCustomFieldName, secondaryEntityOriginalFields, false, csAssert);

                    validateAuditLog(fieldId, "updated", csAssert);
                } else {
                    csAssert.assertTrue(false, "Special Entity Field Edit failed due to " + status);
                }
            } else {
                csAssert.assertTrue(false, "Edit Post API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit of Special Entity Field Id: " + fieldId + ". " + e.getMessage());
        }
    }

    private void validateEditGetAPI(int fieldId, CustomAssert csAssert) {
        try {
            logger.info("Validating Edit Get API.");
            logger.info("Hitting Edit Get API for Special Entity Field Id: {}", fieldId);
            APIResponse editGetResponse = SpecialEntityFieldEditGet.getEditGetResponse(fieldId);

            int responseCode = editGetResponse.getResponseCode();
            csAssert.assertTrue(responseCode == 200, "Edit Get Response Code Validation failed. Expected Code: 200 and Actual Code: " + responseCode);

            String responseBody = editGetResponse.getResponseBody();
            String selectedColumns = "field_id, url_entity_type_id, entity_field_ids, primary_entity_type, is_active";

            List<String> fieldDataInDb = SpecialEntityFieldPropertyDbHelper.getSpecialEntityFieldDataFromDB(selectedColumns, fieldId);

            JSONObject jsonObj = new JSONObject(responseBody).getJSONObject("body").getJSONObject("data");
            String messageInfo = "Edit Get API Data Validation failed. ";

            //Validate PrimaryEntity Custom Field Id
            int actualPrimaryEntityCustomFieldId = jsonObj.getJSONObject("primaryEntityCustomField").getJSONObject("values").getInt("id");
            int primaryEntityCustomFieldIdInDb = Integer.parseInt(fieldDataInDb.get(0));

            csAssert.assertTrue(actualPrimaryEntityCustomFieldId == primaryEntityCustomFieldIdInDb, messageInfo + "PrimaryEntityCustomField Id in DB: " +
                    primaryEntityCustomFieldIdInDb + " and CustomField Id in API Response: " + actualPrimaryEntityCustomFieldId);

            //Validate Primary Entity Type Id
            int actualPrimaryEntityTypeId = jsonObj.getJSONObject("primaryEntity").getJSONObject("values").getInt("id");
            int primaryEntityTypeIdInDb = Integer.parseInt(fieldDataInDb.get(3));

            csAssert.assertTrue(actualPrimaryEntityTypeId == primaryEntityTypeIdInDb, messageInfo + "Primary Entity Type Id in DB: " +
                    primaryEntityTypeIdInDb + " and Primary Entity Type Id in API Response: " + actualPrimaryEntityTypeId);

            //Validate Secondary Entity Type Id
            int actualSecondaryEntityTypeId = jsonObj.getJSONObject("secondaryEntity").getJSONObject("values").getInt("id");
            int secondaryEntityTypeIdInDb = Integer.parseInt(fieldDataInDb.get(1));

            csAssert.assertTrue(actualSecondaryEntityTypeId == secondaryEntityTypeIdInDb, messageInfo + "Secondary EntityTypeId in DB: " +
                    secondaryEntityTypeIdInDb + " and Secondary EntityTypeId in API Response: " + actualSecondaryEntityTypeId);

            //Validate Secondary Entity Fields
            List<Integer> actualSecondaryEntityFieldIds = new ArrayList<>();
            JSONArray jsonArr = jsonObj.getJSONObject("secondaryEntityFields").getJSONArray("values");

            for (int i = 0; i < jsonArr.length(); i++) {
                actualSecondaryEntityFieldIds.add(jsonArr.getJSONObject(i).getInt("id"));
            }

            List<Integer> secondaryEntityFieldIdsInDb = new ArrayList<>();
            String[] secondaryEntityFieldsInDbArr = fieldDataInDb.get(2).replace("{", "").replace("}", "").split(",");
            for (String id : secondaryEntityFieldsInDbArr) {
                secondaryEntityFieldIdsInDb.add(Integer.parseInt(id.trim()));
            }

            if (actualSecondaryEntityFieldIds.size() == secondaryEntityFieldIdsInDb.size()) {
                for (Integer fieldIdInDb : secondaryEntityFieldIdsInDb) {
                    csAssert.assertTrue(actualSecondaryEntityFieldIds.contains(fieldIdInDb), messageInfo + "Secondary Entity Field Id: " +
                            fieldIdInDb + " present in DB but not present in API Response.");
                }
            } else {
                csAssert.assertTrue(false, messageInfo + "Secondary Entity Fields Mismatch. No of Fields in DB: " +
                        secondaryEntityFieldIdsInDb.size() + " and No of Fields in API Response: " + actualSecondaryEntityFieldIds.size());
            }

            //Validate Active Value
            String actualActiveValue = (jsonObj.getJSONObject("active").getBoolean("values")) ? "t" : "f";
            String activeValueInDb = fieldDataInDb.get(4);

            csAssert.assertTrue(actualActiveValue.equalsIgnoreCase(activeValueInDb), messageInfo + "Active Value in DB: " + activeValueInDb +
                    " and Active Value in API Response: " + actualActiveValue);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit Get API. " + e.getMessage());
        }
    }
}