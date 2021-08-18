package com.sirionlabs.test.action;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.CreateEntity;
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
import java.util.regex.Pattern;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestActionCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestActionCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer actionEntityTypeId;
    private static Boolean deleteEntity = true;

    private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ActionCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        actionEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

    @DataProvider
    public Object[][] dataProviderForTestActionCreation() throws ConfigurationException {
        logger.info("Setting all Action Creation Flows to Validate");
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
                    logger.info("Flow having name [{}] not found in Action Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestActionCreation")
    public void testActionCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int actionId = -1;

        try {
            logger.info("Validating Action Creation Flow [{}]", flowToTest);

            //Validate Action Creation
            logger.info("Creating Action for Flow [{}]", flowToTest);
            String createResponse;

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
            if (flowProperties.get("sourceentity").trim().equalsIgnoreCase("contract draft request")) {
                String supplierId = flowProperties.get("supplierid");
                String payload = "{\"body\":{\"data\":{\"supplier\":{\"name\":\"supplier\",\"values\":{\"id\":" + supplierId + "}}," +
                        "\"sourceTitle\":{\"name\":\"sourceTitle\",\"values\":{\"id\":" + flowProperties.get("sourceid") + "}}," +
                        "\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"id\":160}}}}}";

                createResponse = createActionFromCDRResponse(payload, flowToTest);
            } else {
                createResponse = Action.createAction(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
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
                    actionId = CreateEntity.getNewEntityId(createResponse, "actions");

                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (actionId != -1) {
                            logger.info("Action Created Successfully with Id {}: ", actionId);
                            logger.info("Hitting Show API for Action Id {}", actionId);
                            String showResponse = ShowHelper.getShowResponseVersion2(actionEntityTypeId, actionId);

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!ShowHelper.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for Action Id " + actionId);
                                }

                                String sourceEntity = flowProperties.get("sourceentity");
                                int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
                                int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

                                //Validate Source Reference Tab.
                                tabListDataHelperObj.validateSourceReferenceTab("actions", 18, actionId, parentEntityTypeId, parentRecordId, csAssert);

                                //Validate Forward Reference Tab.
                                tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 18, actionId, csAssert);

                                if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
                                    //Validate Supplier on Show Page
                                    String expectedSupplierId = flowProperties.get("multiparentsupplierid");
                                    ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 18, actionId, csAssert);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Action Id " + actionId + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create Action for Flow [" + flowToTest + "] due to " + createStatus);
                    }
                } else {
                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        csAssert.assertTrue(false, "Action Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Action Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Action Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && actionId != -1) {
                logger.info("Deleting Action having Id {}", actionId);
                EntityOperationsHelper.deleteEntityRecord("actions", actionId);
            }
            csAssert.assertAll();
        }
    }

    private String createActionFromCDRResponse(String newPayload, String actionCreateSection) {
        logger.info("Hitting New API for Actions");
        New newObj = new New();
        newObj.hitNew("actions", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                        actionCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("actions");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        extraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Actions");
                    Create createObj = new Create();
                    createObj.hitCreate("actions", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Actions Create Payload is null and hence cannot create Action.");
                }
            } else {
                logger.error("New API Response is an Invalid JSON for Actions.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }
}