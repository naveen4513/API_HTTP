package com.sirionlabs.test.cdr;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestCDRCreation {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRCreation.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Boolean deleteEntity = true;
    private Map<String, String> defaultProperties;
    private TabListData tabListDataObj = new TabListData();
    private Show showObj = new Show();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRCreationTestConfigFileName");
        defaultProperties = ParseConfigFile.getAllProperties(configFilePath, configFileName);

        extraFieldsConfigFilePath = defaultProperties.get("extrafieldsconfigfilepath");
        extraFieldsConfigFileName = defaultProperties.get("extrafieldsconfigfilename");

        String temp = defaultProperties.get("deleteentity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

    @DataProvider
    public Object[][] dataProviderForTestCDRCreation() throws ConfigurationException {
        logger.info("Setting all CDR Creation Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();

        List<String> allSectionNames = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);

        String temp = defaultProperties.get("testallflows");
        if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
            logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
            flowsToTest = allSectionNames;
        } else {
            String[] allFlows = defaultProperties.get("flowstovalidate").split(Pattern.quote(","));
            for (String flow : allFlows) {
                if (allSectionNames.contains(flow.trim())) {
                    flowsToTest.add(flow.trim());
                } else {
                    logger.info("Flow having name [{}] not found in CDR Creation Config File.", flow.trim());
                }
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTestCDRCreation")
    public void testCDRCreation(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        try {
            logger.info("Validating CDR Creation Flow [{}]", flowToTest);

            //Validate CDR Creation
            logger.info("Creating CDR for Flow [{}]", flowToTest);
            String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                    true);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedresult");
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    cdrId = CreateEntity.getNewEntityId(createResponse, "contract draft request");

                if (expectedResult.trim().equalsIgnoreCase("success")) {
                    if (createStatus.equalsIgnoreCase("success")) {
                        if (cdrId != -1) {
                            logger.info("CDR Created Successfully with Id {}: ", cdrId);
                            logger.info("Hitting Show API for CDR Id {}", cdrId);
                            showObj.hitShowVersion2(160, cdrId);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!showObj.isShowPageAccessible(showResponse)) {
                                    csAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdrId);
                                }

                                Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
                                String sourceEntity = flowProperties.get("sourceentity");

                                if (sourceEntity.equalsIgnoreCase("contracts")) {
                                    //Validate Source Field
                                    String sourceTitleValue = ShowHelper.getValueOfField(160, "sourcetitle", showResponse);
                                    String expectedSourceTitleValue = flowProperties.get("sourcename");

                                    if (sourceTitleValue == null) {
                                        csAssert.assertFalse(true, "Couldn't get Source Title Value from CDR Show Page.");
                                    } else {
                                        csAssert.assertEquals(sourceTitleValue, expectedSourceTitleValue, "Expected Source Title: " + expectedSourceTitleValue +
                                                " and Actual Source Title: " + sourceTitleValue);
                                    }

                                    //Validate Related Contracts Tab
                                    logger.info("Hitting Related Contracts Tab List API for CDR Id {}", cdrId);
                                    String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                                            "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
                                    String relatedContractsTabResponse = tabListDataObj.hitTabListData(377, 160, cdrId, payload);

                                    if (ParseJsonResponse.validJsonResponse(relatedContractsTabResponse)) {
                                        jsonObj = new JSONObject(relatedContractsTabResponse).getJSONArray("data").getJSONObject(0);

                                        //Validate Contract Id
                                        String expectedContractId = flowProperties.get("sourceid");
                                        String contractIdColumn = TabListDataHelper.getColumnIdFromColumnName(relatedContractsTabResponse, "contract_id");
                                        String contractIdValue = jsonObj.getJSONObject(contractIdColumn).getString("value");

                                        csAssert.assertTrue(contractIdValue.contains(expectedContractId + ":;61"), "Expected Contract Id " +
                                                expectedContractId + " not found in Related Contracts Tab.");

                                        //Validate Contract Name
                                        String contractNameColumn = TabListDataHelper.getColumnIdFromColumnName(relatedContractsTabResponse, "contract_title");
                                        String contractNameValue = jsonObj.getJSONObject(contractNameColumn).getString("value");

                                        csAssert.assertTrue(contractNameValue.equalsIgnoreCase(expectedSourceTitleValue), "Expected Contract Name " +
                                                expectedSourceTitleValue + " not found in Related Contracts Tab.");

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
                                    } else {
                                        csAssert.assertFalse(true, "TabListData API for Related Contracts Tab of CDR Id " + cdrId + " is an Invalid JSON.");
                                    }
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for CDR Id " + cdrId + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create CDR for Flow [" + flowToTest + "] due to " + createStatus);
                    }
                } else {
                    if (createStatus.trim().equalsIgnoreCase("success")) {
                        csAssert.assertTrue(false, "CDR Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
                    }
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + flowToTest + "] is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating CDR Creation Flow [" + flowToTest + "]. " + e.getMessage());
        } finally {
            if (deleteEntity && cdrId != -1) {
                logger.info("Deleting CDR having Id {}", cdrId);
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
            csAssert.assertAll();
        }
    }
}