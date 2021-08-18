package com.sirionlabs.api.contractCreation;

import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.EntityCreation.CreateEntity;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractCreationWithDocUpload  {

    private final static Logger logger = LoggerFactory.getLogger(CreateEntity.class);
    private static String contractCreationConfigFilePath = null;
    private static String contractCreationConfigFileName = null;


    public String getCreatePayload(String entityName, Map<String, String> properties,String filePath,String fileName, String relationId,boolean viewer) {
        return getCreatePayload(entityName, properties, false, false, null, null,filePath,fileName,relationId,viewer);
    }

    String getCreatePayload(String entityName, Map<String, String> properties, boolean createLocalEntity, boolean createGlobalEntity, Map<String, String> excelFields,
                            String keyPrefix,String filePath,String fileName, String realtionId,boolean viewer) {
        String createPayload = null;
        try {
            ContractCreationWithDocUpload.contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
            ContractCreationWithDocUpload.contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");

            String parentEntityName = properties.get("sourceentity");
            int parentEntityId = -1;
            if (NumberUtils.isParsable(properties.get("sourceid")))
                parentEntityId = Integer.parseInt(properties.get("sourceid"));
                New newObj;

                newObj = this.hitNewForLocalEntityCreation(entityName, parentEntityName, parentEntityId, properties);

            if (newObj != null) {
                String newResponse = newObj.getNewJsonStr();

                if (!ParseJsonResponse.validJsonResponse(newResponse)) {
                    FileUtils.saveResponseInFile(entityName + " New API HTML.txt", newResponse);
                }

                Map<String,String> uploadedDocResponse=TestContractDocumentUpload.uploadedContractDocumentData(filePath,fileName,realtionId);
                Map<String, String> extraFields = this.setExtraRequiredFields(entityName,viewer);
                extraFields.put("contractDocuments",this.setExtraRequiredFields(entityName,viewer).get("contractDocuments").replace("<key>",uploadedDocResponse.get("key"))
                        .replace("<name>",uploadedDocResponse.get("name")).replace("<extension>",uploadedDocResponse.get("extension"))
                        .replace("<totalPages>",uploadedDocResponse.get("totalPages")));

                logger.info("Setting all Required Fields for Entity {}", entityName);
                newObj.setAllRequiredFields(newObj.getNewJsonStr());
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = this.processAllChildFields(allRequiredFields, newObj.getNewJsonStr());
                allRequiredFields = this.processNonChildFields(allRequiredFields, newObj.getNewJsonStr());


                    if (allRequiredFields.containsKey("alias"))
                        allRequiredFields.put("alias", this.getDefaultValueForAlias());
                    createPayload = PayloadUtils.getPayloadForCreate(newObj.getNewJsonStr(), allRequiredFields, extraFields, null);

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

    private Map<String, String> setExtraRequiredFields(String entityName,boolean viewer) throws ConfigurationException {
        Map<String, String> extraFields = new HashMap<>();
        logger.info("Setting Extra Fields for Entity {}", entityName);

        try {
            String extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
            String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");

            if(viewer == true) {
                extraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "extrafields");
            }
            else if(viewer == false){
                extraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, "extrafieldswithviewerdisabled");
            }

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
        try {
            for (int j = 0; j < allRequiredFields.size(); j++) {
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
            }
        } catch (Exception e) {
            logger.error("Exception while processing Required Fields Not Child. {}", e.getMessage());
        }
        return allRequiredFields;
    }

    private String getDefaultValueForAlias() {
        String defaultValue = null;
        logger.info("Getting default Value for Alias");

        try {
            String basePrefix = ParseConfigFile.getValueFromConfigFile(ContractCreationWithDocUpload.contractCreationConfigFilePath, ContractCreationWithDocUpload.contractCreationConfigFileName,
                    "aliasprefix");
            int basePrefixLen = basePrefix.length();
            int maxLength = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ContractCreationWithDocUpload.contractCreationConfigFilePath,
                    ContractCreationWithDocUpload.contractCreationConfigFileName, "maxlengthforalias"));
            String currTime = Long.toString(System.currentTimeMillis());
            currTime = currTime.substring(currTime.length() - (maxLength - basePrefixLen), currTime.length());
            defaultValue = basePrefix + currTime;
        } catch (Exception e) {
            logger.error("Exception while getting Default Value for Alias {}", e.getMessage());
        }
        return defaultValue;
    }

    private String getDefaultValueForField(String fieldType) {
        String value = null;
        logger.info("Getting default Value for Field Type {}", fieldType);

        try {
            String basePrefix = ParseConfigFile.getValueFromConfigFile(ContractCreationWithDocUpload.contractCreationConfigFilePath, ContractCreationWithDocUpload.contractCreationConfigFileName,
                    "baseprefix");
            String currTime = Long.toString(System.currentTimeMillis());
            currTime = currTime.substring(currTime.length() - 7, currTime.length());
            String defaultValue;

            switch (fieldType) {
                case "text":
                case "textarea":
                    defaultValue = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName,
                            "defaulttext");
                    value = basePrefix + defaultValue + currTime;
                    break;

                case "date":
                    value = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName,
                            "defaultdate");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Default Value for Field Type {}. {}", fieldType, e.getMessage());
        }
        return value;
    }

    private New hitNewForLocalEntityCreation(String entityName, String sourceEntityName, int sourceEntityId, Map<String, String> properties) {
        logger.info("Hitting New Get Api for Entity {}", entityName);
        New newObj = new New();

        try {
                    String contractType = properties.get("parententitytype");
                    newObj.hitNew(entityName, sourceEntityName, sourceEntityId, contractType);
        } catch (Exception e) {
            logger.error("Exception while hitting New Api for Global Entity Creation. {}", e.getMessage());
        }
        return newObj;
    }
}
