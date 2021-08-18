package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import com.sirionlabs.helper.dbHelper.RoleGroupDbHelper;
import com.sirionlabs.test.search.TestSearchMetadata;

public class ShowHelper {
    private final static Logger logger = LoggerFactory.getLogger(com.sirionlabs.helper.ShowHelper.class);
    private static String showFieldHierarchyConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath");
    private static String showFieldHierarchyConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ShowFieldHierarchyConfigFileName");
    private static String showPageObjectTypesConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ShowPageObjectTypesConfigFilePath");
    private static String showPageObjectTypesConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ShowPageObjectTypesConfigFileName");
    private static String entityFilterMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityFilterShowPageObjectNameMappingFilePath");
    private static String entityFilterMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityFilterShowPageObjectNameMappingFile");

    public static String getShowResponse(int entityTypeId, int entityId) {
        logger.info("Hitting Show API for Entity Type Id {} and Record Id {}", entityTypeId, entityId);
        Show showObj = new Show();
        showObj.hitShow(entityTypeId, entityId);

        return showObj.getShowJsonStr();
    }

    public static String getShowResponseVersion2(int entityTypeId, int entityId) {
        logger.info("Hitting Show API Version 2.0 for Entity Type Id {} and Record Id {}", entityTypeId, entityId);
        Show showObj = new Show();
        showObj.hitShowVersion2(entityTypeId, entityId);

        return showObj.getShowJsonStr();
    }

    public static String getValueOfField(int entityTypeId, int entityId, String field) {
        return getValueOfField(entityTypeId, entityId, field, true, null);
    }

    public static String getValueOfField(String field, String showJsonStr) {
        return getValueOfField(-1, -1, field, false, showJsonStr);
    }

    public static String getValueOfField(int entityTypeId, String field, String showJsonStr) {
        return getValueOfField(entityTypeId, -1, field, false, showJsonStr);
    }

    public static String getValueOfField(int entityTypeId, int entityId, String field, Boolean hitShow, String showJsonStr) {
        String value = null;
        try {
            String showResponse;
            if (hitShow) {
                logger.info("Hitting Show API for Entity Type Id {} and Entity Id {}.", entityTypeId, entityId);
                Show showObj = new Show();
                showObj.hitShow(entityTypeId, entityId);
                showResponse = showObj.getShowJsonStr();
            } else {
                showResponse = showJsonStr;
            }
            String fieldHierarchy = getShowFieldHierarchy(field, entityTypeId);
            value = getActualValue(showResponse, fieldHierarchy);
        } catch (Exception e) {
            logger.error("Exception while getting Value of Field {} of Record having Entity Type Id {} and Id {}. {}", field, entityTypeId, entityId, e.getStackTrace());
        }
        return value;
    }

    public static boolean isActionCanBePerformed(int entityTypeId, int entityId, String action) {

        try {
            logger.info("Hitting Show API for Entity Type Id {} and Entity Id {}.", entityTypeId, entityId);
            Show showObj = new Show();
            showObj.hitShow(entityTypeId, entityId);
            String showResponse = showObj.getShowJsonStr();
            JSONObject response = new JSONObject(showResponse);

            JSONArray validActions = response.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions");
            for (int i = 0; i < validActions.length(); i++) {

                JSONObject validAction = validActions.getJSONObject(i);
                if (validAction.get("name").toString().equalsIgnoreCase(action))
                    return true;
            }
            return false;
        } catch (Exception e) {
            logger.info("Something Went Wrong While Parsing Show Page Response : [{}]", e.getMessage());
            return false;
        }
    }

