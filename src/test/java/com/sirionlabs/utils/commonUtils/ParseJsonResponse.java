package com.sirionlabs.utils.commonUtils;

import com.sirionlabs.config.ConfigureConstantFields;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by akshay.rohilla on 7/14/2017.
 */
public class ParseJsonResponse {

    private final static Logger logger = LoggerFactory.getLogger(ParseJsonResponse.class);
    private Object jsonNodeValue;

    public static Map<String, String> getFieldByName(String responseStr, String fieldName) {
        Map<String, String> field = new HashMap<>();
        try {
            JSONObject jsonObj = new JSONObject(responseStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).has("label") && (jsonArr.getJSONObject(i).getString("label").trim().toLowerCase().contains("general"))) {
                    field = ParseJsonResponse.setFieldByName(field, jsonArr.get(i).toString(), fieldName);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching Field {} in ParseJsonResponse. {}", fieldName, e.getMessage());
        }
        return field;
    }

    private static Map<String, String> setFieldByName(Map<String, String> field, String jsonStr, String fieldName) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONArray("fields").toString());
            String propertiesToAdd[] = {
                    "id", "name", "label", "type", "model", "dependentField", "groupBy", "displayMode"
            };

            for (int i = 0; i < jsonArr.length(); i++) {
                if (field.size() != 0)
                    return field;

                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    field = ParseJsonResponse.setFieldByName(field, jsonArr.get(i).toString(), fieldName);
                else {
                    if (jsonObj.has("name") && jsonObj.getString("name").trim().toLowerCase().equalsIgnoreCase(fieldName.trim())) {
                        for (String property : propertiesToAdd) {
                            if (jsonObj.has(property))
                                field.put(property, jsonObj.get(property).toString());
                            else
                                field.put(property, null);
                        }
                        //Set Dynamic Field
                        if (jsonObj.has("dynamicField"))
                            field.put("dynamicField", Boolean.toString(jsonObj.getBoolean("dynamicField")));
                        else
                            field.put("dynamicField", "false");

                        //Set Multiple Property
                        jsonObj = jsonObj.getJSONObject("properties");
                        if (field.get("type") != null && field.get("type").trim().equalsIgnoreCase("checkbox")) {
                            if (jsonObj.has("mapTrueFalseToYesNo"))
                                field.put("mapTrueFalseToYesNo", jsonObj.get("mapTrueFalseToYesNo").toString());
                        }

                        if (jsonObj.has("multiple"))
                            field.put("multiple", Boolean.toString(jsonObj.getBoolean("multiple")));
                        else
                            field.put("multiple", "false");

                        //Set SplitFields
                        if (jsonObj.has("splitFields"))
                            field.put("splitFields", Boolean.toString(jsonObj.getBoolean("splitFields")));
                        else
                            field.put("splitFields", "false");

                        //Set DateFormatPicker
                        if (jsonObj.has("dateFormatPicker"))
                            field.put("dateFormatPicker", jsonObj.getString("dateFormatPicker"));
                        else
                            field.put("dateFormatPicker", null);

                        return field;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting Field {} in ParseJsonResponse. {}", fieldName, e.getMessage());
        }
        return field;
    }

    public static String getFieldHierarchy(Map<String, String> field) throws ConfigurationException {
        String hierarchy = null;
        try {
            String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");
            String baseHierarchy = "body" + delimiterForLevel + "data";
            hierarchy = baseHierarchy + delimiterForLevel;

            if (field.get("dynamicField").equalsIgnoreCase("true"))
                hierarchy += "dynamicMetadata" + delimiterForLevel;

            if (field.get("model") != null && field.get("model").equalsIgnoreCase("stakeHolders.values"))
                hierarchy += "stakeHolders" + delimiterForLevel + "values" + delimiterForLevel + field.get("name") + delimiterForLevel + "values";
            else if (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("comment"))
                hierarchy += "comment" + delimiterForLevel + field.get("name") + delimiterForLevel + "values";
            else
                hierarchy += field.get("name") + delimiterForLevel + "values";

        } catch (Exception e) {
            logger.error("Exception while getting Hierarchy. {}", e.getMessage());
        }
        return hierarchy;
    }

    public static boolean containsHierarchy(String responseStr, String fieldType, String hierarchy) {
        boolean hierarchyFound = false;
        try {
            JSONObject jsonObj = new JSONObject(responseStr);
            String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");

            String levels[] = hierarchy.split(delimiterForLevel);
            int length;

            if (fieldType.equalsIgnoreCase("text") || fieldType.equalsIgnoreCase("textarea") || fieldType.equalsIgnoreCase("int")
                    || fieldType.equalsIgnoreCase("checkbox") || fieldType.equalsIgnoreCase("select")
                    || fieldType.equalsIgnoreCase("date") || fieldType.equalsIgnoreCase("datetime"))
                length = levels.length - 1;
            else
                length = levels.length;

            for (int i = 0; i < length - 1; i++) {
                if (jsonObj.has(levels[i]))
                    jsonObj = jsonObj.getJSONObject(levels[i]);

                else
                    return false;
            }

            if (jsonObj.has(levels[length - 1]))
                hierarchyFound = true;

        } catch (Exception e) {
            logger.error("Exception while parsing JSON Response to check if hierarchy exists or not. {}", e.getMessage());
        }
        return hierarchyFound;
    }

    public static boolean isLastNodePopulated(String responseStr, String fieldType, String isMultiple, String hierarchy) {
        boolean lastNodePopulated = false;
        try {
            String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");
            String levels[] = hierarchy.split(delimiterForLevel);
            int length = levels.length;

            JSONObject jsonObj = new JSONObject(ParseJsonResponse.getSecondLastNode(responseStr, hierarchy));

            switch (fieldType) {
                case "text":
                case "textarea":
                case "int":
                    if (jsonObj.has(levels[length - 1]) && !jsonObj.isNull(levels[length - 1]))
                        lastNodePopulated = true;
                    break;

                case "hreftext":
                    if (!jsonObj.getJSONObject(levels[length - 1]).toString().equalsIgnoreCase("{}"))
                        lastNodePopulated = true;
                    break;

                case "select":
                    if (Boolean.parseBoolean(isMultiple)) {
                        if (jsonObj.has(levels[length - 1]) && !jsonObj.getJSONArray(levels[length - 1]).toString().equalsIgnoreCase("[]"))
                            lastNodePopulated = true;
                    } else {
                        if (jsonObj.has(levels[length - 1]) && !jsonObj.getJSONObject(levels[length - 1]).toString().equalsIgnoreCase("{}"))
                            lastNodePopulated = true;
                    }
                    break;

                case "textBoxMultiSelect":
                case "textBoxSingleSelect":
                    if (jsonObj.has(levels[length - 1]) && !jsonObj.getJSONArray(levels[length - 1]).toString().equals("[]"))
                        lastNodePopulated = true;
                    break;
            }

        } catch (Exception e) {
            logger.error("Exception while checking if Last Node Populated for hierarchy {}. {}", hierarchy, e.getMessage());
        }
        return lastNodePopulated;
    }

    public static String getSecondLastNode(String responseStr, String hierarchy) {
        try {
            JSONObject jsonObj = new JSONObject(responseStr);
            String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");
            String levels[] = hierarchy.split(delimiterForLevel);

            for (int i = 0; i < levels.length - 1; i++)
                jsonObj = jsonObj.getJSONObject(levels[i]);

            return jsonObj.toString();
        } catch (Exception e) {
            logger.error("Exception while fetching Second Last Node. {}", e.getMessage());
        }
        return null;
    }

    private static String getFieldJson(String responseStr, String fieldName, boolean dynamicField) {
        String fieldJson = null;
        try {
            JSONObject jsonObj = new JSONObject(responseStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
            if (dynamicField) {
                fieldJson = jsonObj.getJSONObject("dynamicMetadata").getJSONObject(fieldName).toString();
            } else
                fieldJson = jsonObj.getJSONObject(fieldName).toString();
        } catch (Exception e) {
            logger.error("Exception while Parsing Field {}. {}", fieldName, e.getMessage());
        }
        return fieldJson;
    }

    public static List<Map<String, String>> getAllOptionsForField(String responseStr, String fieldName, boolean dynamicField) {
        List<Map<String, String>> allOptions = new ArrayList<>();
        try {
            String fieldJson = ParseJsonResponse.getFieldJson(responseStr, fieldName, dynamicField);
            JSONObject jsonObj = new JSONObject(fieldJson);

            if (jsonObj.has("options")) {
                JSONArray jsonArr = new JSONArray(jsonObj.getJSONObject("options").getJSONArray("data").toString());

                for (int i = 0; i < jsonArr.length(); i++) {
                    Map<String, String> option = new HashMap<>();
                    jsonObj = jsonArr.getJSONObject(i);

                    option.put("id", jsonObj.get("id").toString());
                    option.put("name", jsonObj.get("name").toString());
                    allOptions.add(option);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting all Options for Field {}. {}", fieldName, e.getMessage());
            return null;
        }
        return allOptions;
    }

    public static List<String> getAllOptionsForFieldAsJsonString(String responseStr, String fieldName, boolean dynamicField) {
        List<String> allOptions = new ArrayList<>();
        try {
            String fieldJson = ParseJsonResponse.getFieldJson(responseStr, fieldName, dynamicField);
            JSONObject jsonObj = new JSONObject(fieldJson);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONObject("options").getJSONArray("data").toString());

            for (int i = 0; i < jsonArr.length(); i++)
                allOptions.add(jsonArr.getJSONObject(i).toString());
        } catch (Exception e) {
            logger.error("Exception while getting all Options as Json String for Field {}. {}", fieldName, e.getMessage());
        }
        return allOptions;
    }

    public static List<String> getAllOptionsForParentAsJsonString(String responseStr, String fieldName, boolean dynamicField, int parentId) {
        List<String> allOptions = new ArrayList<>();
        try {
            String fieldJson = ParseJsonResponse.getFieldJson(responseStr, fieldName, dynamicField);
            JSONObject jsonObj = new JSONObject(fieldJson);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONObject("options").getJSONArray("data").toString());

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);
                if (jsonObj.getInt("parentId") == parentId)
                    allOptions.add(jsonObj.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while getting all Options as Json String for Field {}. {}", fieldName, e.getMessage());
        }
        return allOptions;
    }

    public static boolean fieldContainsOptions(String responseStr, String fieldName, boolean dynamicField) {
        boolean hasOptions = false;
        try {
            JSONObject jsonObj = new JSONObject(responseStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

            if (dynamicField)
                jsonObj = jsonObj.getJSONObject("dynamicMetadata");
            jsonObj = jsonObj.getJSONObject(fieldName);

            if (jsonObj.has("options") && jsonObj.getJSONObject("options").has("data") && jsonObj.getJSONObject("options").getJSONArray("data").length() > 0)
                hasOptions = true;
        } catch (Exception e) {
            logger.error("Exception while Parsing Field {} for Options. {}", fieldName, e.getMessage());
        }
        return hasOptions;
    }

    private static String getHierarchyForFieldData(String fieldName, boolean dynamicField) {
        String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");
        String baseHierarchy = "body" + delimiterForLevel + "data";
        String hierarchy = baseHierarchy + delimiterForLevel;
        if (dynamicField)
            hierarchy += "dynamicMetadata" + delimiterForLevel;

        hierarchy += fieldName;
        return hierarchy;
    }

    public static boolean containsApplicationError(String jsonStr) {
        boolean applicationError = false;
        if(jsonStr.startsWith("[")){ //edited by srijan
            logger.error("The Json string is a Json Array");
            return jsonStr.contains("applicationError");
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("applicationError"))
                applicationError = true;
        } catch (Exception e) {
            logger.error("Exception while checking Status in JsonResponse. {}", e.getMessage());
        }
        return applicationError;
    }

    public static boolean hasPermissionError(String jsonStr) {
        boolean permissionError = false;
        try {
            JSONObject jObj = new JSONObject(jsonStr);
            if (jObj.getJSONObject("header").getJSONObject("response").has("errorMessage") &&
                    jObj.getJSONObject("header").getJSONObject("response").getString("errorMessage").toLowerCase().
                            contains("either you do not have the required permissions or requested page does not exist anymore"))
                permissionError = true;
        } catch (Exception e) {
            logger.error("Exception while checking PermissionError in JsonResponse. {}", e.getMessage());
        }
        return permissionError;
    }

    public static boolean successfulResponse(String jsonStr) {
        boolean success = false;
        try {
            JSONObject jObj = new JSONObject(jsonStr);
            if (jObj.getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success"))
                success = true;
        } catch (Exception e) {
            logger.error("Exception while checking Status in JsonResponse. {}", e.getMessage());
        }
        return success;
    }

    public static Boolean validJsonResponse(String strResponse) {
        return validJsonResponse(strResponse, "");
    }

    public static Boolean validJsonResponse(String strResponse, String additionalInfo) {
        // To check if the response is valid JSON or HTML?
        Boolean isValidJson = false;
        try {
            if (strResponse.trim().startsWith("{")) {
                new JSONObject(strResponse);
            } else if (strResponse.trim().startsWith("[")) {
                new JSONArray(strResponse);
            } else
                return false;
            isValidJson = true;
        } catch (Exception e) {
            logger.error("Not a valid JSON response {}: {}", additionalInfo, e.getMessage());
        }
        return isValidJson;
    }

    public static String getHTMLResponseReason(String response) {
        if (response.contains("Status 401 – Unauthorized")) {
            return "Status 401 – Unauthorized";
        } else if (response.contains("504 Gateway Time-out")) {
            return "504 Gateway Time-out";
        } else if (response.contains("502 Bad Gateway")) {
            return "502 Bad Gateway";
        } else if (response.contains("Something went wrong, Please contact administrator")) {
            return "Something went wrong, Please contact administrator";
        } else {
            return "Reason not defined.";
        }
    }

    public static Map<String, String> getFieldByLabel(String responseStr, String fieldLabel) {
        Map<String, String> field = new HashMap<>();
        try {
            JSONObject jsonObj = new JSONObject(responseStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            field = ParseJsonResponse.setFieldByLabel(field, jsonArr.get(0).toString(), fieldLabel);
        } catch (Exception e) {
            logger.error("Exception while fetching Field {} in ParseJsonResponse. {}", fieldLabel, e.getMessage());
        }
        return field;
    }

    private static Map<String, String> setFieldByLabel(Map<String, String> field, String jsonStr, String fieldLabel) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONArray("fields").toString());
            String propertiesToAdd[] = {
                    "id", "name", "label", "type", "model", "dependentField", "groupBy", "displayMode", "bulkEditable"
            };

            for (int i = 0; i < jsonArr.length(); i++) {
                if (field.size() != 0)
                    return field;

                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    field = ParseJsonResponse.setFieldByLabel(field, jsonArr.get(i).toString(), fieldLabel);
                else {
                    if (jsonObj.has("label") && jsonObj.getString("label").trim().toLowerCase().equalsIgnoreCase(fieldLabel.trim())) {
                        for (String property : propertiesToAdd) {
                            if (jsonObj.has(property))
                                field.put(property, jsonObj.get(property).toString());
                            else
                                field.put(property, null);
                        }
                        //Set Dynamic Field
                        if (jsonObj.has("dynamicField"))
                            field.put("dynamicField", Boolean.toString(jsonObj.getBoolean("dynamicField")));
                        else
                            field.put("dynamicField", "false");

                        jsonObj = jsonObj.getJSONObject("properties");
                        if (field.get("type") != null && field.get("type").trim().equalsIgnoreCase("checkbox")) {
                            if (jsonObj.has("mapTrueFalseToYesNo"))
                                field.put("mapTrueFalseToYesNo", jsonObj.get("mapTrueFalseToYesNo").toString());
                        }

                        //Set Multiple Property
                        if (jsonObj.has("multiple"))
                            field.put("multiple", Boolean.toString(jsonObj.getBoolean("multiple")));
                        else
                            field.put("multiple", "false");

                        //Set SplitFields
                        if (jsonObj.has("splitFields"))
                            field.put("splitFields", Boolean.toString(jsonObj.getBoolean("splitFields")));
                        else
                            field.put("splitFields", "false");

                        return field;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting Field {} in ParseJsonResponse. {}", fieldLabel, e.getMessage());
        }
        return field;
    }

    public static List<String> getAllFieldNames(String jsonStr) {
        List<String> allFieldNames = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            allFieldNames = setAllFieldNames(allFieldNames, jsonArr.get(0).toString());
        } catch (Exception e) {
            logger.error("Exception while getting All Field Names. {}", e.getMessage());
        }
        return allFieldNames;
    }

    private static List<String> setAllFieldNames(List<String> allFieldNames, String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);
            JSONArray jsonArr = new JSONArray(jsonUtil.getStringArrayValueFromJSONObject("fields"));

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    allFieldNames = setAllFieldNames(allFieldNames, jsonArr.get(i).toString());
                else {
                    jsonUtil = new JSONUtility(jsonObj);
                    if (jsonObj.has("name")) {
                        if (!(jsonObj.has("model") && jsonObj.getString("model").trim().equalsIgnoreCase("comment")))
                            allFieldNames.add(jsonUtil.getStringJsonValue("name"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting All Field Names in ParseJsonResponse. {}", e.getMessage());
        }
        return allFieldNames;
    }

    public static List<String> getAllFieldLabels(String jsonStr) {
        List<String> allFieldLabels = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            allFieldLabels = setAllFieldLabels(allFieldLabels, jsonArr.get(0).toString());
        } catch (Exception e) {
            logger.error("Exception while getting All Field Labels. {}", e.getMessage());
        }
        return allFieldLabels;
    }

    private static List<String> setAllFieldLabels(List<String> allFieldLabels, String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);
            JSONArray jsonArr = new JSONArray(jsonUtil.getStringArrayValueFromJSONObject("fields"));

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    allFieldLabels = setAllFieldLabels(allFieldLabels, jsonArr.get(i).toString());
                else {
                    jsonUtil = new JSONUtility(jsonObj);
                    if (jsonObj.has("label")) {
                        if (!(jsonObj.has("model") && jsonObj.getString("model").trim().equalsIgnoreCase("comment")))
                            allFieldLabels.add(jsonUtil.getStringJsonValue("label"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting All Field Labels in ParseJsonResponse. {}", e.getMessage());
        }
        return allFieldLabels;
    }

    public static List<String> getAllFieldLabelsWithComments(String jsonStr) {
        List<String> allFieldLabels = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            allFieldLabels = setAllFieldLabelsWithComments(allFieldLabels, jsonArr.get(0).toString());
        } catch (Exception e) {
            logger.error("Exception while getting All Field Labels. {}", e.getMessage());
        }
        return allFieldLabels;
    }

    private static List<String> setAllFieldLabelsWithComments(List<String> allFieldLabels, String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);
            JSONArray jsonArr = new JSONArray(jsonUtil.getStringArrayValueFromJSONObject("fields"));

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    allFieldLabels = setAllFieldLabelsWithComments(allFieldLabels, jsonArr.get(i).toString());
                else {
                    jsonUtil = new JSONUtility(jsonObj);
                    if (jsonObj.has("label")) {
                        allFieldLabels.add(jsonUtil.getStringJsonValue("label"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting All Field Labels in ParseJsonResponse. {}", e.getMessage());
        }
        return allFieldLabels;
    }

    public static String getFieldAttributeFromLabel(String jsonStr, String fieldLabel, String attribute) {
        String attributeValue = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            attributeValue = setFieldAttributeFromLabel(jsonArr.get(0).toString(), fieldLabel, attribute);
        } catch (Exception e) {
            logger.error("Exception while getting Attribute {} for Label {}. {}", attribute, fieldLabel, e.getStackTrace());
        }
        return attributeValue;
    }

    private static String setFieldAttributeFromLabel(String jsonStr, String fieldLabel, String attribute) {
        String attributeValue = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONArray("fields").toString());

            for (int i = 0; i < jsonArr.length(); i++) {
                if (attributeValue != null)
                    return attributeValue;
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    attributeValue = ParseJsonResponse.setFieldAttributeFromLabel(jsonArr.get(i).toString(), fieldLabel, attribute);
                else {
                    if (jsonObj.has("label") && jsonObj.getString("label").toLowerCase().equalsIgnoreCase(fieldLabel.trim())) {
                        attributeValue = jsonObj.getString(attribute);
                        return attributeValue;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting Attribute {} for Label {} in ParseJsonResponse. {}", attribute, fieldLabel, e.getStackTrace());
        }
        return attributeValue;
    }


    public static JSONArray getFieldJSONArrayAttributeFromLabel(String jsonStr, String fieldLabel, String jsonArrayName) {
        JSONArray attributeValue = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            attributeValue = setFieldJSONArrayAttributeFromLabel(jsonArr.get(0).toString(), fieldLabel, jsonArrayName);
        } catch (Exception e) {
            logger.error("Exception while getting JSON Array Attribute {} for Label {}. {}", jsonArrayName, fieldLabel, e.getStackTrace());
        }
        return attributeValue;
    }

    private static JSONArray setFieldJSONArrayAttributeFromLabel(String jsonStr, String fieldLabel, String jsonArrayName) {
        JSONArray arrayValue = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONArray("fields").toString());

            for (int i = 0; i < jsonArr.length(); i++) {
                if (arrayValue != null)
                    return arrayValue;
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());

                if (jsonObj.has("label") && jsonObj.getString("label").toLowerCase().equalsIgnoreCase(fieldLabel.trim())) {
                    if (jsonObj.has(jsonArrayName))
                        arrayValue = jsonObj.getJSONArray(jsonArrayName);
                    return arrayValue;
                } else if (jsonObj.has("fields"))
                    arrayValue = ParseJsonResponse.setFieldJSONArrayAttributeFromLabel(jsonArr.get(i).toString(), fieldLabel, jsonArrayName);
            }
        } catch (Exception e) {
            logger.error("Exception while setting JSON Array Attribute {} for Label {} in ParseJsonResponse. {}", jsonArrayName, fieldLabel, e.getStackTrace());
        }
        return arrayValue;
    }

    public static String getFieldAttributeFromName(String jsonStr, String fieldName, String attribute) {
        String attributeValue = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            attributeValue = setFieldAttributeFromName(jsonArr.get(0).toString(), fieldName, attribute);
        } catch (Exception e) {
            logger.error("Exception while getting Attribute {} for Name {}. {}", attribute, fieldName, e.getStackTrace());
        }
        return attributeValue;
    }

    private static String setFieldAttributeFromName(String jsonStr, String fieldName, String attribute) {
        String attributeValue = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = new JSONArray(jsonObj.getJSONArray("fields").toString());

            for (int i = 0; i < jsonArr.length(); i++) {
                if (attributeValue != null)
                    return attributeValue;
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
//                    attributeValue = ParseJsonResponse.setFieldAttributeFromLabel(jsonArr.get(i).toString(), fieldName, attribute);
                    attributeValue = ParseJsonResponse.setFieldAttributeFromName(jsonArr.get(i).toString(), fieldName, attribute);
                else {
                    if (jsonObj.has("name") && jsonObj.getString("name").toLowerCase().equalsIgnoreCase(fieldName.trim())) {
                        attributeValue = jsonObj.getString(attribute);
                        return attributeValue;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting Attribute {} for Name {} in ParseJsonResponse. {}", attribute, fieldName, e.getStackTrace());
        }
        return attributeValue;
    }

    public static String getStatusFromResponse(String jsonStr) {
        String status = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") &&
                    jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
                status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
            } else {
                logger.error("Couldn't find Header -> Response -> Status hierarchy in Response.");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Status from Response. {}", e.getMessage());
        }
        return status;
    }

    public static List<String> getAllTabLabels(String jsonStr) {
        List<String> allTabLabels = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                allTabLabels.add(jsonArr.getJSONObject(i).getString("label").trim());
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Tab Labels. {}", e.getMessage());
        }
        return allTabLabels;
    }

    //Returns all Field Group Labels from New API Response
    public static List<String> getAllFieldGroupLabels(String jsonStr) {
        List<String> allFieldGroups = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            jsonArr = jsonArr.getJSONObject(0).getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                allFieldGroups.add(jsonArr.getJSONObject(i).getString("label").trim());
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Field Groups. {}", e.getMessage());
        }
        return allFieldGroups;
    }

    //Returns all Field Names of a Group from New API Response
    public static List<String> getAllFieldNamesOfAGroup(String jsonStr, String groupLabel) {
        List<String> allFieldNamesOfGroup = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            jsonArr = jsonArr.getJSONObject(0).getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).getString("label").trim().equalsIgnoreCase(groupLabel.trim())) {
                    JSONArray fieldsArr;

                    if (groupLabel.trim().equalsIgnoreCase("BASIC INFORMATION") || groupLabel.trim().equalsIgnoreCase("FINANCIAL INFORMATION")) {
                        fieldsArr = jsonArr.getJSONObject(i).getJSONArray("fields");
                    } else {
                        fieldsArr = jsonArr.getJSONObject(i).getJSONArray("fields").getJSONObject(0).getJSONArray("fields");
                    }

                    for (int j = 0; j < fieldsArr.length(); j++) {
                        allFieldNamesOfGroup.add(fieldsArr.getJSONObject(j).getString("name").trim());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Field Names of Group {}. {}", groupLabel, e.getStackTrace());
        }
        return allFieldNamesOfGroup;
    }

    public static List<String> getAllFieldNamesOfSelectType(String jsonResponse) {
        return getAllFieldNamesOfSelectType(jsonResponse, true);
    }

    public static List<String> getAllFieldNamesOfSelectType(String jsonResponse, Boolean includeDynamicFields) {
        List<String> fieldNames = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(jsonResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");
            fieldNames = setAllFieldNamesOfSelectType(fieldNames, jsonArr.get(0).toString(), includeDynamicFields);
        } catch (Exception e) {
            logger.error("Exception while Getting All Field Names of Select Type. {}", e.getMessage());
        }
        return fieldNames;
    }

    private static List<String> setAllFieldNamesOfSelectType(List<String> allFieldNames, String jsonStr, Boolean includeDynamicFields) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    allFieldNames = setAllFieldNamesOfSelectType(allFieldNames, jsonArr.get(i).toString(), includeDynamicFields);
                else {
                    if (jsonObj.has("type") && jsonObj.getString("type").trim().equalsIgnoreCase("select")) {
                        if (jsonObj.getBoolean("dynamicField")) {
                            if (includeDynamicFields)
                                allFieldNames.add(jsonObj.getString("name"));
                        } else {
                            allFieldNames.add(jsonObj.getString("name"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting All Field Names of Select Type. {}", e.getMessage());
        }
        return allFieldNames;
    }

    public static List<Map<String, String>> getMultipleAttributesOfAllFields(String jsonResponse, List<String> attributesList) {
        List<Map<String, String>> fieldAttributes = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(jsonResponse);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            fieldAttributes = ParseJsonResponse.setMultipleAttributesOfAllFields(fieldAttributes, jsonArr.get(0).toString(), attributesList);
        } catch (Exception e) {
            logger.error("Exception while fetching Multiple Attributes of All Fields. {}", e.getMessage());
        }
        return fieldAttributes;
    }

    private static List<Map<String, String>> setMultipleAttributesOfAllFields(List<Map<String, String>> fieldAttributes, String jsonResponse, List<String> attributesList) {
        try {
            JSONObject jsonObj = new JSONObject(jsonResponse);
            JSONUtility jsonUtil = new JSONUtility(jsonObj);
            JSONArray jsonArr = new JSONArray(jsonUtil.getStringArrayValueFromJSONObject("fields"));

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
                if (jsonObj.has("fields"))
                    fieldAttributes = setMultipleAttributesOfAllFields(fieldAttributes, jsonArr.get(i).toString(), attributesList);
                else {
                    Map<String, String> attributesMap = new HashMap<>();

                    for (String attribute : attributesList) {
                        attribute = attribute.trim();
                        if (jsonObj.has(attribute)) {
                            attributesMap.put(attribute, jsonObj.get(attribute).toString());
                        } else {
                            attributesMap.put(attribute, null);
                        }
                    }

                    fieldAttributes.add(attributesMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting Multiple Attributes of All Fields. {}", e.getMessage());
        }
        return fieldAttributes;
    }

    public static List<String> getAllFieldLabelsOfAllTabs(String jsonStr) {
        List<String> allFieldLabels = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).has("label") && !jsonArr.getJSONObject(i).isNull("label")) {
                    allFieldLabels.add(jsonArr.getJSONObject(i).getString("label"));
                }

                allFieldLabels = setAllFieldLabelsOfAllTabs(allFieldLabels, jsonArr.get(i).toString());
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Field Labels of All Tabs. {}", e.getMessage());
        }
        return allFieldLabels;
    }

    private static List<String> setAllFieldLabelsOfAllTabs(List<String> allFieldLabels, String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = jsonObj.getJSONArray("fields");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.has("label") && !jsonObj.isNull("label")) {
                    allFieldLabels.add(jsonObj.getString("label"));
                }

                if (jsonObj.has("fields")) {
                    allFieldLabels = setAllFieldLabelsOfAllTabs(allFieldLabels, jsonArr.get(i).toString());
                } /*else {
					if (jsonObj.has("label") && !jsonObj.isNull("label")) {
						allFieldLabels.add(jsonObj.getString("label"));
					}
				}*/
            }
        } catch (Exception e) {
            logger.error("Exception while setting All Field Labels of All Tabs in ParseJsonResponse. {}", e.getMessage());
        }
        return allFieldLabels;
    }

    public static List<String> getAllEditableFieldLabels(String editGetJsonStr) {
        List<String> allEditableFieldLabels = new ArrayList<>();
        allEditableFieldLabels = setAllEditableFieldLabels(editGetJsonStr, allEditableFieldLabels);
        return allEditableFieldLabels;
    }

    private static List<String> setAllEditableFieldLabels(String editGetJsonStr, List<String> editableFieldLabels) {
        try {
            editGetJsonStr = getFieldsJsonStr(editGetJsonStr);
            JSONObject jsonObj = new JSONObject(editGetJsonStr);
            JSONArray jsonArr = null;

            if (jsonObj.has("fields"))
                jsonArr = jsonObj.getJSONArray("fields");

            if (jsonArr != null) {
                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());

                    if (jsonObj.has("fields"))
                        editableFieldLabels = setAllEditableFieldLabels(jsonArr.get(i).toString(), editableFieldLabels);
                    else {
                        if (jsonObj.has("displayMode") && jsonObj.getString("displayMode").trim().equalsIgnoreCase("editable")) {
                            editableFieldLabels.add(jsonObj.getString("label"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while setting all Editable Fields. {}", e.getMessage());
        }
        return editableFieldLabels;
    }

    private static String getFieldsJsonStr(String jsonStr) {
        String fieldsJsonStr = null;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            if (jsonObj.has("body") && jsonObj.getJSONObject("body").has("layoutInfo")) {
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
                JSONArray jsonArr = jsonObj.getJSONArray("fields");
                fieldsJsonStr = jsonArr.get(0).toString();
            } else
                return jsonStr;
        } catch (Exception e) {
            logger.error("Exception while getting Fields Json String. {}", e.getMessage());
        }
        return fieldsJsonStr;
    }

    public void getNodeFromJsonWithValue(JSONObject jsonObject, List key, int value) {
        getNodeFromJson(jsonObject, key, value, "value");
    }

    public void getNodeFromJsonWithValues(JSONObject jsonObject, List key, int value) {
        getNodeFromJson(jsonObject, key, value, "values");
    }

    public void getNodeFromJsonWithValue(JSONObject jsonObject, List key, String value) {
        getNodeFromJson(jsonObject, key, value, "value");
    }

    public void getNodeFromJsonWithValues(JSONObject jsonObject, List key, String value) {
        getNodeFromJson(jsonObject, key, value, "values");
    }

    public void getNodeFromJson(JSONObject jsonObject, List key, String value, String valueToGet) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (key.contains(nextKey)) {
                if (jsonObject.get(nextKey) instanceof String) {
                    if (value.equalsIgnoreCase(jsonObject.getString(nextKey))) {
                        if (jsonObject.has(valueToGet))
                            jsonNodeValue = jsonObject.get(valueToGet);

                        return;
                    }
                } else {
                    logger.error("String node expected but found {}", jsonObject.get(nextKey));
                }
            } else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    getNodeFromJson((JSONObject) jsonObject.get(nextKey), key, value, valueToGet);
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    getNodeFromJson((JSONArray) jsonObject.get(nextKey), key, value, valueToGet);
            }
        }
        //new ArrayList<String>(List.of("abc"));
    }

    public void getNodeFromJson(JSONObject jsonObject, List key, int value, String valueToGet) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (key.contains(nextKey)) {
                if (jsonObject.get(nextKey) instanceof Integer) {
                    if (value == jsonObject.getInt(nextKey)) {
                        if (jsonObject.has(valueToGet))
                            jsonNodeValue = jsonObject.get(valueToGet);
                        return;
                    }
                } else {
                    logger.error("Integer node expected but found {}", jsonObject.get(nextKey));
                }
            } else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    getNodeFromJson((JSONObject) jsonObject.get(nextKey), key, value, valueToGet);
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    getNodeFromJson((JSONArray) jsonObject.get(nextKey), key, value, valueToGet);
            }
        }
        //new ArrayList<String>(List.of("abc"));
    }

    public void getNodeFromJsonForValueGeneric(JSONObject jsonObject, List key, int value) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (key.contains(nextKey)) {
                if (jsonObject.get(nextKey) instanceof Integer) {
                    if (value == jsonObject.getInt(nextKey)) {
                        if (jsonObject.has("value"))
                            jsonNodeValue = jsonObject.get("value");
                        else if (jsonObject.has("values")) {
                            if (jsonObject.get("values") instanceof JSONObject) {
                                jsonNodeValue = jsonObject.getJSONObject("values").getString("name");
                            } else if (jsonObject.get("values") instanceof JSONArray) {
                                jsonNodeValue = jsonObject.getJSONArray("values").getJSONObject(0).get("name");
                            } else
                                jsonNodeValue = jsonObject.get("values");
                        } else
                            return;
                    }
                } else {
                    logger.error("String node expected but found {}", jsonObject.get(nextKey));
                }
            } else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    getNodeFromJsonForValueGeneric((JSONObject) jsonObject.get(nextKey), key, value);
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    getNodeFromJsonForValueGeneric((JSONArray) jsonObject.get(nextKey), key, value);
            }
        }
        //new ArrayList<String>(List.of("abc"));
    }

    public void getNodeFromJsonForValueGeneric(JSONArray jsonArray, List key, int value) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                getNodeFromJsonForValueGeneric((JSONArray) jsonArray.get(index), key, value);
            else if (jsonArray.get(index) instanceof JSONObject)
                getNodeFromJsonForValueGeneric((JSONObject) jsonArray.get(index), key, value);
        }
    }

    public void getNodeFromJson(JSONArray jsonArray, List key, int value, String valueToGet) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                getNodeFromJson((JSONArray) jsonArray.get(index), key, value, valueToGet);
            else if (jsonArray.get(index) instanceof JSONObject)
                getNodeFromJson((JSONObject) jsonArray.get(index), key, value, valueToGet);
        }
    }

    public void getNodeFromJson(JSONArray jsonArray, List key, String value, String valueToGet) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                getNodeFromJson((JSONArray) jsonArray.get(index), key, value, valueToGet);
            else if (jsonArray.get(index) instanceof JSONObject)
                getNodeFromJson((JSONObject) jsonArray.get(index), key, value, valueToGet);
        }
    }

    public Object getJsonNodeValue() {
        return jsonNodeValue;
    }

    //added by srijan for enpty check
    public static boolean isEmptyJson(String strResponse) {
        // To check if the response is empty JSON or not?
        try {
            if (strResponse.trim().startsWith("{")) {
                return new JSONObject(strResponse).length()==0;
            } else if (strResponse.trim().startsWith("[")) {
                return new JSONArray(strResponse).length()==0;
            } else
                return false;
        } catch (Exception e) {
            logger.error("Not a valid JSON response : {}", e.getMessage());
        }
        return false;
    }

}
