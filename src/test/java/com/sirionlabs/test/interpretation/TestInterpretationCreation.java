package com.sirionlabs.test.interpretation;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Interpretation;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
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
public class TestInterpretationCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestInterpretationCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer interpretationEntityTypeId;
    private static Boolean deleteEntity = true;

    private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InterpretationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InterpretationCreationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        interpretationEntityTypeId = ConfigureConstantFields.getEntityIdByName("interpretations");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

    @DataProvider
    public Object[][] dataProviderForTestInterpretationCreation() throws ConfigurationException {
        logger.info("Setting all Interpretation Creation Flows to Validate");
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
                    logger.info("Flow having name [{}] not found in Interpretation Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestInterpretationCreation")
    public void testInterpretationCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int interpretationId = -1;

        try {
            logger.info("Validating Interpretation Creation Flow [{}]", flowToTest);

            //Validate Interpretation Creation
            logger.info("Creating Interpretation for Flow [{}]", flowToTest);
            String createResponse = Interpretation.createInterpretation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                    true);

            if (createResponse == null) {
                throw new SkipException("Couldn't get Create Response for Flow [" + flowToTest + "]");
            }

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    interpretationId = CreateEntity.getNewEntityId(createResponse, "interpretations");

                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (interpretationId != -1) {
                            logger.info("Interpretation Created Successfully with Id {}: ", interpretationId);
                            logger.info("Hitting Show API for Interpretation Id {}", interpretationId);
                            Show showObj = new Show();
                            showObj.hitShow(interpretationEntityTypeId, interpretationId);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!showObj.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for Interpretation Id " + interpretationId);
                                }

								Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
								String sourceEntity = flowProperties.get("sourceentity");
								int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
								int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

								//Validate Source Reference Tab.
								tabListDataHelperObj.validateSourceReferenceTab("interpretations", 16, interpretationId, parentEntityTypeId,
										parentRecordId, csAssert);

								//Validate Forward Reference Tab.
								tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 16, interpretationId,
										csAssert);

                                if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
                                    //Validate Supplier on Show Page
                                    String expectedSupplierId = flowProperties.get("multiparentsupplierid");
                                    ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 16, interpretationId, csAssert);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Interpretation Id " + interpretationId + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create Interpretation for Flow [" + flowToTest + "] due to " + createStatus);
                    }
                } else {
                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        csAssert.assertTrue(false, "Interpretation Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for Interpretation Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Interpretation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && interpretationId != -1) {
                logger.info("Deleting Interpretation having Id {}", interpretationId);
                EntityOperationsHelper.deleteEntityRecord("interpretations", interpretationId);
            }
            csAssert.assertAll();
        }
    }
}