package com.sirionlabs.test.contract;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
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


import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestContractCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestContractCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer contractEntityTypeId;
    private static Integer contractListId;
    private static Boolean deleteEntity = true;
    private String listingPayloadConfigFilePath;
    private String listingPayloadConfigFileName;

    private Show showObj = new Show();
    private TabListData tabListDataObj = new TabListData();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");

        listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");
        listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;

        contractListId = 2;
    }

    @DataProvider
    public Object[][] dataProviderForTestContractCreation() throws ConfigurationException {
        logger.info("Setting all Contract Creation Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
            logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
            flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
        } else {
            String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
            for (String flow : allFlows) {
                if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                    flowsToTest.add(flow.trim());
                } else {
                    logger.info("Flow having name [{}] not found in Contract Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestContractCreation")
    public void testContractCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int contractId = -1;
        Boolean recFoundOnListDefPayload = false;
        String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter name");
        String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter id");
        String uniqueString = DateUtils.getCurrentTimeStamp();

        uniqueString = uniqueString.replaceAll("_", "");
        uniqueString = uniqueString.replaceAll(" ", "");
        uniqueString = uniqueString.replaceAll("0", "1");
        uniqueString = uniqueString.substring(10);
        String min = "1";
        String max = "100000";
        try {
            logger.info("Validating Contract Creation Flow [{}]", flowToTest);

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            //Validate Contract Creation
            logger.info("Creating Contract for Flow [{}]", flowToTest);

            String createResponse;
            if(filter_name != null) {
                String dynamicField = "dyn" + filter_name;

                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);

            }
            if (flowProperties.get("sourceentity").trim().equalsIgnoreCase("contract draft request")) {
                String[] parentSupplierIdsArr = flowProperties.get("supplierids").split(",");
                int documentTypeId = ConfigureConstantFields.getEntityIdByName(flowProperties.get("parententitytype"));

                String payload = null;

                if (!flowProperties.containsKey("parentcontractid")) {
                    payload = "{\"documentTypeId\":" + documentTypeId + ",\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) +
                            ",\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + flowProperties.get("sourceid") + "],\"entityTypeId\":160}," +
                            "\"actualParentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}}";
                } else {
                    payload = "{\"documentTypeId\":9,\"parentEntity\":{\"entityIds\":[" + flowProperties.get("parentcontractid") +
                            "],\"entityTypeId\":61},\"sourceEntity\":{\"entityIds\":[" + flowProperties.get("sourceid") + "],\"entityTypeId\":160}," +
                            "\"actualParentEntity\":{\"entityIds\":[" + flowProperties.get("parentcontractid") + "],\"entityTypeId\":61},\"relationIds\":[" +
                            flowProperties.get("supplierids") + "]}";
                }


                createResponse = createContractFromCDRResponse(payload, flowToTest);
            } else {
                if(filter_name != null) {
                    String dynamicField = "dyn" + filter_name;

                    UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
                    UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);

                    createResponse = Contract.createContract(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                            true);

                    UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString,"unqString");
                    UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString,"unqString");

                }else {
                    createResponse = Contract.createContract(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                            true);
                }
            }

            if(filter_name != null) {
                String dynamicField = "dyn" + filter_name;
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString, "unqString");
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString, "unqString");
            }

            if (createResponse == null) {
                csAssert.assertTrue(false, "Create Response is null.");
            } else {
                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                    String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
                    logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                    if (createStatus.equalsIgnoreCase("success"))
                        contractId = CreateEntity.getNewEntityId(createResponse, "contracts");



                    if (expectedResult.trim().equalsIgnoreCase("success")) {
                        if (createStatus.equalsIgnoreCase("success")) {
                            if (contractId != -1) {
                                int idColId = 17;
                                ListRendererListData listRendererListData = new ListRendererListData();
                                String generalPayload = "{\"filterMap\":{\"entityTypeId\":" + contractEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                                        "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":" +
                                        "[{\"columnId\":" + 11751 + ",\"columnQueryName\":\"bulkcheckbox\"}," +
                                        "{\"columnId\":" + idColId + ",\"columnQueryName\":\"id\"}]}";

                                if (filter_name != null) {
                                    min = new BigDecimal(uniqueString).subtract(new BigDecimal("5")).toString();
                                    max = new BigDecimal(uniqueString).add(new BigDecimal("5")).toString();
                                    //String payload = "";
                                    String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "contracts", "payload");
                                    String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "contracts", "columnidstoignore");
                                    payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);

                                    if (flowToTest.equalsIgnoreCase("flow 1")) {
                                        Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(contractEntityTypeId, filter_id,
                                                filter_name, min, max, payload, csAssert);

                                        String entityId = "";
                                        try {
                                            entityId = listColumnValuesMap.get("id").split(":;")[1];

                                        } catch (Exception e) {

                                        }

                                        if (!entityId.equalsIgnoreCase(String.valueOf(contractId))) {
                                            csAssert.assertTrue(false, "On Listing page After Entity Creation Contract entity " + contractId + " Not Found on applying Automation numeric Filter with values Minimum " + min + " Maximum " + max);
                                        } else {
                                            logger.info("On Listing page Contract entity " + contractId + "  Found");
                                        }

                                    }
                                }
                                logger.info("Contract Created Successfully with Id {}: ", contractId);
                                logger.info("Hitting Show API for Contract Id {}", contractId);
                                showObj.hitShowVersion2(contractEntityTypeId, contractId);
                                String showResponse = showObj.getShowJsonStr();

                                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                    if (!showObj.isShowPageAccessible(showResponse)) {
                                        csAssert.assertTrue(false, "Show Page is Not Accessible for Contract Id " + contractId);
                                    }

                                    if (flowProperties.get("sourceentity").trim().equalsIgnoreCase("contract draft request")) {
                                        logger.info("Validating CDR Related Contracts Tab Positive flow");
                                        int parentCDRId = Integer.parseInt(flowProperties.get("sourceid"));
                                        validateCDRRelatedContractTab(parentCDRId, contractId, showResponse, true, csAssert);

                                        EntityOperationsHelper.deleteEntityRecord("contracts", contractId);

                                        if(filter_name != null) {
                                            if (flowToTest.equalsIgnoreCase("flow 1")) {
                                                String payload = "";
                                                Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(contractEntityTypeId, filter_id,
                                                        filter_name, uniqueString, uniqueString, payload, csAssert);

                                                String entityId = "";
                                                try {
                                                    entityId = listColumnValuesMap.get("id").split(":;")[1];

                                                } catch (Exception e) {

                                                }

                                                if (entityId.equalsIgnoreCase(String.valueOf(contractId))) {
                                                    csAssert.assertTrue(false, "On Listing page Contract entity " + contractId + "  Found Since it is deleted it should not be found");
                                                } else {
                                                    logger.info("On Listing page Contract entity " + contractId + "  Not Found Since it is deleted it should not be found");
                                                }
                                            }
                                        }
                                        /*
                                            Disabling negative check of Related Contract tab as the bug is deferred.
                                         */
                                        /*validateCDRRelatedContractTab(parentCDRId, contractId, showResponse, false, csAssert);*/
                                        contractId = -1;
                                    }
                                } else {
                                    csAssert.assertTrue(false, "Show API Response for Contract Id " + contractId + " is an Invalid JSON.");
                                }
                            }
                        } else {
                            csAssert.assertTrue(false, "Couldn't create Contract for Flow [" + flowToTest + "] due to " + createStatus);
                        }
                    } else {
                        if (createStatus.trim().equalsIgnoreCase("success")) {
                            csAssert.assertTrue(false, "Contract Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Create API Response for Contract Creation Flow [" + flowToTest + "] is an Invalid JSON.");
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Contract Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && contractId != -1) {
                logger.info("Deleting Contract having Id {}", contractId);
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
            csAssert.assertAll();
        }
    }

    private String createContractFromCDRResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        extraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

    private void validateCDRRelatedContractTab(int parentCDRId, int contractId, String contractShowResponse, boolean positiveCase, CustomAssert csAssert) {
        try {
            logger.info("Hitting Related Contracts Tab List API for CDR Id {}", parentCDRId);
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String relatedContractsTabResponse = tabListDataObj.hitTabListData(377, 160, parentCDRId, payload);

            if (ParseJsonResponse.validJsonResponse(relatedContractsTabResponse)) {
                JSONObject jsonObj = new JSONObject(relatedContractsTabResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                boolean contractFound = false;

                String contractIdColumn = TabListDataHelper.getColumnIdFromColumnName(relatedContractsTabResponse, "contract_id");

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    //Validate Contract Id
                    String contractIdValue = jsonObj.getJSONObject(contractIdColumn).getString("value");

                    int actualContractId = ListDataHelper.getRecordIdFromValue(contractIdValue);

                    if (contractId == actualContractId) {
                        contractFound = true;

                        //Validate Contract Name
                        String contractNameColumn = TabListDataHelper.getColumnIdFromColumnName(relatedContractsTabResponse, "contract_title");
                        String contractNameValue = jsonObj.getJSONObject(contractNameColumn).getString("value");

                        String expectedContractName = ShowHelper.getValueOfField(61, "name", contractShowResponse);

                        csAssert.assertTrue(contractNameValue.equalsIgnoreCase(expectedContractName), "Expected Contract Name " +
                                expectedContractName + " not found in Related Contracts Tab.");

                        //Validate Type
                        String typeColumn = TabListDataHelper.getColumnIdFromColumnName(relatedContractsTabResponse, "contract_type");
                        String typeValue = jsonObj.getJSONObject(typeColumn).getString("value");

                        csAssert.assertTrue(typeValue.equalsIgnoreCase("Related"),
                                "Expected Contract Type: Related not found in Related Contracts Tab.");

                        //Validate Entity
                        String linkedEntityTypeColumn = TabListDataHelper.getColumnIdFromColumnName(relatedContractsTabResponse,
                                "linkedentitytype");
                        String linkedEntityTypeValue = jsonObj.getJSONObject(linkedEntityTypeColumn).getString("value");

                        csAssert.assertTrue(linkedEntityTypeValue.equalsIgnoreCase("Contracts"),
                                "Expected Linked Entity Type: Contracts not found in Related Contracts Tab.");

                        break;
                    }
                }

                if (positiveCase && !contractFound) {
                    csAssert.assertFalse(true, "Contract having Id " + contractId + " not found in CDR Related Contracts Tab.");
                } else if (!positiveCase && contractFound) {
                    csAssert.assertFalse(true, "Contract having Id " + contractId + " found in CDR Related Contracts Tab even after deleting contract.");
                }
            } else {
                csAssert.assertFalse(true, "TabListData API for Related Contracts Tab of CDR Id " + parentCDRId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating CDR Related Contracts Tab " + e.getMessage());
        }
    }
}
