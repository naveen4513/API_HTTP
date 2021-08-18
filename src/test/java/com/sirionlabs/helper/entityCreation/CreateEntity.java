package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class CreateEntity {

    private final static Logger logger = LoggerFactory.getLogger(com.sirionlabs.helper.entityCreation.CreateEntity.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFilePath;
    private String extraFieldsConfigFileName;
    private String sectionName;
    private Map<String, String> properties = new HashMap<>();

    public CreateEntity(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName, String sectionName) {
        this.setConfigValues(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
        properties = this.setProperties();
    }

    public CreateEntity(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName, String sectionName,
                        Boolean createFromExcel) {
        this.setConfigValues(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
        if (!createFromExcel)
            properties = this.setProperties();
    }


    public static String create(String entityName, String payload) {
        String createResponse = null;

        try {
            logger.info("Hitting Create API for Entity {}.", entityName);
            Create createObj = new Create();
            createObj.hitCreate(entityName, payload);
            createResponse = createObj.getCreateJsonStr();
        } catch (Exception e) {
            logger.error("Exception while Creating Entity {}. {}", entityName, e.getStackTrace());
        }
        return createResponse;
    }

    public static int getNewEntityId(String createJsonStr) {
        return getNewEntityId(createJsonStr, "unknown");
    }

    public static String getShortId(String createJsonStr, String entityName) {
        String newEntityId = "";
        try {
            if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
                JSONObject jsonObj = new JSONObject(createJsonStr);
                String notificationStr = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");
                String[] temp = notificationStr.trim().split(Pattern.quote("show/"));
                if (temp.length > 1) {
                    String temp2 = temp[1];
                    String[] temp3 = temp2.trim().split(Pattern.quote("\""));
                    if (temp3.length > 1) {
                        String temp4 = temp3[3];
                        String[] temp5 = temp4.trim().split(Pattern.quote("/"));
                        if (temp5.length > 1)
                            newEntityId = temp5[0].substring(1, temp5[0].length() - 1);
                    }
                }
            } else {
                logger.error("New Entity {} not created. ", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getStackTrace());
        }
        return newEntityId;
    }
    public static int getNewEntityId(String createJsonStr, String entityName) {
        int newEntityId = -1;
        try {
            if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
                JSONObject jsonObj = new JSONObject(createJsonStr);
                if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
                    if (jsonObj.getJSONObject("header").getJSONObject("response").has("entityId")) {
                        return jsonObj.getJSONObject("header").getJSONObject("response").getInt("entityId");
                    }

                    String notificationStr = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");

                    String temp[] = notificationStr.trim().split(Pattern.quote("show/"));
                    if (temp.length > 1) {
                        String temp2 = temp[1];
                        String temp3[] = temp2.trim().split(Pattern.quote("\""));
                        if (temp3.length > 1) {
                            String temp4 = temp3[0];
                            String temp5[] = temp4.trim().split(Pattern.quote("/"));
                            if (temp5.length > 1)
                                newEntityId = Integer.parseInt(temp5[1]);
                        }
                    }
                } else {
                    logger.error("New Entity {} not created. ", entityName);
                }
            } else {
                logger.error("Create Response for Entity {} is not valid JSON.", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getStackTrace());
        }
        return newEntityId;
    }

    public static void validateSourceFieldForEntity(String entityName, int entityTypeId, String sourceEntityName, String showResponse, CustomAssert csAssert) {
        try {
            logger.info("Validating Source Field for Entity {} having EntityTypeId {}, Source Entity {}", entityName, entityTypeId, sourceEntityName);
            String actualParentEntityTypeId = ShowHelper.getValueOfField(entityTypeId, "parentEntityTypeId", showResponse);

            if (actualParentEntityTypeId != null) {
                String expectedParentEntityTypeId = String.valueOf(getExpectedParentEntityTypeIdForEntity(entityName, sourceEntityName));

                if (!actualParentEntityTypeId.trim().equalsIgnoreCase(expectedParentEntityTypeId.trim())) {
                    csAssert.assertTrue(false, "Expected Source Id: " + expectedParentEntityTypeId + " and Actual Source Id: " + actualParentEntityTypeId);
                }
            } else {
                csAssert.assertTrue(false, "Couldn't get Actual Parent Entity Type Id from Show Response for Entity Type " + entityName +
                        " and Source Entity " + sourceEntityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Source Field for Entity Type Id " + entityTypeId +
                    ", Source Entity Name " + sourceEntityName + ". " + e.getMessage());
        }
    }

    public static void validateSourceNameFieldForEntity(String entityName, int entityTypeId, String sourceEntityName, int sourceEntityId, String showResponse,
                                                        CustomAssert csAssert) {
        try {
            logger.info("Validating Source Name Field for Entity {} having EntityTypeId {}, Source Entity {} and Source Entity Id {}", entityName, entityTypeId,
                    sourceEntityName, sourceEntityId);
            String actualSourceName = ShowHelper.getValueOfField(entityTypeId, "sourceTitle", showResponse);

            if (actualSourceName != null) {
                String expectedSourceName = getExpectedSourceNameForEntity(entityName, entityTypeId, showResponse, sourceEntityName);

                if (expectedSourceName != null) {
                    if (!expectedSourceName.trim().equalsIgnoreCase(actualSourceName)) {
                        csAssert.assertTrue(false, "Expected Source Name: " + expectedSourceName + " and Actual Source Name: " + actualSourceName);
                    }
                } else {
                    csAssert.assertTrue(false, "Couldn't get Expected Source Name for Source Entity " + sourceEntityName + " and Source Entity Id " +
                            sourceEntityId);
                }
            } else {
                csAssert.assertTrue(false, "Couldn't get Actual Source Name for Entity " + entityName + ", Source Entity " + sourceEntityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Source Name Field for Entity Type Id " + entityTypeId +
                    ", Source Entity Name " + sourceEntityName + " and Source Entity Id " + sourceEntityId + ". " + e.getMessage());
        }
    }

    private static int getExpectedParentEntityTypeIdForEntity(String entityName, String sourceEntityName) {
        switch (entityName) {
            case "issues":
                switch (sourceEntityName) {
                    case "governance body meetings":
                        sourceEntityName = "suppliers";
                        break;
                }
        }

        return ConfigureConstantFields.getEntityIdByName(sourceEntityName);
    }

    private static String getExpectedSourceNameForEntity(String entityName, int entityTypeId, String showResponse, String sourceEntityName) {
        int sourceEntityTypeId = getExpectedParentEntityTypeIdForEntity(entityName, sourceEntityName);
        int sourceEntityId = Integer.parseInt(ShowHelper.getValueOfField(entityTypeId, "parentEntityId", showResponse));

        return ShowHelper.getValueOfField(sourceEntityTypeId, sourceEntityId, "name");
    }

    private void setConfigValues(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName, String sectionName) {
        this.configFilePath = configFilePath;
        this.configFileName = configFileName;
        this.extraFieldsConfigFilePath = extraFieldsConfigFilePath;
        this.extraFieldsConfigFileName = extraFieldsConfigFileName;
        this.sectionName = sectionName;
    }

    public String create(String entityName, Boolean isLocalEntity) {
        String createResponse = null;

        try {
            String createPayload = isLocalEntity ? this.getCreatePayload(entityName, true, false) :
                    this.getCreatePayload(entityName, false, true);

            if (createPayload != null) {
                logger.info("Hitting Create Api for Entity {}.", entityName);
                Create createObj = new Create();
                createObj.hitCreate(entityName, createPayload);
                createResponse = createObj.getCreateJsonStr();

                if (!ParseJsonResponse.validJsonResponse(createResponse)) {
                    FileUtils.saveResponseInFile(entityName + " Create API HTML.txt", createResponse);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Entity {}. {}", entityName, e.getStackTrace());
        }
        return createResponse;
    }

    public Long createFromExcel(String excelFilePath, String excelFileName, String excelSheetName, String entityName, int noOfRecordsToBeProcessedAtOnce,
                                List<String> fieldsToHaveMultipleValues, int maxNoOfMultipleValues, List<String> ignoreFields) {
        return createFromExcel(excelFilePath, excelFileName, excelSheetName, entityName, noOfRecordsToBeProcessedAtOnce, fieldsToHaveMultipleValues, maxNoOfMultipleValues,
                ignoreFields, null);
    }

    public Long createFromExcel(String excelFilePath, String excelFileName, String excelSheetName, String entityName, int noOfRecordsToBeProcessedAtOnce,
                                List<String> fieldsToHaveMultipleValues, int maxNoOfMultipleValues, List<String> ignoreFields, String stakeHolderColumnName) {
        Long noOfRecordsCreated = 0L;
        try {
            String clientId = null;
            if (entityName.trim().equalsIgnoreCase("vendors")) {
                String listIdStr = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "entity_url_id");

                ListRendererDefaultUserListMetaData defaultUserListMetaDataObj = new ListRendererDefaultUserListMetaData();
                defaultUserListMetaDataObj.hitListRendererDefaultUserListMetadata(Integer.parseInt(listIdStr), null, "{}");
                JSONObject jsonObj = new JSONObject(defaultUserListMetaDataObj.getListRendererDefaultUserListMetaDataJsonStr());
                if (jsonObj.has("popupUrl") && jsonObj.getString("popupUrl") != null) {
                    String popUrl = jsonObj.getString("popupUrl");
                    String tokens[] = popUrl.split("clientId=");
                    if (tokens[1].contains("&")) {
                        String temp[] = tokens[1].trim().split(Pattern.quote("&"));
                        clientId = temp[0].trim();
                    } else
                        clientId = tokens[1].trim();
                }
            }

            final String finalClientId = clientId;

            Boolean stakeHolderRequired = false;
            if (stakeHolderColumnName != null)
                stakeHolderRequired = true;

            int stakeHolderColumnNo = -1;

            List<String> allHeaders = new ArrayList<>();
            logger.info("Getting all Headers from the Excel file {} and Sheet {}.", excelFilePath + "/" + excelFileName, excelSheetName);
            List<String> excelHeaders = XLSUtils.getHeaders(excelFilePath, excelFileName, excelSheetName);
            if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "updateExcelHeaders").trim().equalsIgnoreCase("true"))
                updateExcelHeaders(excelHeaders, entityName);

            allHeaders.addAll(excelHeaders);

            if (stakeHolderRequired) {
                for (int i = 0; i < excelHeaders.size(); i++) {
                    if (excelHeaders.get(i).trim().equalsIgnoreCase(stakeHolderColumnName.trim())) {
                        stakeHolderColumnNo = i;
                        break;
                    }
                }
            }

            Long totalRows = XLSUtils.getNoOfRows(excelFilePath, excelFileName, excelSheetName);
            logger.info("Total Records found for Entity {} in Excel Sheet: {}", entityName, totalRows);
            Long noOfRows = 1L;

            Map<String, String> uniqueStakeHoldersJsonMap = new ConcurrentHashMap<>();
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<FutureTask<Boolean>> taskList = new ArrayList<>();

            final Boolean finalStakeHolderRequired = stakeHolderRequired;
            final int finalStakeHolderColumnNo = stakeHolderColumnNo;

            while (noOfRows <= totalRows) {
                List<List<String>> allExcelData = XLSUtils.getExcelDataOfMultipleRows(excelFilePath, excelFileName, excelSheetName, noOfRows.intValue(),
                        noOfRecordsToBeProcessedAtOnce);
                noOfRows += noOfRecordsToBeProcessedAtOnce;

                for (List<String> excelData : allExcelData) {
                    FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            String stakeHolderOptionsJson = null;
                            if (finalStakeHolderRequired) {
                                String stakeHolderName = excelData.get(finalStakeHolderColumnNo).trim();
                                if (!uniqueStakeHoldersJsonMap.containsKey(stakeHolderName)) {
                                    stakeHolderOptionsJson = getStakeHolderOptionsJsonStr(stakeHolderName);
                                    uniqueStakeHoldersJsonMap.put(stakeHolderName, stakeHolderOptionsJson);
                                } else {
                                    stakeHolderOptionsJson = uniqueStakeHoldersJsonMap.get(stakeHolderName);
                                }
                            }
                            if (entityName.trim().equalsIgnoreCase("vendors")) {
                                properties.put("sourceentity", "client");

                                properties.put("sourceid", finalClientId);
                            } else {
                                properties = setPropertiesForExcel(entityName, allHeaders, excelData);
                            }
                            List<String> multipleValuesFields = new ArrayList<>();
                            multipleValuesFields.addAll(fieldsToHaveMultipleValues);
                            logger.info("Setting ExcelFields Map for Entity {}", entityName);
                            Map<String, String> excelFields = new HashMap<>();
                            for (int j = 0; j < allHeaders.size(); j++) {
                                excelFields.put(allHeaders.get(j), excelData.get(j));
                            }

                            logger.info("Creating Payload for Entity {}", entityName);
                            String payloadForCreate;
                            if (entityName.trim().equalsIgnoreCase("purchase orders")) {
                                payloadForCreate = getCreatePayloadExcel(entityName, false, true, excelFields, multipleValuesFields,
                                        maxNoOfMultipleValues, ignoreFields, stakeHolderOptionsJson);
                            } else {
                                payloadForCreate = getCreatePayloadExcel(entityName, true, false, excelFields, multipleValuesFields,
                                        maxNoOfMultipleValues, ignoreFields, stakeHolderOptionsJson);
                            }

                            logger.info("Hitting Create Api for Entity {}.", entityName);
                            Create createObj = new Create();
                            createObj.hitCreate(entityName, payloadForCreate);
                            String createResponse = createObj.getCreateJsonStr();

                            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                                JSONObject jsonObj = new JSONObject(createResponse);
                                try {
                                    if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
//		`								noOfRecordsCreated++;
                                    } else {
                                        if (ParseJsonResponse.containsApplicationError(createResponse))
                                            logger.error("Couldn't create Record for Entity {} due to Application Error", entityName);
                                        else {
                                            if (jsonObj.has("body") && jsonObj.getJSONObject("body").has("errors") &&
                                                    jsonObj.getJSONObject("body").getJSONObject("errors").has("genericErrors"))
                                                logger.error("Couldn't create Record for Entity {} due to Validation Error");
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.error("Couldn't get Status field in Create Response for Entity {}.", entityName);
                                }
                            } else {
                                logger.error("Create Response is not a Valid JSON for Entity {}.", entityName);
                            }
                            return true;
                        }
                    });
                    taskList.add(result);
                    executor.execute(result);
                }
                for (FutureTask<Boolean> task : taskList)
                    task.get();
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Entity {} using Excel File {}. {}", entityName, excelFilePath + "/" + excelFileName, e.getStackTrace());
        }
        return noOfRecordsCreated;
    }

    private String getCreatePayload(String entityName) {
        return getCreatePayload(entityName, false, true);
    }

    public String getCreatePayload(String entityName, boolean createLocalEntity, boolean createGlobalEntity) {
        String createPayload = null;
        try {
            String parentEntityName = null;
            int parentEntityId = -1;
            if (properties.size() == 0) {
                logger.info("Entity : {} does not have any parent entity.", entityName);
            } else {
                parentEntityName = properties.get("sourceentity");
                if (NumberUtils.isParsable(properties.get("sourceid")))
                    parentEntityId = Integer.parseInt(properties.get("sourceid"));
            }
            New newObj;

            boolean isMultiSupplier = properties.containsKey("multisupplier") && properties.get("multisupplier").trim().equalsIgnoreCase("true");
            int multiParentSupplierId = -1;

            if (isMultiSupplier) {
                multiParentSupplierId = Integer.parseInt(properties.get("multiparentsupplierid"));
            }

            if (createGlobalEntity)
                newObj = this.hitNewForGlobalEntityCreation(entityName, parentEntityId);
            else if (createLocalEntity)
                newObj = this.hitNewForLocalEntityCreation(entityName, parentEntityName, parentEntityId, isMultiSupplier, multiParentSupplierId);
            else {
                logger.info("Both Local Entity Creation and Global Entity Creation are set to False. Hence skipping Entity Creation.");
                return null;
            }

            if (newObj != null) {
                String newResponse = newObj.getNewJsonStr();

                if (newResponse == null) {
                    logger.error("New API Response is null. Can't proceed for Creation of Entity {}.", entityName);
                    return null;
                }

                if (!ParseJsonResponse.validJsonResponse(newResponse)) {
                    FileUtils.saveResponseInFile(entityName + " New API HTML.txt", newResponse);
                }

                Map<String, String> extraFields = this.setExtraRequiredFields(entityName);
                logger.info("Setting all Required Fields for Entity {}", entityName);
                newObj.setAllRequiredFields(newObj.getNewJsonStr());
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = this.processAllChildFields(allRequiredFields, newObj.getNewJsonStr());
                allRequiredFields = this.processNonChildFields(allRequiredFields, newObj.getNewJsonStr());

                if (allRequiredFields.containsKey("alias"))
                    allRequiredFields.put("alias", this.getDefaultValueForAlias());
                createPayload = PayloadUtils.getPayloadForCreate(newObj.getNewJsonStr(), allRequiredFields, extraFields, null, extraFieldsConfigFilePath,
                        extraFieldsConfigFileName);
            } else {
                logger.error("New Object is null. Can't proceed for Creation of Entity {}.", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Entity {}. {}", entityName, e.getStackTrace());
        }
        return createPayload;
    }

    private String getCreatePayloadExcel(String entityName, boolean createLocalEntity, boolean createGlobalEntity, Map<String, String> excelFields,
                                         List<String> fieldsToHaveMultipleFields, int maxNoOfMultipleValues, List<String> ignoreFields, String stakeHolderOptionsJson) {
        String createPayload = null;
        try {
            String parentEntityName = properties.get("sourceentity");
            int parentEntityId = -1;
            if (NumberUtils.isParsable(properties.get("sourceid")))
                parentEntityId = Integer.parseInt(properties.get("sourceid"));
            New newObj;

            if (createGlobalEntity)
                newObj = this.hitNewForGlobalEntityCreation(entityName, parentEntityId);
            else if (createLocalEntity)
                newObj = this.hitNewForLocalEntityCreation(entityName, parentEntityName, parentEntityId);
            else {
                logger.info("Both Local Entity Creation and Global Entity Creation are set to False. Hence skipping Entity Creation.");
                return null;
            }

            if (newObj != null) {
                List<String> allFieldNames = ParseJsonResponse.getAllFieldNames(newObj.getNewJsonStr());
                Map<String, String> extraFields = this.setExtraRequiredFields(entityName);
                logger.info("Setting all Required Fields for Entity {}", entityName);
                Map<String, String> allRequiredFields = new HashMap<>();
                for (String fieldName : allFieldNames) {
                    allRequiredFields.put(fieldName, null);
                }

                allRequiredFields = this.processAllChildFieldsForExcel(allRequiredFields, excelFields, newObj.getNewJsonStr(), fieldsToHaveMultipleFields,
                        maxNoOfMultipleValues, ignoreFields);
                allRequiredFields = this.processNonChildFieldsForExcel(allRequiredFields, excelFields, newObj.getNewJsonStr(), fieldsToHaveMultipleFields,
                        maxNoOfMultipleValues, stakeHolderOptionsJson);

                createPayload = PayloadUtils.getPayloadForCreateExcel(newObj.getNewJsonStr(), allRequiredFields, extraFields, excelFields, extraFieldsConfigFilePath,
                        extraFieldsConfigFileName);
            } else {
                logger.error("New Object is null. Can't proceed for Creation of Entity {}.", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Entity {} from Excel. {}", entityName, e.getStackTrace());
        }
        return createPayload;
    }

    private New hitNewForLocalEntityCreation(String entityName) {
        return hitNewForLocalEntityCreation(entityName, null, -1);
    }

    private New hitNewForLocalEntityCreation(String entityName, String sourceEntityName, int sourceEntityId) {
        return hitNewForLocalEntityCreation(entityName, sourceEntityName, sourceEntityId, false, -1);
    }

    private New hitNewForLocalEntityCreation(String entityName, String sourceEntityName, int sourceEntityId, boolean isMultiSupplier, int multiParentSupplierId) {
        logger.info("Hitting New Get Api for Entity {}", entityName);
        New newObj = new New();

        try {
            if (entityName.equalsIgnoreCase("governance body")) {
                logger.warn("Can't hit New Get for Entity Governance Body. Hitting New Post instead.");
                newObj = this.hitNewForGlobalEntityCreation(entityName, sourceEntityId);
            } else {
                if (entityName.equalsIgnoreCase("contracts")) {
                    String contractType = properties.get("parententitytype");

                    if (contractType.trim().equalsIgnoreCase("other")) {
                        newObj.hitNewForSubContract(sourceEntityId, contractType, Integer.parseInt(properties.get("parentsourceid")));
                    } else {
                        newObj.hitNew(entityName, sourceEntityName, sourceEntityId, contractType);
                    }
                } else if (entityName.equalsIgnoreCase("creditearnbacks")) {
                    newObj.hitNewForCreditEarnBack(sourceEntityId);
                } else if (entityName.equalsIgnoreCase("applicationGroups")) {
                    newObj.hitNewForApplicationGroup(sourceEntityId);
                } else if (entityName.equalsIgnoreCase("invoice line item")) {
                    String lineItemTypeId = properties.get("lineitemtypeid");
                    newObj.hitNew(entityName, sourceEntityName, sourceEntityId, null, lineItemTypeId);
                } else if (entityName.equalsIgnoreCase("clauses") || entityName.equalsIgnoreCase("definition") || entityName.equalsIgnoreCase("contract template structure")) {
                    newObj.hitNew(entityName);
                } else
                    newObj.hitNew(entityName, sourceEntityName, sourceEntityId, null, null, isMultiSupplier, multiParentSupplierId);
            }
        } catch (Exception e) {
            logger.error("Exception while hitting New Api for Global Entity Creation. {}", e.getMessage());
        }
        return newObj;
    }

    private New hitNewForGlobalEntityCreation(String entityName, int sourceEntityId) {
        logger.info("Hitting New Post Api for Entity {}", entityName);
        New newObj = new New();
        String payload;

        try {
            if (entityName.equalsIgnoreCase("suppliers") || entityName.equalsIgnoreCase("contracts") ||
                    entityName.equalsIgnoreCase("purchase orders")) {
                logger.error("Can't hit New Post for Entity {}", entityName);
                return null;
            } else if (entityName.trim().equalsIgnoreCase("vendors") || entityName.trim().equalsIgnoreCase("contract templates")) {
                logger.warn("Can't hit New Post for Entity {}. Hitting New Get instead.", entityName);
                newObj = this.hitNewForLocalEntityCreation(entityName, "client", sourceEntityId);
            } else if (entityName.trim().equalsIgnoreCase("contract draft request")) {
                newObj.hitNewForGlobalCDR();
            } else if (entityName.trim().equalsIgnoreCase("clauses") || entityName.trim().equalsIgnoreCase("definition") || entityName.trim().equalsIgnoreCase("contract template structure")) {
                logger.info("Can't hit New Post for Entity {}. Hitting New Get api instead.", entityName);
                newObj = this.hitNewForLocalEntityCreation(entityName);
            } else {
                String supplierName = null;
                int supplierId = -1;
                if (!properties.get("sourceentity").equalsIgnoreCase("suppliers") &&
                        !properties.get("sourceentity").equalsIgnoreCase("supplier")) {
                    int showEntityTypeId = ConfigureConstantFields.getEntityIdByName(properties.get("sourceentity"));
                    logger.info("Hitting Show Api to get Supplier Details.");
                    Show showObj = new Show();
                    showObj.hitShow(showEntityTypeId, Integer.parseInt(properties.get("sourceid")));
                    JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
                    jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                    if (jsonObj.has("supplier") && jsonObj.getJSONObject("supplier").has("values")) {
                        supplierName = jsonObj.getJSONObject("supplier").getJSONObject("values").getString("name");
                        supplierId = jsonObj.getJSONObject("supplier").getJSONObject("values").getInt("id");
                    } else if (properties.containsKey("suppliername")) {
                        supplierName = properties.get("suppliername");

                        if (properties.containsKey("supplierid"))
                            supplierId = Integer.parseInt(properties.get("supplierid"));
                    }
                } else {
                    supplierName = properties.get("sourcename");
                    supplierId = Integer.parseInt(properties.get("sourceid"));
                }

                switch (entityName) {
                    case "governance body":
                        String contractName = properties.get("contractname");

                        if (contractName == null) {
                            sourceEntityId = supplierId;
                        }

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
            String basePrefix = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "baseprefix");
            if (basePrefix == null || basePrefix.trim().equalsIgnoreCase("null")) {
                basePrefix = "API";
            }

            String currTime = Long.toString(System.currentTimeMillis());
            currTime = currTime.substring(currTime.length() - 7, currTime.length());
            String defaultValue;

            switch (fieldType) {
                case "text":
                case "textarea":
                    defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "defaulttext");
                    if (defaultValue == null || defaultValue.trim().equalsIgnoreCase("null")) {
                        defaultValue = " Automation ";
                    }

                    value = basePrefix + defaultValue + currTime;
                    break;

                case "date":
                    value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "defaultdate");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Default Value for Field Type {}. {}", fieldType, e.getMessage());
        }
        return value;
    }

    private String getDefaultValueForFieldForExcel(String fieldType) {
        String value = null;
        logger.info("Getting default Value for Field Type {}", fieldType);

        try {
            String basePrefix = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default values", "baseprefix");
            String currTime = Long.toString(System.currentTimeMillis());
            currTime = currTime.substring(currTime.length() - 7, currTime.length());
            String defaultValue;

            switch (fieldType) {
                case "text":
                case "textarea":
                    defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default values", "defaulttext");
                    value = basePrefix + defaultValue + currTime;
                    break;

                case "date":
                    value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default values", "defaultdate");
            }
        } catch (Exception e) {
            logger.error("Exception while getting Default Value for Field Type {}. {}", fieldType, e.getMessage());
        }
        return value;
    }

    public Map<String, String> setExtraRequiredFields(String entityName) {
        Map<String, String> extraFields = new HashMap<>();
        logger.info("Setting Extra Fields for Entity {} using Section {}", entityName, sectionName);

        try {
            Map<String, String> commonExtraFields = new HashMap<>();
            if (ParseConfigFile.containsSection(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields")) {
                commonExtraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName,
                        "common extra fields");
            }

            Map<String, String> flowSpecificExtraFields = new HashMap<>();
            if (ParseConfigFile.containsSection(extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName)) {
                flowSpecificExtraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
            }

            extraFields.putAll(commonExtraFields);
            extraFields.putAll(flowSpecificExtraFields);

            List<String> extraFieldsKeys = new ArrayList<>(extraFields.keySet());

            for (int i = 0; i < extraFields.size(); i++) {
                if (extraFields.get(extraFieldsKeys.get(i)).trim().toLowerCase().contains("date->")
                        || extraFields.get(extraFieldsKeys.get(i)).trim().toLowerCase().contains("date ->"))
                    extraFields.put(extraFieldsKeys.get(i), "{\"name\":\"" + extraFieldsKeys.get(i).trim() + "\", \"values\": \"" +
                            this.calculateValueForDateField(extraFields.get(extraFieldsKeys.get(i))) + "\"}");
            }
        } catch (Exception e) {
            logger.error("Exception while setting Extra Fields for Entity {} using Section {}. {}", entityName, sectionName, e.getStackTrace());
        }
        return extraFields;
    }

    public Map<String, String> processAllChildFields(Map<String, String> allRequiredFields, String newJsonStr) {
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

    public Map<String, String> processNonChildFields(Map<String, String> allRequiredFields, String newJsonStr) {
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

    private Map<String, String> processAllChildFieldsForExcel(Map<String, String> allRequiredFields, Map<String, String> excelFields, String newJsonStr,
                                                              List<String> fieldsToHaveMultipleFields, int maxNoOfMultipleValues, List<String> ignoreFields) {
        List<String> allRequiredFieldsKeys = new ArrayList<>(allRequiredFields.keySet());
        for (int j = 0; j < allRequiredFields.size(); j++) {
            try {
                if (!(excelFields.containsKey(allRequiredFieldsKeys.get(j))) || excelFields.get(allRequiredFieldsKeys.get(j)) == null) {
                    logger.info("Getting attributes of Field {}", allRequiredFieldsKeys.get(j));
                    Map<String, String> field = ParseJsonResponse.getFieldByName(newJsonStr, allRequiredFieldsKeys.get(j));

                    try {
                        if (field.get("label") == null) {
                            if (allRequiredFields.containsKey(field.get("name").trim())) {
                                allRequiredFields.remove(field.get("name").trim());
                                allRequiredFieldsKeys.remove(j);
                                j--;
                            }
                            continue;
                        }

                        if (ignoreFields.contains(field.get("label").trim())) {
                            if (allRequiredFields.containsKey(field.get("name").trim())) {
                                allRequiredFields.remove(field.get("name").trim());
                                allRequiredFieldsKeys.remove(j);
                                j--;
                            }
                            continue;
                        }

                        if (field.get("groupBy") != null) {
                            if (field.get("type") != null) {
                                switch (field.get("type").trim().toLowerCase()) {
                                    case "select":
                                        logger.info("Checking if Field {} has options available", field.get("name"));
                                        if (ParseJsonResponse.fieldContainsOptions(newJsonStr, field.get("name"), Boolean.parseBoolean(field.get("dynamicField")))) {
                                            List<String> allOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(newJsonStr, field.get("name"),
                                                    Boolean.parseBoolean(field.get("dynamicField")));
                                            if (!fieldsToHaveMultipleFields.contains(field.get("label").trim().toLowerCase())) {
                                                int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
                                                allRequiredFields.put(field.get("name"), allOptions.get(randomNumber));
                                            } else {
                                                fieldsToHaveMultipleFields.remove(field.get("label").trim().toLowerCase());
                                                fieldsToHaveMultipleFields.add(field.get("name").trim().toLowerCase());
                                                int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1,
                                                        maxNoOfMultipleValues);
                                                logger.info("Selecting Multiple Values for Field {}", field.get("label"));

                                                String multipleValuesJson = "";
                                                int i;
                                                for (i = 0; i < randomNumbers.length - 1; i++) {
                                                    multipleValuesJson = multipleValuesJson.concat(allOptions.get(randomNumbers[i]) + ",");
                                                }
                                                multipleValuesJson += allOptions.get(randomNumbers[i]);
                                                allRequiredFields.put(field.get("name"), multipleValuesJson);
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
                                            allRequiredFields.put(field.get("name"), this.getDefaultValueForFieldForExcel(field.get("type")));
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception while processing Child Field {}. {}", field.get("label"), e.getStackTrace());
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while getting field details when processing Child Field. {}", e.getMessage());
            }
        }
        return allRequiredFields;
    }

    private Map<String, String> processNonChildFieldsForExcel(Map<String, String> allRequiredFields, Map<String, String> excelFields, String newJsonStr,
                                                              List<String> fieldsToHaveMultipleFields, int maxNoOfMultipleValues, String stakeHolderOptionsJson) {
        List<String> allRequiredFieldsKeys = new ArrayList<>(allRequiredFields.keySet());
        for (int j = 0; j < allRequiredFields.size(); j++) {
            try {
                if (!(excelFields.containsKey(allRequiredFieldsKeys.get(j)) && excelFields.get(allRequiredFieldsKeys.get(j)).trim().equalsIgnoreCase("null"))) {
                    logger.info("Getting attributes of Field {}", allRequiredFieldsKeys.get(j));
                    Map<String, String> field = ParseJsonResponse.getFieldByName(newJsonStr, allRequiredFieldsKeys.get(j));

                    try {
                        //For Stakeholders
                        if (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("stakeHolders.values")) {
                            if (stakeHolderOptionsJson != null) {
                                allRequiredFields.put("stakeHolders", stakeHolderOptionsJson);
                                allRequiredFieldsKeys.add("stakeHolders");
                            }
                        } else if (field.get("groupBy") == null) {
                            if (field.get("type") != null) {
                                switch (field.get("type").trim().toLowerCase()) {
                                    case "select":
                                        logger.info("Checking if Field {} has options available", field.get("name"));
                                        if (ParseJsonResponse.fieldContainsOptions(newJsonStr, field.get("name"), Boolean.parseBoolean(field.get("dynamicField")))) {
                                            List<String> allOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(newJsonStr, field.get("name"),
                                                    Boolean.parseBoolean(field.get("dynamicField")));

                                            if (field.get("dependentField") != null && allRequiredFields.containsKey(field.get("dependentField"))) {
                                                String[] childFieldValue;
                                                if (fieldsToHaveMultipleFields.contains(field.get("dependentField").trim().toLowerCase())) {
                                                    String dependentFieldJsonStr = "[" + allRequiredFields.get(field.get("dependentField")) + "]";
                                                    JSONArray jsonArr = new JSONArray(dependentFieldJsonStr);
                                                    List<Integer> uniqueParentIds = new ArrayList<>();

                                                    for (int i = 0; i < jsonArr.length(); i++) {
                                                        JSONObject jsonObj = jsonArr.getJSONObject(i);
                                                        Integer parentId = jsonObj.getInt("parentId");

                                                        if (!uniqueParentIds.contains(parentId)) {
                                                            uniqueParentIds.add(parentId);
                                                        }
                                                    }

                                                    String multipleValuesJson = "";
                                                    for (String optionJson : allOptions) {
                                                        JSONObject jsonObj = new JSONObject(optionJson);
                                                        Integer id = jsonObj.getInt("id");
                                                        if (uniqueParentIds.contains(id)) {
                                                            multipleValuesJson = multipleValuesJson.concat(jsonObj.toString() + ",");
                                                        }
                                                    }
                                                    multipleValuesJson = multipleValuesJson.substring(0, multipleValuesJson.length() - 1);
                                                    allRequiredFields.put(field.get("name"), multipleValuesJson);
                                                } else {
                                                    childFieldValue = allRequiredFields.get(field.get("dependentField")).split("parentId\":");
                                                    if (childFieldValue.length > 1) {
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
                                                    }
                                                }
                                            } else {
                                                if (fieldsToHaveMultipleFields.contains(field.get("label").trim().toLowerCase())) {
                                                    int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1,
                                                            maxNoOfMultipleValues);
                                                    logger.info("Selecting Multiple Values for Field {}", field.get("label"));

                                                    String multipleValuesJson = "";
                                                    int i;
                                                    for (i = 0; i < randomNumbers.length - 1; i++) {
                                                        multipleValuesJson = multipleValuesJson.concat(allOptions.get(randomNumbers[i]) + ",");
                                                    }
                                                    multipleValuesJson += allOptions.get(randomNumbers[i]);
                                                    allRequiredFields.put(field.get("name"), multipleValuesJson);
                                                } else {
                                                    int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
                                                    allRequiredFields.put(field.get("name"), allOptions.get(randomNumber));
                                                }
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
                                            allRequiredFields.put(field.get("name"), this.getDefaultValueForFieldForExcel(field.get("type")));
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception while processing Required Field {}. {}", field.get("label"), e.getStackTrace());
                    }
                }
            } catch (Exception e) {
                logger.error("Exception while getting field details when processing Required Fields [Non Child]. {}", e.getMessage());
            }
        }
        return allRequiredFields;
    }

    private String getDefaultValueForAlias() {
        String defaultValue = null;
        logger.info("Getting default Value for Alias");

        try {
            String basePrefix = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "aliasprefix");
            int basePrefixLen = basePrefix.length();
            int maxLength = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "maxlengthforalias"));
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

    private Map<String, String> setProperties() {
        logger.info("Getting Properties for Section {} from Config File.", sectionName);
        Map<String, String> properties = new HashMap<>();
        try {
            properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, sectionName);
        } catch (Exception e) {
            logger.error("Exception while getting Properties for Section {}. {}", sectionName, e.getStackTrace());
        }
        return properties;
    }

    private Map<String, String> updateExcelFields(String newJsonStr, Map<String, String> excelFields) {
        try {
            if (excelFields != null) {
                String requiredDateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "requireddateformat");
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
                    modifiedFieldLabels.add(temp);
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

    private void updateExcelHeaders(List<String> excelHeaders, String entityName) throws ConfigurationException {
        String excelHeadersMappingFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelHeadersMappingFilePath");
        String excelHeadersMappingFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelHeadersMappingFileName");
        Map<String, String> allHeadersMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(excelHeadersMappingFilePath, excelHeadersMappingFileName, entityName);
        List<String> allHeadersKeys = new ArrayList<>(allHeadersMap.keySet());
        for (String key : allHeadersKeys) {
            if (excelHeaders.contains(key)) {
                int keyIndex = excelHeaders.indexOf(key);
                excelHeaders.set(keyIndex, allHeadersMap.get(key));
            }
        }
    }

    private Map<String, String> setPropertiesForExcel(String entityName, List<String> allHeaders, List<String> enabledData) {
        Map<String, String> properties = new HashMap<>();
        int dropDownType;
        String parentType;
        String sourceId;
        Map<String, String> optionsParametersMap = null;
        try {
            switch (entityName.trim().toLowerCase()) {
                case "contracts":
					/*int contractsParentTypeColNo = this.getColumnNoOfHeader(allHeaders, "Type");
					parentType = enabledData.get(contractsParentTypeColNo).trim();
					properties.put("sourceentity", parentType);*/

                    parentType = "suppliers";
                    properties.put("sourceentity", parentType);

//					int contractsParentNameColNo = this.getColumnNoOfHeader(allHeaders, "parent");
//					properties.put("sourcename", enabledData.get(contractsParentNameColNo));
                    int contractsParentNameColNo = this.getColumnNoOfHeader(allHeaders, "Supplier");
                    properties.put("sourcename", enabledData.get(contractsParentNameColNo));
                    int parentEntityTypeColNo = this.getColumnNoOfHeader(allHeaders, "type");
                    properties.put("parententitytype", enabledData.get(parentEntityTypeColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(contractsParentNameColNo));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "governance body":
                    parentType = "contracts";
                    properties.put("sourceentity", parentType);
                    int gbContractsColNo = this.getColumnNoOfHeader(allHeaders, "Contract");
                    properties.put("contractname", enabledData.get(gbContractsColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(gbContractsColNo));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "suppliers":
                    parentType = "vendors";
                    properties.put("sourceentity", parentType);
                    int suppliersParentNameColNo = this.getColumnNoOfHeader(allHeaders, "Vendor Hierarchy");
                    String suppliersParentName = enabledData.get(suppliersParentNameColNo);
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(suppliersParentName);
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "vendors":
                    parentType = "client";
                    properties.put("sourceentity", parentType);
                    String listIdStr = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "entity_url_id");
                    String clientId = null;
                    ListRendererDefaultUserListMetaData defaultUserListMetaDataObj = new ListRendererDefaultUserListMetaData();
                    defaultUserListMetaDataObj.hitListRendererDefaultUserListMetadata(Integer.parseInt(listIdStr), null, "{}");
                    JSONObject jsonObj = new JSONObject(defaultUserListMetaDataObj.getListRendererDefaultUserListMetaDataJsonStr());
                    if (jsonObj.has("popupUrl") && jsonObj.getString("popupUrl") != null) {
                        String popUrl = jsonObj.getString("popupUrl");
                        String tokens[] = popUrl.split("clientId=");
                        if (tokens[1].contains("&")) {
                            String temp[] = tokens[1].trim().split(Pattern.quote("&"));
                            clientId = temp[0].trim();
                        } else
                            clientId = tokens[1].trim();
                    }
                    properties.put("sourceid", clientId);
                    break;

                case "obligations":
                    parentType = "contracts";
                    properties.put("sourceentity", parentType);
                    int obligationsSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "SourceName");
                    properties.put("sourcename", enabledData.get(obligationsSourceNameColNo));
                    int obligationsSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, "source");
                    properties.put("parententitytype", enabledData.get(obligationsSourceTypeColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(obligationsSourceNameColNo));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "service levels":
                    parentType = "contracts";
                    properties.put("sourceentity", parentType);
                    int slSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "Contract");
                    properties.put("sourcename", enabledData.get(slSourceNameColNo));
                    int slSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, "Contract Type");
                    properties.put("parententitytype", enabledData.get(slSourceTypeColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(slSourceNameColNo));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "actions":
                    int actionsParentTypeColumnNo = this.getColumnNoOfHeader(allHeaders, "sourceType");
                    if (enabledData.get(actionsParentTypeColumnNo).trim().equalsIgnoreCase("supplier")) {
                        parentType = "suppliers";
                        int actionsSupplierColNo = this.getColumnNoOfHeader(allHeaders, "supplier");
                        properties.put("sourcename", enabledData.get(actionsSupplierColNo));
                        properties.put("parententitytype", "supplier");
                        optionsParametersMap = this.getOptionsParametersMap(enabledData.get(actionsSupplierColNo));
                    } else {
                        parentType = "contracts";
                        int actionsSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "sourceName");
                        properties.put("sourcename", enabledData.get(actionsSourceNameColNo));
                        properties.put("parententitytype", enabledData.get(actionsParentTypeColumnNo));
                        optionsParametersMap = this.getOptionsParametersMap(enabledData.get(actionsSourceNameColNo));
                    }
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    properties.put("sourceentity", parentType);
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "contract draft request":
                    parentType = "suppliers";
                    properties.put("sourceentity", parentType);
                    //int cdrSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "suppliers");
                    //properties.put("sourcename", enabledData.get(cdrSourceNameColNo));
                    properties.put("sourceid", "-1");
                    break;

                case "contract template structure":
                    parentType = "suppliers";
                    properties.put("sourceentity", parentType);
                    //int cdrSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "suppliers");
                    //properties.put("sourcename", enabledData.get(cdrSourceNameColNo));
                    properties.put("sourceid", "-1");
                    break;

                case "issues":
                    int issuesParentTypeColNo = this.getColumnNoOfHeader(allHeaders, "sourceType");
                    parentType = enabledData.get(issuesParentTypeColNo);
                    if (parentType.trim().equalsIgnoreCase("supplier")) {
                        parentType = "suppliers";
                        int issuesSupplierColNo = this.getColumnNoOfHeader(allHeaders, "supplier");
                        properties.put("sourcename", enabledData.get(issuesSupplierColNo));
                        properties.put("parententitytype", "supplier");
                        optionsParametersMap = this.getOptionsParametersMap(enabledData.get(issuesSupplierColNo));
                    } else {
                        int issuesSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "sourceName");
                        if (parentType.trim().equalsIgnoreCase("msa") || parentType.trim().equalsIgnoreCase("psa") ||
                                parentType.trim().equalsIgnoreCase("sow") || parentType.trim().equalsIgnoreCase("work order")) {
                            parentType = "contracts";
                            properties.put("sourcename", enabledData.get(issuesSourceNameColNo));
                            properties.put("parententitytype", enabledData.get(issuesParentTypeColNo));
                            optionsParametersMap = this.getOptionsParametersMap(enabledData.get(issuesSourceNameColNo));
                        }
                    }
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    properties.put("sourceentity", parentType);
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "purchase orders":
                    int poParentTypeColNo = this.getColumnNoOfHeader(allHeaders, "sourceType");
                    if (enabledData.get(poParentTypeColNo).trim().equalsIgnoreCase("Supplier") ||
                            enabledData.get(poParentTypeColNo).trim().equalsIgnoreCase("suppliers"))
                        parentType = "suppliers";
                    else
                        parentType = "contracts";
                    int poSouceNameColNo = this.getColumnNoOfHeader(allHeaders, "sourceName");
                    properties.put("sourcename", enabledData.get(poSouceNameColNo));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(poSouceNameColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    properties.put("sourceentity", parentType);
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "change requests":
                    int crParentTypeColNo = this.getColumnNoOfHeader(allHeaders, "sourceType");
                    parentType = enabledData.get(crParentTypeColNo);
                    if (parentType.trim().equalsIgnoreCase("supplier")) {
                        parentType = "suppliers";
                        int crSupplierColNo = this.getColumnNoOfHeader(allHeaders, "supplier");
                        properties.put("sourcename", enabledData.get(crSupplierColNo));
                        properties.put("parententitytype", "supplier");
                        optionsParametersMap = this.getOptionsParametersMap(enabledData.get(crSupplierColNo));
                    } else {
                        int crSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "sourceName");
                        if (parentType.trim().equalsIgnoreCase("msa") || parentType.trim().equalsIgnoreCase("psa") ||
                                parentType.trim().equalsIgnoreCase("sow") || parentType.trim().equalsIgnoreCase("work order")) {
                            parentType = "contracts";
                            properties.put("sourcename", enabledData.get(crSourceNameColNo));
                            properties.put("parententitytype", enabledData.get(crParentTypeColNo));
                            optionsParametersMap = this.getOptionsParametersMap(enabledData.get(crSourceNameColNo));
                        }
                    }
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    properties.put("sourceentity", parentType);
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "work order requests":
                    parentType = "contracts";
                    properties.put("sourceentity", parentType);
                    int worSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "sourceName");
                    properties.put("sourcename", enabledData.get(worSourceNameColNo));
                    int worSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, "sourceType");
                    properties.put("parententitytype", enabledData.get(worSourceTypeColNo));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(worSourceNameColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "disputes":
                    parentType = "suppliers";
                    properties.put("sourceentity", parentType);
                    int disputeSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "SourceName");
                    properties.put("sourcename", enabledData.get(disputeSourceNameColNo));
					/*int obligationsSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, "source");
					properties.put("parententitytype", enabledData.get(obligationsSourceTypeColNo));*/
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(disputeSourceNameColNo));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "interpretations":
                    parentType = "suppliers";
                    properties.put("sourceentity", parentType);
                    int intrpSourceNameColNo = this.getColumnNoOfHeader(allHeaders, "SourceName");
                    properties.put("sourcename", enabledData.get(intrpSourceNameColNo));
                    int intrpSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, "source");
                    properties.put("parententitytype", enabledData.get(intrpSourceTypeColNo));
                    dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                            ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
                    optionsParametersMap = this.getOptionsParametersMap(enabledData.get(intrpSourceNameColNo));
                    sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
                    properties.put("sourceid", sourceId);
                    break;

                case "contract templates":
                    parentType = "client";
                    properties.put("sourceentity", parentType);

                    int sourceNameColNo = this.getColumnNoOfHeader(allHeaders, "Client Id");
                    properties.put("sourceid", enabledData.get(sourceNameColNo));

                    break;
            }
        } catch (Exception e) {
            logger.error("Exception while setting Properties Map for Entity {}. {}", entityName, e.getStackTrace());
        }
        return properties;
    }

    private int getColumnNoOfHeader(List<String> excelHeaders, String columnName) {
        int columnNo = -1;
        try {
            for (int i = 0; i < excelHeaders.size(); i++) {
                if (excelHeaders.get(i).trim().equalsIgnoreCase(columnName)) {
                    columnNo = i;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting Column No of Header {}. {}", columnName, e.getStackTrace());
        }
        return columnNo;
    }

    private String getSourceId(int dropDownType, Map<String, String> optionsParametersMap, String parentType) {
        String sourceId = null;
        try {
            Options optionObj = new Options();
            optionObj.hitOptions(dropDownType, optionsParametersMap);
            sourceId = optionObj.getIds();
            if (sourceId != null && sourceId.contains(",")) {
                String ids[] = sourceId.split(Pattern.quote(","));
                sourceId = ids[0].trim();
                String name = Options.getNameFromId(optionObj.getOptionsJsonStr(), Integer.parseInt(ids[0].trim()));
                logger.warn("Multiple records found for Parent Entity {}. Hence selecting first record having Name \"{}\"", parentType, name);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Source Id. {}", e.getMessage());
        }
        return sourceId;
    }

    private Map<String, String> getOptionsParametersMap(String query) {
        Map<String, String> optionsParameters = new HashMap<>();
        try {
            String pageType = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pagetype", "listdata");
            optionsParameters.put("pageType", pageType);
            if (query.length() > 15) {
                query = query.substring(0, 15);
            }
            optionsParameters.put("query", query);
        } catch (Exception e) {
            logger.error("Exception while getting Options Parameters Map. {}", e.getMessage());
        }
        return optionsParameters;
    }

    private String getStakeHolderOptionsJsonStr(String stakeHolderName) {
        String optionsJsonStr = null;
        try {
            Options optionObj = new Options();
            int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", "stakeholders"));
            Map<String, String> params = new HashMap<>();
            params.put("pageType", ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pagetype", "listdata"));
            params.put("query", stakeHolderName);
            optionObj.hitOptions(dropDownType, params);
            String responseJson = optionObj.getOptionsJsonStr();

            if (ParseJsonResponse.validJsonResponse(responseJson)) {
                JSONObject jsonObj = new JSONObject(responseJson);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    if (jsonObj.has("name") && jsonObj.getString("name").trim().equalsIgnoreCase(stakeHolderName)) {
                        optionsJsonStr = jsonObj.toString();
                        break;
                    }
                }
            } else {
                logger.error("Options Response for StakeHolder {} is not a valid JSON. Hence couldn't get StakeHolder Options JSON.", stakeHolderName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Options Json Str for StakeHolder {}. {}", stakeHolderName, e.getStackTrace());
        }
        return optionsJsonStr;
    }
}