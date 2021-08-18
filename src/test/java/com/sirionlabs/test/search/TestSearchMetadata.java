package com.sirionlabs.test.search;

import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Created by akshay.rohilla on 6/21/2017.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestSearchMetadata {

    private final static Logger logger = LoggerFactory.getLogger(TestSearchMetadata.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String searchPayloadConfigFilePath = null;
    private static String searchPayloadConfigFileName = null;
    private static String dateFormat = null;
    private static String payloadDateFormat = null;
    private static int NoOfRecordsToShow = 5;
    private static String valuesDelimiter = null;
    private static int listDataOffset = 0;
    private static boolean applyRandomization = false;
    private static int maxNoOfFieldsOptions = 3;

    @BeforeClass
    public void setStaticFields() {
        final int DefaultMaxNoOfRecordsToShow = 20;

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MetadataSearchConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MetadataSearchConfigFileName");
        Map<String, String> properties = ParseConfigFile.getAllProperties(configFilePath, configFileName);

        dateFormat = properties.get("dateformat");
        payloadDateFormat = properties.get("payloaddateformat");
        valuesDelimiter = properties.get("delimiterforvalues");

        if (properties.get("maxnoofrecordstoshow").equalsIgnoreCase("") || Integer.parseInt(properties.get("maxnoofrecordstoshow").trim()) == 0)
            NoOfRecordsToShow = DefaultMaxNoOfRecordsToShow;

        else
            NoOfRecordsToShow = Integer.parseInt(properties.get("maxnoofrecordstoshow"));

        searchPayloadConfigFilePath = properties.get("payloadhierarchyfilepath");
        searchPayloadConfigFileName = properties.get("payloadhierarchyfilename");

        String temp = properties.get("listdataoffset");
        if (temp != null && Integer.parseInt(temp) != 0)
            listDataOffset = Integer.parseInt(temp);

        if (properties.containsKey("applyrandomization") && properties.get("applyrandomization").trim().equalsIgnoreCase("true"))
            applyRandomization = true;

        if (properties.containsKey("maxnooffieldoptions") && !properties.get("maxnooffieldoptions").trim().equalsIgnoreCase(""))
            maxNoOfFieldsOptions = Integer.parseInt(properties.get("maxnooffieldoptions"));
    }

    @DataProvider(parallel = true)
    public Object[][] entitiesToTest() {
        logger.info("Setting all Entities to Test.");
        List<String> entitiesToTest = getEntitiesToTest();
        logger.info("Total Entities to Test : {}", entitiesToTest.size());
        List<Object[]> allTestData = new ArrayList<>();
        for (String entity : entitiesToTest) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "entitiesToTest")
    public void testSearchMetadata(String entityName) {
        CustomAssert csAssert = new CustomAssert();
        ExecutorService executor = Executors.newFixedThreadPool(1);

        try {
            logger.info("Getting Entity Type Id for Entity {}", entityName);
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), entityName));

            logger.info("Hitting MetadataSearch Api for Entity : {}", entityName);
            MetadataSearch metadataSearchObj = new MetadataSearch();
            String metaSearchJsonStr = metadataSearchObj.hitMetadataSearch(entityTypeId);

            logger.info("Setting Fields to Test for Entity : {}", entityName);
            Map<String, String> fieldsToTest = getFieldsToTest(entityName, metaSearchJsonStr);
            List<String> labels = new ArrayList<>(fieldsToTest.keySet());

            logger.info("Total Fields to Test for Entity {} are {}", entityName, fieldsToTest.size());
            for (int j = 0; j < fieldsToTest.size(); j++) {
                logger.info("**********************************************************************************************");
                Map<String, String> field = ParseJsonResponse.getFieldByLabel(metaSearchJsonStr, labels.get(j));

                if (field.size() == 0) {
                    logger.info("No Such Field {}", labels.get(j));
                    continue;
                }

                String payload;
                String expectedValue;
                String fieldType = field.get("type");
                logger.info("Verifying Search Data for Field {} of Type {}", labels.get(j), field.get("type"));
                try {
                    switch (fieldType.trim().toLowerCase()) {
                        default:
                            if (field.get("model") != null && field.get("model").equalsIgnoreCase("stakeHolders.values")) {
                                if (fieldsToTest.get(labels.get(j)) != null)
                                    expectedValue = fieldsToTest.get(labels.get(j));
                                else
                                    expectedValue = getDefaultValue("stakeholder");

                                payload = getPayload(field, expectedValue, entityTypeId, field.get("name"), Integer.parseInt(field.get("id")));
                                if (payload == null) {
                                    csAssert.assertTrue(false, "Payload for Field " + field.get("Label") + " and Entity " + entityName +
                                            " having Value " + expectedValue + " is Null and hence skipping Validation");
                                    continue;
                                }
                                logger.info("Verifying Search For Field {} having Value {}", field.get("label"), expectedValue);
                                this.verifySearchMetadata(entityTypeId, field.get("name"), expectedValue, "stakeholders", payload, csAssert);
                            } else {
                                if (field.get("splitFields").equalsIgnoreCase("true")) {
                                    fieldType = "slider";
                                    String fromValue;
                                    String toValue;
                                    if (fieldsToTest.get(labels.get(j)) != null) {
                                        String[] fieldValues = fieldsToTest.get(labels.get(j)).toLowerCase().split("to");
                                        fromValue = fieldValues[0].trim();
                                        toValue = fieldValues[1].trim();
                                    } else {
                                        fromValue = getDefaultValue(field.get("type"), true, "from");
                                        toValue = getDefaultValue(field.get("type"), true, "to");
                                    }

                                    if (field.get("type").equalsIgnoreCase("date") || field.get("type").equalsIgnoreCase("dateTime")) {
                                        fieldType = field.get("type");
                                        String requiredDateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                                                "requireddateformat");
                                        fromValue = DateUtils.converDateToAnyFormat(fromValue, dateFormat, payloadDateFormat);
                                        toValue = DateUtils.converDateToAnyFormat(toValue, dateFormat, payloadDateFormat);
                                        String expectedFromValue = DateUtils.converDateToAnyFormat(fromValue, dateFormat, requiredDateFormat);
                                        String expectedToValue = DateUtils.converDateToAnyFormat(toValue, dateFormat, requiredDateFormat);
                                        expectedValue = expectedFromValue + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + expectedToValue;
                                    } else {
                                        field.put("type", "double");
                                        expectedValue = fromValue + " ::To:: " + toValue;
                                    }

                                    payload = getPayload(field, fromValue, toValue, entityTypeId);
                                } else {
                                    if (fieldsToTest.get(labels.get(j)) != null)
                                        expectedValue = fieldsToTest.get(labels.get(j));
                                    else
                                        expectedValue = getDefaultValue(field.get("type"));
                                    payload = getPayload(field, expectedValue, entityTypeId, entityName);
                                    if (payload == null) {
                                        csAssert.assertTrue(false, "Payload for Field " + field.get("label") + " and Entity " + entityName +
                                                " having Value " + expectedValue + " is Null and hence skipping Validation");
                                        continue;
                                    }
                                }

                                logger.info("Verifying Search For Field {} having Value {}", field.get("label"), expectedValue);
                                this.verifySearchMetadata(entityTypeId, field.get("name"), expectedValue, fieldType, payload, csAssert);
                            }
                            break;

                        case "select":
                            //Get all available options for Field
                            List<Map<String, String>> allOptions = MetadataSearch.getAvailableOptionsForField(metaSearchJsonStr, field.get("name"),
                                    Boolean.getBoolean(field.get("dynamicField")));

                            if (fieldsToTest.get(labels.get(j)) != null) {
                                String fieldId = null;
                                expectedValue = fieldsToTest.get(labels.get(j));

                                for (Map<String, String> option : allOptions) {
                                    if (option.get("name").equalsIgnoreCase(expectedValue)) {
                                        fieldId = option.get("id");
                                        expectedValue = option.get("name");
                                        break;
                                    }
                                }
                                payload = getPayload(field, expectedValue, entityTypeId, entityName, fieldId);
                                if (payload == null) {
                                    csAssert.assertTrue(false, "Payload for Field " + field.get("Label") + " and Entity " + entityName +
                                            " having Value " + expectedValue + " is Null and hence skipping Validation");
                                    continue;
                                }

                                if (field.get("name").equalsIgnoreCase("parentEntityType")) {
                                    fieldType = null;
                                    expectedValue = fieldId;
                                }

                                logger.info("Verifying Search For Field {} having Value {}", field.get("label"), expectedValue);
                                this.verifySearchMetadata(entityTypeId, field.get("name"), expectedValue, fieldType, payload, csAssert);
                            } else {
                                logger.info("Verifying Search For all Options for Field {}", field.get("label"));
                                logger.info("Total Available Options for Field {} are {}", field.get("label"), allOptions.size());

                                List<FutureTask<Boolean>> taskList = new ArrayList<>();

                                List<Map<String, String>> allSelectedOptions;

                                if (applyRandomization) {
                                    int[] randomNumbersForOptions = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1,
                                            maxNoOfFieldsOptions);

                                    allSelectedOptions = new ArrayList<>();

                                    for (int randomNumber : randomNumbersForOptions) {
                                        allSelectedOptions.add(allOptions.get(randomNumber));
                                    }
                                } else {
                                    allSelectedOptions = allOptions;
                                }

                                for (int k = 0; k < allSelectedOptions.size(); k++) {
                                    final int index = k;
                                    final int outerIndex = j;

                                    FutureTask<Boolean> result = new FutureTask<>(() -> {
                                        String finalPayload = getPayload(field, allSelectedOptions.get(index).get("name"), entityTypeId, entityName,
                                                allSelectedOptions.get(index).get("id"));
                                        if (finalPayload == null) {
                                            csAssert.assertTrue(false, "Payload for Field " + field.get("Label") + " and Entity " + entityName +
                                                    " having Value " + allSelectedOptions.get(index).get("name") + " is Null and hence skipping Validation");
                                            return true;
                                        }
                                        logger.info("Verifying Search For Field #{} {}, Option #{} having Value {}", (outerIndex + 1), field.get("label"), (index + 1),
                                                allSelectedOptions.get(index).get("name"));

                                        String finalExpectedValue = allSelectedOptions.get(index).get("name");

                                        if (field.get("name").equalsIgnoreCase("parentEntityType")) {
                                            finalExpectedValue = allSelectedOptions.get(index).get("id");

                                            verifySearchMetadata(entityTypeId, field.get("name"), finalExpectedValue, null, finalPayload, csAssert);
                                        } else {
                                            verifySearchMetadata(entityTypeId, field.get("name"), finalExpectedValue, field.get("type"), finalPayload, csAssert);
                                        }
                                        return true;
                                    });
                                    taskList.add(result);
                                    executor.execute(result);
                                }
                                for (FutureTask<Boolean> task : taskList)
                                    task.get();

                                executor.shutdownNow();
                            }
                            break;
                    }
                } catch (Exception e) {
                    logger.error("Exception while Verifying Search Data for Field {} of Type {}. {}", labels.get(j), field.get("type"), e.getStackTrace());
                }
            }
        } catch (Exception e) {
            executor.shutdownNow();
            logger.error("Exception while Verifying Search Metadata. {}", e.getMessage());
            csAssert.assertFalse(true, "Exception while Verifying Search Metadata. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void verifySearchMetadata(int entityTypeId, String fieldName, String expectedValue, String fieldType, String payload, CustomAssert csAssert) throws Exception {
        //Special handling for ParentEntityType field
        if (fieldName.equalsIgnoreCase("parentEntityType")) {
            fieldName = "parentEntityTypeId";
        }

        Search searchObj = new Search();
        logger.info("Hitting Search Api for EntityTypeId {} and Field {} and Value {}", entityTypeId, fieldName, expectedValue);
        String searchResponse = searchObj.hitSearch(entityTypeId, payload);

        String additionalInfo = " for EntityTypeId " + entityTypeId + ", Field " + fieldName + " and Option " + expectedValue;

        if (!ParseJsonResponse.validJsonResponse(searchResponse)) {
            csAssert.assertTrue(false, "Search Response is an invalid JSON " + additionalInfo + ". Hence skipping further validation");
            return;
        }

        if (ParseJsonResponse.containsApplicationError(searchResponse)) {
            csAssert.assertTrue(false, "Search Response contains Application Error " + additionalInfo + ". Hence skipping further validation");
            return;
        }

        List<Map<Integer, Map<String, String>>> searchData = Search.getSearchData(searchResponse);

        if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failifzerorecords").trim().equalsIgnoreCase("yes")
                && searchData.size() == 0) {
            csAssert.assertFalse(true, "No Result Found for EntityTypeId " + entityTypeId + " and Field " + fieldName + " and Value " + expectedValue);
        } else if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failifzerorecords").trim().equalsIgnoreCase("no")
                && searchData.size() == 0)
            logger.info("No Result Found for EntityTypeId {} and Field {} and Value {}", entityTypeId, fieldName, expectedValue);

        else {
            logger.info("Total Results Found for EntityTypeId {} and Field {} and Value {}: {}", entityTypeId, fieldName, expectedValue, searchData.size());
            int[] randomNumbersForSearchMetadata = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (searchData.size() - 1),
                    Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "MaxNoOfRecordsToValidate")));

            ListRendererDefaultUserListMetaData listMetadatObj = new ListRendererDefaultUserListMetaData();

            String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
            String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
            Map<String, String> entityMap = ParseConfigFile.getAllConstantProperties(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName);

            logger.info("Hitting ListRendererDefaultUserListMetaData Api for EntityTypeId {} and Field {} and Value {}", entityTypeId, fieldName, expectedValue);
            listMetadatObj.hitListRendererDefaultUserListMetadata(Integer.parseInt(entityMap.get("entity_url_id")), null, "{}");
            listMetadatObj.setColumns(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());
            String[] recordIds = this.setRecordIds(searchData, randomNumbersForSearchMetadata, listMetadatObj.getIdFromQueryName("id"));

            logger.info("Total Records to be Validated for Field {} are {}", fieldName, recordIds.length);

            ExecutorService executor = Executors.newFixedThreadPool(3);
            List<FutureTask<Boolean>> taskList = new ArrayList<>();

            for (int i = 0; i < recordIds.length; i++) {
                final int index = i;

                String finalFieldName = fieldName;
                FutureTask<Boolean> result = new FutureTask<>(() -> {
                    Show show = new Show();
                    logger.info("Hitting Show Api for Record #{} having EntityTypeId {}, Id {} and Value {} of Field {}", (index + 1), entityTypeId,
                            recordIds[index], expectedValue, finalFieldName);
                    show.hitShow(entityTypeId, Integer.parseInt(recordIds[index]));
                    String showJsonStr = show.getShowJsonStr();

                    if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
                        logger.error("Invalid Show Json Response for Record #{} having EntityTypeId {}, Id {} and Value {} of Field {}", (index + 1), entityTypeId,
                                recordIds[index], expectedValue, finalFieldName);
                        csAssert.assertFalse(true, "Invalid Show Json Response for Record #" + (index + 1) + " having EntityTypeId " + entityTypeId
                                + ", Id " + recordIds[index] + " and Value " + expectedValue + " of Field " + finalFieldName);
                    } else {
                        String status = ParseJsonResponse.getStatusFromResponse(showJsonStr);
                        if (status.equalsIgnoreCase("success")) {
                            if (!APIUtils.isPermissionDeniedInResponse(showJsonStr)) {
                                //Special Handling for Relations/Multi Suppliers Field
                                if (entityName.equalsIgnoreCase("contracts") && finalFieldName.equalsIgnoreCase("relations")) {
                                    List<String> allSelectedRelationValues = ShowHelper.getAllSelectValuesOfField(showJsonStr, "relations",
                                            ShowHelper.getShowFieldHierarchy("relations", entityTypeId), Integer.parseInt(recordIds[index]), entityTypeId);

                                    if (allSelectedRelationValues == null || allSelectedRelationValues.isEmpty()) {
                                        csAssert.assertTrue(false, "Couldn't get All Selected Values of Relations for Record #" + (index + 1) +
                                                " having EntityTypeId " + entityTypeId + ", Id " + recordIds[index] + " and Value " + expectedValue + " of Field " +
                                                finalFieldName);
                                    } else {
                                        String[] allExpectedSuppliers = expectedValue.trim().split(",");
                                        List<String> allExpectedValues = new ArrayList<>();

                                        for (String expectedSupplierValue : allExpectedSuppliers) {
                                            allExpectedValues.add(expectedSupplierValue.trim().toLowerCase());
                                        }

                                        boolean expectedValueFoundInResult = false;

                                        for (String relationValue : allSelectedRelationValues) {
                                            for (String expectedVal : allExpectedValues) {
                                                if (relationValue.contains(expectedVal)) {
                                                    expectedValueFoundInResult = true;
                                                    break;
                                                }
                                            }

                                            if (expectedValueFoundInResult) {
                                                break;
                                            }
                                        }

                                        csAssert.assertTrue(expectedValueFoundInResult, "Search Validation for Record #" + (index + 1) +
                                                " having EntityTypeId " + entityTypeId + ", Id " + recordIds[index] + " and Value " + expectedValue + " of Field " +
                                                finalFieldName);
                                    }
                                } else {
                                    if(fieldType.equalsIgnoreCase("stakeHolders")) {
                                        ShowHelper.verifyShowFieldOfStakeHolderType(showJsonStr,ShowHelper.getShowFieldHierarchy("stakeholders", entityTypeId), "stakeholders", expectedValue, entityTypeId,  Integer.parseInt(recordIds[index]), csAssert, true, finalFieldName);
                                    } else {
                                        ShowHelper.verifyShowField(showJsonStr, finalFieldName, fieldType, expectedValue, entityTypeId, Integer.parseInt(recordIds[index]),
                                                csAssert, true);
                                    }
                                }
                            }
                        }
                    }
                    return true;
                });
                taskList.add(result);
                executor.execute(result);
            }
            for (FutureTask<Boolean> task : taskList)
                task.get();

            executor.shutdownNow();
        }
    }

    private String[] setRecordIds(List<Map<Integer, Map<String, String>>> searchData, int[] indexArray, int columnId) {
        String recordIds = "";
        boolean first = true;
        try {
            if (searchData.size() > 0) {
                for (int index : indexArray) {
                    if (first) {
                        recordIds = recordIds.concat(searchData.get(index).get(columnId).get("valueId"));
                        first = false;
                    } else
                        recordIds = recordIds.concat("," + searchData.get(index).get(columnId).get("valueId"));
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while setting Record Ids in TestSearchMetadata. {}", e.getMessage());
        }
        return recordIds.split(",");
    }

    //For SplitFields ...Currently only default values are fetched for splitFields
    private String getPayload(Map<String, String> field, String fromValue, String toValue, int entityTypeId) {
        if (field.get("type").equalsIgnoreCase("dateTime")) {
            fromValue = fromValue.concat(" 00:00:00");
            toValue = toValue.concat(" 00:00:00");
        }

        String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
                + "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":" + listDataOffset + "},\"size\":{\"name\":\"size\",\"values\":" + NoOfRecordsToShow + "}},";

        String temp = "\"" + field.get("name") + "From\":{\"values\":\"" + fromValue + "\"},\"" + field.get("name") + "To\":{\"values\":\"" + toValue + "\"}";

        if (field.get("dynamicField").equalsIgnoreCase("true"))
            payload += "\"dynamicMetadata\":{" + temp + "}";
        else
            payload += temp;
        payload += "}}}";

        return payload;
    }

    //For Non-SplitFields
    private String getPayload(Map<String, String> field, String expectedValue, int entityTypeId, String entityName) throws ConfigurationException {
        return getPayload(field, expectedValue, entityTypeId, entityName, null);
    }

    //For Non-SplitFields .... Need FieldId for Fields of type Select
    public static String getPayload(Map<String, String> field, String expectedValue, int entityTypeId, String entityName, String fieldId) throws ConfigurationException {
        String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
                + "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":" + listDataOffset + "},\"size\":{\"name\":\"size\",\"values\":" + NoOfRecordsToShow + "}},";
        String temp;

        String fieldName = field.get("name");

        if (field.get("type").trim().equalsIgnoreCase("select")) {
            if (field.get("multiple").equalsIgnoreCase("true"))
                temp = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"values\":[{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}]}";
            else
                temp = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"values\":{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}}";
        } else if (field.get("type").trim().equalsIgnoreCase("checkbox")) {
            temp = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"values\":" + expectedValue + "}";
        } else {
            if (ParseConfigFile.hasPropertyCaseSensitive(searchPayloadConfigFilePath, searchPayloadConfigFileName, entityName, fieldName))
                temp = getSubPayload(entityName, fieldName, expectedValue);
            else if (ParseConfigFile.hasPropertyCaseSensitive(searchPayloadConfigFilePath, searchPayloadConfigFileName, "default", fieldName))
                temp = getSubPayload("default", fieldName, expectedValue);
            else {
                if (fieldName.equalsIgnoreCase("relations")) {
                    temp = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"id\":" + field.get("id") + ",\"values\":[{\"name\":\"" + expectedValue + "\"}]}";
                } else {
                    temp = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"values\":\"" + expectedValue + "\"}";
                }
            }

            if (temp == null)
                return null;
        }

        if (field.get("dynamicField").equalsIgnoreCase("true"))
            payload += "\"dynamicMetadata\":{" + temp + "}";
        else
            payload += temp;
        payload += "}}}";

        return payload;
    }

    //For Stakeholder Field
    public static String getPayload(Map<String, String> field, String expectedValue, int entityTypeId, String roleGroup, int fieldId) {
        String payload;

        try {
            payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
                    + "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":" + listDataOffset + "},\"size\":{\"name\":\"size\",\"values\":" + NoOfRecordsToShow +
                    "}},";

            Map<String, String> parameters = new HashMap<>();
            parameters.put("pageType", "1");
            parameters.put("query", expectedValue);
            parameters.put("entityTpeId", Integer.toString(entityTypeId));
            parameters.put("fieldId", Integer.toString(fieldId));

            logger.info("Hitting Options Api");
            Options optionsObj = new Options();
            int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                    ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", "stakeholders"));
            optionsObj.hitOptions(dropDownType, parameters);
            String optionsStr = optionsObj.getOptionsJsonStr();

            JSONObject jsonObj = new JSONObject(optionsStr);
            JSONArray jsonArr = jsonObj.getJSONArray("data");
            int id = fieldId;

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);
                if (jsonObj.has("name") && jsonObj.getString("name").trim().equalsIgnoreCase(expectedValue)) {
                    id = jsonObj.getInt("id");
                    break;
                }
            }
            String temp = "\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"" + roleGroup + "\":{\"values\":[{\"id\":" + id + ",\"name\":\"" +
                    expectedValue + "\"}]," + "\"name\":\"" + roleGroup + "\",\"label\":\"" + field.get("label") + "\"}}}";

            payload += temp + "}}}";
        } catch (Exception e) {
            logger.error("Exception while getting payload for Field {}. {}", field.get("label"), e.getMessage());
            return null;
        }
        return payload;
    }

    //For Non-SplitFields
    private String getDefaultValue(String fieldType) {
        return getDefaultValue(fieldType, false, null);
    }

    private String getDefaultValue(String fieldType, boolean isSplitField, String subString) {
        String defaultValue = null;

        switch (fieldType.trim().toLowerCase()) {
            case "text":
            case "textarea":
                defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaulttext");
                break;

            case "date":
            case "datetime":
                if (!isSplitField && subString == null)
                    defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DefaultDate");
                else {
                    if (subString.trim().equalsIgnoreCase("from"))
                        defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DefaultFromDate");
                    else
                        defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DefaultToDate");
                }
                break;

            case "number":
                if (!isSplitField && subString == null)
                    defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultnumber");
                else {
                    if (subString.trim().equalsIgnoreCase("from"))
                        defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultFromNumber");
                    else
                        defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultToNumber");
                }
                break;

            case "stakeholder":
                defaultValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultstakeholder");
                break;

            default:
                logger.info("Field Type {} not found.", fieldType);
        }
        return defaultValue;
    }

    private List<String> getEntitiesToTest() {
        List<String> entitiesToTest = new ArrayList<>();
        String[] allEntities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "allentities").split(valuesDelimiter);

        logger.info("Total Entity Types {}", allEntities.length);
        if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallentities")
                .equalsIgnoreCase("yes") && allEntities.length > 0) {
            int[] randomNumbersForEntities = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (allEntities.length - 1),
                    Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "MaxNoOfEntitiesToTest")));
            for (int randomNumber : randomNumbersForEntities)
                entitiesToTest.add(allEntities[randomNumber]);
        } else {
            String[] entities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestotest").trim().split(valuesDelimiter);
            for (String entity : entities)
                entitiesToTest.add(entity.trim());
        }
        return entitiesToTest;
    }

    private Map<String, String> getFieldsToTest(String entity, String jsonStr) {
        Map<String, String> fieldsToTest = new HashMap<>();

        if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallfields").equalsIgnoreCase("yes")) {
            List<String> poolFields = new ArrayList<>();

            if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "usefieldspool").equalsIgnoreCase("yes")) {
                logger.info("Parsing Fields Pool for Entity {}", entity);
                String[] entityFieldsPool = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldspool", entity).split(valuesDelimiter);

                for (String field : entityFieldsPool)
                    poolFields.add(field.trim());
            } else
                poolFields = MetadataSearch.getAllFieldLabels(jsonStr);

            logger.info("Total Fields are {}", poolFields.size());
            if (poolFields.size() > 0) {
                int[] randomNumbersForFields = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (poolFields.size() - 1),
                        Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "MaxNoOfFieldsToTest")));

                for (int randomNumber : randomNumbersForFields)
                    fieldsToTest.put(poolFields.get(randomNumber), null);
            }
        } else {
            String fieldsToTestFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "FieldsToTestFilePath");
            String fieldsToTestFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "FieldsToTestFileName");
            Map<String, String> allFieldsInFile = ParseConfigFile.getAllConstantProperties(fieldsToTestFilePath, fieldsToTestFileName, entity);
            List<String> fieldLabels = new ArrayList<>(allFieldsInFile.keySet());

            for (String label : fieldLabels) {
                if (allFieldsInFile.get(label) == null || allFieldsInFile.get(label).equalsIgnoreCase(""))
                    fieldsToTest.put(label, null);

                else
                    fieldsToTest.put(label, allFieldsInFile.get(label));
            }
        }
        return fieldsToTest;
    }

    private static String getSubPayload(String sectionName, String fieldName, String expectedValue) {
        String subPayload = null;
        String payloadHierarchy = ParseConfigFile.getValueFromConfigFileCaseSensitive(searchPayloadConfigFilePath, searchPayloadConfigFileName, sectionName, fieldName);
        switch (payloadHierarchy.trim().toLowerCase()) {
            case "hierarchy1":
                subPayload = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"values\":{\"name\":\"" + expectedValue + "\"}}";
                break;

            case "default":
                subPayload = "\"" + fieldName + "\":{\"name\":\"" + fieldName + "\",\"values\":\"" + expectedValue + "\"}";
                break;

            default:
                logger.error("Hierarchy for Field {} and Section {} is Incorrect/Not Defined.", fieldName, sectionName);
                break;
        }
        return subPayload;
    }
}