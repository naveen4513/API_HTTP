package com.sirionlabs.test.issue;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Issue;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PayloadUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestIssueCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestIssueCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer issueEntityTypeId;
    private static Boolean deleteEntity = true;

    private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("IssueCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("IssueCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        issueEntityTypeId = ConfigureConstantFields.getEntityIdByName("issues");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

    @DataProvider
    public Object[][] dataProviderForTestIssueCreation() throws ConfigurationException {
        logger.info("Setting all Issue Creation Flows to Validate");
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
                    logger.info("Flow having name [{}] not found in Issue Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestIssueCreation")
    public void testIssueCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int issueId = -1;

        try {
            logger.info("Validating Issue Creation Flow [{}]", flowToTest);

            //Validate Issue Creation
            logger.info("Creating Issue for Flow [{}]", flowToTest);
            String createResponse;

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
            if (flowProperties.get("sourceentity").trim().equalsIgnoreCase("contract draft request")) {
                String supplierId = flowProperties.get("supplierid");
                String payload = "{\"body\":{\"data\":{\"supplier\":{\"name\":\"supplier\",\"values\":{\"id\":" + supplierId + "}}," +
                        "\"sourceTitle\":{\"name\":\"sourceTitle\",\"values\":{\"id\":" + flowProperties.get("sourceid") + "}}," +
                        "\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"id\":160}}}}}";

                createResponse = createIssueFromCDRResponse(payload, flowToTest);
            } else {
                createResponse = Issue.createIssue(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                        true);
            }

            if (createResponse == null) {
                throw new SkipException("Couldn't get Create Response for Flow [" + flowToTest + "]");
            }

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    issueId = CreateEntity.getNewEntityId(createResponse, "issues");

                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (issueId != -1) {
                            logger.info("Issue Created Successfully with Id {}: ", issueId);
                            logger.info("Hitting Show API for Issue Id {}", issueId);
                            String showResponse = ShowHelper.getShowResponseVersion2(issueEntityTypeId, issueId);

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!ShowHelper.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for Issue Id " + issueId);
                                } else {
                                    //Verify Source Field. TC-C8248, TC-C8261
                                    String sourceEntityName = flowProperties.get("sourceentity");
                                    CreateEntity.validateSourceFieldForEntity("issues", issueEntityTypeId, sourceEntityName, showResponse, csAssert);

                                    //Verify Source Name Field. TC-C8251, TC-C8269
                                    int sourceEntityId = Integer.parseInt(flowProperties.get("sourceid"));
                                    CreateEntity.validateSourceNameFieldForEntity("issues", issueEntityTypeId, sourceEntityName, sourceEntityId,
                                            showResponse, csAssert);

                                    int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntityName);
                                    String sourceEntityShowResponse = ShowHelper.getShowResponseVersion2(parentEntityTypeId, sourceEntityId);

                                    //Validate Supplier Name
                                    validateSupplierName(flowToTest, sourceEntityShowResponse, parentEntityTypeId, showResponse, csAssert);

                                    //Validate Contract Field
                                    validateContractField(flowToTest, sourceEntityShowResponse, parentEntityTypeId, showResponse, csAssert);

                                    //Validate Source Reference Tab.
                                    tabListDataHelperObj.validateSourceReferenceTab("issues", 17, issueId, parentEntityTypeId, sourceEntityId,
                                            csAssert);

                                    //Validate Forward Reference Tab.
                                    tabListDataHelperObj.validateForwardReferenceTab(sourceEntityName, parentEntityTypeId, sourceEntityId, 17,
                                            issueId, csAssert);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Issue Id " + issueId + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create Issue for Flow [" + flowToTest + "] due to " + createStatus);
                    }
                } else {
                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        csAssert.assertTrue(false, "Issue Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Issue Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Issue Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && issueId != -1) {
                logger.info("Deleting Issue having Id {}", issueId);
                EntityOperationsHelper.deleteEntityRecord("issues", issueId);
            }
            csAssert.assertAll();
        }
    }

    private void validateSupplierName(String flowToTest, String sourceEntityShowResponse, int parentEntityTypeId, String showResponse, CustomAssert csAssert) {
        try {
            List<String> allExpectedSupplierNames = ShowHelper.getAllSupplierNamesFromShowResponse(sourceEntityShowResponse, parentEntityTypeId);

            if (allExpectedSupplierNames == null || allExpectedSupplierNames.isEmpty()) {
                csAssert.assertTrue(false, "Couldn't get Expected Supplier Name for Flow [" + flowToTest + "]");
                return;
            }

            List<String> allSupplierNames = ShowHelper.getAllSupplierNamesFromShowResponse(showResponse, issueEntityTypeId);

            if (allSupplierNames == null || allSupplierNames.isEmpty()) {
                csAssert.assertTrue(false, "Couldn't get All Supplier Names for Flow [" + flowToTest + "]");
                return;
            }

            String actualSupplierName = allSupplierNames.get(0);
            csAssert.assertTrue(allExpectedSupplierNames.contains(actualSupplierName.toLowerCase()), "Actual Supplier Name: " + actualSupplierName +
                    " not found in Show Response for Flow [" + flowToTest + "]");
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Supplier Name for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

    private void validateContractField(String flowToTest, String sourceEntityShowResponse, int parentEntityTypeId, String showResponse, CustomAssert csAssert) {
        try {
            if (parentEntityTypeId != 87) {
                String expectedContractName = ShowHelper.getContractNameFromShowResponse(sourceEntityShowResponse, parentEntityTypeId);

                if (expectedContractName == null) {
                    csAssert.assertTrue(false, "Couldn't get Expected Contract Name for Flow [" + flowToTest + "]");
                    return;
                }

                String actualContractName = ShowHelper.getActualValue(showResponse, ShowHelper.getShowFieldHierarchy("contract", issueEntityTypeId));

                if (actualContractName == null) {
                    if (expectedContractName.equalsIgnoreCase("N/A")) {
                        return;
                    }

                    csAssert.assertTrue(false, "Couldn't get Actual Contract Name for Flow [" + flowToTest + "]");
                    return;
                }

                csAssert.assertTrue(actualContractName.equalsIgnoreCase(expectedContractName), "Expected Contract Name: " + expectedContractName +
                        " and Actual Contract Name: " + actualContractName + " for Flow [" + flowToTest + "]");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Contract Name for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

    /*
    TC-C63346: Verify Negative Value in Issue Financial Fields.
     */
    @Test
    public void testC63346() {
        CustomAssert csAssert = new CustomAssert();
        int issueId = -1;

        try {
            logger.info("Starting Test TC-C63346: Verify Negative Value in Issue Financial Fields.");
            String sectionName = "c63346";

            String createResponse = Issue.createIssue(sectionName, true);

            if (createResponse == null) {
                throw new SkipException("Couldn't Create Issue using Section [" + sectionName + "]");
            }

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                if (createStatus.equalsIgnoreCase("success"))
                    issueId = CreateEntity.getNewEntityId(createResponse, "issues");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (issueId != -1) {
                        logger.info("Issue Created Successfully with Id {}: ", issueId);
                        logger.info("Hitting Show API for Issue Id {}", issueId);
                        String showResponse = ShowHelper.getShowResponse(issueEntityTypeId, issueId);

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!ShowHelper.isShowPageAccessible(showResponse)) {
                                csAssert.assertTrue(false, "Show Page is Not Accessible for Obligation Id " + issueId);
                            }

                            //Verify Values on Show Page.
                            logger.info("Verifying Financial Impact Value On Show Page");
                            String financialValue = ShowHelper.getActualValue(showResponse,
                                    ShowHelper.getShowFieldHierarchy("financialimpact", 17));

                            if (financialValue == null) {
                                throw new SkipException("Couldn't get Financial Impact Value from Show Response of Issue Id " + issueId);
                            }

                            if (!financialValue.startsWith("-")) {
                                csAssert.assertTrue(false, "Financial Impact Value is not Negative on Show Page.");
                            }
                        } else {
                            csAssert.assertTrue(false, "Show API Response for Issue Id " + issueId + " is an Invalid JSON.");
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Couldn't create Issue due to " + createStatus);
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Issue is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C63346. " + e.getMessage());
        } finally {
            if (issueId != -1) {
                EntityOperationsHelper.deleteEntityRecord("issues", issueId);
            }
        }
        csAssert.assertAll();
    }

    private String createIssueFromCDRResponse(String newPayload, String issueCreateSection) {
        logger.info("Hitting New API for Issues");
        New newObj = new New();
        newObj.hitNew("issues", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                        issueCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("issues");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        extraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Issues");
                    Create createObj = new Create();
                    createObj.hitCreate("issues", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Issues Create Payload is null and hence cannot create Issue.");
                }
            } else {
                logger.error("New API Response is an Invalid JSON for Issue.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }
}