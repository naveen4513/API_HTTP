package com.sirionlabs.helper.bulk;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BulkOperationsHelper {

    private final static Logger logger = LoggerFactory.getLogger(BulkOperationsHelper.class);

    public String getLatestBulkEditRequestId() {
        String requestId = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select id from bulk_edit_request order by id desc limit 1";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                requestId = results.get(0).get(0);
            }

            sqlObj.closeConnection();
            return requestId;
        } catch (Exception e) {
            logger.error("Exception while Getting Latest Bulk Edit Request Id. " + e.getMessage());
        }

        return requestId;
    }

    public String getErrorDataForBulkEditRequestId(String bulkEditRequestId) {
        String errorMessages = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select error_data from bulk_edit_request where id=" + bulkEditRequestId;

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                errorMessages = results.get(0).get(0);

                if (errorMessages == null) {
                    logger.info("No Error Data Found in Bulk_Edit_Request Table. Now trying in Table Entity_Bulk_Edit_Pending.");

                    query = "select error_data from entity_bulk_edit_pending where bulk_edit_request_id=" + bulkEditRequestId;

                    results = sqlObj.doSelect(query);
                    errorMessages = results.get(0).get(0);

                    if (errorMessages == null) {
                        errorMessages = "null";
                    } else {
                        errorMessages = errorMessages.trim().replace("{", "").replace("}", "").replaceAll("\"", "");
                    }
                } else {
                    errorMessages = errorMessages.trim().replace("{", "").replace("}", "").replaceAll("\"", "");
                }
            }

            sqlObj.closeConnection();
            return errorMessages;
        } catch (Exception e) {
            logger.error("Exception while Getting Error Messages for Bulk Edit Request Id " + bulkEditRequestId + ". " + e.getMessage());
        }

        return errorMessages;
    }

    public String getErrorMessagesForBulkEditRequestId(String bulkEditRequestId) {
        String errorMessages = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select error_messages from entity_bulk_edit_pending where bulk_edit_request_id=" + bulkEditRequestId;

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                errorMessages = results.get(0).get(0);

                if (errorMessages == null) {
                    errorMessages = "null";
                } else {
                    errorMessages = errorMessages.trim().replace("{", "").replace("}", "").replaceAll("\"", "");
                }
            }

            sqlObj.closeConnection();
            return errorMessages;
        } catch (Exception e) {
            logger.error("Exception while Getting Error Messages for Bulk Edit Request Id " + bulkEditRequestId + ". " + e.getMessage());
        }

        return errorMessages;
    }

    public String getErrorMessagesForBulkEditRequestIdAndEntityId(String bulkEditRequestId, Integer entityId) {
        String errorMessages = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select error_messages from entity_bulk_edit_pending where bulk_edit_request_id=" + bulkEditRequestId + " and entity_id=" + entityId;

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                errorMessages = results.get(0).get(0);

                if (errorMessages == null) {
                    errorMessages = "null";
                } else {
                    errorMessages = errorMessages.trim().replace("{", "").replace("}", "").replaceAll("\"", "");
                }
            }

            sqlObj.closeConnection();
            return errorMessages;
        } catch (Exception e) {
            logger.error("Exception while Getting Error Messages for Bulk Edit Request Id " + bulkEditRequestId + ". " + e.getMessage());
        }

        return errorMessages;
    }

    public String getLatestBulkActionAttachmentName() {
        String bulkActionAttachmentName = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select attachment_name from system_emails where subject='Bulk action request response' order by id desc limit 1";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                bulkActionAttachmentName = results.get(0).get(0);
            }

            sqlObj.closeConnection();
            return bulkActionAttachmentName;
        } catch (Exception e) {
            logger.error("Exception while Getting Latest Bulk Action Attachment Name. " + e.getMessage());
        }

        return bulkActionAttachmentName;
    }


    public String getLatestBulkEditAttachmentName() {
        String bulkActionAttachmentName = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select attachment_name from system_emails where subject like '%modified in bulk%' order by id desc limit 1";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                bulkActionAttachmentName = results.get(0).get(0);
            }

            sqlObj.closeConnection();
            return bulkActionAttachmentName;
        } catch (Exception e) {
            logger.error("Exception while Getting Latest Bulk Action Attachment Name. " + e.getMessage());
        }

        return bulkActionAttachmentName;
    }

    public boolean removeBulkEditPermission(int bulkEditId, int endUserId) {
        boolean isupdatedSuccessfully = false;
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "update app_user set permissions = array_remove(permissions," + bulkEditId + ")  where id =" + endUserId;
            isupdatedSuccessfully = sqlObj.updateDBEntry(query);
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Error in removing bulk edit permission " + e.getStackTrace());
        }
        return isupdatedSuccessfully;
    }

    public boolean appendBulkEditPermission(int bulkEditId, int endUserId) {
        boolean isupdatedSuccessfully = false;
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "update app_user set permissions = array_append(permissions," + bulkEditId + ")  where id =" + endUserId;
            isupdatedSuccessfully = sqlObj.updateDBEntry(query);
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Error in appending bulk edit permission " + e.getStackTrace());
        }
        return isupdatedSuccessfully;
    }

    public boolean entityActionEmailCreated(String emailName) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

            String query = "select id from entity_action_email where name ='" + emailName.trim() + " (Bulk)'";
            int entriesForBulk = sqlObj.deleteDBEntry(query);

            query = "select id from entity_action_email where name ='" + emailName.trim() + " (Individual)'";
            int entriesForIndividual = sqlObj.deleteDBEntry(query);

            if (entriesForBulk == 0 || entriesForIndividual == 0) {
                //Special case for entities like Clause where (Individual and Bulk suffix) is not added.
                query = "select id from entity_action_email where name='" + emailName.trim() + "'";
                int entries = sqlObj.deleteDBEntry(query);
                sqlObj.closeConnection();

                return entries != 0;
            }

            sqlObj.closeConnection();
            return true;
        } catch (Exception e) {
            logger.error("Exception while Checking if there is any entry in Entity Action Email for Email Name: [" + emailName + "]. " + e.getMessage());
        }

        return false;
    }

    public boolean deleteAllEntriesInEntityActionEmailForName(String emailName) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

            String query = "delete from entity_action_email where name ='" + emailName.trim() + " (Bulk)'";
            int deleteEntriesForBulk = sqlObj.deleteDBEntry(query);

            query = "delete from entity_action_email where name ='" + emailName.trim() + " (Individual)'";
            int deleteEntriesForIndividual = sqlObj.deleteDBEntry(query);

            if (deleteEntriesForBulk == 0 || deleteEntriesForIndividual == 0) {
                //Special case for entities like Clause where (Individual and Bulk suffix) is not added.
                query = "delete from entity_action_email where name='" + emailName.trim() + "'";
                int deleteEntries = sqlObj.deleteDBEntry(query);
                sqlObj.closeConnection();

                return deleteEntries != 0;
            }

            sqlObj.closeConnection();
            return true;
        } catch (Exception e) {
            logger.error("Exception while deleting all entries in Entity Action Email for Email Name: [" + emailName + "]. " + e.getMessage());
        }

        return false;
    }

    public List<Map<String, String>> getAllFieldsInBulkCreateTemplate(String templatePath, String templateName, String sheetName) {
        List<Map<String, String>> allFieldsInBulkCreateTemplate = new ArrayList<>();

        try {
            logger.info("Getting All Fields in Bulk Create Template from Template {} and Sheet {}", templatePath + "/" + templateName, sheetName);
            List<List<String>> allFieldsInTemplate = XLSUtils.getAllExcelDataColumnWise(templatePath, templateName, sheetName);

            if (allFieldsInTemplate == null || allFieldsInTemplate.isEmpty()) {
                logger.error("Couldn't get All Fields in Bulk Create Template.");
                return null;
            }

            for (List<String> oneColumnDataList : allFieldsInTemplate) {
                Map<String, String> fieldMap = new HashMap<>();

                fieldMap.put("label", oneColumnDataList.get(0).trim());
                fieldMap.put("id", oneColumnDataList.get(1).trim());

                String parentChildValue = oneColumnDataList.get(2);
                fieldMap.put("isParentField", "false");
                fieldMap.put("isChildField", "false");
                fieldMap.put("parentFieldId", null);

                if (!parentChildValue.equalsIgnoreCase("")) {
                    if (parentChildValue.equalsIgnoreCase("1")) {
                        fieldMap.put("isParentField", "true");
                    } else if (parentChildValue.equalsIgnoreCase("2")) {
                        fieldMap.put("isChildField", "true");
                        fieldMap.put("parentFieldId", oneColumnDataList.get(3));
                    }
                }

                String validationStr = oneColumnDataList.get(4);
                fieldMap.put("isMandatory", "false");

                if (validationStr == null || validationStr.trim().equalsIgnoreCase("")) {
                    fieldMap.put("validationStr", "");
                } else {
                    String[] temp = validationStr.split(Pattern.quote(","));

                    for (String str : temp) {
                        if (str.trim().equalsIgnoreCase("Mandatory")) {
                            fieldMap.put("isMandatory", "true");
                            break;
                        }
                    }

                    fieldMap.put("validationStr", validationStr);
                }

                fieldMap.put("fieldType", oneColumnDataList.get(5));

                allFieldsInBulkCreateTemplate.add(fieldMap);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Fields in Bulk Create Template from Location [{}] and Sheet {}. {}", templatePath + "/" + templateName,
                    sheetName, e.getStackTrace());
            return null;
        }

        return allFieldsInBulkCreateTemplate;
    }

    public List<Map<String, String>> getAllFieldsInBulkUpdateTemplate(String templatePath, String templateName, String sheetName) {
        List<Map<String, String>> allFieldsInBulkUpdateTemplate = new ArrayList<>();

        try {
            logger.info("Getting All Fields in Bulk Update Template from Template {} and Sheet {}", templatePath + "/" + templateName, sheetName);
            List<List<String>> allFieldsInTemplate = XLSUtils.getAllExcelDataColumnWise(templatePath, templateName, sheetName);

            if (allFieldsInTemplate == null || allFieldsInTemplate.isEmpty()) {
                logger.error("Couldn't get All Fields in Bulk Update Template.");
                return null;
            }

            for (List<String> oneColumnDataList : allFieldsInTemplate) {
                Map<String, String> fieldMap = new HashMap<>();

                fieldMap.put("label", oneColumnDataList.get(0).trim());
                fieldMap.put("id", oneColumnDataList.get(1).trim());

                String parentChildValue = oneColumnDataList.get(2);
                fieldMap.put("isParentField", "false");
                fieldMap.put("isChildField", "false");
                fieldMap.put("parentFieldId", null);

                if (!parentChildValue.equalsIgnoreCase("")) {
                    if (parentChildValue.equalsIgnoreCase("1")) {
                        fieldMap.put("isParentField", "true");
                    } else if (parentChildValue.equalsIgnoreCase("2")) {
                        fieldMap.put("isChildField", "true");
                        fieldMap.put("parentFieldId", oneColumnDataList.get(3));
                    }
                }

                String validationStr = oneColumnDataList.get(4);
                fieldMap.put("isMandatory", "false");

                if (validationStr == null || validationStr.trim().equalsIgnoreCase("")) {
                    fieldMap.put("validationStr", "");
                } else {
                    String[] temp = validationStr.split(Pattern.quote(","));

                    for (String str : temp) {
                        if (str.trim().equalsIgnoreCase("Mandatory")) {
                            fieldMap.put("isMandatory", "true");
                            break;
                        }
                    }

                    fieldMap.put("validationStr", validationStr);
                }

                fieldMap.put("fieldType", oneColumnDataList.get(5));

                allFieldsInBulkUpdateTemplate.add(fieldMap);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Fields in Bulk Update Template from Location [{}] and Sheet {}. {}", templatePath + "/" + templateName,
                    sheetName, e.getStackTrace());
            return null;
        }

        return allFieldsInBulkUpdateTemplate;
    }

    public String getErrorMessagesForBulkCreateFileName(String bulkCreateTemplateName) {
        String errorMessages = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select error_data from bulk_edit_request where file_name ='" + bulkCreateTemplateName + "' order by id desc limit 1";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                errorMessages = results.get(0).get(0);

                if (errorMessages == null) {
                    logger.info("No Error Data Found in Bulk_Edit_Request Table. Now trying in Table Entity_Bulk_Edit_Pending.");

                    query = "select error_data from entity_bulk_edit_pending where bulk_edit_request_id in (select id from bulk_edit_request where file_name ='" +
                            bulkCreateTemplateName + "' order by id desc limit 1)";

                    results = sqlObj.doSelect(query);
                    errorMessages = results.get(0).get(0);

                    if (errorMessages == null) {
                        errorMessages = "null";
                    } else {
                        errorMessages = errorMessages.trim().replace("{", "").replace("}", "").replaceAll("\"", "");
                    }
                } else {
                    errorMessages = errorMessages.trim().replace("{", "").replace("}", "").replaceAll("\"", "");
                }
            }

            sqlObj.closeConnection();
            return errorMessages;
        } catch (Exception e) {
            logger.error("Exception while Getting Error Messages for Bulk Create Template Name" + bulkCreateTemplateName + ". " + e.getMessage());
        }

        return errorMessages;
    }

    public int getNewlyCreatedRecordIdFromBulkCreateFileName(String bulkCreateTemplateName) {
        int recordId = -1;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select entity_id from entity_bulk_edit_pending where bulk_edit_request_id in (select id from bulk_edit_request where file_name ='" +
                    bulkCreateTemplateName + "' order by id desc limit 1)";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                recordId = Integer.parseInt(results.get(0).get(0));
            }

            sqlObj.closeConnection();
            return recordId;
        } catch (Exception e) {
            logger.error("Exception while Getting Newly Created Record Id from Bulk Create Template Name" + bulkCreateTemplateName + ". " + e.getMessage());
        }

        return recordId;
    }

    public List<Map<String, String>> getAllDynamicFieldsOfEntity(int entityTypeId) {
        List<Map<String, String>> allDynamicFields = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select id, active, api_name from entity_field where entity_type_id = " + entityTypeId + " and is_dynamic_field = true and client_id = 1002";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                for (List<String> oneRecord : results) {
                    Map<String, String> dynamicFieldMap = new HashMap<>();

                    dynamicFieldMap.put("id", oneRecord.get(0));

                    if (oneRecord.get(1).equalsIgnoreCase("t")) {
                        dynamicFieldMap.put("active", "true");
                    } else if (oneRecord.get(1).equalsIgnoreCase("f")) {
                        dynamicFieldMap.put("active", "false");
                    } else {
                        dynamicFieldMap.put("active", oneRecord.get(1));
                    }

                    dynamicFieldMap.put("apiName", oneRecord.get(2));

                    allDynamicFields.add(dynamicFieldMap);
                }
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Couldn't get All Dynamic Fields of EntityTypeId {} and Client Id 1002. {}", entityTypeId, e.getStackTrace());
            return null;
        }

        return allDynamicFields;
    }

    public String getEntityBulkUpdateFieldShowPageObjectNameFromHeaderId(String entityName, String headerId) {
        String showPageObjectName = null;

        try {
            String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityBulkUpdateFieldShowPageObjectNameMappingFilePath");
            String configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityBulkUpdateFieldShowPageObjectNameMappingFile");

            String[] value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, headerId).split(Pattern.quote("::"));

            showPageObjectName = value[0].trim();
        } catch (Exception e) {
            logger.error("Exception while Getting Show Page Object Name for Field Header Id {} of Entity {}. {}", headerId, entityName, e.getStackTrace());
        }

        return showPageObjectName;
    }

    public String getEntityBulkUpdateFieldTypeFromHeaderId(String entityName, String headerId) {
        String fieldType = "text";

        try {
            String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityBulkUpdateFieldShowPageObjectNameMappingFilePath");
            String configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityBulkUpdateFieldShowPageObjectNameMappingFile");

            String[] value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, headerId).split(Pattern.quote("::"));

            if (value.length > 1) {
                fieldType = value[1].trim();
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Type for Field Header Id {} of Entity {}. {}", headerId, entityName, e.getStackTrace());
        }

        return fieldType;
    }

    public void verifyBulkUpdateAuditLog(String entityName, int entityTypeId, Integer recordId, List<String> allFieldNames, List<String> allExpectedValues,
                                         CustomAssert csAssert) {
        List<Integer> allRecordIds = new ArrayList<>();
        allRecordIds.add(recordId);

        List<List<String>> allFieldNamesList = new ArrayList<>();
        allFieldNamesList.add(allFieldNames);

        List<List<String>> allExpectedValuesList = new ArrayList<>();
        allExpectedValuesList.add(allExpectedValues);

        verifyBulkUpdateAuditLog(entityName, entityTypeId, allRecordIds, null, allFieldNamesList, allExpectedValuesList, csAssert);
    }

    public void verifyBulkUpdateAuditLog(String entityName, int entityTypeId, List<Integer> allRecordIds, List<String> allShowResponseBeforeUpdate,
                                         List<List<String>> allFieldNamesList, List<List<String>> allExpectedValuesList, CustomAssert csAssert) {

        for (int i = 0; i < allRecordIds.size(); i++) {
            int recordId = allRecordIds.get(i);
            String showResponseBeforeUpdate = allShowResponseBeforeUpdate.get(i);
            List<String> allFieldNames = allFieldNamesList.get(i);
            List<String> allExpectedValues = allExpectedValuesList.get(i);

            try {
                logger.info("Verifying Bulk Update Audit Log for Entity {} and Record Id {}", entityName, recordId);
                String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
                        "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                String auditLogResponse = TabListDataHelper.hitTabListDataAPIForAuditLogTab(entityTypeId, recordId, payload);

                if (ParseJsonResponse.validJsonResponse(auditLogResponse)) {
                    JSONObject jsonObj = new JSONObject(auditLogResponse);
                    jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);

                    //Validate Action in Audit Log
                    String actionNameColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogResponse, "action_name");
                    String actionValue = jsonObj.getJSONObject(actionNameColumnId).getString("value");

                    if (!actionValue.equalsIgnoreCase("Updated (Bulk)")) {
                        csAssert.assertTrue(false, "Action Name Column Validation failed. Expected Value: [Updated (Bulk)] and Actual Value: " +
                                actionValue + " for Entity " + entityName + " and Record Id " + recordId);
                    }

                    //Validate History Field in Audit Log
                    String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogResponse, "history");
                    String historyValue = jsonObj.getJSONObject(historyColumnId).getString("value");
                    Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                    FieldHistory fieldHistoryObj = new FieldHistory();
                    String fieldHistoryResponse = fieldHistoryObj.hitFieldHistory(historyId, entityTypeId);

                    if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {
                        jsonObj = new JSONObject(fieldHistoryResponse);
                        JSONArray jsonArr = jsonObj.getJSONArray("value");

                        if (allFieldNames == null || allFieldNames.isEmpty()) {
                            if (jsonArr.length() > 0) {
                                csAssert.assertTrue(false, "Field History Validation failed for History Id " + historyId + " of Entity " +
                                        entityName + " and Record Id " + recordId + ". Expected Array Length: 0 and Actual Array Length: " + jsonArr.length());
                            }
                        } else {
                            if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {

                                for (int j = 0; j < allFieldNames.size(); j++) {
                                    String editedFieldName = allFieldNames.get(j);
                                    String showFieldHierarchy = ShowHelper.getShowFieldHierarchy(editedFieldName, entityTypeId);

                                    boolean fieldOfMultiSelectType = false;

                                    Map<String, String> fieldAttributesMap = ParseJsonResponse.getFieldByName(showResponseBeforeUpdate, editedFieldName);

                                    if (fieldAttributesMap != null && !fieldAttributesMap.isEmpty()) {
                                        if (fieldAttributesMap.get("multiple").equalsIgnoreCase("true")) {
                                            fieldOfMultiSelectType = true;
                                        }
                                    }

                                    String editedFieldPreviousValue = null;

                                    if (!fieldOfMultiSelectType) {
                                        editedFieldPreviousValue = ShowHelper.getValueOfField(entityTypeId, recordId, editedFieldName, false,
                                                showResponseBeforeUpdate);
                                    } else {
                                        List<String> allValues = ShowHelper.getActualValues(showResponseBeforeUpdate, showFieldHierarchy);

                                        if (allValues != null) {
                                            editedFieldPreviousValue = allValues.toString().replace("[", "").replace("]", "");
                                        }
                                    }

                                    if (editedFieldPreviousValue == null || editedFieldPreviousValue.equalsIgnoreCase("")) {
                                        editedFieldPreviousValue = "null";
                                    }

                                    String expectedNewValue = allExpectedValues.get(j);

                                    if (entityName.equalsIgnoreCase("governance body") && editedFieldName.equalsIgnoreCase("weekType")) {
                                        expectedNewValue = ShowHelper.getValueOfField(entityTypeId, recordId, "weekTypeId");
                                    }

                                    if (expectedNewValue.equalsIgnoreCase("Not Supported")) {
                                        throw new SkipException("Currently Do Not Support Audit Log Validation for such field. Hence skipping test case");
                                    }

                                    if (expectedNewValue.equalsIgnoreCase("")) {
                                        expectedNewValue = "null";
                                    }

                                    List<String> allExpectedNewValuesList = new ArrayList<>();

                                    if (fieldOfMultiSelectType) {
                                        List<String> allPreviousValuesList = new ArrayList<>();

                                        //Special Condition for Multi Select Fields.
                                        if (expectedNewValue.contains(";") && expectedNewValue.contains(editedFieldPreviousValue)) {
                                            expectedNewValue = expectedNewValue.replace(editedFieldPreviousValue, "").trim();
                                            editedFieldPreviousValue = "null";

                                            if (expectedNewValue.startsWith(";")) {
                                                expectedNewValue = expectedNewValue.substring(1).trim();
                                            } else if (expectedNewValue.endsWith(";")) {
                                                expectedNewValue = expectedNewValue.substring(0, expectedNewValue.length() - 1);
                                            }
                                        }

                                        if (editedFieldPreviousValue.contains(",") && editedFieldPreviousValue.contains(expectedNewValue)) {
                                            editedFieldPreviousValue = editedFieldPreviousValue.replace(expectedNewValue, "").trim();
                                            expectedNewValue = "null";

                                            if (editedFieldPreviousValue.startsWith(",")) {
                                                editedFieldPreviousValue = editedFieldPreviousValue.substring(1).trim();
                                            } else if (editedFieldPreviousValue.endsWith(",")) {
                                                editedFieldPreviousValue = editedFieldPreviousValue.substring(0, editedFieldPreviousValue.length() - 1);
                                            }
                                        }

                                        String[] allPreviousValuesArr = editedFieldPreviousValue.split(Pattern.quote(","));
                                        String[] allExpectedNewValuesArr = expectedNewValue.split(Pattern.quote(";"));

                                        for (String previousValue : allPreviousValuesArr) {
                                            allPreviousValuesList.add(previousValue.trim());
                                        }

                                        for (String newValue : allExpectedNewValuesArr) {
                                            allExpectedNewValuesList.add(newValue.trim());
                                        }

                                        boolean allValuesSame = true;

                                        for (String previousValue : allPreviousValuesList) {
                                            if (!allExpectedNewValuesList.contains(previousValue)) {
                                                allValuesSame = false;
                                                break;
                                            }
                                        }

                                        if (allValuesSame) {
                                            for (String newValue : allExpectedNewValuesList) {
                                                if (!allPreviousValuesList.contains(newValue)) {
                                                    allValuesSame = false;
                                                    break;
                                                }
                                            }
                                        }

                                        if (allValuesSame) {
                                            throw new SkipException("Old and New Value is same. Hence skipping case validation.");
                                        }
                                    } else if (editedFieldPreviousValue.equalsIgnoreCase(expectedNewValue)) {
                                        throw new SkipException("Old and New Value is same. Hence skipping case validation.");
                                    }

                                    //Special condition for Boolean type values.
                                    if (expectedNewValue.equalsIgnoreCase("true")) {
                                        expectedNewValue = "yes";
                                    }

                                    if (expectedNewValue.equalsIgnoreCase("false")) {
                                        expectedNewValue = "no";
                                    }

                                    Boolean fieldFoundInHistory = false;

                                    if (fieldAttributesMap.size() > 0) {
                                        String fieldLabel = fieldAttributesMap.get("label");

                                        for (int k = 0; k < jsonArr.length(); k++) {
                                            jsonObj = jsonArr.getJSONObject(k);

                                            if (jsonObj.getString("property").trim().equalsIgnoreCase(fieldLabel.trim())) {
                                                fieldFoundInHistory = true;
                                                String historyState = jsonObj.getString("state");
                                                String actualOldValue = jsonObj.isNull("oldValue") ? "null" : jsonObj.getString("oldValue");
                                                actualOldValue = actualOldValue.equalsIgnoreCase("") ? "null" : actualOldValue;
                                                String actualNewValue = jsonObj.isNull("newValue") ? "null" : jsonObj.getString("newValue");
                                                actualNewValue = actualNewValue.equalsIgnoreCase("") ? "null" : actualNewValue;

                                                //Verify State Value
                                                logger.info("Verifying State Value in Audit Logs");
                                                if (editedFieldPreviousValue.equalsIgnoreCase("null") ||
                                                        editedFieldPreviousValue.equalsIgnoreCase("")) {
                                                    if (!historyState.trim().equalsIgnoreCase("ADDED")) {
                                                        csAssert.assertTrue(false, "Expected History State: ADDED and Actual History State: " +
                                                                historyState + " for History Id " + historyId + " and EntityTypeId " + entityTypeId);
                                                    }
                                                } else {
                                                    if (!editedFieldPreviousValue.equalsIgnoreCase("") && expectedNewValue.equalsIgnoreCase("null")) {
                                                        if (!historyState.trim().equalsIgnoreCase("REMOVED")) {
                                                            csAssert.assertTrue(false, "Expected History State: REMOVED and Actual History State: " +
                                                                    historyState + " for History Id " + historyId + " and EntityTypeId " + entityTypeId);
                                                        }
                                                    } else if (!historyState.trim().equalsIgnoreCase("MODIFIED")) {
                                                        csAssert.assertTrue(false, "Expected History State: MODIFIED and Actual History State: " +
                                                                historyState + " for History Id " + historyId + " and EntityTypeId " + entityTypeId);
                                                    }
                                                }

                                                //Verify Old Value
                                                logger.info("Verifying Old Value in Audit Logs");
                                                String expectedOldValue = editedFieldPreviousValue;

                                                //Special condition for Boolean type values.
                                                if (expectedOldValue.equalsIgnoreCase("true")) {
                                                    expectedOldValue = "yes";
                                                }

                                                if (expectedOldValue.equalsIgnoreCase("false")) {
                                                    expectedOldValue = "no";
                                                }

                                                if (NumberUtils.isParsable(expectedOldValue)) {
                                                    Double expectedOldDoubleValue = Double.parseDouble(expectedOldValue);
                                                    Double actualOldDoubleValue = Double.parseDouble(actualOldValue);

                                                    if (!expectedOldDoubleValue.equals(actualOldDoubleValue)) {
                                                        csAssert.assertTrue(false, "Expected Old Value: " + expectedOldValue + " and Actual Old Value: " +
                                                                actualOldValue);
                                                    }
                                                } else {
                                                    if (!expectedOldValue.equalsIgnoreCase(actualOldValue)) {
                                                        csAssert.assertTrue(false, "Expected Old Value: " + expectedOldValue + " and Actual Old Value: " +
                                                                actualOldValue);
                                                    }
                                                }

                                                //Verify New Value
                                                logger.info("Verifying New Value in Audit Logs");
                                                if (fieldOfMultiSelectType) {
                                                    List<String> actualNewValuesList = new ArrayList<>();
                                                    String[] actualValuesArr = actualNewValue.split(Pattern.quote(","));

                                                    for (String actualVal : actualValuesArr) {
                                                        actualNewValuesList.add(actualVal.trim());
                                                    }

                                                    boolean expectedValuesMatch = true;

                                                    for (String expectedVal : allExpectedNewValuesList) {
                                                        if (!actualNewValuesList.contains(expectedVal)) {
                                                            expectedValuesMatch = false;
                                                            break;
                                                        }
                                                    }

                                                    if (expectedValuesMatch) {
                                                        for (String actualVal : actualNewValuesList) {
                                                            if (!allExpectedNewValuesList.contains(actualVal)) {
                                                                expectedValuesMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    }

                                                    if (!expectedValuesMatch) {
                                                        csAssert.assertTrue(false, "Expected New Value: " + expectedNewValue + " and Actual New Value: " +
                                                                actualNewValue);
                                                    }
                                                } else if (!expectedNewValue.equalsIgnoreCase(actualNewValue)) {
                                                    if (!(expectedNewValue.equalsIgnoreCase("")))
                                                        csAssert.assertTrue(false, "Expected New Value: " + expectedNewValue + " and Actual New Value: " +
                                                                actualNewValue);
                                                }
                                                break;
                                            }
                                        }

                                        if (!fieldFoundInHistory) {
                                            csAssert.assertTrue(false, "Field having Label " + fieldLabel + " and Name " + editedFieldName +
                                                    " not found in Field History Response for History Id " + historyId + " and EntityTypeId " + entityTypeId);
                                        }
                                    } else {
                                        csAssert.assertTrue(false, "Couldn't get Attributes by Name for Field " + editedFieldName);
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Field History API Response for History Id " + historyId + ", EntityTypeId " + entityTypeId +
                                        " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Field History API Response for History Id " + historyId + " of Entity " + entityName +
                                " and Record Id " + recordId + " is an Invalid JSON.");
                    }
                } else {
                    csAssert.assertTrue(false, "Audit Log TabListData API Response for Entity " + entityName + " and Record Id " +
                            recordId + " is an Invalid JSON.");
                }
            } catch (SkipException e) {
                throw new SkipException(e.getMessage());
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Audit Log for Entity " + entityName + " and Record Id " +
                        recordId + ". " + e.getMessage());
            }
        }
    }
}