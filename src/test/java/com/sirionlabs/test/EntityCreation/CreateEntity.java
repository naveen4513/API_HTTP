package com.sirionlabs.test.EntityCreation;

import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by akshay.rohilla on 7/12/2017.
 */
public class CreateEntity {

    private final static Logger logger = LoggerFactory.getLogger(CreateEntity.class);
    private static String defaultEntityCreationConfigFilePath = null;
    private static String defaultEntityCreationConfigFileName = null;

    public String getCreatePayload(String entityName, Map<String, String> properties) {
        return getCreatePayload(entityName, properties, false, true, null, null);
    }

    String getCreatePayload(String entityName, Map<String, String> properties, boolean createLocalEntity, boolean createGlobalEntity) {
        return getCreatePayload(entityName, properties, createLocalEntity, createGlobalEntity, null, null);
    }

    String getCreatePayload(String entityName, Map<String, String> properties, boolean createLocalEntity, boolean createGlobalEntity, Map<String, String> excelFields,
                            String keyPrefix) {
        String createPayload = null;
        try {
            CreateEntity.defaultEntityCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationConfigFilePath");
            CreateEntity.defaultEntityCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationDefaultConfigFileName");

            String parentEntityName = properties.get("sourceentity");
            int parentEntityId = -1;
            if (NumberUtils.isParsable(properties.get("sourceid")))
                parentEntityId = Integer.parseInt(properties.get("sourceid"));
            New newObj;

            if (createGlobalEntity)
                newObj = this.hitNewForGlobalEntityCreation(entityName, parentEntityId, properties);
            else if (createLocalEntity)
                newObj = this.hitNewForLocalEntityCreation(entityName, parentEntityName, parentEntityId, properties);
            else {
                logger.info("Both Local Entity Creation and Global Entity Creation are set to False. Hence skipping Entity Creation.");
                return null;
            }

            if (newObj != null) {
                String newResponse = newObj.getNewJsonStr();

                if (!ParseJsonResponse.validJsonResponse(newResponse)) {
                    FileUtils.saveResponseInFile(entityName + " New API HTML.txt", newResponse);
                }

                Map<String, String> extraFields = this.setExtraRequiredFields(entityName);
                if (excelFields != null) {
                    excelFields = this.updateExcelFields(newObj.getNewJsonStr(), excelFields, keyPrefix);
                    createPayload = PayloadUtils.getPayloadForCreate(newObj.getNewJsonStr(), null, extraFields, excelFields);
                } else {
                    logger.info("Setting all Required Fields for Entity {}", entityName);
                    newObj.setAllRequiredFields(newObj.getNewJsonStr());
                    Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                    allRequiredFields = this.processAllChildFields(allRequiredFields, newObj.getNewJsonStr());
                    allRequiredFields = this.processNonChildFields(allRequiredFields, newObj.getNewJsonStr());

                    if (allRequiredFields.containsKey("alias"))
                        allRequiredFields.put("alias", this.getDefaultValueForAlias());
                    createPayload = PayloadUtils.getPayloadForCreate(newObj.getNewJsonStr(), allRequiredFields, extraFields, null);
                }
            } else {
                logger.error("New Object is null. Can't proceed for Creation of Entity {}.", entityName);
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception while Creating Entity. {}", errors.toString());
        }
        return createPayload;
    }

    private New hitNewForLocalEntityCreation(String entityName, String sourceEntityName, int sourceEntityId, Map<String, String> properties) {
        logger.info("Hitting New Get Api for Entity {}", entityName);
        New newObj = new New();

        try {
            if (entityName.equalsIgnoreCase("governance body")) {
                logger.warn("Can't hit New Get for Entity Governance Body. Hitting New Post instead.");
                newObj = this.hitNewForGlobalEntityCreation(entityName, sourceEntityId, properties);
            } else {
                if (entityName.equalsIgnoreCase("contracts")) {
                    String contractType = properties.get("parententitytype");
                    newObj.hitNew(entityName, sourceEntityName, sourceEntityId, contractType);
                } else
                    newObj.hitNew(entityName, sourceEntityName, sourceEntityId);
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api for Global Entity Creation. {}", e.getMessage());
        }
        return newObj;
    }

    private New hitNewForGlobalEntityCreation(String entityName, int sourceEntityId, Map<String, String> properties) {
        logger.info("Hitting New Post Api for Entity {}", entityName);
        New newObj = new New();
        String payload;

        try {
            if (entityName.equalsIgnoreCase("contracts")) {
                logger.warn("Can't hit New Post for Entity {}. Hitting New Get", entityName);
                newObj = this.hitNewForLocalEntityCreation(entityName, properties.get("sourceentity"), sourceEntityId, properties);
            } else if (entityName.equalsIgnoreCase("suppliers")) {
                logger.error("Cannot Create Supplier from Global Page.");
                return null;
            } else if (entityName.equalsIgnoreCase("purchase orders")) {
                logger.error("Can't hit New Post for Entity {}", entityName);
                return null;
            } else if (entityName.trim().equalsIgnoreCase("vendors")) {
                logger.warn("Can't hit New Post for Entity {}. Hitting New Get instead.", entityName);
                newObj = this.hitNewForLocalEntityCreation(entityName, "client", sourceEntityId, properties);
            } else if (entityName.trim().equalsIgnoreCase("contract draft request")) {
                logger.warn("Can't hit New Post for Entity {}. Hitting New Get instead.", entityName);
                newObj = this.hitNewForLocalEntityCreation(entityName, "suppliers", sourceEntityId, properties);
            } else {
                String supplierName = null;
                int supplierId = -1;
                if (!properties.get("sourceentity").equalsIgnoreCase("suppliers") &&
                        !properties.get("sourceentity").equalsIgnoreCase("supplier")) {
                    int showEntityTypeId = ConfigureConstantFields.getEntityIdByName(properties.get("sourceentity"));

                    if (properties.containsKey("supplierid")) {
                        supplierId = Integer.parseInt(properties.get("supplierid"));
                        supplierName = properties.get("suppliername");
                    } else {
                        logger.info("Hitting Show Api to get Supplier Details.");
                        Show showObj = new Show();
                        showObj.hitShow(showEntityTypeId, Integer.parseInt(properties.get("sourceid")));
                        JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
                        jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
                        if (jsonObj.has("supplier") && jsonObj.getJSONObject("supplier").has("values")) {
                            supplierName = jsonObj.getJSONObject("supplier").getJSONObject("values").getString("name");
                            supplierId = jsonObj.getJSONObject("supplier").getJSONObject("values").getInt("id");
                        }
                    }
                } else {
                    supplierName = properties.get("sourcename");
                    supplierId = Integer.parseInt(properties.get("sourceid"));
                }

                switch (entityName) {
                    case "governance body":
                        String contractName = properties.get("contractname");
                        payload = PayloadUtils.getPayloadForNewGovernanceBody(contractName, sourceEntityId, supplierName, supplierId);
                        break;

                    default:
                        String parentEntityTypeName = properties.get("parententitytype");
                        int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityTypeName);
                        String sourceTitleName;
                        int sourceTitleId;

                        switch (parentEntityTypeName.toLowerCase()) {
                            case "supplier":
                            case "suppliers":
                                payload = PayloadUtils.getPayloadForNew(supplierName, supplierId, parentEntityTypeName, parentEntityTypeId);
                                break;

                            case "obligations":
                            case "service levels":
                                sourceTitleName = properties.get("sourcename");
                                sourceTitleId = Integer.parseInt(properties.get("sourceid"));
                                String parentName = properties.get("parentname");
                                int parentId = Integer.parseInt(properties.get("parentid"));
                                payload = PayloadUtils.getPayloadForNew(sourceTitleName, sourceTitleId, supplierName, supplierId, parentEntityTypeName, parentEntityTypeId,
                                        parentName, parentId);
                                break;

                            default:
                                sourceTitleName = properties.get("sourcename");
                                sourceTitleId = Integer.parseInt(properties.get("sourceid"));
                                payload = PayloadUtils.getPayloadForNew(sourceTitleName, sourceTitleId, supplierName, supplierId, parentEntityTypeName, parentEntityTypeId);
                                break;
                        }
                }
                newObj.hitNew(entityName, payload);
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api for Local Entity Creation. {}", e.getMessage());
        }
        return newObj;
    }

    private String getDefaultValueForField(String fieldType) {
        String value = null;
        logger.info("Getting default Value for Field Type {}", fieldType);

        try {
            String basePrefix = ParseConfigFile.getValueFromConfigFile(CreateEntity.defaultEntityCreationConfigFilePath, CreateEntity.defaultEntityCreationConfigFileName,
                    "baseprefix");
            String currTime = Long.toString(System.currentTimeMillis());
            currTime = currTime.substring(currTime.length() - 7, currTime.length());
            String defaultValue;

            switch (fieldType) {
                case "text":
                case "textarea":
                    defaultValue = ParseConfigFile.getValueFromConfigFile(defaultEntityCreationConfigFilePath, defaultEntityCreationConfigFileName,
                            "defaulttext");
                    value = basePrefix + defaultValue + currTime;
                    break;

                case "date":
                    value = ParseConfigFile.getValueFromConfigFile(defaultEntityCreationConfigFilePath, defaultEntityCreationConfigFileName,
                            "defaultdate");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Default Value for Field Type {}. {}", fieldType, e.getMessage());
        }
        return value;
    }

    private Map<String, String> setExtraRequiredFields(String entityName) throws ConfigurationException {
        Map<String, String> extraFields = new HashMap<>();
        logger.info("Setting Extra Fields for Entity {}", entityName);

        try {
            String extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationConfigFilePath");
            String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationExtraFieldsConfigFileName");

            extraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, entityName);
            List<String> extraFieldsKeys = new ArrayList<>(extraFields.keySet());

            for (int i = 0; i < extraFields.size(); i++) {
                if (extraFields.get(extraFieldsKeys.get(i)).trim().toLowerCase().contains("date->")
                        || extraFields.get(extraFieldsKeys.get(i)).trim().toLowerCase().contains("date ->"))
                    extraFields.put(extraFieldsKeys.get(i), "{\"name\":\"" + extraFieldsKeys.get(i).trim() + "\", \"values\": \"" +
                            this.calculateValueForDateField(extraFields.get(extraFieldsKeys.get(i))) + "\"}");
            }
        } catch (Exception e) {
            logger.error("Exception while setting Extra Fields for Entity {}. {}", entityName, e.getMessage());
        }
        return extraFields;
    }

    private Map<String, String> processAllChildFields(Map<String, String> allRequiredFields, String newJsonStr) {
        List<String> allRequiredFieldsKeys = new ArrayList<>(allRequiredFields.keySet());
        try {
            for (int j = 0; j < allRequiredFields.size(); j++) {
                logger.info("Getting attributes of Field {}", allRequiredFieldsKeys.get(j));
                Map<String, String> field = ParseJsonResponse.getFieldByName(newJsonStr, allRequiredFieldsKeys.get(j));

                if (field.get("groupBy") != null) {
                    if (field.get("type") != null) {
                        switch (field.get("type").trim().toLowerCase()) {
                            case "select":
                                logger.info("Checking if Field {} has options available", field.get("name"));
                                if (ParseJsonResponse.fieldContainsOptions(newJsonStr, field.get("name"), Boolean.parseBoolean(field.get("dynamicField")))) {
                                    List<String> allOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(newJsonStr, field.get("name"),
                                            Boolean.parseBoolean(field.get("dynamicField")));

                                    int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
                                    allRequiredFields.put(field.get("name"), allOptions.get(randomNumber));
                                }
                                break;

                            case "checkbox":
                                allRequiredFields.put(field.get("name"), "true");
                                break;

                            case "text":
                            case "textarea":
                            case "date":
                                if (allRequiredFields.get(field.get("name")) == null)
                                    allRequiredFields.put(field.get("name"), this.getDefaultValueForField(field.get("type")));
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while processing all Child Required Fields. {}", e.getMessage());
        }
        return allRequiredFields;
    }

    private Map<String, String> processNonChildFields(Map<String, String> allRequiredFields, String newJsonStr) {
        List<String> allRequiredFieldsKeys = new ArrayList<>(allRequiredFields.keySet());

        for (int j = 0; j < allRequiredFields.size(); j++) {
            try {
                Map<String, String> field = ParseJsonResponse.getFieldByName(newJsonStr, allRequiredFieldsKeys.get(j));

                if (field.get("groupBy") == null) {
                    if (field.get("type") != null) {
                        switch (field.get("type").trim().toLowerCase()) {
                            case "select":
                                logger.info("Checking if Field {} has options available", field.get("name"));
                                if (ParseJsonResponse.fieldContainsOptions(newJsonStr, field.get("name"), Boolean.parseBoolean(field.get("dynamicField")))) {
                                    List<String> allOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(newJsonStr, field.get("name"),
                                            Boolean.parseBoolean(field.get("dynamicField")));

                                    if (field.get("dependentField") != null && allRequiredFields.containsKey(field.get("dependentField"))) {
                                        String childFieldValue[] = allRequiredFields.get(field.get("dependentField")).split("parentId\":");
                                        String temp[] = childFieldValue[1].split(",");

                                        if (temp[0].endsWith("}"))
                                            temp[0] = temp[0].replaceAll("}", "");

                                        int parentId = Integer.parseInt(temp[0]);

                                        for (String option : allOptions) {
                                            if (option.toLowerCase().contains("id\":" + parentId)) {
                                                allRequiredFields.put(field.get("name"), option);
                                                break;
                                            }
                                        }
                                    } else {
                                        int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
                                        allRequiredFields.put(field.get("name"), allOptions.get(randomNumber));
                                    }
                                }
                                break;

                            case "checkbox":
                                allRequiredFields.put(field.get("name"), "true");
                                break;

                            case "text":
                            case "textarea":
                            case "date":
                                if (allRequiredFields.get(field.get("name")) == null)
                                    allRequiredFields.put(field.get("name"), this.getDefaultValueForField(field.get("type")));
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while processing Non-Child Required Fields. {}", e.getMessage());
            }
        }

        return allRequiredFields;
    }

    private String getDefaultValueForAlias() {
        String defaultValue = null;
        logger.info("Getting default Value for Alias");

        try {
            String basePrefix = ParseConfigFile.getValueFromConfigFile(CreateEntity.defaultEntityCreationConfigFilePath, CreateEntity.defaultEntityCreationConfigFileName,
                    "aliasprefix");
            int basePrefixLen = basePrefix.length();
            int maxLength = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(CreateEntity.defaultEntityCreationConfigFilePath,
                    CreateEntity.defaultEntityCreationConfigFileName, "maxlengthforalias"));
            String currTime = Long.toString(System.currentTimeMillis());
            currTime = currTime.substring(currTime.length() - (maxLength - basePrefixLen), currTime.length());
            defaultValue = basePrefix + currTime;
        } catch (Exception e) {
            logger.error("Exception while getting Default Value for Alias {}", e.getMessage());
        }
        return defaultValue;
    }

    private String calculateValueForDateField(String value) {
        String outputDate = null;

        try {
            String dateFields[] = value.split("->");
            String dateFormat = dateFields[1].trim();
            int daysToAdd = Integer.parseInt(dateFields[2].trim());
            String currentDate = DateUtils.getCurrentDateInAnyFormat(dateFormat);

            outputDate = DateUtils.getDateOfXDaysFromYDate(currentDate, daysToAdd, dateFormat);
        } catch (Exception e) {
            logger.error("Exception while Calculating Default Value for Date field with Value {}. {}", value, e.getMessage());
        }
        return outputDate;
    }

    private Map<String, String> updateExcelFields(String newJsonStr, Map<String, String> excelFields, String keyPrefix) {
        try {
            if (excelFields != null) {
                String requiredDateFormat = ParseConfigFile.getValueFromConfigFile(defaultEntityCreationConfigFilePath, defaultEntityCreationConfigFileName,
                        "requireddateformat");
                List<String> allFieldLabels = ParseJsonResponse.getAllFieldLabelsWithComments(newJsonStr);
                List<String> excelFieldsKeyList = new ArrayList<>(excelFields.keySet());
                List<String> modifiedFieldLabels = new ArrayList<>();
                for (String oneFieldLabel : allFieldLabels) {
                    String temp = oneFieldLabel.replaceAll(" ", "");
                    temp = temp.replaceAll(Pattern.quote("("), "");
                    temp = temp.replaceAll(Pattern.quote(")"), "");
                    temp = temp.replaceAll(Pattern.quote("."), "");
                    temp = temp.replaceAll(Pattern.quote("-"), "");
                    temp = temp.replaceAll(Pattern.quote("/"), "");
                    temp = temp.replaceAll(Pattern.quote("?"), "");
                    modifiedFieldLabels.add(keyPrefix + temp);
                }
                for (String oldKey : excelFieldsKeyList) {
                    String oldValue = excelFields.get(oldKey);
                    String fieldLabel = null;
                    int flag = 0;
                    for (int i = 0; i < modifiedFieldLabels.size(); i++) {
                        if (modifiedFieldLabels.get(i).trim().equalsIgnoreCase(oldKey.trim())) {
                            fieldLabel = allFieldLabels.get(i).trim();
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0)
                        System.out.println(oldKey);
                    Map<String, String> field = ParseJsonResponse.getFieldByLabel(newJsonStr, fieldLabel);
                    String fieldValue = oldValue;

                    if (field != null && field.size() > 0 && field.get("name").trim().equalsIgnoreCase("contractReference")) {
                        logger.warn("Currently field \"contractReference\" having Excel Header as {} is not handled. The value must be passed through extra fields.", oldKey);
                        excelFields.remove(oldKey);
                        continue;
                    }
                    if (field != null && field.get("displayMode") != null && field.get("displayMode").trim().equalsIgnoreCase("display")) {
                        excelFields.remove(oldKey);
                    } else {
                        if (!fieldValue.equalsIgnoreCase("") && field != null && field.get("type") != null &&
                                (field.get("type").trim().equalsIgnoreCase("date") || field.get("type").trim().equalsIgnoreCase("dateTime"))) {
                            String currentFormat = DateUtils.getDateFormat(fieldValue);
                            fieldValue = DateUtils.converDateToAnyFormat(fieldValue, currentFormat, requiredDateFormat);
                        } else if (!fieldValue.equalsIgnoreCase("") && field != null && field.get("type") != null &&
                                field.get("type").trim().equalsIgnoreCase("select")) {
                            fieldValue = null;
                            JSONObject jsonObj = new JSONObject(newJsonStr);
                            jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
                            if (field.get("dynamicField").trim().equalsIgnoreCase("true"))
                                jsonObj = jsonObj.getJSONObject("dynamicMetadata");
                            else if (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("comment"))
                                jsonObj = jsonObj.getJSONObject("comment");
                            jsonObj = jsonObj.getJSONObject(field.get("name")).getJSONObject("options");
                            JSONArray jsonArr = jsonObj.getJSONArray("data");

                            if (field.get("multiple").trim().equalsIgnoreCase("false")) {
                                for (int j = 0; j < jsonArr.length(); j++) {
                                    jsonObj = jsonArr.getJSONObject(j);
                                    if (jsonObj.getString("name").trim().equalsIgnoreCase(oldValue.trim())) {
                                        fieldValue = jsonObj.toString();
                                        break;
                                    }
                                }
                            } else {
                                String allValues[] = oldValue.trim().split(Pattern.quote(";"));
                                fieldValue = "";
                                boolean first = true;
                                for (String value : allValues) {
                                    for (int k = 0; k < jsonArr.length(); k++) {
                                        jsonObj = jsonArr.getJSONObject(k);
                                        if (jsonObj.getString("name").trim().equalsIgnoreCase(value.trim())) {
                                            if (!first)
                                                fieldValue = fieldValue.concat("," + jsonObj.toString());
                                            else {
                                                fieldValue = fieldValue.concat(jsonObj.toString());
                                                first = false;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            if (fieldValue == null)
                                logger.warn("Value \"{}\" provided for Field \"{}\" doesn't match any existing option. Hence skipping this field.", oldValue, oldKey);
                        } else if (!fieldValue.equalsIgnoreCase("") && field != null && field.get("type") != null &&
                                field.get("type").trim().equalsIgnoreCase("checkbox")) {
                            if (field.get("mapTrueFalseToYesNo").trim().equalsIgnoreCase("true")) {
                                if (fieldValue.trim().equalsIgnoreCase("yes"))
                                    fieldValue = "true";
                                else
                                    fieldValue = "false";
                            }
                        }
                        excelFields.remove(oldKey);
                        if (fieldValue != null && fieldLabel != null && !fieldValue.equalsIgnoreCase(""))
                            excelFields.put(fieldLabel, fieldValue);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while updating Other Fields. {}", e.getMessage());
        }
        return excelFields;
    }
}
