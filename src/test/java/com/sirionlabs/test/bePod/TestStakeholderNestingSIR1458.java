package com.sirionlabs.test.bePod;

import com.sirionlabs.api.clientAdmin.nestedField.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestStakeholderNestingSIR1458 {

    private final static Logger logger = LoggerFactory.getLogger(TestStakeholderNestingSIR1458.class);

    private int clientId;

    @BeforeClass
    public void beforeClass() {
        clientId = new AdminHelper().getClientId();
    }

    @DataProvider
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();

        String nestedFieldsEntityTypesResponse = NestedFieldEntityTypes.getEntityTypesResponse().getResponseBody();
        List<Integer> allEntityTypeIds = NestedFieldEntityTypes.getAllEntityTypeIds(nestedFieldsEntityTypesResponse);

        if (allEntityTypeIds == null) {
            throw new NullPointerException();
        }

        int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allEntityTypeIds.size() - 1, 5);

        for (int randomNumber : randomNumbers) {
            int entityTypeId = allEntityTypeIds.get(randomNumber);

            if (entityTypeId == 72) {
                continue;
            }

            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
            allTestData.add(new Object[]{entityName, entityTypeId});
        }

        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProvider")
    public void testStakeholderNesting(String entityName, int entityTypeId) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Stakeholder Nesting for Entity {}", entityName);
            logger.info("Hitting NestedField FormData API for Entity {}", entityName);

            APIResponse formDataResponse = NestedFieldFormData.getFormDataV1Response(entityTypeId);
            int actualStatusCode = formDataResponse.getResponseCode();

            csAssert.assertTrue(actualStatusCode == 200, "FormData API, Expected Status Code: 200 and Actual Status Code: " + actualStatusCode);

            String formDataResponseBody = formDataResponse.getResponseBody();

            logger.info("Getting All Expected Fields from DB for Entity {}", entityName);
            List<Integer> allStakeholderFieldIdsInDb = getAllStakeholderFieldIdsFromDb(entityTypeId);

            if (allStakeholderFieldIdsInDb.isEmpty()) {
                throw new SkipException("Couldn't get All Stakeholder Field Ids from DB for Entity " + entityName);
            }

            Map<Integer, String> allChildFields = NestedFieldFormData.getAllChildFields(formDataResponseBody);
            if (allChildFields.isEmpty()) {
                throw new SkipException("No Child Field Present in NestedField FormData API Response for Entity " + entityName);
            }

            //Validate Stakeholder Fields in FormData API Response.
            validateAllStakeholderFieldsInFormDataResponse(entityName, allChildFields, allStakeholderFieldIdsInDb, csAssert);

            List<Map<String, String>> allMasterFields = NestedFieldFormData.getAllMasterFields(formDataResponseBody);
            if (allMasterFields.isEmpty()) {
                throw new SkipException("Couldn't find any master field in FormData API Response for Entity " + entityName);
            }

            //Validate Nested Field Creation
            validateNestedFieldCreation(entityName, entityTypeId, allChildFields, allMasterFields, csAssert);
        } catch (SkipException e) {
            logger.warn("Skipping Test. " + e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Stakeholder Nesting for Entity " + entityName + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private List<Integer> getAllStakeholderFieldIdsFromDb(int entityTypeId) throws SQLException {
        List<Integer> allStakeholderFieldIds = new ArrayList<>();

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "select id from entity_field where entity_type_id = " + entityTypeId + " and active = true and client_id = " + clientId +
                " and api_model = 'stakeHolders.values'";
        List<List<String>> allResults = sqlObj.doSelect(query);

        sqlObj.closeConnection();

        if (!allResults.isEmpty()) {
            for (List<String> result : allResults) {
                allStakeholderFieldIds.add(Integer.parseInt(result.get(0)));
            }
        }

        return allStakeholderFieldIds;
    }

    private void validateAllStakeholderFieldsInFormDataResponse(String entityName, Map<Integer, String> allChildFields, List<Integer> allStakeholderFields,
                                                                CustomAssert csAssert) {
        try {
            logger.info("Validating All Stakeholder Fields in FormData API Response for Entity {}", entityName);

            for (Integer fieldId : allStakeholderFields) {
                csAssert.assertTrue(allChildFields.containsKey(fieldId), "Stakeholder Field having Id " + fieldId +
                        " is not present in FormData API Response for Entity " + entityName);
            }

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating All Stakeholder Fields in FormData API Response for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateNestedFieldCreation(String entityName, int entityTypeId, Map<Integer, String> allChildFields, List<Map<String, String>> allMasterFields,
                                             CustomAssert csAssert) {
        try {
            int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allMasterFields.size() - 1);
            Map<String, String> masterFieldMap = allMasterFields.get(randomNumber);

            int masterFieldId = Integer.parseInt(masterFieldMap.get("id"));
            String masterFieldName = masterFieldMap.get("name");

            logger.info("Hitting MasterField Options API for Field Id {} of Entity {}", masterFieldId, entityName);
            String optionsResponse = NestedFieldMasterFieldOption.getMasterFieldOptionResponse(masterFieldId).getResponseBody();
            List<Map<String, String>> allOptions = NestedFieldMasterFieldOption.getAllOptionsOfField(optionsResponse);

            randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
            int masterFieldOptionId = Integer.parseInt(allOptions.get(randomNumber).get("id"));
            String masterFieldOptionName = allOptions.get(randomNumber).get("name");

            Map.Entry<Integer, String> childFieldMap = allChildFields.entrySet().iterator().next();
            int childFieldId = childFieldMap.getKey();
            String childFieldName = childFieldMap.getValue();

            String masterFieldType = masterFieldMap.get("type");

            String createPayload = NestedFieldCreate.getCreatePayload(masterFieldId, masterFieldType, childFieldId, masterFieldOptionId);
            logger.info("Hitting NestedField Create API for Entity {} using MasterField {}, MasterField Option {} and Child Field {}", entityName, masterFieldName,
                    masterFieldOptionName, childFieldName);

            int createResponseCode = NestedFieldCreate.getCreateResponseCode(createPayload);

            if (createResponseCode == 200) {
                //Check if Entry created in DB.
                String editablePropertyInDb = getEditablePropertyForNestedField(masterFieldId, childFieldId);

                if (editablePropertyInDb == null) {
                    csAssert.assertTrue(false, "Couldn't find data in DB for Nested Field of Entity " + entityName);
                    return;
                }

                //Validate Correct Data in Db.
                if (masterFieldType.equalsIgnoreCase("singleselect")) {
                    csAssert.assertTrue(editablePropertyInDb.contains("fieldVals\":[\"" + masterFieldOptionId + "\"]"), "MasterFieldOption Id " +
                            masterFieldOptionId + " not found in DB for Entity " + entityName);
                } else if (masterFieldType.equalsIgnoreCase("checkbox")) {
                    String masterFieldApiName = getFieldApiName(masterFieldId, entityTypeId);

                    if (masterFieldApiName == null) {
                        throw new SkipException("Couldn't get ApiName for Master Field " + masterFieldName + " of Entity " + entityName);
                    }

                    csAssert.assertTrue(editablePropertyInDb.contains(masterFieldApiName), "MasterField ApiName: " + masterFieldApiName +
                            " not found in DB for Entity " + entityName);
                }

                //Validate Show Data.
                validateShowData(entityName, masterFieldName, masterFieldId, masterFieldOptionName, masterFieldOptionId, childFieldName, childFieldId, csAssert);

                //Validate UpdateForm Data.
                validateUpdateFormData(entityName, masterFieldName, masterFieldId, masterFieldOptionName, masterFieldOptionId, childFieldName, childFieldId,
                        allChildFields, allOptions, csAssert);

                //Delete Field
                int deleteResponseCode = NestedFieldDelete.getDeleteResponseCode(masterFieldId);
                csAssert.assertTrue(deleteResponseCode == 200, "Delete failed for Master Field " + masterFieldName + " of Entity " + entityName);
            } else {
                csAssert.assertTrue(false, "Nested Field Creation failed for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Nested Field Creation for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private String getEditablePropertyForNestedField(int masterFieldId, int childFieldId) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "select editable_properties from request_field_mapping where nested_master_field_id = " + masterFieldId + " and field_id = " + childFieldId;
        List<List<String>> allResults = sqlObj.doSelect(query);

        sqlObj.closeConnection();

        if (!allResults.isEmpty()) {
            return allResults.get(0).get(0);
        }

        return null;
    }

    private void validateShowData(String entityName, String masterFieldName, int masterFieldId, String masterFieldOptionName, int masterFieldOptionId,
                                  String childFieldName, int childFieldId, CustomAssert csAssert) {
        try {
            logger.info("Hitting NestedField ShowData API for MasterField Id {} of Entity {}", masterFieldId, entityName);
            String showDataResponse = NestedFieldShowData.getShowDataResponse(masterFieldId).getResponseBody();

            JSONObject jsonObj = new JSONObject(showDataResponse);
            String errorMessageInfo = "ShowData API Validation for Entity " + entityName + ". ";

            //Validate Master Field Details
            JSONObject masterFieldJsonObj = jsonObj.getJSONObject("masterField");
            csAssert.assertTrue(masterFieldJsonObj.getInt("id") == masterFieldId, errorMessageInfo + "Expected Master Field Id " +
                    masterFieldId + " and Actual Master Field Id " + masterFieldJsonObj.getInt("id"));
            csAssert.assertTrue(masterFieldJsonObj.getString("name").equalsIgnoreCase(masterFieldName), errorMessageInfo + "Expected Master Field Name: " +
                    masterFieldName + " and Actual Master Field Name: " + masterFieldJsonObj.getString("name"));

            //Validate Master Field Option Details
            JSONArray jsonArr = jsonObj.getJSONArray("data");
            JSONObject optionJsonObj = jsonArr.getJSONObject(0).getJSONObject("option");

            csAssert.assertTrue(optionJsonObj.getInt("id") == masterFieldOptionId, errorMessageInfo + "Expected Master Field Option Id " +
                    masterFieldOptionId + " and Actual Master Field Option Id " + optionJsonObj.getInt("id"));
            csAssert.assertTrue(optionJsonObj.getString("name").equalsIgnoreCase(masterFieldOptionName), errorMessageInfo +
                    "Expected Master Field Option Name: " + masterFieldOptionName + " and Actual Master Field Option Name: " + optionJsonObj.getString("name"));

            //Validate Child Field Details
            jsonArr = jsonArr.getJSONObject(0).getJSONArray("childs");
            JSONObject childJsonObj = jsonArr.getJSONObject(0);

            csAssert.assertTrue(childJsonObj.getInt("id") == childFieldId, errorMessageInfo + "Expected Child Field Id " +
                    childFieldId + " and Actual Child Field Id " + childJsonObj.getInt("id"));
            csAssert.assertTrue(childJsonObj.getString("name").equalsIgnoreCase(childFieldName), errorMessageInfo + "Expected Child Field Name: " +
                    childFieldName + " and Actual Child Field Name: " + childJsonObj.getString("name"));
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Show Data of MasterField Id " + masterFieldId + " of Entity " + entityName +
                    ". " + e.getMessage());
        }
    }

    private void validateUpdateFormData(String entityName, String masterFieldName, int masterFieldId, String masterFieldOptionName, int masterFieldOptionId,
                                        String childFieldName, int childFieldId, Map<Integer, String> allChildFields, List<Map<String, String>> allMasterFieldOptions,
                                        CustomAssert csAssert) {
        try {
            logger.info("Validating UpdateFormData for Entity {}, MasterField {}, MasterFieldOption {} and ChildField {}", entityName, masterFieldName,
                    masterFieldOptionName, childFieldName);

            logger.info("Hitting UpdateFormData API for Entity {}", entityName);
            String updateFormDataResponse = NestedFieldUpdateFormData.getUpdateFormDataV1Response(masterFieldId).getResponseBody();

            //Validate Master Field Details
            JSONObject jsonObj = new JSONObject(updateFormDataResponse);
            JSONObject masterFieldJsonObj = jsonObj.getJSONObject("masterField");

            String errorMessageInfo = "UpdateFormData API Validation for Entity " + entityName + ". ";

            csAssert.assertTrue(masterFieldJsonObj.getInt("id") == masterFieldId, errorMessageInfo + "Expected Master Field Id: " + masterFieldId +
                    " and Actual Master Field Id: " + masterFieldJsonObj.getInt("id"));
            csAssert.assertTrue(masterFieldJsonObj.getString("name").equalsIgnoreCase(masterFieldName), errorMessageInfo + "Expected Master Field Name: " +
                    masterFieldName + " and Actual Master Field Name: " + masterFieldJsonObj.getString("name"));

            //Validate Selected Child Details
            JSONObject childFieldJsonObj = jsonObj.getJSONArray("selectedChilds").getJSONObject(0);
            csAssert.assertTrue(childFieldJsonObj.getInt("fieldId") == childFieldId, errorMessageInfo + "Expected Child Field Id: " +
                    childFieldId + " and Actual Child Field Id: " + childFieldJsonObj.getInt("fieldId"));
            csAssert.assertTrue(childFieldJsonObj.getString("name").equalsIgnoreCase(childFieldName), errorMessageInfo + "Expected Child Field Name: " +
                    childFieldName + " and Actual Child Field Name: " + childFieldJsonObj.getString("name"));

            //Validate Master Field Option Details
            int actualMasterFieldOptionId = childFieldJsonObj.getJSONArray("selectedOptionIds").getInt(0);
            csAssert.assertTrue(actualMasterFieldOptionId == masterFieldOptionId, errorMessageInfo + "Expected MasterField Option Id: " +
                    masterFieldOptionId + " and Actual MasterField Option Id: " + actualMasterFieldOptionId);

            //Validate Child Field Options
            int actualSizeOfAllChildFields = jsonObj.getJSONArray("allChildFields").length();
            csAssert.assertTrue(actualSizeOfAllChildFields == allChildFields.size(), errorMessageInfo + "Expected Size of All Child Fields: " +
                    allChildFields.size() + " and Actual Size: " + actualSizeOfAllChildFields);

            //Validate Master Field Options
            int actualSizeOfAllMasterFields = jsonObj.getJSONArray("masterFieldOptions").length();
            csAssert.assertTrue(actualSizeOfAllMasterFields == allMasterFieldOptions.size(), errorMessageInfo +
                    "Expected Size of All Master Field Options: " + allMasterFieldOptions.size() + " and Actual Size: " + actualSizeOfAllMasterFields);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating UpdateFormData for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private String getFieldApiName(int fieldId, int entityTypeId) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "select api_name from entity_field where id = " + fieldId + " and entity_type_id = " + entityTypeId + " and active = true";
        List<List<String>> allResults = sqlObj.doSelect(query);

        sqlObj.closeConnection();

        if (!allResults.isEmpty()) {
            return allResults.get(0).get(0);
        }

        return null;
    }
}