    public static String getShowFieldHierarchy(String field, int entityTypeId) {
        String hierarchy = null;
        try {
            if (ParseConfigFile.containsSection(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName, field.trim().toLowerCase())) {
                if (ParseConfigFile.hasProperty(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName, field.trim().toLowerCase(), String.valueOf(entityTypeId)))
                    hierarchy = ParseConfigFile.getValueFromConfigFile(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName, field.trim().toLowerCase(),
                            String.valueOf(entityTypeId));
                else
                    hierarchy = ParseConfigFile.getValueFromConfigFile(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName, field.trim().toLowerCase(),
                            "0");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Show Hierarchy for Field {}. {}", field, e.getStackTrace());
        }
        return hierarchy;
    }

    public static String getActualValue(String showResponse, String hierarchy) {
        String delimiterForLevel = "->";
        String openingDelimiterForTypeOfValue = "[";
        String closingDelimiterForTypeOfValue = "]";
        String actualValue = null;
        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);

            String[] levels = hierarchy.split(delimiterForLevel);
            String valueType = null;
            String valueName;
            int i;

            for (i = 0; i < levels.length - 1; i++) {
                String[] temp = levels[i].split(Pattern.quote(openingDelimiterForTypeOfValue));
                valueName = temp[0].trim();
                temp = temp[1].trim().split(Pattern.quote(closingDelimiterForTypeOfValue));
                valueType = temp[0].trim();

                if (jsonObj.has(valueName) && !jsonUtil.getStringJsonValue(valueName).trim().equalsIgnoreCase("null")) {
                    switch (valueType) {
                        case "text":
                        case "object":
                            jsonObj = new JSONObject(jsonUtil.getStringJsonValue(valueName));
                            jsonUtil = new JSONUtility(jsonObj);
                            break;

                        default:
                            logger.info("Currently field of Type {} is not supported.", valueType);
                            return null;
                    }
                } else {
                    logger.info("Couldn't find {}", valueName);
                    break;
                }
            }

            if (valueType != null && !valueType.equalsIgnoreCase("array") && !valueType.equalsIgnoreCase("stakeholder")) {
                String[] temp = levels[i].split(Pattern.quote(openingDelimiterForTypeOfValue));
                valueName = temp[0].trim();
                temp = temp[1].trim().split(Pattern.quote(closingDelimiterForTypeOfValue));
                valueType = temp[0].trim();

                switch (valueType) {
                    case "int":
                        actualValue = Integer.toString(jsonObj.getInt(valueName));
                        break;

                    case "boolean":
                        actualValue = Boolean.toString(jsonObj.getBoolean(valueName));
                        break;

                    case "long":
                        actualValue = Long.toString(jsonObj.getLong(valueName));
                        break;

                    case "double":
                        actualValue = Double.toString(jsonObj.getDouble(valueName));
                        break;

                    case "object":
                        actualValue = jsonObj.getJSONObject(valueName).toString();
                        break;

                    case "array":
                        actualValue = jsonObj.getJSONArray(valueName).toString();
                        break;
                    case "bigint":
                        actualValue = BigInteger.valueOf(Long.valueOf(jsonObj.get(valueName).toString())).toString();
                        break;
                    default:
                        actualValue = jsonObj.getString(valueName);
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching Actual Value using Hierarchy {}. {}", hierarchy, e.getMessage());
        }
        return actualValue;
    }

    /*Function to return multi select values*/
    public static List<String> getActualValues(String showResponse, String hierarchy) {
        String delimiterForLevel = "->";
        String openingDelimiterForTypeOfValue = "[";
        String closingDelimiterForTypeOfValue = "]";
        List<String> actualValues = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);
            JSONArray jsonArray = new JSONArray();

            String[] levels = hierarchy.split(delimiterForLevel);
            String valueType = null;
            String valueName;
            int i;

            for (i = 0; i < levels.length - 1; i++) {
                String[] temp = levels[i].split(Pattern.quote(openingDelimiterForTypeOfValue));
                valueName = temp[0].trim();
                temp = temp[1].trim().split(Pattern.quote(closingDelimiterForTypeOfValue));
                valueType = temp[0].trim();

                if (jsonObj.has(valueName) && !jsonUtil.getStringJsonValue(valueName).trim().equalsIgnoreCase("null")) {
                    switch (valueType) {
                        case "text":
                        case "object":
                            jsonObj = new JSONObject(jsonUtil.getStringJsonValue(valueName));
                            jsonUtil = new JSONUtility(jsonObj);
                            break;
                        case "array":
                            jsonArray = new JSONArray(jsonUtil.getStringJsonValue(valueName));
                            break;

                        default:
                            logger.info("Currently field of Type {} is not supported.", valueType);
                            return null;
                    }
                } else {
                    logger.info("Couldn't find {}", valueName);
                    break;
                }
            }

            if (valueType != null && valueType.equalsIgnoreCase("array")) {
                String[] temp = levels[i].split(Pattern.quote(openingDelimiterForTypeOfValue));
                valueName = temp[0].trim();
                temp = temp[1].trim().split(Pattern.quote(closingDelimiterForTypeOfValue));
                valueType = temp[0].trim();

                switch (valueType) {
                    case "int":
                        for (int j = 0; j < jsonArray.length(); j++) {
                            actualValues.add(String.valueOf(jsonArray.getJSONObject(j).getInt(valueName)));
                        }
                        break;

                    case "long":
                        for (int j = 0; j < jsonArray.length(); j++) {
                            actualValues.add(String.valueOf(jsonArray.getJSONObject(j).getLong(valueName)));
                        }
                        break;

                    case "double":
                        for (int j = 0; j < jsonArray.length(); j++) {
                            actualValues.add(String.valueOf(jsonArray.getJSONObject(j).getDouble(valueName)));
                        }
                        break;

                    default:
                        for (int j = 0; j < jsonArray.length(); j++) {
                            actualValues.add(jsonArray.getJSONObject(j).getString(valueName));
                        }
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching Actual Values using Hierarchy {}. {}", hierarchy, e.getMessage());
        }
        return actualValues;
    }

    public static boolean isLineItemUnderOngoingValidation(int invoiceLineItemId) {
        return isLineItemUnderOngoingValidation(-1, invoiceLineItemId);
    }

    public static boolean isLineItemUnderOngoingValidation(int invoiceLineItemEntityTypeId, int invoiceLineItemId) {
        boolean underOngoingValidation = false;
        try {
            logger.info("Checking if Invoice Line Item having Id {} is Under Ongoing Validation.", invoiceLineItemId);
            if (invoiceLineItemEntityTypeId == -1)
                invoiceLineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");

            logger.info("Hitting Show Api for Invoice Line Item having Id {}.", invoiceLineItemId);
            Show showObj = new Show();
            showObj.hitShow(invoiceLineItemEntityTypeId, invoiceLineItemId);
            String showResponse = showObj.getShowJsonStr();

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("errors");

                if (jsonObj.has("genericErrors")) {
                    JSONArray jsonArr = jsonObj.getJSONArray("genericErrors");
                    if (jsonArr.getJSONObject(0).getString("message").trim().toLowerCase().contains("this line item is undergoing validation"))
                        underOngoingValidation = true;
                }
            } else {
                logger.error("Invalid Show JSON Response.");
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Line Item is Under Ongoing Validation");
        }
        return underOngoingValidation;
    }

    public static Boolean deleteEntity(String entityName, int entityId) {
        return deleteEntity(entityName, -1, entityId);
    }

    public static Boolean deleteEntity(String entityName, int entityTypeId, int entityId) {
        boolean entityDeleted = false;
        try {
            String urlName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "url_name");

            if (entityTypeId == -1)
                entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName.trim());

            logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
            Show showObj = new Show();
            showObj.hitShow(entityTypeId, entityId);
            if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
                JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
                String prefix = "{\"body\":{\"data\":";
                String suffix = "}}";
                String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
                String deletePayload = prefix + showBodyStr + suffix;

                logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
                Delete deleteObj = new Delete();
                deleteObj.hitDelete(entityName, deletePayload, urlName);
                String deleteJsonStr = deleteObj.getDeleteJsonStr();
                jsonObj = new JSONObject(deleteJsonStr);
                String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.trim().equalsIgnoreCase("success")) {
                    entityDeleted = true;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
        }
        return entityDeleted;
    }

    public static String getCurrencyShortCode(String showResponse, int entityTypeId) {
        String currencyShortCode = null;

        try {
            String currencyShortCodeHierarchy;

            if (ParseConfigFile.hasProperty(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName, "currency short code",
                    String.valueOf(entityTypeId))) {
                currencyShortCodeHierarchy = ParseConfigFile.getValueFromConfigFile(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName,
                        "currency short code", String.valueOf(entityTypeId));
            } else if (ParseConfigFile.hasProperty(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName, "currency short code", "0")) {
                currencyShortCodeHierarchy = ParseConfigFile.getValueFromConfigFile(showFieldHierarchyConfigFilePath, showFieldHierarchyConfigFileName,
                        "currency short code", "0");
            } else {
                logger.error("Couldn't find Hierarchy of Currency Short Code for EntityTypeId {}.", entityTypeId);
                return null;
            }

            currencyShortCode = getValueAtHierarchy(showResponse, currencyShortCodeHierarchy);
        } catch (Exception e) {
            logger.error("Exception while getting Currency Short Code. {}", e.getMessage());
        }
        return currencyShortCode;
    }

    public static List<String> getAllOptionsOfField(String showResponse, String fieldHierarchy) {
        List<String> allSelectValues = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            String[] hierarchyLevels = fieldHierarchy.split(Pattern.quote("->"));
            String lastObjectName = hierarchyLevels[2].substring(0, hierarchyLevels[2].indexOf("["));

            jsonObj = jsonObj.getJSONObject(lastObjectName).getJSONObject("options");

            String valueObjectName = hierarchyLevels[hierarchyLevels.length - 1].substring(0, hierarchyLevels[hierarchyLevels.length - 1].indexOf("["));

            if (!jsonObj.getBoolean("autoComplete")) {
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    allSelectValues.add(jsonArr.getJSONObject(i).get(valueObjectName).toString().trim().toLowerCase());
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Options of Field having Hierarchy {}. {}", fieldHierarchy, e.getStackTrace());
            return null;
        }
        return allSelectValues;
    }

    public static List<String> getAllSelectValuesOfField(String showResponse, String showPageObjectName, String fieldHierarchy, int recordId, int entityTypeId) {
        List<String> allSelectValues = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            String[] hierarchyLevels = fieldHierarchy.split(Pattern.quote("->"));
            String lastObjectName = hierarchyLevels[2].substring(0, hierarchyLevels[2].indexOf("["));

            jsonObj = jsonObj.getJSONObject(lastObjectName);

            String valueObjectName = hierarchyLevels[hierarchyLevels.length - 1].substring(0, hierarchyLevels[hierarchyLevels.length - 1].indexOf("["));

            if (fieldHierarchy.contains("values[array]")) {
                if (jsonObj.has("values") && !jsonObj.isNull("values")) {
                    JSONArray jsonArr = jsonObj.getJSONArray("values");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        allSelectValues.add(jsonArr.getJSONObject(i).get(valueObjectName).toString().trim().toLowerCase());
                    }
                } else {
                    return null;
                }
            } else if (fieldHierarchy.contains("values[object]")) {
                allSelectValues.add(jsonObj.getJSONObject("values").get(valueObjectName).toString().trim().toLowerCase());
            } else {
                logger.info("");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Select Values of Field having Show Page Object Name [{}] of Record Id  {}, Entity Type Id {} and Hierarchy [{}]. {}",
                    showPageObjectName, recordId, entityTypeId, fieldHierarchy, e.getStackTrace());
            return null;
        }
        return allSelectValues;
    }

    public static List<String> getAllSelectValuesOfField(int entityTypeId, int entityId, String field) {
        List<String> allSelectValues = new ArrayList<>();

        try {
            String showResponse;
            logger.info("Hitting Show API for Entity Type Id {} and Entity Id {}.", entityTypeId, entityId);
            Show showObj = new Show();
            showObj.hitShow(entityTypeId, entityId);
            showResponse = showObj.getShowJsonStr();

            String fieldHierarchy = getShowFieldHierarchy(field, entityTypeId);

            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            String[] hierarchyLevels = fieldHierarchy.split(Pattern.quote("->"));
            String lastObjectName = hierarchyLevels[2].substring(0, hierarchyLevels[2].indexOf("["));

            jsonObj = jsonObj.getJSONObject(lastObjectName);

            String valueObjectName = hierarchyLevels[hierarchyLevels.length - 1].substring(0, hierarchyLevels[hierarchyLevels.length - 1].indexOf("["));

            if (fieldHierarchy.contains("values[array]")) {
                if (jsonObj.has("values") && !jsonObj.isNull("values")) {
                    JSONArray jsonArr = jsonObj.getJSONArray("values");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        allSelectValues.add(jsonArr.getJSONObject(i).get(valueObjectName).toString().trim().toLowerCase());
                    }
                } else {
                    return null;
                }
            } else if (fieldHierarchy.contains("values[object]")) {
                allSelectValues.add(jsonObj.getJSONObject("values").get(valueObjectName).toString().trim().toLowerCase());
            } else {
                logger.info("");
            }
        } catch (Exception e) {
                return null;
        }
        return allSelectValues;
    }

    public static List<String> getAllSelectIdsOfField(int entityTypeId, int entityId, String field) {
        List<String> allSelectValues = new ArrayList<>();

        try {
            String showResponse;
            logger.info("Hitting Show API for Entity Type Id {} and Entity Id {}.", entityTypeId, entityId);
            Show showObj = new Show();
            showObj.hitShow(entityTypeId, entityId);
            showResponse = showObj.getShowJsonStr();

            String fieldHierarchy = getShowFieldHierarchy(field, entityTypeId);

            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            String[] hierarchyLevels = fieldHierarchy.split(Pattern.quote("->"));
            String lastObjectName = hierarchyLevels[2].substring(0, hierarchyLevels[2].indexOf("["));

            jsonObj = jsonObj.getJSONObject(lastObjectName);

            int supplierID;
            if (fieldHierarchy.contains("values[array]")) {
                if (jsonObj.has("values") && !jsonObj.isNull("values")) {
                    JSONArray jsonArr = jsonObj.getJSONArray("values");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        supplierID = (int) jsonArr.getJSONObject(i).get("id");
                        String supplierIdString = Integer.toString(supplierID);
                        allSelectValues.add(supplierIdString);
                    }
                } else {
                    return null;
                }
            } else if (fieldHierarchy.contains("values[object]")) {
                supplierID = (int) jsonObj.getJSONObject("values").get("id");
                String supplierIdString = Integer.toString(supplierID);
                allSelectValues.add(supplierIdString);
            } else {
                logger.info("");
            }
        } catch (Exception e) {
            return null;
        }
        return allSelectValues;
    }

    private static String getValueAtHierarchy(String showResponse, String hierarchy) {
        String valueAtHierarchy = null;

        try {
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                String[] hierarchyLevels = hierarchy.trim().split(Pattern.quote("->"));
                JSONObject jsonObj = new JSONObject(showResponse);

                for (String level : hierarchyLevels) {
                    String[] temp = level.trim().split(Pattern.quote("["));
                    String levelType = temp[1].substring(0, temp[1].length() - 1);

                    switch (levelType.trim().toLowerCase()) {
                        case "object":
                            jsonObj = jsonObj.getJSONObject(temp[0].trim());
                            break;

                        case "text":
                            valueAtHierarchy = jsonObj.getString(temp[0].trim());
                            break;

                        case "double":
                            valueAtHierarchy = String.valueOf(jsonObj.getDouble(temp[0].trim()));
                            break;
                    }
                }
            } else {
                logger.error("Invalid Show JSON Response.");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Value at Hierarchy {}. {}", hierarchy, e.getStackTrace());
        }
        return valueAtHierarchy;
    }

    public static Boolean verifyShowPageOfDeletedRecord(String showResponse) {
        try {
            JSONObject jsonObj = new JSONObject(showResponse);

            if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("applicationError")) {
                return (jsonObj.getJSONObject("header").getJSONObject("response").getString("errorMessage").trim().toLowerCase().contains("deleted"));
            }

            return false;
        } catch (Exception e) {
            logger.error("Exception while verifying Show Page of Deleted Record. {}", e.getMessage());
            return false;
        }
    }

    public static int getParentEntityTypeId(String showpageResponse) {
        logger.info("Getting parent entity type id from show page response");
        int parententitytypeid = 0;
        try {
            JSONObject jobj = new JSONObject(showpageResponse);

            parententitytypeid = jobj.getJSONObject("body").getJSONObject("data").getJSONObject("parentEntityTypeId").getInt("values");
        } catch (Exception e) {
            logger.error("Exception while fetching parent entity typeid from show page response");
        }
        return parententitytypeid;
    }

    public static int getParentEntityId(String showpageResponse) {
        logger.info("Getting parent entity type id from show page response");
        int parententityid = 0;
        try {
            JSONObject jobj = new JSONObject(showpageResponse);

            parententityid = jobj.getJSONObject("body").getJSONObject("data").getJSONObject("parentEntityId").getInt("values");
        } catch (Exception e) {
            logger.error("Exception while fetching parent entity typeid from show page response");
        }
        return parententityid;
    }

    public static String getLastObjectNameFromHierarchy(String showHierarchy) {
        String lastObjectName = null;

        try {
            if (showHierarchy.trim().toLowerCase().contains("stakeholders[stakeholder]")) {
                return "stakeHolders";
            }

            String[] allTokens = showHierarchy.trim().split(Pattern.quote("->"));

            for (int i = allTokens.length - 1; i > 0; i--) {
                if (allTokens[i].trim().toLowerCase().contains("object") && !allTokens[i].trim().toLowerCase().contains("values[object]")) {
                    lastObjectName = allTokens[i].trim().substring(0, allTokens[i].trim().indexOf("["));
                    break;
                }
            }

            return lastObjectName;
        } catch (Exception e) {
            logger.error("Exception while Getting Last Object Name from Hierarchy [{}]. {}", showHierarchy, e.getStackTrace());
            return null;
        }
    }

    public static String getExpectedDateFormat(String showResponse, String fieldName) {
        return getExpectedDateFormat(showResponse, fieldName, null);
    }

    public static String getExpectedDateFormat(String showResponse, String fieldName, String hierarchy) {
        try {
            logger.info("Getting Attributes of Field Name {}", fieldName);
            Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(showResponse, fieldName);

            logger.info("Getting Expected Date Format for Field Name {}", fieldName);

            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("globalData");

            if (hierarchy != null) {
                String[] hierarchyArr = hierarchy.split(Pattern.quote("->"));
                if (!hierarchyArr[hierarchyArr.length - 1].contains("display")) {
                    return "MM-dd-yyyy";
                }
            }

            if (!fieldMap.isEmpty()) {
                String dateFormatPicker = fieldMap.get("dateFormatPicker");

                if (dateFormatPicker != null) {
                    if (dateFormatPicker.trim().equalsIgnoreCase("dateFormatToShow")) {
                        return jsonObj.getString("dateFormatToShow");
                    } else if (dateFormatPicker.trim().equalsIgnoreCase("datetimeFormatToShow")) {
                        return jsonObj.getString("datetimeFormatToShow");
                    } else {
                        logger.error("Invalid DateFormatPicker {}", dateFormatPicker);
                        return null;
                    }
                } else {
                    String fieldType = fieldMap.get("type");

                    if (fieldType.trim().equalsIgnoreCase("date")) {
                        return jsonObj.getString("dateFormatToShow");
                    } else if (fieldType.trim().equalsIgnoreCase("dateTime")) {
                        return jsonObj.getString("datetimeFormatToShow");
                    } else {
                        logger.error("Invalid Field Type {}", fieldType);
                        return null;
                    }
                }
            } else {
                logger.error("Couldn't get Attributes of Field Name {}", fieldName);
                return jsonObj.getString("dateFormatToShow");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Expected Date Format for Field Name {}. {}", fieldName, e.getStackTrace());
        }
        return null;
    }

    public static void verifyShowField(String showResponse, String showPageObjectName, String expectedValue, int entityTypeId, int recordId,
                                       CustomAssert csAssert) {
        verifyShowField(showResponse, showPageObjectName, null, expectedValue, entityTypeId, recordId, csAssert, true);
    }

    public static void verifyShowField(String showResponse, String showPageObjectName, String expectedValue, int entityTypeId, int recordId,
                                       CustomAssert csAssert, boolean positiveTest) {
        verifyShowField(showResponse, showPageObjectName, null, expectedValue, entityTypeId, recordId, csAssert, positiveTest);
    }

    public static void verifyShowField(String showResponse, String showPageObjectName, String fieldType, String expectedValue, int entityTypeId, int recordId,
                                       CustomAssert csAssert) {
        verifyShowField(showResponse, showPageObjectName, fieldType, expectedValue, entityTypeId, recordId, csAssert, true);
    }

    public static void verifyShowField(String showResponse, String showPageObjectName, String fieldType, String expectedValue, int entityTypeId, int recordId,
                                       CustomAssert csAssert, boolean positiveTest) {
        try {
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                showPageObjectName = showPageObjectName.trim().toLowerCase();
                String fieldHierarchy = getShowFieldHierarchy(showPageObjectName, entityTypeId);

                if (fieldHierarchy != null) {
                    String lastObjectName = getLastObjectNameFromHierarchy(fieldHierarchy);

                    if (lastObjectName != null) {
                        if (fieldType == null) {
                            fieldType = getTypeOfShowField(showResponse, showPageObjectName, lastObjectName, recordId, entityTypeId);
                        }

                        if (fieldType != null) {

                            switch (fieldType.trim().toLowerCase()) {
                                case "date":
                                case "datetime":
                                    verifyShowFieldOfDateType(showResponse, fieldHierarchy, showPageObjectName, lastObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                                    break;

                                case "select":
                                    verifyShowFieldOfSelectType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                                    break;

                                case "stakeholders":
                                    verifyShowFieldOfStakeHolderType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                                    break;

                                case "checkbox":
                                    verifyShowFieldOfCheckBoxType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                                    break;

                                case "number":
                                case "numeric":
                                    verifyShowFieldOfNumericType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                                    break;

                                case "slider":
                                    verifyShowFieldOfSliderType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                                    break;

                                case "text":
                                default:
                                    verifyShowFieldOfTextType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
                            }
                        } else {
                            logger.error("Couldn't get Type of Field having Show Page Object Name [{}] for Record Id {} of Entity Type Id {}. Hence Cannot validate data.",
                                    showPageObjectName, recordId, entityTypeId);
                            csAssert.assertTrue(false, "Couldn't get Type of Field having Show Page Object Name [" + showPageObjectName + "] for Record Id " +
                                    recordId + " of Entity Type Id " + entityTypeId + ". Hence Cannot validate data");
                        }
                    } else {
                        logger.error("Couldn't get Last Object Name from Hierarchy [{}] for Field having Show Page Object Name [{}] for Record Id {} " +
                                " of Entity Type Id {}. Hence cannot validate data.", fieldHierarchy, showPageObjectName, recordId, entityTypeId);
                        csAssert.assertTrue(false, "Couldn't get Last Object Name from Hierarchy [" + fieldHierarchy +
                                "] for Field having Show Page Object Name [" + showPageObjectName + "] for Record Id " + recordId + " " + " of Entity Type Id " +
                                entityTypeId + ". Hence cannot validate data.");
                    }
                } else {
                    logger.error("Couldn't get Hierarchy for Field having Show Page Object Name [{}] for Record Id {} of Entity Type Id {}. Hence cannot validate data.",
                            showPageObjectName, recordId, entityTypeId);
                    csAssert.assertTrue(false, "Couldn't get Hierarchy for Field having Show Page Object Name [" + showPageObjectName +
                            "] for Record Id " + recordId + " of Entity Type Id " + entityTypeId + ". Hence cannot validate data.");
                }
            } else {
                logger.error("Show API Response for Record Id {} of Entity Type Id {} is an Invalid JSON.", recordId, entityTypeId);
                csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity Type Id " + entityTypeId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Show Field having Show Page Object Name [{}], Expected Value [{}] of Entity Type Id {} and Record Id {}. {}",
                    showPageObjectName, expectedValue, entityTypeId, recordId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Show Field having Show Page Object Name [" + showPageObjectName +
                        "], Expected Value [" + expectedValue + "] of Entity Type Id " + entityTypeId + " and Record Id " + recordId + ". " + e.getMessage());
            }
        }
    }

    public static String getTypeOfShowField(String showResponse, String showPageObjectName, int recordId, int entityTypeId) {
        String hierarchy = getShowFieldHierarchy(showPageObjectName, entityTypeId);
        String lastObjectName = getLastObjectNameFromHierarchy(hierarchy);

        return getTypeOfShowField(showResponse, showPageObjectName, lastObjectName, recordId, entityTypeId);
    }

    private static String getTypeOfShowField(String showResponse, String showPageObjectName, String lastObjectName, int recordId, int entityTypeId) {
        String fieldType = null;

        try {
            logger.info("Getting Type of Field having Show Page Object Name [{}] of Record Id {} and Entity Type Id {}", showPageObjectName, recordId, entityTypeId);
            if (lastObjectName.trim().equalsIgnoreCase("stakeHolders")) {
                return "stakeholders";
            }

            String fieldHierarchy = getShowFieldHierarchy(showPageObjectName, entityTypeId);

            if (fieldHierarchy != null) {
                fieldHierarchy = fieldHierarchy.trim();

                if (fieldHierarchy.contains("array")) {
                    return "select";
                } else if (fieldHierarchy.endsWith("[double]") || fieldHierarchy.endsWith("[int]") || fieldHierarchy.endsWith("[long]")) {
                    return "numeric";
                }
            }

            Map<String, String> fieldAttributesMap = ParseJsonResponse.getFieldByName(showResponse, lastObjectName);

            if (!fieldAttributesMap.isEmpty()) {
                if (fieldAttributesMap.get("model") != null && fieldAttributesMap.get("model").trim().equalsIgnoreCase("stakeHolders.values")) {
                    fieldType = "stakeholders";
                } else if (fieldAttributesMap.get("type") != null) {
                    fieldType = fieldAttributesMap.get("type").trim();

                    if (fieldType.equalsIgnoreCase("date") || fieldType.equalsIgnoreCase("dateTime")) {
                        fieldType = "date";
                    }
                }
            } else {
                logger.info("Couldn't get Type of Field from Show Response. Now getting Type of Field from ShowPageObjectTypes Mapping Config File.");
                String temp = ParseConfigFile.getValueFromConfigFile(showPageObjectTypesConfigFilePath, showPageObjectTypesConfigFileName, "default",
                        showPageObjectName);

                if (temp != null && !temp.trim().equalsIgnoreCase("")) {
                    return temp.trim();
                }

                logger.error("Couldn't get Attributes for Field having Show Page Object Name [{}] for Entity Type Id {} and Record Id {}.", showPageObjectName, entityTypeId,
                        recordId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Type of Field having Show Page Object Name [{}] for Entity Type Id {} and Record Id {}. {}", showPageObjectName, entityTypeId,
                    recordId, e.getStackTrace());
        }
        return fieldType;
    }

    public static Map<String, String> getShowPageObjectTypesMap() {
        return ParseConfigFile.getAllConstantProperties(showPageObjectTypesConfigFilePath, showPageObjectTypesConfigFileName, "default");
    }

    private static void verifyShowFieldOfNumericType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId,
                                                     int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        try {
            String actualValue = getActualValue(showResponse, fieldHierarchy);

            if (actualValue != null) {
                Double actualDoubleValue = Double.parseDouble(actualValue);
                Double expectedDoubleValue = Double.parseDouble(expectedValue);

                Long actualLongValue = actualDoubleValue.longValue();
                Long expectedLongValue = expectedDoubleValue.longValue();

                logger.info("Expected Value: {} and Actual Value: {}", expectedLongValue, actualDoubleValue);

                if (!actualLongValue.equals(expectedLongValue)) {
                    if (positiveTest) {
                        csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                                "], Hierarchy [" + fieldHierarchy + "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". Expected Value: " +
                                expectedLongValue + " and Actual Value: " + actualLongValue);
                    }
                }
            } else {
                logger.error("Couldn't get Actual Value for Field having Show Page Object Name [{}] of Record Id {}, Entity Type Id {}, Hierarchy [{}] and Expected Value {}",
                        showPageObjectName, recordId, entityTypeId, fieldHierarchy, expectedValue);

                if (positiveTest) {
                    csAssert.assertTrue(false, "Couldn't get Actual Value for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + ", Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "] and Expected Value " + expectedValue + ".");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of Numeric Type of Record Id {} and Entity Type Id {}. {}", showPageObjectName,
                    recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of Numeric Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
    }

    private static void verifyShowFieldOfDateType(String showResponse, String fieldHierarchy, String showPageObjectName, String lastObjectName, String expectedValue,
                                                  int recordId, int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        try {
            String actualValue = getActualValue(showResponse, fieldHierarchy);

            if (actualValue != null) {
                String expectedDateFormat = getExpectedDateFormat(showResponse, lastObjectName, fieldHierarchy);

                if (expectedDateFormat != null) {
                    if (!fieldHierarchy.contains("displayValues")) {
                        Date date = new SimpleDateFormat("MM-dd-yyyy").parse(actualValue);
                        actualValue = new SimpleDateFormat(expectedDateFormat).format(date);
                    }

                    logger.info("Expected Value: {} and Actual Value: {}", expectedValue, actualValue);

                    String dateRangeDelimiter = ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter");
                    boolean result = true;

                    if (expectedValue.contains(dateRangeDelimiter)) {
                        String[] temp = expectedValue.split(Pattern.quote(dateRangeDelimiter));
                        String fromDate = temp[0];
                        String toDate = temp[1];

                        if (!DateUtils.isDateWithinRange(actualValue, fromDate, toDate, expectedDateFormat)) {
                            result = false;
                        }
                    } else {
                        if (!actualValue.trim().equalsIgnoreCase(expectedValue)) {
                            result = false;
                        }
                    }

                    if (!result && positiveTest) {
                        csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                                "], Hierarchy [" + fieldHierarchy + "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". Expected Value: " +
                                expectedValue + " and Actual Value: " + actualValue);
                    }
                } else {
                    logger.error("Couldn't get Expected Date Format for Field having Show Page Object Name [{}], Hierarchy [{}] of Record Id {} and Entity Type Id {}. Hence cannot validate data.",
                            showPageObjectName, fieldHierarchy, recordId, entityTypeId);
                    csAssert.assertTrue(false, "Couldn't get Expected Date Format for Field having Show Page Object Name [" + showPageObjectName + "], " +
                            "Hierarchy [" + fieldHierarchy + "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". Hence cannot validate data");
                }
            } else {
                logger.error("Couldn't get Actual Value for Field having Show Page Object Name [{}] of Record Id {}, Entity Type Id {}, Hierarchy [{}] and Expected Value {}",
                        showPageObjectName, recordId, entityTypeId, fieldHierarchy, expectedValue);

                if (positiveTest) {
                    csAssert.assertTrue(false, "Couldn't get Actual Value for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + ", Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "] and Expected Value " + expectedValue + ".");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of Date Type of Record Id {} and Entity Type Id {}. {}", showPageObjectName,
                    recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of Date Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
    }

    private static void verifyShowFieldOfTextType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId,
                                                  int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        try {
            if (!showPageObjectName.equalsIgnoreCase("relations")) {
                String actualValue = getActualValue(showResponse, fieldHierarchy);

                if (actualValue != null) {
                    /* Below check to handle ServiceIdSupplier Field of Invoice Line Item */
                    if (showPageObjectName.trim().equalsIgnoreCase("serviceidsupplier") && entityTypeId == 165) {
                        actualValue = actualValue.trim().substring(actualValue.trim().indexOf("(") + 1, actualValue.trim().indexOf(")"));
                    }

                    //Handling for Text where Id is also coming in Expected Value
                    if (expectedValue.contains(":;")) {
                        expectedValue = expectedValue.split(":;")[0];
                    }

                    logger.info("Expected Value: {} and Actual Value: {}", expectedValue, actualValue);

                    if (!actualValue.trim().toLowerCase().contains(expectedValue.trim().toLowerCase()) && positiveTest) {
                        csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                                "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "]. Expected Value: " +
                                expectedValue + " and Actual Value: " + actualValue);
                    }
                } else {
                    logger.error("Couldn't get Actual Value for Field having Show Page Object Name [{}] of Record Id " + recordId + ", Entity Type Id " + entityTypeId +
                            ", Hierarchy [{}] and Expected Value {}", showPageObjectName, fieldHierarchy, expectedValue);

                    if (positiveTest) {
                        csAssert.assertTrue(false, "Couldn't get Actual Value for Field having Show Page Object Name [" + showPageObjectName +
                                "] of Record Id " + recordId + ", Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "] and Expected Value " + expectedValue + ".");
                    }
                }
            } else {
                verifyShowFieldOfSelectType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, positiveTest);
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of Text Type of Record Id {} and Entity Type Id {}. {}",
                    showPageObjectName, recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of Text Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
    }

    private static void verifyShowFieldOfCheckBoxType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId,
                                                      int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        try {
            String actualValue = getActualValue(showResponse, fieldHierarchy);

            if (actualValue != null) {
                expectedValue = expectedValue.trim().equalsIgnoreCase("yes") ? "true" : expectedValue;
                expectedValue = expectedValue.trim().equalsIgnoreCase("no") ? "false" : expectedValue;

                logger.info("Expected Value: {} and Actual Value: {}", expectedValue, actualValue);

                if (!expectedValue.trim().equalsIgnoreCase(actualValue.trim()) && positiveTest) {
                    csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "]. Expected Value: " +
                            expectedValue + " and Actual Value: " + actualValue);
                }
            } else {
                logger.error("Couldn't get Actual Value for Field having Show Page Object Name [{}] of Record Id " + recordId + ", Entity Type Id " + entityTypeId +
                        ", Hierarchy [{}] and Expected Value {}", showPageObjectName, fieldHierarchy, expectedValue);

                if (positiveTest) {
                    csAssert.assertTrue(false, "Couldn't get Actual Value for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + ", Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "] and Expected Value " + expectedValue + ".");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of CheckBox Type of Record Id {} and Entity Type Id {}. {}",
                    showPageObjectName, recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of CheckBox Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
    }

    private static void verifyShowFieldOfSelectType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId,
                                                    int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        try {
            List<String> allSelectValues = getAllSelectValuesOfField(showResponse, showPageObjectName, fieldHierarchy, recordId, entityTypeId);

            if (allSelectValues != null) {
                boolean matchFound = false;

                for (String value : allSelectValues) {
                    if (value.contains(expectedValue.trim().toLowerCase())) {
                        matchFound = true;
                        break;
                    }
                }

                if (!matchFound && positiveTest) {
                    csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "]. Expected Value: " + expectedValue);
                }
            } else {
                logger.error("Couldn't get All Select Values for Field having Show Page Object Name [{}] of Record Id " + recordId + ", Entity Type Id " + entityTypeId +
                        ", Hierarchy [{}] and Expected Value {}", showPageObjectName, fieldHierarchy, expectedValue);

                if (positiveTest) {
                    csAssert.assertTrue(false, "Couldn't get All Select Values for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + ", Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "] and Expected Value " + expectedValue + ".");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of Select Type of Record Id {} and Entity Type Id {}. {}",
                    showPageObjectName, recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of Select Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
    }

    public static boolean verifyShowFieldOfStakeHolderType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId,
                                                           int entityTypeId, CustomAssert csAssert, boolean positiveTest, String roleGroupId) {

        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");

            JSONArray rgArr = jsonObj.names();
            boolean matchFound = false;
            if (roleGroupId==null) {

                for (int i = 0; i < rgArr.length(); i++) {
                    JSONArray valuesArr = jsonObj.getJSONObject(rgArr.getString(i)).getJSONArray("values");

                    for (int j = 0; j < valuesArr.length(); j++) {
                        String stakeHolderName = valuesArr.getJSONObject(j).getString("name");

                        if (stakeHolderName.trim().toLowerCase().contains(expectedValue.trim().toLowerCase())) {
                            matchFound = true;
                            break;
                        }
                    }

                    if (matchFound) {
                        break;
                    }
                }
            } else {
                JSONArray valuesArr = jsonObj.getJSONObject(roleGroupId).getJSONArray("values");
                for (int j = 0; j < valuesArr.length(); j++) {
                    String stakeHolderName = valuesArr.getJSONObject(j).getString("name");

                    if (stakeHolderName.trim().toLowerCase().contains(expectedValue.trim().toLowerCase())) {
                        matchFound = true;
                        break;
                    }
                }
            }

            if (!matchFound && positiveTest) {
                csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                        "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "]. Expected Value: " +
                        expectedValue);
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of StakeHolder Type of Record Id {} and Entity Type Id {}. {}",
                    showPageObjectName, recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of StakeHolder Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
        return true;
    }

    public static boolean verifyShowFieldOfStakeHolderType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId, int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        return verifyShowFieldOfStakeHolderType(showResponse, fieldHierarchy, showPageObjectName, expectedValue, recordId, entityTypeId, csAssert, true, null);
    }

    private static void verifyShowFieldOfSliderType(String showResponse, String fieldHierarchy, String showPageObjectName, String expectedValue, int recordId,
                                                    int entityTypeId, CustomAssert csAssert, boolean positiveTest) {
        try {
            Double actualSliderValue;
            String[] temp = expectedValue.split(ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter"));
            Double fromValue = Double.parseDouble(temp[0]);
            Double toValue = Double.parseDouble(temp[1]);

            String actualValue = getActualValue(showResponse, fieldHierarchy);

            if (actualValue != null) {
                actualValue = actualValue.replaceAll(",", "");
                actualSliderValue = Double.parseDouble(actualValue);

                logger.info("Expected Value: {} and Actual Value: {}", expectedValue, actualValue);

                if (actualSliderValue < fromValue || actualSliderValue > toValue) {
                    if (positiveTest) {
                        csAssert.assertTrue(false, "Show Page Validation failed for Field having Show Page Object Name [" + showPageObjectName +
                                "], Hierarchy [" + fieldHierarchy + "] of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". Expected Value: " +
                                expectedValue + " and Actual Value: " + actualValue);
                    }
                }
            } else {
                logger.error("Couldn't get Actual Value for Field having Show Page Object Name [{}] of Record Id {}, Entity Type Id {}, Hierarchy [{}] and Expected Value {}",
                        showPageObjectName, recordId, entityTypeId, fieldHierarchy, expectedValue);

                if (positiveTest) {
                    csAssert.assertTrue(false, "Couldn't get Actual Value for Field having Show Page Object Name [" + showPageObjectName +
                            "] of Record Id " + recordId + ", Entity Type Id " + entityTypeId + ", Hierarchy [" + fieldHierarchy + "] and Expected Value " + expectedValue + ".");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Field having Show Page Object Name [{}] of Slider Type of Record Id {} and Entity Type Id {}. {}", showPageObjectName,
                    recordId, entityTypeId, e.getStackTrace());

            if (positiveTest) {
                csAssert.assertTrue(false, "Exception while Validating Field having Show Page Object Name [" + showPageObjectName +
                        "] of Slider Type of Record Id " + recordId + " and Entity Type Id " + entityTypeId + ". " + e.getMessage());
            }
        }
    }

    public static boolean isShowPageAccessible(String showResponse) {
        return !APIUtils.isPermissionDeniedInResponse(showResponse);
    }

    public static Map<Integer, String> getAllCreateLinksMap(String showResponse) {
        Map<Integer, String> allCreateLinksMap = new HashMap<>();

        try {
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse);

                if (jsonObj.has("createLinks") && !jsonObj.isNull("createLinks")) {
                    jsonObj = jsonObj.getJSONObject("createLinks");
                    JSONArray jsonArr = jsonObj.getJSONArray("fields");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject internalJsonObj = jsonArr.getJSONObject(i);

                        if (internalJsonObj.has("fields") && !internalJsonObj.isNull("fields")) {
                            JSONArray internalJsonArr = internalJsonObj.getJSONArray("fields");

                            for (int j = 0; j < internalJsonArr.length(); j++) {
                                internalJsonObj = internalJsonArr.getJSONObject(j);

                                if (!internalJsonObj.isNull("createEntityTypeId")) {
                                    allCreateLinksMap.put(internalJsonObj.getInt("createEntityTypeId"), internalJsonObj.getString("jspApi"));
                                }
                            }
                        } else {
                            if (!internalJsonObj.isNull("createEntityTypeId")) {
                                allCreateLinksMap.put(internalJsonObj.getInt("createEntityTypeId"), internalJsonObj.getString("jspApi"));
                            }
                        }
                    }

                    return allCreateLinksMap;
                }
            } else {
                logger.error("Show Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Create Links Map from Show Response. {}", e.getMessage());
        }
        return null;
    }

    public static String getCreateLinkForEntity(String showResponse, int entityTypeId, String lineItemTypeId) {
        if (ParseJsonResponse.validJsonResponse(showResponse) && !ParseJsonResponse.hasPermissionError(showResponse)) {
            JSONObject jsonObj = new JSONObject(showResponse);

            try {
                jsonObj = jsonObj.getJSONObject("createLinks");
                JSONArray jsonArr = jsonObj.getJSONArray("fields");

                if (entityTypeId == ConfigureConstantFields.getEntityIdByName("suppliers")) {
                    return jsonArr.getJSONObject(0).getString("jspApi");
                }

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    if (!jsonObj.isNull("createEntityTypeId")) {
                        int createEntityTypeId = jsonArr.getJSONObject(i).getInt("createEntityTypeId");

                        if (entityTypeId == createEntityTypeId) {
                            return jsonObj.getString("jspApi");
                        }
                    } else {
                        if (jsonObj.has("fields")) {
                            JSONArray internalJsonArr = jsonObj.getJSONArray("fields");

                            for (int j = 0; j < internalJsonArr.length(); j++) {
                                jsonObj = internalJsonArr.getJSONObject(j);

                                if (!jsonObj.isNull("createEntityTypeId")) {
                                    if (entityTypeId == jsonObj.getInt("createEntityTypeId")) {
                                        if (lineItemTypeId != null) {
                                            if (!jsonObj.getString("api").contains("lineItemTypeId=" + lineItemTypeId.trim())) {
                                                continue;
                                            }
                                        }

                                        return jsonObj.getString("jspApi");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while Getting Create Link for Entity Type Id {}. {}", entityTypeId, e.getStackTrace());
            }
        }

        return null;
    }

    public static List<Map<String, String>> getAllSupplierTypeUsersFromShowResponse(String showResponse) {
        List<Map<String, String>> allSupplierTypeUsers = new ArrayList<>();

        try {
            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders");

                if (!jsonObj.has("values") || jsonObj.getJSONObject("values").toString().equalsIgnoreCase("{}")) {
                    return allSupplierTypeUsers;
                }

                jsonObj = jsonObj.getJSONObject("values");

                String[] allRgObjectNames = JSONObject.getNames(jsonObj);

                for (String rgObjectName : allRgObjectNames) {
                    JSONObject rgJsonObj = jsonObj.getJSONObject(rgObjectName);
                    JSONArray rgJsonArr = rgJsonObj.getJSONArray("values");

                    for (int i = 0; i < rgJsonArr.length(); i++) {
                        int userType = rgJsonArr.getJSONObject(i).getInt("type");

                        if (userType == 4) {
                            Map<String, String> userMap = new HashMap<>();

                            String userName = rgJsonArr.getJSONObject(i).getString("name");
                            int userId = rgJsonArr.getJSONObject(i).getInt("id");
                            String rgLabel = rgJsonObj.getString("label");

                            userMap.put("name", userName);
                            userMap.put("id", String.valueOf(userId));
                            userMap.put("rgLabel", rgLabel);

                            allSupplierTypeUsers.add(userMap);
                        }
                    }
                }

                return allSupplierTypeUsers;
            } else {
                logger.error("Show Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Supplier Type Users from Show Response. {}", e.getMessage());
        }

        return null;
    }

    public static List<String> getAllActionLabelsFromShowResponse(String showResponse) {
        List<String> allActionLabels = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(showResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo");

            JSONArray jsonArr = jsonObj.getJSONArray("actions");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).has("label")) {
                    String label = jsonArr.getJSONObject(i).getString("label");
                    allActionLabels.add(label);
                }
            }

            return allActionLabels;
        } catch (Exception e) {
            logger.error("Exception while Getting All Action Labels from Show Response. " + e.getMessage());
        }

        return null;
    }

    public static Boolean hasBulkCreateOptionForEntity(String showResponse, String childEntityName) {
        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
            JSONObject jsonObj = new JSONObject(showResponse);

            if (jsonObj.has("createLinks") && !jsonObj.isNull("createLinks")) {
                JSONArray jsonArr = jsonObj.getJSONObject("createLinks").getJSONArray("fields");

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject internalJsonObj = jsonArr.getJSONObject(i);

                    if (internalJsonObj.has("fields") && !internalJsonObj.isNull("fields")) {
                        JSONArray internalJsonArr = internalJsonObj.getJSONArray("fields");

                        for (int j = 0; j < internalJsonArr.length(); j++) {
                            internalJsonObj = internalJsonArr.getJSONObject(j);

                            if (internalJsonObj.has("bulkSystemLabel") && !internalJsonObj.isNull("bulkSystemLabel") &&
                                    internalJsonObj.getString("bulkSystemLabel").trim().equalsIgnoreCase("Bulk")) {
                                String uploadAPI = internalJsonObj.getJSONObject("properties").getString("uploadAPI");

                                if (uploadAPI.contains("/bulkupload/uploadBulkData/" + entityTypeId + "/"))
                                    return true;
                            }
                        }
                    }
                }
            } else {
                return false;
            }

            return false;
        } catch (Exception e) {
            logger.error("Exception while Checking if Bulk Create Option is present or not in Show Response for Child Entity {}. {}", childEntityName, e.getStackTrace());
        }
        return null;
    }

    public static String getBulkCreateTemplateDownloadAPIForEntity(String showResponse, String childEntityName) {
        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
            JSONObject jsonObj = new JSONObject(showResponse);

            if (jsonObj.has("createLinks") && !jsonObj.isNull("createLinks")) {
                JSONArray jsonArr = jsonObj.getJSONObject("createLinks").getJSONArray("fields");

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject internalJsonObj = jsonArr.getJSONObject(i);

                    if (internalJsonObj.has("fields") && !internalJsonObj.isNull("fields")) {
                        JSONArray internalJsonArr = internalJsonObj.getJSONArray("fields");

                        for (int j = 0; j < internalJsonArr.length(); j++) {
                            internalJsonObj = internalJsonArr.getJSONObject(j);

                            if (internalJsonObj.has("bulkSystemLabel") && !internalJsonObj.isNull("bulkSystemLabel") &&
                                    internalJsonObj.getString("bulkSystemLabel").trim().equalsIgnoreCase("Bulk")) {
                                String uploadAPI = internalJsonObj.getJSONObject("properties").getString("uploadAPI");

                                if (uploadAPI.contains("/bulkupload/uploadBulkData/" + entityTypeId + "/"))
                                    return internalJsonObj.getJSONObject("properties").getString("downloadAPI");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Bulk Create Template Download API for Child Entity {}. {}", childEntityName, e.getStackTrace());
        }
        return null;
    }

    public static Boolean hasCreateOptionForEntity(String showResponse, String childEntityName) {
        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
            JSONObject jsonObj = new JSONObject(showResponse);

            if (jsonObj.has("createLinks") && !jsonObj.isNull("createLinks")) {
                JSONArray jsonArr = jsonObj.getJSONObject("createLinks").getJSONArray("fields");

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject internalJsonObj = jsonArr.getJSONObject(i);

                    if (internalJsonObj.has("fields") && !internalJsonObj.isNull("fields")) {
                        JSONArray internalJsonArr = internalJsonObj.getJSONArray("fields");

                        for (int j = 0; j < internalJsonArr.length(); j++) {
                            internalJsonObj = internalJsonArr.getJSONObject(j);

                            if (internalJsonObj.has("createEntityTypeId") && !internalJsonObj.isNull("createEntityTypeId") &&
                                    internalJsonObj.getInt("createEntityTypeId") == entityTypeId) {
                                return true;
                            }
                        }
                    } else {
                        if (internalJsonObj.getInt("createEntityTypeId") == entityTypeId) {
                            return true;
                        }
                    }
                }
            } else {
                return false;
            }

            return false;
        } catch (Exception e) {
            logger.error("Exception while Checking if Create Option is present or not in Show Response for Child Entity {}. {}", childEntityName, e.getStackTrace());
        }
        return null;
    }

    public static String getSupplierIdFromShowResponse(String showResponse, int entityTypeId) {
        try {
            String supplierIdHierarchy = getShowFieldHierarchy("supplierid", entityTypeId);
            return getActualValue(showResponse, supplierIdHierarchy);
        } catch (Exception e) {
            logger.error("Exception while Getting Supplier Id from Show Response for EntityTypeId {}. {}", entityTypeId, e.getMessage());
        }

        return null;
    }

    public static String getShowPageObjectNameMapping(String entityName, String fieldName) {
        String showPageObjectName = null;

        try {
            if (ParseConfigFile.hasPropertyCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
                    entityName + " filter name show page object mapping", fieldName)) {
                showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
                        entityName + " filter name show page object mapping", fieldName);
            } else if (ParseConfigFile.hasPropertyCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
                    "default filter name show page object mapping", fieldName)) {
                showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
                        "default filter name show page object mapping", fieldName);
            } else {
                logger.info("Show Page Object Mapping not available for Field {} of Entity {}", fieldName, entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Show Page Object Name Mapping of Field {} and Entity {}. {}", fieldName, entityName, e.getStackTrace());
        }
        return showPageObjectName;
    }

    public static List<String> getAllSupplierNamesFromShowResponse(String showResponse, int entityTypeId) {
        String hierarchy = getShowFieldHierarchy("supplier", entityTypeId);
        String lastObjectName = getLastObjectNameFromHierarchy(hierarchy);

        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

        if (hierarchy.contains("values[array]")) {
            return getAllSelectValuesOfField(showResponse, "supplier", hierarchy, -1, entityTypeId);
        } else {
            List<String> allSupplierNames = new ArrayList<>();

            if (jsonObj.has(lastObjectName)) {
                String actualValue = getActualValue(showResponse, hierarchy);

                if (actualValue != null)
                    allSupplierNames.add(actualValue.toLowerCase());
            } else {
                String parentEntityTypeId = getActualValue(showResponse, getShowFieldHierarchy("parententitytypeid", entityTypeId));
                String parentEntityId = getActualValue(showResponse, getShowFieldHierarchy("parententityid", entityTypeId));

                if (parentEntityId == null || parentEntityTypeId == null) {
                    return null;
                }

                showResponse = getShowResponse(Integer.parseInt(parentEntityTypeId), Integer.parseInt(parentEntityId));

                allSupplierNames.add(getSupplierNameFromShowResponse(showResponse, Integer.parseInt(parentEntityTypeId)));
            }

            return allSupplierNames;
        }
    }

    public static String getSupplierNameFromShowResponse(String showResponse, int entityTypeId) {
        String hierarchy = getShowFieldHierarchy("supplier", entityTypeId);
        String lastObjectName = getLastObjectNameFromHierarchy(hierarchy);

        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

        if (jsonObj.has(lastObjectName)) {
            return getActualValue(showResponse, hierarchy);
        } else {
            String parentEntityTypeId = getActualValue(showResponse, getShowFieldHierarchy("parententitytypeid", entityTypeId));
            String parentEntityId = getActualValue(showResponse, getShowFieldHierarchy("parententityid", entityTypeId));

            if (parentEntityId == null || parentEntityTypeId == null) {
                return null;
            }

            showResponse = getShowResponse(Integer.parseInt(parentEntityTypeId), Integer.parseInt(parentEntityId));

            return getSupplierNameFromShowResponse(showResponse, Integer.parseInt(parentEntityTypeId));
        }
    }

    public static String getContractNameFromShowResponse(String showResponse, int entityTypeId) {
        if (entityTypeId == 1) {
            return "N/A";
        }

        if (entityTypeId == 61) {
            return getActualValue(showResponse, getShowFieldHierarchy("name", 61));
        }

        String parentEntityTypeId = getActualValue(showResponse, getShowFieldHierarchy("parententitytypeid", entityTypeId));
        String parentEntityId = getActualValue(showResponse, getShowFieldHierarchy("parententityid", entityTypeId));

        /*if(entityTypeId == 17) {
            if (parentEntityTypeId != null && parentEntityTypeId.equalsIgnoreCase("1")) {
                return "N/A";
            }
        }*/

        if (parentEntityTypeId != null && parentEntityTypeId.equalsIgnoreCase("1")) {
            return "N/A";
        }

        String hierarchy = getShowFieldHierarchy("contract", entityTypeId);
        String lastObjectName = getLastObjectNameFromHierarchy(hierarchy);

        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

        if (jsonObj.has(lastObjectName)) {
            return getActualValue(showResponse, hierarchy);
        } else {
            if (parentEntityId == null || parentEntityTypeId == null) {
                return null;
            }

            if (parentEntityTypeId.equalsIgnoreCase("1")) {
                return "N/A";
            }

            showResponse = getShowResponse(Integer.parseInt(parentEntityTypeId), Integer.parseInt(parentEntityId));

            return getSupplierNameFromShowResponse(showResponse, Integer.parseInt(parentEntityTypeId));
        }
    }

    public static List<String> getAllSelectedStakeholdersFromShowResponse(String showResponse) {
        List<String> allStakeholderNames = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");
            String[] allRgObjectNames = JSONObject.getNames(jsonObj);

            if (allRgObjectNames != null) {
                for (String rgObjName : allRgObjectNames) {
                    JSONArray jsonArr = jsonObj.getJSONObject(rgObjName).getJSONArray("values");

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String stakeholderName = jsonArr.getJSONObject(i).getString("name");
                        allStakeholderNames.add(stakeholderName);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Stakeholder Names from Show Response. {}", e.getMessage());
            return null;
        }

        return allStakeholderNames;
    }

    public synchronized static HashMap<Integer,String> getFieldIdNameMap(int entityTypeId,int entityId,CustomAssert customAssert){

        HashMap<Integer,String> fieldIdNameMap = new HashMap<>();
        try{

            Show show = new Show();
            show.hitShowVersion2(entityTypeId,entityId);
            String showResponse = show.getShowJsonStr();

            if(JSONUtility.validjson(showResponse)){
                JSONObject showResponseJson = new JSONObject(showResponse);

                JSONObject dataJson = showResponseJson.getJSONObject("body").getJSONObject("data");
                Iterator<String> keys = dataJson.keys();
                String key;
                String dynamicKey;
                String value;
                int id;
                while (keys.hasNext()){
                    key = keys.next();
                    try {
                        if (dataJson.getJSONObject(key).has("id")) {
                            id = Integer.parseInt(dataJson.getJSONObject(key).get("id").toString());
                            value = dataJson.getJSONObject(key).get("name").toString();
                            fieldIdNameMap.put(id,value);

                        }else if(key.equals("dynamicMetadata")){
                            Iterator<String> dynamicKeys = dataJson.getJSONObject(key).keys();

                            while (dynamicKeys.hasNext()){
                                try {
                                    dynamicKey = dynamicKeys.next();
                                    id = Integer.parseInt(dataJson.getJSONObject(key).getJSONObject(dynamicKey).get("id").toString());
                                    value = dataJson.getJSONObject(key).getJSONObject(dynamicKey).get("name").toString();
                                    fieldIdNameMap.put(id, value);
                                }catch (Exception e){
                                    logger.error("Exception while creating dynamic values map");
                                }
                            }
                        }
                    }catch (Exception e){
                        logger.error("Exception while creating static values map");
                    }
                }

            }else {
                customAssert.assertTrue(false,"Show Response is not a valid json for entity " + entityId);
            }


        }catch (Exception e){
            logger.error("Exception while preparing Field Id Name Map ");
        }
        return fieldIdNameMap;
    }


}