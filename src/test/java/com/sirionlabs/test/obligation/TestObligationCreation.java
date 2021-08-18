package com.sirionlabs.test.obligation;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestObligationCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestObligationCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer obligationEntityTypeId;
    private static Boolean deleteEntity = true;
    private String listingPayloadConfigFilePath;
    private String listingPayloadConfigFileName;
    private String obligations = "obligations";
    private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

        listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");
        listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

    @DataProvider
    public Object[][] dataProviderForTestObligationCreation() throws ConfigurationException {
        logger.info("Setting all Obligation Creation Flows to Validate");
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
                    logger.info("Flow having name [{}] not found in Obligation Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestObligationCreation")
    public void testObligationCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int obligationId = -1;

        String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter name");
        String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter id");
        String uniqueString = DateUtils.getCurrentTimeStamp();
        uniqueString = uniqueString.substring(10);
        uniqueString = uniqueString.replaceAll("_", "");
        uniqueString = uniqueString.replaceAll(" ", "");

        try {
            logger.info("Validating Obligation Creation Flow [{}]", flowToTest);

            //Validate Obligation Creation
            logger.info("Creating Obligation for Flow [{}]", flowToTest);
            String createResponse = "";

            if(filter_name != null) {
                String dynamicField = "dyn" + filter_name;

                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);

                createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                        true);

                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString,"unqString");
                UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString,"unqString");

            }else {

                createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                        true);
            }
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    obligationId = CreateEntity.getNewEntityId(createResponse, "obligations");

                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (obligationId != -1) {

                            int idColId = 56;
                            ListRendererListData listRendererListData = new ListRendererListData();
                            String generalPayload = "{\"filterMap\":{\"entityTypeId\":" + obligationEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":" +
                                    "[{\"columnId\":" + 276 + ",\"columnQueryName\":\"bulkcheckbox\"}," +
                                    "{\"columnId\":" + idColId + ",\"columnQueryName\":\"id\"}]}";
                            int obListId = 4;
                            listRendererListData.checkRecFoundOnListPage(obListId,idColId,obligationId,generalPayload,csAssert);

                            if(filter_name != null) {

                                String min = new BigDecimal(uniqueString).subtract(new BigDecimal("2")).toString();
                                String max = new BigDecimal(uniqueString).add(new BigDecimal("1")).toString();

                                String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath,listingPayloadConfigFileName,obligations,"payload");
                                String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath,listingPayloadConfigFileName,obligations,"columnidstoignore");

                                if(payload == null){
                                    payload = "";
                                }
                                Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(obligationEntityTypeId, filter_id,
                                        filter_name, min, max,payload, csAssert);

                                String entityId = "";
                                try {
                                    entityId = listColumnValuesMap.get("id").split(":;")[1];

                                } catch (Exception e) {

                                }

                                if (!entityId.equalsIgnoreCase(String.valueOf(obligationId))) {
                                    csAssert.assertTrue(false, "On Listing page Obligation entity " + obligationId + " Not Found After Applying Custom Numeric Filter");
                                } else {
                                    logger.info("On Listing page PO entity " + obligationId + "  Found");
                                }
                            }

                            logger.info("Obligation Created Successfully with Id {}: ", obligationId);
                            logger.info("Hitting Show API for Obligation Id {}", obligationId);
                            String showResponse = ShowHelper.getShowResponse(obligationEntityTypeId, obligationId);

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!ShowHelper.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for Obligation Id " + obligationId);
                                }

                                Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
                                String sourceEntity = flowProperties.get("sourceentity");
                                int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
                                int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

                                //Validate Source Reference Tab.
                                tabListDataHelperObj.validateSourceReferenceTab("obligations", 12, obligationId, parentEntityTypeId,
                                        parentRecordId, csAssert);

                                //Validate Forward Reference Tab.
                                tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 12, obligationId, csAssert);

                                if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
                                    //Validate Supplier on Show Page
                                    String expectedSupplierId = flowProperties.get("multiparentsupplierid");
                                    ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 12, obligationId, csAssert);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Obligation Id " + obligationId + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create Obligation for Flow [" + flowToTest + "] due to " + createStatus);
                    }
                } else {
                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        csAssert.assertTrue(false, "Obligation Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Obligation Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && obligationId != -1) {
                logger.info("Deleting Obligation having Id {}", obligationId);
                EntityOperationsHelper.deleteEntityRecord("obligations", obligationId);
            }
            csAssert.assertAll();
        }
    }


    /*
    TC-C63341: Verify Negative Value in Obligation Financial Fields.
     */
    @Test(enabled = true)
    public void testC63341() {
        CustomAssert csAssert = new CustomAssert();
        int obligationId = -1;

        try {
            logger.info("Starting Test TC-C63341: Verify Negative Value in Obligation Financial Fields.");
            String sectionName = "c63341";

            String createResponse = Obligations.createObligation(sectionName, true);

            if (createResponse == null) {
                throw new SkipException("Couldn't Create Obligations using Section [" + sectionName + "]");
            }

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                if (createStatus.equalsIgnoreCase("success"))
                    obligationId = CreateEntity.getNewEntityId(createResponse, "obligations");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (obligationId != -1) {
                        logger.info("Obligation Created Successfully with Id {}: ", obligationId);
                        logger.info("Hitting Show API for Obligation Id {}", obligationId);
                        String showResponse = ShowHelper.getShowResponse(obligationEntityTypeId, obligationId);

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!ShowHelper.isShowPageAccessible(showResponse)) {
                                csAssert.assertTrue(false, "Show Page is Not Accessible for Obligation Id " + obligationId);
                            }

                            //Verify Values on Show Page.
                            logger.info("Verifying Financial Impact Value On Show Page");
                            String financialValue = ShowHelper.getActualValue(showResponse,
                                    ShowHelper.getShowFieldHierarchy("financialimpactcurrencyvalue", 12));

                            if (financialValue == null) {
                                throw new SkipException("Couldn't get Financial Impact Value from Show Response of Obligation Id " + obligationId);
                            }

                            if (!financialValue.startsWith("-")) {
                                csAssert.assertTrue(false, "Financial Impact Value is not Negative on Show Page.");
                            }

                            logger.info("Verifying Credit Impact Value on Show Page");
                            String creditValue = ShowHelper.getActualValue(showResponse,
                                    ShowHelper.getShowFieldHierarchy("creditimpactcurrencyvalue", 12));

                            if (creditValue == null) {
                                throw new SkipException("Couldn't get Credit Impact Value from Show Response of Obligation Id " + obligationId);
                            }

                            if (!creditValue.startsWith("-")) {
                                csAssert.assertTrue(false, "Credit Impact Value is not Negative on Show Page.");
                            }

                        } else {
                            csAssert.assertTrue(false, "Show API Response for Obligation Id " + obligationId + " is an Invalid JSON.");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Couldn't create Obligation due to " + createStatus);
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Obligation is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C63341. " + e.getMessage());
        } finally {
            if (obligationId != -1) {
                EntityOperationsHelper.deleteEntityRecord("obligations", obligationId);
            }
        }
        csAssert.assertAll();
    }
}